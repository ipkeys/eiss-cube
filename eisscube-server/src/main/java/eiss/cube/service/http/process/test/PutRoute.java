package eiss.cube.service.http.process.test;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import xyz.morphia.Datastore;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/test/{deviceID}")
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

        String deviceID = request.getParam("deviceID");
        if (deviceID != null) {
            String json = context.getBodyAsString();
            if (json != null) {
                JsonObject body = new JsonObject(json);
                Boolean relay1 = body.getBoolean("relay1");
                if (relay1 != null) {
                    vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", deviceID)
                        .put("cmd", (relay1 ? "c=ron&t=1" : "c=roff&t=1"))
                    );
                }

                Boolean relay2 = body.getBoolean("relay2");
                if (relay2 != null) {
                    vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", deviceID)
                        .put("cmd", (relay2 ? "c=ron&t=2" : "c=roff&t=2"))
                    );
                }
            }
        }

        response
            .putHeader("content-type", "application/json")
            .setStatusCode(SC_OK)
            .end();
    }

}
