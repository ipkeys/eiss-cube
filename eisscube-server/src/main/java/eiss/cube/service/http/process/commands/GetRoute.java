package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/commands/{id}")
public class GetRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public GetRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = context.request().getParam("id");
        if (!ObjectId.isValid(id)) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id: %s is not valid", id))
                .end();
            return;
        }

        Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
        q.criteria("_id").equal(new ObjectId(id));

        // projections

        vertx.executeBlocking(op -> {
            CubeCommand result = q.get();
            if (result != null) {
                op.complete(result);
            } else {
                op.fail(String.format("Command id: %s not found", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader("content-type", "application/json")
                    .setStatusCode(SC_OK)
                    .end(gson.toJson(res.result()));
            } else {
                response
                    .setStatusCode(SC_NOT_FOUND)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
