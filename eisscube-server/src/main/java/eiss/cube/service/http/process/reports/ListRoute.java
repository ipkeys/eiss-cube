package eiss.cube.service.http.process.reports;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeReport;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import xyz.morphia.Datastore;
import xyz.morphia.query.FindOptions;
import xyz.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

import static eiss.utils.AdminOnRest.ParamName.*;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/reports")
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

        Query<CubeReport> q = datastore.createQuery(CubeReport.class);

        // filters
        String filter = request.getParam(FILTER);
        if (filter != null && !filter.isEmpty()) {
            q.field("deviceID").startsWithIgnoreCase(filter);
        }

        // sorts
        String sort = context.request().getParam(SORT);
        String order = context.request().getParam(ORDER);
        if (sort != null && order != null && !sort.isEmpty() && !order.isEmpty()) {
            q.order(order.equalsIgnoreCase(ASC) ? sort : "-" + sort);
        } else {
            q.order("deviceID");
        }

        // projections

        // skip/limit
        FindOptions o = new FindOptions();
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.valueOf(s)).limit(Integer.valueOf(e));
        }

        vertx.executeBlocking(op -> {
            List<CubeReport> result = q.asList(o);
            op.complete(result);
        }, res -> {
            if (res.succeeded()) {
                vertx.executeBlocking(c -> {
                    Long result = q.count();
                    c.complete(result);
                }, c -> {
                    response
                        .putHeader("content-type", "application/json")
                        .putHeader("X-Total-Count", String.valueOf(c.result()))
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
                });
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Cannot get list of Reports")
                    .end();
            }
        });
    }

}
