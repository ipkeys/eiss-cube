package eiss.cube.service.http.process.test;

import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
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
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/test")
public class PostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;

    @Inject
    public PostRoute(Vertx vertx, @Cube Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
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
        String deviceType = json.getString("deviceType");

        vertx.executeBlocking(op -> {
            if (deviceType.equalsIgnoreCase("c")) {
                doEISScubeTest(cubeID, duration, cycle, op);
            }

            if (deviceType.equalsIgnoreCase("l")) {
                doLORAcubeTest(cubeID, duration, cycle, op);
            }

        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end();
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

    private void doEISScubeTest(String cubeID, Integer duration, Integer cycle, Promise<Object> op) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("id").equal(new ObjectId(cubeID));

        EISScube cube = q.first();
        if (cube != null) {
            // remove old test's results
            Query<CubeTest> qt = datastore.createQuery(CubeTest.class);
            qt.criteria("cubeID").equal(cube.getId());
            datastore.delete(qt);

            long now = Instant.now().getEpochSecond();
            // do Input Cycle
            vertx.eventBus().send("eisscubetest", new JsonObject()
                    .put("to", cube.getDeviceID())
                    .put("socket", cube.getSocket())
                    .put("cmd", String.format("c=status&each=5&st=%d&dur=%d&id=test", now, duration))
            );
            // do Relay Cycle after 1 second
            vertx.setTimer(1000, id -> {
                vertx.eventBus().send("eisscubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("socket", cube.getSocket())
                        .put("cmd", String.format("c=rcyc&each=%d&pct=50&st=%d&dur=%d&id=test", cycle, now, duration))
                );
            });

            op.complete();
        } else {
            op.fail(String.format("Cannot start Test for: %s", cubeID));
        }
    }

    private void doLORAcubeTest(String cubeID, Integer duration, Integer cycle, Promise<Object> op) {
        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("id").equal(new ObjectId(cubeID));
        int numberOfRun = duration / (cycle/2);

        LORAcube cube = q.first();
        if (cube != null) {
            // remove old test's results
            Query<CubeTest> qt = datastore.createQuery(CubeTest.class);
            qt.criteria("cubeID").equal(cube.getId());
            datastore.delete(qt);

            vertx.eventBus().send("loracubetest", new JsonObject()
                .put("to", cube.getDeviceID())
                .put("cmd", "c=ron&id=test")
            );
            // do Relay Cycle each 1 minute
            AtomicInteger counter = new AtomicInteger();
            vertx.setPeriodic(60000, id -> {
                if (counter.getAndIncrement() % 2 == 0) {
                    vertx.eventBus().send("loracubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("cmd", "c=roff&id=test")
                    );
                } else {
                    vertx.eventBus().send("loracubetest", new JsonObject()
                        .put("to", cube.getDeviceID())
                        .put("cmd", "c=ron&id=test")
                    );
                }

                if (counter.get() == numberOfRun) {
                    vertx.cancelTimer(id);
                }
            });

            op.complete();
        } else {
            op.fail(String.format("Cannot start Test for: %s", cubeID));
        }
    }

}
