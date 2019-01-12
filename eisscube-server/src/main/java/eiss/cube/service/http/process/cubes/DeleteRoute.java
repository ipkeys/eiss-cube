package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.WriteResult;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/cubes/{id}")
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
            response.setStatusCode(SC_BAD_REQUEST);
            response.setStatusMessage(String.format("id: %s is not valid", id));
            response.end();
            return;
        }

        ObjectId oid = new ObjectId(id);

        vertx.executeBlocking(op -> {
            WriteResult result = datastore.delete(EISScube.class, oid);
            if (result.wasAcknowledged() && result.getN() > 0) {
                op.complete(new JsonObject().put("id", id));
            } else {
                op.fail(String.format("Cannot delete EISScube id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader("content-type", "application/json");
                response.setStatusCode(SC_OK);
                response.end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_NOT_FOUND);
                response.setStatusMessage(res.cause().getMessage());
                response.end();
            }
        });
    }

}
