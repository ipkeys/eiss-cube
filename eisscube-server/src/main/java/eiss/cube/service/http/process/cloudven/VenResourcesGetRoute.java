package eiss.cube.service.http.process.cloudven;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/cloudven/resources/{ven}")
public class VenResourcesGetRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;

    @Inject
    public VenResourcesGetRoute(Vertx vertx, @Cube Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String ven = context.request().getParam("ven");

        vertx.executeBlocking(op -> {
            JsonArray resources = new JsonArray();

            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("settings.VEN").equal(ven);

            List<EISScube> cubes = q.find().toList();
            if (cubes != null) {
                cubes.forEach(cube -> {
                    resources.add(cube.getName());
                });
            }

            op.complete(resources.encodePrettily());
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(res.result()));
            }
        });
    }

}
