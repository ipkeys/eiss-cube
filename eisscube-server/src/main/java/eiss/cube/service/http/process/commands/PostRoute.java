package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import dev.morphia.UpdateOptions;
import dev.morphia.query.UpdateOperations;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.cube.json.messages.CycleAndDutyCycleExtractor;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.EISScube;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/commands")
public class PostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PostRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();
        log.info("Create new CubeCommand: {}", json);

        CubeCommand cmd = gson.fromJson(json, CubeCommand.class);
        if (cmd == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to create a cube command")
                    .end();
            return;
        }

        CycleAndDutyCycleExtractor cdc = gson.fromJson(json, CycleAndDutyCycleExtractor.class);

        if (cdc.getCycleAndDutyCycle() != null && !cdc.getCycleAndDutyCycle().isEmpty()) {
            String[] a = cdc.getCycleAndDutyCycle().split("/");
            if (a.length == 2) {
                cmd.setCompleteCycle(Integer.valueOf(a[0]));
                cmd.setDutyCycle(Integer.valueOf(a[1]));
            }
        }

        vertx.executeBlocking(op -> {
            cmd.setStatus("Created");
            cmd.setCreated(Instant.now());

            // EISS cube
            if (cmd.getDeviceType().equalsIgnoreCase("e")) {
                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.criteria("id").equal(cmd.getCubeID());
                EISScube cube = q.first();
                if (cube != null) {
                    cmd.setCubeName(cube.getName());
                    cmd.setGroup(cube.getGroup());
                    cmd.setGroup_id(cube.getGroup_id());
                }
            }

            // LORA cube
            if (cmd.getDeviceType().equalsIgnoreCase("l")) {
                Query<LORAcube> q = datastore.createQuery(LORAcube.class);
                q.criteria("id").equal(cmd.getCubeID());
                LORAcube cube = q.first();
                if (cube != null) {
                    cmd.setCubeName(cube.getName());
                    cmd.setGroup(cube.getGroup());
                    cmd.setGroup_id(cube.getGroup_id());
                }
            }

            try {
                Key<CubeCommand> key = datastore.save(cmd);
                cmd.setId((ObjectId)key.getId());
                op.complete(cmd);
            } catch (Exception e) {
                log.error(e.getMessage());
                op.fail("Unable to create a cube command");
            }
        }, res -> {
            if (res.succeeded()) {

                // EISS cube
                if (cmd.getDeviceType().equalsIgnoreCase("e")) {
                    sendToEISScube(cmd);
                }
                // LORA cube
                if (cmd.getDeviceType().equalsIgnoreCase("l")) {
                    sendToLORAcube(cmd);
                }
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end(gson.toJson(cmd));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

    private void sendToEISScube(CubeCommand cmd) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("id").equal(cmd.getCubeID());

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube != null) {
                // send command to device
                vertx.eventBus().send("eisscube",
                    new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", cube.getDeviceID())
                        .put("socket", cube.getSocket())
                        .put("cmd", cmd.toString())
                );

                // prepare report record
                if (cmd.getCommand().startsWith("ic")) {
                    Query<CubeReport> qR = datastore.createQuery(CubeReport.class);
                    qR.criteria("cubeID").equal(cube.getId());

                    UpdateOperations<CubeReport> ops = datastore.createUpdateOperations(CubeReport.class);
                    ops.setOnInsert("cubeID", cube.getId());
                    ops.set("type", cmd.getCommand().replace("ic", "")); // leave just "p" or "c"

                    datastore.update(qR, ops, new UpdateOptions().upsert(true));
                }

                op.complete();
            } else {
                op.fail(String.format("Cannot find EISSCube for id: %s", cmd.getCubeID().toString()));
            }
        }, cube_res -> {
            if (cube_res.succeeded()) {
                log.info("Command sent");
            } else {
                log.error("Failed to send Command");
            }
        });
    }

    private void sendToLORAcube(CubeCommand cmd) {
        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("id").equal(cmd.getCubeID());

        vertx.executeBlocking(op -> {
            LORAcube cube = q.first();
            if (cube != null) {
                // send command to device
                vertx.eventBus().send("loracube",
                        new JsonObject()
                                .put("id", cmd.getId().toString())
                                .put("to", cube.getDeviceID())
                                .put("cmd", cmd.toString())
                );

/*
                // prepare report record
                if (cmd.getCommand().startsWith("ic")) {
                    Query<CubeReport> qR = datastore.createQuery(CubeReport.class);
                    qR.criteria("cubeID").equal(cube.getId());

                    UpdateOperations<CubeReport> ops = datastore.createUpdateOperations(CubeReport.class);
                    ops.setOnInsert("cubeID", cube.getId());
                    ops.set("type", cmd.getCommand().replace("ic", "")); // leave just "p" or "c"

                    datastore.update(qR, ops, new UpdateOptions().upsert(true));
                }
*/

                op.complete();
            } else {
                op.fail(String.format("Cannot find LORACube for id: %s", cmd.getCubeID().toString()));
            }
        }, cube_res -> {
            if (cube_res.succeeded()) {
                log.info("Command sent");
            } else {
                log.error("Failed to send Command");
            }
        });
    }

}
