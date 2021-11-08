package eiss.cube.service.http.process.properties;

import com.google.gson.Gson;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;

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

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public PostRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.getBodyAsString();
        log.info("Create new CubeProperty: {}", json);

        CubeProperty property = gson.fromJson(json, CubeProperty.class);
        if (property == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to create a cube property")
                    .end();
            return;
        }

        vertx.executeBlocking(op -> {
            try{
                CubeProperty new_property = datastore.save(property);
                op.complete(new_property);
            } catch (Exception e) {
                log.error(e.getMessage());
                op.fail("Unable to create a cube property");
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
