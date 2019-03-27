package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/properties/{id}")
public class DeleteRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public DeleteRoute(Vertx vertx, Datastore datastore, Gson gson) {
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
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id: %s is not valid", id))
                .end();
            return;
        }

        Query<CubeProperty> q = datastore.createQuery(CubeProperty.class);
        q.criteria("_id").equal(new ObjectId(id));

        vertx.executeBlocking(op -> {
            // react-admin expect previous data
            CubeProperty property = q.get();

            if (property != null) {
                // remove the property from all EISSCube records
                Query<EISScube> qc = datastore.createQuery(EISScube.class);
                UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class);
                ops.unset("settings." + property.getName());
                datastore.update(qc, ops);

                // delete Property
                datastore.delete(q);

                op.complete(property);
            } else {
                op.fail(String.format("Cannot delete Property id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
