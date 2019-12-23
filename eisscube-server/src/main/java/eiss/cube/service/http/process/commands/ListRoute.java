package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import dev.morphia.query.Sort;
import eiss.cube.db.Cube;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
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
import java.time.Instant;
import java.util.List;

import static eiss.utils.reactadmin.ParamName.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/commands")
public class ListRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);

        // search
        String search = request.getParam(FILTER);
        if (search != null && !search.isEmpty()) {
            search = search.toLowerCase()
                .replaceAll(" ", "")
                .replaceAll("relay", "r")
                .replaceAll("cointing", "i")
                .replaceAll("cycle", "cyc")
                .replaceAll("pulse", "cp")
                .replaceAll("stop", "off");

            q.field("command").containsIgnoreCase(search);
        }
        // ~search

        // filters
        String cubeID = request.getParam("cubeID");
        if (cubeID != null && !cubeID.isEmpty()) {
            if (ObjectId.isValid(cubeID)) {
                q.field("cubeID").equal(new ObjectId(cubeID));
            }
        }
        String sinceTime = request.getParam("timestamp_gte");
        if (sinceTime != null && !sinceTime.isEmpty()) {
            q.field("created").greaterThanOrEq(Instant.parse(sinceTime));
        }
        String beforeTime = request.getParam("timestamp_lte");
        if (beforeTime != null && !beforeTime.isEmpty()) {
            q.field("created").lessThanOrEq(Instant.parse(beforeTime));
        }
        // ~filters

        // sorts
        String byField = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (byField != null && order != null && !byField.isEmpty() && !order.isEmpty()) {
            q.order(order.equalsIgnoreCase(ASC) ? Sort.ascending(byField) : Sort.descending(byField));
        } else {
            q.order(Sort.ascending("created"));
        }
        // ~sorts

        // skip/limit
        FindOptions o = new FindOptions();
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.valueOf(s)).limit(Integer.valueOf(e) - Integer.valueOf(s));
        }
        // ~skip/limit

        vertx.executeBlocking(list_op -> {
            List<CubeCommand> list = q.find(o).toList();
            if (list != null) {
                list_op.complete(gson.toJson(list));
            } else {
                list_op.fail("Cannot get list of Commands");
            }
        }, list_res -> {
            if (list_res.succeeded()) {
                vertx.executeBlocking(count_op -> {
                    Long count = q.count();
                    count_op.complete(count);
                }, count_res -> {
                    log.debug("List of Commands - success");
                    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .putHeader("X-Total-Count", String.valueOf(count_res.result()))
                            .setStatusCode(SC_OK)
                            .end(String.valueOf(list_res.result()));
                });
            } else {
                log.debug("List of Commands - failed");
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(list_res.cause().getMessage())
                        .end();
            }
        });
    }

}
