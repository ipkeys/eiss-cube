package cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.client.model.Collation;
import dev.morphia.query.Sort;
import cube.db.Cube;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
import cube.models.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.CollationStrength.SECONDARY;
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

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

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
        Session session = context.session();

        Query<EISScube> q = datastore.find(EISScube.class);
        FindOptions o = new FindOptions();
        o.collation(Collation.builder().locale("en").collationStrength(SECONDARY).build());

        if (session.get("role").equals("securityadmin")) {
            // filters
            String group_id = request.getParam("group_id");
            if (group_id != null) {
                q.filter(Filters.eq("group_id", group_id));
            }
            // ~filters
        } else {
            q.filter(Filters.eq("group", session.get("group")));
        }

        // search
        String search = request.getParam(FILTER);
        if (search != null && !search.isEmpty()) {
            q.filter(Filters.regex("name").pattern("^" + search).caseInsensitive());
        }
        // ~search

        // filters
        String ids_like = request.getParam("id_like");
        if (ids_like != null && !ids_like.isEmpty()) {
            if (ids_like.contains("|")) {
                List<ObjectId> ids = Stream.of(ids_like.split("\\|")).map(ObjectId::new).collect(Collectors.toList());
                q.filter(Filters.in("_id", ids));
            } else {
                q.filter(Filters.eq("_id", new ObjectId(ids_like)));
            }
        }
        String online = request.getParam("online");
        if (online != null && !online.isEmpty()) {
            q.filter(Filters.eq("online", Boolean.valueOf(online)));
        }
        // ~filters

        // sorts
        String byField = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (byField != null && order != null && !byField.isEmpty() && !order.isEmpty()) {
            o.sort(order.equalsIgnoreCase(ASC) ? Sort.ascending(byField) : Sort.descending(byField));
        } else {
            o.sort(Sort.ascending("name"));
        }
        // ~sorts

        // projections
        o.projection().include("deviceID", "deviceType", "name", "group_id", "group", "online", "lastPing", "timeStarted", "location", "customerID");
        // ~projections

        // skip/limit
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.parseInt(s)).limit(Integer.parseInt(e) - Integer.parseInt(s));
        }
        // ~skip/limit

        vertx.executeBlocking(list_op -> {
            List<EISScube> list = q.iterator(o).toList();
            if (list != null) {
                list_op.complete(new Object[]{q.count(), gson.toJson(list)});
            } else {
                list_op.fail("Cannot get list of EISScubes");
            }
        }, list_res -> {
            if (list_res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader("X-Total-Count", String.valueOf(Array.get(list_res.result(), 0)))
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(Array.get(list_res.result(), 1)));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(list_res.cause().getMessage())
                        .end();
            }
        });
    }

}
