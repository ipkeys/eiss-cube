package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.query.Query;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.commands.*;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/commands/send")
public class MultipleSendRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public MultipleSendRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    MultipleCommandsSendRequest req = gson.fromJson(jsonBody, MultipleCommandsSendRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        MultipleCommandsSendResponse res = sendCommands(req);
                        op.complete(gson.toJson(res));
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end((String)res.result());
                } else {
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private MultipleCommandsSendResponse sendCommands(MultipleCommandsSendRequest req) {
        MultipleCommandsSendResponse rc = new MultipleCommandsSendResponse();

        List<Command> commandList = req.getCommands();
        commandList.forEach(c -> {
            if (c != null && ObjectId.isValid(c.getDeviceID())) {
                CubeCommand cmd = new CubeCommand();
                cmd.setCubeID(new ObjectId(c.getDeviceID()));

                // put command under cube's group
                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.criteria("id").equal(cmd.getCubeID());
                EISScube cube = q.first();
                if (cube != null) {
                    cmd.setCubeName(cube.getName());
                    cmd.setGroup(cube.getGroup());
                    cmd.setGroup_id(cube.getGroup_id());
                }
                // ~put command under cube's group

                cmd.setCommand(c.getCommand());

                cmd.setCompleteCycle(c.getCompleteCycle());
                cmd.setDutyCycle(c.getDutyCycle());
                cmd.setTransition(c.getTransition());

                cmd.setStartTime(c.getStartTime());
                cmd.setEndTime(c.getEndTime());

                cmd.setStatus("Created");
                cmd.setCreated(Instant.now());

                Key<CubeCommand> key = datastore.save(cmd);
                cmd.setId((ObjectId)key.getId());

                // send to device
                sendIt(cmd);

            }
        });

        rc.setStatus("Success"); // or Failed

        return rc;
    }

    private void sendIt(CubeCommand cmd) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("id").equal(cmd.getCubeID());

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube != null) {
                vertx.eventBus().send("eisscube",
                    new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", cube.getDeviceID())
                        .put("socket", cube.getSocket())
                        .put("cmd", cmd.toString())
                );
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
