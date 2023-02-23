package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.cube.json.messages.CycleAndDutyCycleExtractor;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
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
import java.util.HashMap;
import java.util.Map;

import static dev.morphia.query.filters.Filters.eq;
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
    public PostRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.body().asString();
        log.info("Create new CubeCommand: {}", json);

        CubeCommand cmd = gson.fromJson(json, CubeCommand.class);
        if (cmd == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to create a cube command")
                    .end();
            return;
        }

/*
        CycleAndDutyCycleExtractor cdc = gson.fromJson(json, CycleAndDutyCycleExtractor.class);

        if (cdc.getCycleAndDutyCycle() != null && !cdc.getCycleAndDutyCycle().isEmpty()) {
            String[] a = cdc.getCycleAndDutyCycle().split("/");
            if (a.length == 2) {
                cmd.setCompleteCycle(Integer.valueOf(a[0]));
                cmd.setDutyCycle(Integer.valueOf(a[1]));
            }
        }
*/
        vertx.executeBlocking(op -> {
            cmd.setStatus("Created");
            cmd.setCreated(Instant.now());

            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("id", cmd.getCubeID()));
            EISScube cube = q.first();
            if (cube != null) {
                cmd.setCubeName(cube.getName());
                cmd.setGroup(cube.getGroup());
                cmd.setGroup_id(cube.getGroup_id());
                cmd.setDeviceType(cube.getDeviceType());
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

                sendToEISScube(cmd);

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
        q.filter(eq("id", cmd.getCubeID()));

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube != null) {
                String busAddress = cmd.getDeviceType().equalsIgnoreCase("e") ? "eisscube" : "loracube";

                // send command to device
                vertx.eventBus().send(busAddress,
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
                    qR.filter(eq("cubeID", cube.getId()));
                    qR.filter(eq("type", type));

                    Map<String, Object> values = new HashMap<>();
                    values.put("type", type);
                    values.put("deviceType", cmd.getDeviceType());
                    values.put("cubeID", cube.getId());

                    UpdateOperator op1 = UpdateOperators.setOnInsert(values);
                    UpdateOperator op2 = UpdateOperators.set("group_id", cube.getGroup_id());
                    UpdateOperator op3 = UpdateOperators.set("group", cube.getGroup());
                    UpdateOperator op4 = UpdateOperators.set("cubeName", cube.getName());

                    qR.update(new UpdateOptions().upsert(TRUE), op1, op2, op3, op4);
                }

                op.complete();
            } else {
                op.fail(String.format("Cannot find EISSCube for id: %s", cmd.getCubeID().toString()));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("Command sent");
            } else {
                log.error("Failed to send Command");
            }
        });
    }

}
