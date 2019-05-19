package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import eiss.cube.config.AppConfig;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/properties/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PutRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id '%s' is not valid", id))
                    .end();
            return;
        }

        String json = context.getBodyAsString();
        log.info("Update existing Property: {} by: {}", id, json);
        CubeProperty property = gson.fromJson(json, CubeProperty.class);
        if (property == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("Unable to update Property with id: %s", id))
                    .end();
            return;
        }

        Query<CubeProperty> q = datastore.createQuery(CubeProperty.class);
        q.criteria("_id").equal(new ObjectId(id));

        UpdateOperations<CubeProperty> ops = datastore.createUpdateOperations(CubeProperty.class);

        if (property.getName() == null) {
            ops.unset("name");
        } else {
            ops.set("name", property.getName());
        }

        if (property.getLabel() == null) {
            ops.unset("label");
        } else {
            ops.set("label", property.getLabel());
        }

        if (property.getDescription() == null) {
            ops.unset("description");
        } else {
            ops.set("description", property.getDescription());
        }

        vertx.executeBlocking(op -> {
            UpdateResults result = datastore.update(q, ops);
            if (result.getUpdatedCount() == 1) {
                op.complete(gson.toJson(property));
            } else {
                op.fail(String.format("Unable to update Property with id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
