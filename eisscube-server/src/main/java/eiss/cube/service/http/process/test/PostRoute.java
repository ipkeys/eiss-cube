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
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.time.Instant;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/test")
public class PostRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;

    @Inject
    public PostRoute(Vertx vertx, Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        JsonObject json = context.getBodyAsJson();
        if (json == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                .end();
            return;
        }

        String cubeID = json.getString("cubeID");
        if (!ObjectId.isValid(cubeID)) {
            response.setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id: %s is not valid", cubeID))
                .end();
            return;
        }

        Integer duration = Optional.of(json.getInteger("duration")).orElse(60);
        Integer cycle = Optional.of(json.getInteger("cycle")).orElse(10) * 2; // with duty cycle = 50%

        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("id").equal(new ObjectId(cubeID));

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube != null) {
                // remove old test's results
                Query<CubeTest> qt = datastore.createQuery(CubeTest.class);
                qt.criteria("cubeID").equal(cube.getId());
                datastore.delete(qt);


                long now = Instant.now().getEpochSecond();
                // do Input Cycle
                vertx.eventBus().send("eisscubetest", new JsonObject()
                    .put("to", cube.getDeviceID())
                    .put("cmd", String.format("c=status&each=5&st=%d&dur=%d&id=test", now, duration))
                );
                // do Relay Cycle after 1 second
                vertx.setTimer(1000, id -> {
                    vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("cmd", String.format("c=rcyc&each=%d&pct=50&st=%d&dur=%d&id=test", cycle, now, duration))
                    );
                });

                future.complete(cube);
            } else {
                future.fail(String.format("No Test results for: %s", cubeID));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(SC_OK)
                    .end();
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
