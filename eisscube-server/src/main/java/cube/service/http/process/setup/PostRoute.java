package cube.service.http.process.setup;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import cube.db.Cube;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import cube.models.CubeInput;
import cube.models.CubeRelay;
import cube.models.CubeSetup;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/setup")
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
        log.info("Create a new CubeSetup: {}", json);

        CubeSetup setup = gson.fromJson(json, CubeSetup.class);
        if (setup == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to save CubeSetup")
                    .end();
            return;
        }

        Query<CubeSetup> q = datastore.find(CubeSetup.class);
        q.filter(Filters.eq("cubeID", setup.getCubeID()));

        List<UpdateOperator> updates = new ArrayList<>();
        //updates.add(UpdateOperators.set("deviceType", setup.getDeviceType()));

        CubeRelay relay = setup.getRelay();
        if (relay != null) {
            updates.add(UpdateOperators.set("relay", relay));
        }
        CubeInput input = setup.getInput();
        if (input != null) {
            updates.add(UpdateOperators.set("input", input));
        }

        vertx.executeBlocking(op -> {
            UpdateResult result = q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute(new UpdateOptions().upsert(TRUE));
            if (result.getModifiedCount() == 1) {
                op.complete(gson.toJson(setup));
            } else {
                op.fail(String.format("Unable to create/update setup for EISScube: %s", setup.getCubeID()));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_CREATED)
                        .end(String.valueOf(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
