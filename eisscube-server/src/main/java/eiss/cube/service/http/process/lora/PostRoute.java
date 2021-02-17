package eiss.cube.service.http.process.lora;

import com.google.gson.Gson;
import com.mongodb.DuplicateKeyException;
import dev.morphia.Datastore;
import dev.morphia.Key;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubePoint;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

@Slf4j
@Api
@Path("/lora")
public class PostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PostRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();
        log.debug("Create a new LORAcube: {}", json);

        LORAcube cube = gson.fromJson(json, LORAcube.class);
        if (cube != null) {
            if (cube.getDeviceID().isEmpty()) {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage("Unable to add LORAcube - device ID is missed")
                        .end();
                return;
            }

            vertx.executeBlocking(cube_op -> {
                try {
                    CubePoint location = new CubePoint();
                    location.setLat(40.2769179);
                    location.setLng(-74.0388226);
                    cube.setLocation(location);

                    cube.setLastPing(Instant.now());
                    Key<LORAcube> key = datastore.save(cube);
                    cube.setId((ObjectId)key.getId());
                    cube_op.complete(gson.toJson(cube));
                } catch (DuplicateKeyException dup) {
                    log.error(dup.getMessage());
                    cube_op.fail("DeviceID already exists");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    cube_op.fail("Unable to add LORAcube");
                }
            }, cube_res -> {
                if (cube_res.succeeded()) {
                    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .setStatusCode(SC_CREATED)
                            .end(String.valueOf(cube_res.result()));
                } else {
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(cube_res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to add LORAcube")
                    .end();
        }
    }

}
