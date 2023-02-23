package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/properties/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PutRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
        if (!isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id '%s' is not valid", id))
                    .end();
            return;
        }

        String json = context.body().asString();
        log.info("Update existing Property: {} by: {}", id, json);
        CubeProperty property = gson.fromJson(json, CubeProperty.class);
        if (property == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("Unable to update Property with id: %s", id))
                    .end();
            return;
        }

        Query<CubeProperty> q = datastore.find(CubeProperty.class);
        q.filter(eq("_id", new ObjectId(id)));

        List<UpdateOperator> updates = new ArrayList<>();
        if (property.getType() == null) {
            updates.add(unset("type"));
        } else {
            updates.add(set("type", property.getType()));
        }

        if (property.getName() == null) {
            updates.add(unset("name"));
        } else {
            updates.add(set("name", property.getName()));
        }

        if (property.getLabel() == null) {
            updates.add(unset("label"));
        } else {
            updates.add(set("label", property.getLabel()));
        }

        if (property.getDescription() == null) {
            updates.add(unset("description"));
        } else {
            updates.add(set("description", property.getDescription()));
        }

        vertx.executeBlocking(op -> {
            UpdateResult result = q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));
            if (result.getModifiedCount() == 1) {
                op.complete(property);
            } else {
                op.fail(String.format("Unable to update Property with id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
