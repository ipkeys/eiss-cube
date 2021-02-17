package eiss.cube.service.http.process.lora;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubePoint;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/lora/location/{id}")
public class PutLocationRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PutLocationRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String id = context.request().getParam("id");
        if (!ObjectId.isValid(id)) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id '%s' is not valid", id))
                .end();
            return;
        }

        String json = context.getBodyAsString();
        log.info("Update existing LORAcube: id={} by: {}", id, json);

        CubePoint location = gson.fromJson(json, CubePoint.class);
        if (location == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Location is not valid")
                    .end();
            return;
        }

        vertx.executeBlocking(op -> {
            Query<LORAcube> q = datastore.createQuery(LORAcube.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<LORAcube> ops = datastore.createUpdateOperations(LORAcube.class);
            ops.set("location", location);

            UpdateResults result = datastore.update(q, ops);

            if (result.getUpdatedCount() == 1) {
                op.complete();
            } else {
                op.fail(String.format("Unable to update location of LORAcube with id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end();
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
