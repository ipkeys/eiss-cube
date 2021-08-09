package cube.service.http.process.properties;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import cube.db.Cube;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import cube.models.CubeProperty;
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

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/properties/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PutRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
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

        Query<CubeProperty> q = datastore.find(CubeProperty.class);
        q.filter(Filters.eq("_id", new ObjectId(id)));

        List<UpdateOperator> updates = new ArrayList<>();
        if (property.getName() == null) {
            updates.add(UpdateOperators.unset("name"));
        } else {
            updates.add(UpdateOperators.set("name", property.getName()));
        }

        if (property.getLabel() == null) {
            updates.add(UpdateOperators.unset("label"));
        } else {
            updates.add(UpdateOperators.set("label", property.getLabel()));
        }

        if (property.getDescription() == null) {
            updates.add(UpdateOperators.unset("description"));
        } else {
            updates.add(UpdateOperators.set("description", property.getDescription()));
        }

        vertx.executeBlocking(op -> {
            UpdateResult result = q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();
            if (result.getModifiedCount() == 1) {
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
