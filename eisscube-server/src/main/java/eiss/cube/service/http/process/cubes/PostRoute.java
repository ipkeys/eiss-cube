package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.DuplicateKeyException;
import eiss.cube.config.AppConfig;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubePoint;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.Key;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/cubes")
public class PostRoute implements Handler<RoutingContext> {

    private AppConfig cfg;
    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PostRoute(AppConfig cfg, Vertx vertx, Datastore datastore, Gson gson) {
        this.cfg = cfg;
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();
        log.debug("Create a new EISScube: {}", json);

        EISScube cube = gson.fromJson(json, EISScube.class);
        if (cube != null) {
            if (cube.getDeviceID().isEmpty()) {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to add EISScube - device ID is missed")
                    .end();
                return;
            }

            vertx.executeBlocking(op -> {
                try {
                    CubePoint location = new CubePoint();
                    location.setLat(40.2769179);
                    location.setLng(-74.0388226);
                    cube.setLocation(location);

                    cube.setLastPing(Instant.now());
                    Key<EISScube> key = datastore.save(cube);
                    cube.setId((ObjectId)key.getId());
                    op.complete(cube);
                } catch (DuplicateKeyException dup) {
                    log.error(dup.getMessage());
                    op.fail("DeviceID already exists");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    op.fail("Unable to add EISScube");
                }
            }, res -> {
                if (res.succeeded()) {
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end(gson.toJson(cube));
                } else {
                    response
                        .setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage("Unable to add EISScube")
                .end();
        }
    }

}
