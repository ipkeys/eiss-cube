package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.models.cubes.EISScube;
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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/properties/{id}")
public class DeleteRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public DeleteRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @DELETE
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id: %s is not valid", id))
                    .end();
            return;
        }

        Query<CubeProperty> q = datastore.find(CubeProperty.class);
        q.filter(eq("_id", new ObjectId(id)));

        vertx.executeBlocking(op -> {
            // react-admin expect previous data
            CubeProperty property = q.findAndDelete();

            if (property != null) {
                // remove the property from all EISSCube records
                Query<EISScube> qc = datastore.find(EISScube.class);
                qc.update(new UpdateOptions(), unset("settings." + property.getName()));

                op.complete(property);
            } else {
                op.fail(String.format("Cannot delete Property id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_NOT_FOUND)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
