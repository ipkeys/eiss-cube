package eiss.cube.service.http.process.test;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeTest;
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
import xyz.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.FALSE;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/test/{cubeID}")
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

        String cubeID = request.getParam("cubeID");
        if (cubeID != null && ObjectId.isValid(cubeID)) {
            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("id").equal(new ObjectId(cubeID));

            vertx.executeBlocking(future -> {
                EISScube cube = q.get();
                future.complete(cube);
            }, res_future -> {
                EISScube cube = (EISScube) res_future.result();
                if (cube != null) {
                    vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("cmd", "c=status")
                    );
                }
            });
        }

        Query<CubeTest> q = datastore.createQuery(CubeTest.class);
        q.criteria("cubeID").equal(cubeID);

        // projections
        q.project("_id", FALSE);
        q.project("cubeID", FALSE);

        vertx.executeBlocking(op -> {
            CubeTest result = q.get();
            if (result != null) {
                op.complete(result);
            } else {
                op.fail(String.format("No Test results for: %s", cubeID));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(SC_OK)
                    .end(gson.toJson(res.result()));
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }

        });
    }

}
