package eiss.cube.service.http.process.meters;

import com.google.gson.Gson;
import dev.morphia.query.Sort;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeMeter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

import static eiss.utils.reactadmin.ParamName.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.FALSE;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/meters")
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

        Query<CubeMeter> q = datastore.createQuery(CubeMeter.class);

        // filters
        String filter = request.getParam(FILTER);
        if (filter != null && !filter.isEmpty()) {
            q.field("reportID").startsWithIgnoreCase(filter);
        }

        String type = request.getParam("type");
        if (type != null && !type.isEmpty()) {
            q.field("type").startsWithIgnoreCase(type);
        }
        // ~filters

        // sorts
        String byField = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (byField != null && order != null && !byField.isEmpty() && !order.isEmpty()) {
            q.order(order.equalsIgnoreCase(ASC) ? Sort.ascending(byField) : Sort.descending(byField));
        } else {
            q.order(Sort.ascending("timestamp"));
        }
        // ~sorts

        // projections
        q.project("_id", FALSE);
        q.project("reportID", FALSE);
        q.project("type", FALSE);
        // ~projections

        // skip/limit
        FindOptions o = new FindOptions();
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.valueOf(s)).limit(Integer.valueOf(e) - Integer.valueOf(s));
        }
        // ~skip/limit

        vertx.executeBlocking(list_op -> {
            List<CubeMeter> list = q.find(o).toList();
            if (list != null) {
                list_op.complete(gson.toJson(list));
            } else {
                list_op.fail("Cannot get list of Meter data");
            }
        }, list_res -> {
            if (list_res.succeeded()) {
                vertx.executeBlocking(count_op -> {
                    Long result = q.count();
                    count_op.complete(result);
                }, count_res -> {
                    log.debug("List of meters - success");
                    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .putHeader("X-Total-Count", String.valueOf(count_res.result()))
                            .setStatusCode(SC_OK)
                            .end(String.valueOf(list_res.result()));
                });
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(list_res.cause().getMessage())
                        .end();
            }
        });
    }

}
