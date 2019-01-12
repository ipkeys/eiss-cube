package eiss.cube.service.http.process.reports;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeReport;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.query.Query;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/reports/{id}")
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

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id: %s is not valid", id))
                .end();
            return;
        }

        Query<CubeReport> q = datastore.createQuery(CubeReport.class);
        q.criteria("_id").equal(new ObjectId(id));

        vertx.executeBlocking(op -> {
            CubeReport result = q.get();
            if (result != null) {
                op.complete(result);
            } else {
                op.fail(String.format("CubeReport: %s not found", id));
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
