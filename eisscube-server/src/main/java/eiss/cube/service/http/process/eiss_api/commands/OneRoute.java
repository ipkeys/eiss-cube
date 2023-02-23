package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.commands.Command;
import eiss.cube.json.messages.commands.CommandIdRequest;
import eiss.cube.json.messages.commands.CommandResponse;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/eiss-api/commands/id")
public class OneRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public OneRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
                    CommandIdRequest req = gson.fromJson(jsonBody, CommandIdRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        CommandResponse res = getCommand(req);
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

    private CommandResponse getCommand(CommandIdRequest req) {
        CommandResponse rc = new CommandResponse();

        String id = req.getId();
        if (isValid(id)) {
            Query<CubeCommand> command = datastore.find(CubeCommand.class);
            command.filter(eq("_id", new ObjectId(id)));

            CubeCommand c = command.first();
            if (c != null) {
                rc.setCommand(
                    Command.builder()
                        .id(c.getId().toString())
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
            }
        }

        return rc;
    }


}
