package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.commands.Command;
import eiss.cube.json.messages.commands.CommandListForDeviceRequest;
import eiss.cube.json.messages.commands.CommandListResponse;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/eiss-api/commands/listfordevice")
public class ListForDeviceRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListForDeviceRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
                    CommandListForDeviceRequest req = gson.fromJson(jsonBody, CommandListForDeviceRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        CommandListResponse res = getListOfCommandsForDevice(req);
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
                    response.setStatusCode(SC_INTERNAL_SERVER_ERROR)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                .end();
        }
    }

    private CommandListResponse getListOfCommandsForDevice(CommandListForDeviceRequest req) {
        CommandListResponse rc = new CommandListResponse();

        Query<CubeCommand> commands = datastore.find(CubeCommand.class);
        commands.filter(eq("cubeID", new ObjectId(req.getDeviceID())));

        FindOptions options = new FindOptions();
        Integer s = req.getStart();
        Integer l = req.getLimit();
        if (s != null && l != null) {
            options.skip(s).limit(l);
        }

        commands.iterator(options).toList().forEach(c -> {
            rc.getCommands().add(
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
        );

        // total number of records
        rc.setTotal(commands.count());

        return rc;
    }

}
