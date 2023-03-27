package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.commands.Command;
import eiss.cube.json.messages.commands.CommandRequest;
import eiss.cube.json.messages.commands.CommandResponse;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.time.Instant;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/eiss-api/commands/new")
public class NewRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public NewRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    CommandRequest req = gson.fromJson(jsonBody, CommandRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        CommandResponse res = newCommand(req);
                        op.complete(res);
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(gson.toJson(res.result()));
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

    private CommandResponse newCommand(CommandRequest req) {
        CommandResponse rc = new CommandResponse();

        Command c = req.getCommand();
        if (c != null && isValid(c.getDeviceID())) {
            CubeCommand cmd = new CubeCommand();
            cmd.setCubeID(new ObjectId(c.getDeviceID()));

            // put command under cube's group
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("id", cmd.getCubeID()));
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

            CubeCommand new_command = datastore.save(cmd);

            // send to device
            sendIt(new_command);

            rc.setCommand(
                Command.builder()
                    .id(cmd.getId().toString())
                    .deviceID(cmd.getCubeID().toString())
                    .command(cmd.getCommand())

                    .completeCycle(cmd.getCompleteCycle())
                    .dutyCycle(cmd.getDutyCycle())
                    .transition(cmd.getTransition())

                    .startTime(cmd.getStartTime())
                    .endTime(cmd.getEndTime())

                    .sent(cmd.getSent())
                    .created(cmd.getCreated())
                    .received(cmd.getReceived())
                    .status(cmd.getStatus())
                .build()
            );
        }

        return rc;
    }

    private void sendIt(CubeCommand cmd) {
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(eq("id", cmd.getCubeID()));

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
