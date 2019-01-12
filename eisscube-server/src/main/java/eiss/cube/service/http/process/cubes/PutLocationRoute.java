package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubePoint;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.query.Query;
import xyz.morphia.query.UpdateOperations;
import xyz.morphia.query.UpdateResults;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/cubes/location/{id}")
public class PutLocationRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PutLocationRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = context.request().getParam("id");
        if (!ObjectId.isValid(id)) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id '%s' is not valid", id))
                .end();
            return;
        }

        ObjectId oid = new ObjectId(id);

        String json = context.getBodyAsString();
        log.info("Update existing EISScube: id={} by: {}", id, json);

        CubePoint location = gson.fromJson(json, CubePoint.class);
        if (location != null) {
            Query<EISScube> query = datastore.createQuery(EISScube.class).field("_id").equal(oid);
            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class);

            ops.set("location", location);

            vertx.executeBlocking(op -> {
                try {

                    UpdateResults result = datastore.update(query, ops);

                    if (result.getUpdatedCount() == 1) {
                        op.complete();
                    } else {
                        op.fail(String.format("Unable to update location of EISScube with id: %s", id));
                    }
                } catch (Exception e) {
                    op.fail(String.format("Unable to update location of  EISScube: %s", e.getMessage()));
                }
            }, res -> {
                if (res.succeeded()) {
                    response
                        .putHeader("content-type", "application/json")
                        .setStatusCode(SC_OK)
                        .end();
                } else {
                    response
                        .setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("Unable to update location of EISScube with id: %s", id))
                .end();
        }
    }

}
