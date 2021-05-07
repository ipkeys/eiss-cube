package cube.service.http.process.commands;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import cube.db.Cube;
import dev.morphia.UpdateOptions;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import cube.json.messages.CycleAndDutyCycleExtractor;
import cube.models.CubeCommand;
import cube.models.CubeReport;
import cube.models.EISScube;
import cube.models.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
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
                Query<EISScube> q = datastore.find(EISScube.class);
                q.filter(Filters.eq("id", cmd.getCubeID()));
                EISScube cube = q.first();
                if (cube != null) {
                    cmd.setCubeName(cube.getName());
                    cmd.setGroup(cube.getGroup());
                    cmd.setGroup_id(cube.getGroup_id());
                }
            }

            // LORA cube
            if (cmd.getDeviceType().equalsIgnoreCase("l")) {
                Query<LORAcube> q = datastore.find(LORAcube.class);
                q.filter(Filters.eq("id", cmd.getCubeID()));
                LORAcube lora = q.first();
                if (lora != null) {
                    cmd.setCubeName(lora.getName());
                    cmd.setGroup(lora.getGroup());
                    cmd.setGroup_id(lora.getGroup_id());
                }
            }

            try {
                CubeCommand new_cmd = datastore.save(cmd);
                op.complete(new_cmd);
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
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(Filters.eq("id", cmd.getCubeID()));

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
                    String type = cmd.getCommand().replace("ic", ""); // leave just "p" or "c"
                    Query<CubeReport> qR = datastore.find(CubeReport.class);
                    qR.filter(Filters.eq("cubeID", cube.getId()));
                    qR.filter(Filters.eq("type", type));

                    Map<String, Object> values = new HashMap<>();
                    values.put("type", type);
                    values.put("deviceType", "e");
                    values.put("cubeID", cube.getId());

                    UpdateOperator op1 = UpdateOperators.setOnInsert(values);
                    UpdateOperator op2 = UpdateOperators.set("group_id", cube.getGroup_id());
                    UpdateOperator op3 = UpdateOperators.set("group", cube.getGroup());
                    UpdateOperator op4 = UpdateOperators.set("cubeName", cube.getName());

                    qR.update(op1, op2, op3, op4).execute(new UpdateOptions().upsert(TRUE));
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
        Query<LORAcube> q = datastore.find(LORAcube.class);
        q.filter(Filters.eq("id", cmd.getCubeID()));

        vertx.executeBlocking(op -> {
            LORAcube cube = q.first();
            if (cube != null) {
                // send command to device - will work from DB, not a command string!!!
                vertx.eventBus().send("loracube",
                    new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", cube.getDeviceID())
                        .put("cmd", cmd.toString())
                );

                // prepare report record
                if (cmd.getCommand().startsWith("ic")) {
                    String type = cmd.getCommand().replace("ic", ""); // leave just "p" or "c"
                    Query<CubeReport> qR = datastore.find(CubeReport.class);
                    qR.filter(Filters.eq("cubeID", cube.getId()));
                    qR.filter(Filters.eq("type", type));

                    List<UpdateOperator> updates = new ArrayList<>();
                    updates.add(UpdateOperators.set("group_id", cube.getGroup_id()));
                    updates.add(UpdateOperators.set("group", cube.getGroup()));
                    updates.add(UpdateOperators.set("cubeName", cube.getName()));

                    Map<String, Object> values = new HashMap<>();
                    values.put("type", type);
                    values.put("deviceType", "l");
                    values.put("cubeID", cube.getId());
                    updates.add(UpdateOperators.setOnInsert(values));

                    if (type.equalsIgnoreCase("c")) { // for cycle counting - keep edge for report
                        updates.add(UpdateOperators.set("edge", cmd.getTransition()));
                    }

                    qR.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute(new UpdateOptions().upsert(TRUE));
                }
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
