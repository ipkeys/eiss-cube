package eiss.cube.service.http.process.test;

import com.google.gson.Gson;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeTest;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/test/{cubeID}")
public class GetRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public GetRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String cubeID = request.getParam("cubeID");
        if (!isValid(cubeID)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id: %s is not valid", cubeID))
                    .end();
            return;
        }

        Query<CubeTest> qt = datastore.find(CubeTest.class);
        qt.filter(eq("cubeID", new ObjectId(cubeID)));

        FindOptions o = new FindOptions();
        o.projection().exclude("_id", "cubeID");

        vertx.executeBlocking(op -> {
            List<CubeTest> list = qt.iterator(o).toList();
            op.complete(list);
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_NOT_FOUND)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }

        });
    }

}
