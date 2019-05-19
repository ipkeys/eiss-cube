package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import dev.morphia.query.Sort;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Arrays;
import java.util.List;

import static eiss.utils.reactadmin.ParamName.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/cubes")
public class ListRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public ListRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        Query<EISScube> q = datastore.createQuery(EISScube.class);

        // search
        String search = request.getParam(FILTER);
        if (search != null && !search.isEmpty()) {
            q.field("name").containsIgnoreCase(search);
        }
        // ~search

        // filters
        String id_like = request.getParam("id_like");
        if (id_like != null && !id_like.isEmpty()) {
            if (id_like.contains("|")) { // multiple ids
                String[] ids = id_like.split("|");
                Arrays.stream(ids).forEach(item -> {
                    if (ObjectId.isValid(item)) {
                        ObjectId oid = new ObjectId(item);
                        q.field("_id").equal(oid);
                    } else {
                        q.field("name").equal(item);
                    }
                });
            } else { // single
                if (ObjectId.isValid(id_like)) {
                    ObjectId oid = new ObjectId(id_like);
                    q.field("_id").equal(oid);
                } else {
                    q.field("name").equal(id_like);
                }
            }
        }
        String online = context.request().getParam("online");
        if (online != null && !online.isEmpty()) {
            q.field("online").equal(Boolean.valueOf(online));
        }
        // ~filters

        // sorts
        String byField = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (byField != null && order != null && !byField.isEmpty() && !order.isEmpty()) {
            q.order(order.equalsIgnoreCase(ASC) ? Sort.ascending(byField) : Sort.descending(byField));
        } else {
            q.order(Sort.ascending("name"));
        }
        // ~sorts

        // projections
        q.project("deviceID", TRUE)
            .project("name", TRUE)
            .project("online", TRUE)
            .project("lastPing", TRUE)
            .project("timeStarted", TRUE)
            .project("location", TRUE)
            .project("customerID", TRUE);
        // ~projections

        // skip/limit
        FindOptions o = new FindOptions();
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.valueOf(s)).limit(Integer.valueOf(e));
        }
        // ~skip/limit

        // 2 request to DB
        vertx.executeBlocking(list_op -> {
            List<EISScube> list = q.find(o).toList();
            if (list != null) {
                list_op.complete(gson.toJson(list));
            } else {
                list_op.fail("Cannot get list of EISScubes");
            }
        }, list_res -> {
            if (list_res.succeeded()) {
                vertx.executeBlocking(count_op -> {
                    Long result = q.count();
                    count_op.complete(result);
                }, count_res -> {
                    log.debug("List of cubes - success");
                    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .putHeader("X-Total-Count", String.valueOf(count_res.result()))
                            .setStatusCode(SC_OK)
                            .end(String.valueOf(list_res.result()));
                });
            } else {
                log.debug("List of cubes - failed");
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(list_res.cause().getMessage())
                        .end();
            }
        });
    }

}
