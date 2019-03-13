package eiss.cube.service.http.process.test;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/test/{cubeID}")
public class PutRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;

    @Inject
    public PutRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String cubeID = request.getParam("cubeID");
        if (!ObjectId.isValid(cubeID)) {
            response.setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id: %s is not valid", cubeID))
                .end();
            return;
        }

        String json = context.getBodyAsString();
        if (json != null) {
            JsonObject body = new JsonObject(json);
            Boolean relay = body.getBoolean("relay");


            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("id").equal(new ObjectId(cubeID));

            vertx.executeBlocking(future -> {
                EISScube cube = q.get();
                future.complete(cube);
            }, res_future -> {
                EISScube cube = (EISScube)res_future.result();
                if (cube != null) {
                    vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("cmd", (relay ? "c=ron" : "c=roff")));
                }
            });
        }

        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(SC_OK)
            .end();
    }

}
