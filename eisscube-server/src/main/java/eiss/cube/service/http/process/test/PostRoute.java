package eiss.cube.service.http.process.test;

import dev.morphia.DeleteOptions;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/test")
public class PostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;

    @Inject
    public PostRoute(Vertx vertx, @Cubes Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        JsonObject json = context.body().asJsonObject();
        if (json == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
            return;
        }

        String cubeID = json.getString("cubeID");
        if (!isValid(cubeID)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id: %s is not valid", cubeID))
                    .end();
            return;
        }

        Integer duration = Optional.of(json.getInteger("duration")).orElse(60);
        Integer cycle = Optional.of(json.getInteger("cycle")).orElse(10) * 2; // with duty cycle = 50%
        String deviceType = json.getString("deviceType");

        vertx.executeBlocking(op -> {

            doEISScubeTest(cubeID, duration, cycle, op);

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
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(eq("id", new ObjectId(cubeID)));

        EISScube cube = q.first();
        if (cube != null && cube.getOnline()) {
            // remove old test's results
            Query<CubeTest> qt = datastore.find(CubeTest.class);
            qt.filter(eq("cubeID", cube.getId()));
            qt.delete(new DeleteOptions().multi(TRUE));

            String busAddress = cube.getDeviceType().equalsIgnoreCase("e")
                    ? "eisscubetest"
                    : "loracubetest";
            String testCmd = cube.getDeviceType().equalsIgnoreCase("e")
                    ? "c=status&each=5&st=%d&dur=%d&id=test"
                    : "c=test&each=5&st=%d&dur=%d&id=test";

            long startTime = Instant.now().plus(3, ChronoUnit.SECONDS).getEpochSecond();

            // do Input Cycle
            vertx.eventBus().send(busAddress, new JsonObject()
                .put("to", cube.getDeviceID())
                .put("socket", cube.getSocket())
                .put("cmd", String.format(testCmd, startTime, duration))
            );

            vertx.setTimer(1000, id -> {
                // do Relay Cycle
                vertx.eventBus().send(busAddress, new JsonObject()
                    .put("to", cube.getDeviceID())
                    .put("socket", cube.getSocket())
                    .put("cmd", String.format("c=rcyc&each=%d&pct=50&st=%d&dur=%d&id=test", cycle, startTime, duration))
                );
            });

            op.complete();
        } else {
            op.fail(String.format("Cannot start Test for: %s", cubeID));
        }
    }

}
