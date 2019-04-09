package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
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

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

@Slf4j
@Api
@Path("/properties")
public class PostRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PostRoute(Vertx vertx, Datastore datastore, Gson gson) {
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
        log.info("Create new CubeProperty: {}", json);

        CubeProperty cmd = gson.fromJson(json, CubeProperty.class);
        if (cmd == null) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage("Unable to create a cube property")
                .end();
            return;
        }

        vertx.executeBlocking(op -> {
            try{
                Key<CubeProperty> key = datastore.save(cmd);
                cmd.setId((ObjectId)key.getId());
                op.complete(cmd);
            } catch (Exception e) {
                log.error(e.getMessage());
                op.fail("Unable to create a cube property");
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(SC_CREATED)
                    .end(gson.toJson(cmd));
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
