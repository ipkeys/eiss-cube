package cube.service.http.process.groups;

import com.google.gson.Gson;
import com.mongodb.client.model.Collation;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import cube.db.Eiss;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
import eiss.models.Group;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

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
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/groups")
public class ListRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Eiss Datastore datastore, Gson gson) {
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

        Query<Group> q = datastore.find(Group.class);
        FindOptions o = new FindOptions().collation(
            Collation.builder().locale("en").collationStrength(SECONDARY).build()
        );

        if (session.get("role").equals("securityadmin")) {
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

            // skip/limit
            String s = request.getParam(START);
            String e = request.getParam(END);
            if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
                o.skip(Integer.parseInt(s)).limit(Integer.parseInt(e) - Integer.parseInt(s));
            }
            // ~skip/limit

        /* All other users can only see own group */
        } else {
            q.filter(Filters.eq("name", session.get("group")));
        }

        vertx.executeBlocking(list_op -> {
            List<Group> list = q.iterator(o).toList();
            if (list != null) {
                list_op.complete(new Object[]{q.count(), gson.toJson(list)});
            } else {
                list_op.fail("Cannot get list of Groups");
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .putHeader("X-Total-Count", String.valueOf(Array.get(res.result(), 0)))
                    .setStatusCode(SC_OK)
                    .end(String.valueOf(Array.get(res.result(), 1)));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
