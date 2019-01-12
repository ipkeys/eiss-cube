package eiss.cube.service.http.process.geometry;

import com.google.gson.Gson;
import com.mongodb.DuplicateKeyException;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeGeo;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.Key;
import xyz.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/geometry")
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
        log.info("Create a new CubeGeo: {}", json);

        CubeGeo geo = gson.fromJson(json, CubeGeo.class);
        if (geo != null) {
            Query<CubeGeo> q = datastore.createQuery(CubeGeo.class);
            q.criteria("deviceID").equal(geo.getDeviceID());

            vertx.executeBlocking(op -> {
                try {
                    // 1. remove old geometry
                    datastore.delete(q);

                    // 2. save the new geometry
                    Key<CubeGeo> key = datastore.save(geo);
                    geo.setId((ObjectId)key.getId());
                    op.complete(geo);
                } catch (DuplicateKeyException dup) {
                    log.error(dup.getMessage());
                    op.fail("CubeGeo already exists");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    op.fail("Unable to add CubeGeo");
                }
            }, res -> {
                if (res.succeeded()) {
                    response
                        .putHeader("content-type", "application/json")
                        .setStatusCode(SC_CREATED)
                        .end(gson.toJson(geo));
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
                .setStatusMessage("Unable to add CubeGeo")
                .end();
        }
    }

}
