package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.commands.Command;
import eiss.cube.json.messages.commands.CommandIdRequest;
import eiss.cube.json.messages.commands.CommandResponse;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyIdRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeProperty;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/commands/delete")
public class DeleteRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public DeleteRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
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
                    CommandIdRequest req = gson.fromJson(jsonBody, CommandIdRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        CommandResponse res = deleteCommand(req);
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

    private CommandResponse deleteCommand(CommandIdRequest req) {
        CommandResponse rc = new CommandResponse();

        String id = req.getId();
        if (ObjectId.isValid(id)) {
            Query<CubeCommand> command = datastore.createQuery(CubeCommand.class);

            // filter
            command.criteria("_id").equal(new ObjectId(id));

            // projections

            // get
            CubeCommand c = command.first();
            if (c != null) {
                rc.setCommand(
                    Command.builder()
                        //.id(c.getId().toString()) // id is not valid after delete
                        .deviceID(c.getCubeID().toString())
                        .command(c.getCommand())
                        .completeCycle(c.getCompleteCycle())
                        .dutyCycle(c.getDutyCycle())
                        .transition(c.getTransition())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .sent(c.getSent())
                        .created(c.getCreated())
                        .received(c.getReceived())
                        .status(c.getStatus())
                    .build()
                );

                datastore.delete(command);
            }
        }

        return rc;
    }


}
