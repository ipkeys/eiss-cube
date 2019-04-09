package eiss.cube.service.http.process.setup;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeSetup;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/setup")
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
        log.info("Create a new CubeSetup: {}", json);

        CubeSetup setup = gson.fromJson(json, CubeSetup.class);
        if (setup == null) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage("Unable to save CubeSetup")
                .end();
            return;
        }

        Query<CubeSetup> q = datastore.createQuery(CubeSetup.class);
        q.criteria("cubeID").equal(setup.getCubeID());

        UpdateOperations<CubeSetup> ops = datastore.createUpdateOperations(CubeSetup.class);

        ops.set("relay", setup.getRelay());
        ops.set("input", setup.getInput());

        vertx.executeBlocking(op -> {
            UpdateResults result = datastore.update(q, ops, TRUE); // createIfMissing
            if (result.getUpdatedCount() == 1 || result.getInsertedCount() == 1) {
                op.complete(setup);
            } else {
                op.fail(String.format("Unable to create/update setup for EISScube: %s", setup.getCubeID()));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(SC_CREATED)
                    .end(gson.toJson(setup));
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
