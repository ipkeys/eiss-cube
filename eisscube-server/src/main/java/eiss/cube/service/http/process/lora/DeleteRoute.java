package eiss.cube.service.http.process.lora;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.EISScube;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/lora/{id}")
public class DeleteRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public DeleteRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @DELETE
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id: %s is not valid", id))
                    .end();
            return;
        }

        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("_id").equal(new ObjectId(id));

        vertx.executeBlocking(cube_op -> {
            // react-admin expect previous data
            LORAcube cube = q.first();
            if (cube != null) {
                // delete LORAcube
                datastore.delete(q);
                cube_op.complete(gson.toJson(cube));
            } else {
                cube_op.fail(String.format("Cannot delete LORAcube id: %s", id));
            }
        }, cube_res -> {
            if (cube_res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end((String)cube_res.result());
            } else {
                response.setStatusCode(SC_NOT_FOUND)
                        .setStatusMessage(cube_res.cause().getMessage())
                        .end();
            }
        });
    }

}
