package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import eiss.db.Cubes;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.EISScube;
import eiss.db.Users;
import eiss.models.users.Group;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toMap;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/cubes/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore usersDatastore;
    private final Datastore cubesDatastore;
    private final Gson gson;

    @Inject
    public PutRoute(Vertx vertx, @Users Datastore usersDatastore, @Cubes Datastore cubesDatastore, Gson gson) {
        this.vertx = vertx;
        this.usersDatastore = usersDatastore;
        this.cubesDatastore = cubesDatastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id '%s' is not valid", id))
                    .end();
            return;
        }

        String json = context.body().asString();
        log.info("Update existing EISScube: {} by: {}", id, json);
        EISScube cube = gson.fromJson(json, EISScube.class);
        if (cube == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("Unable to update EISScube with id: %s", id))
                    .end();
            return;
        }

        vertx.executeBlocking(op -> {
            Query<EISScube> q = cubesDatastore.find(EISScube.class);
            q.filter(eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            if (cube.getName() == null) {
                updates.add(unset("name"));
            } else {
                updates.add(set("name", cube.getName()));
            }

            if (cube.getAddress() == null) {
                updates.add(unset("address"));
            } else {
                updates.add(set("address", cube.getAddress()));
            }

            if (cube.getCity() == null) {
                updates.add(unset("city"));
            } else {
                updates.add(set("city", cube.getCity()));
            }

            if (cube.getZipCode() == null) {
                updates.add(unset("zipCode"));
            } else {
                updates.add(set("zipCode", cube.getZipCode()));
            }

            if (cube.getCustomerID() == null) {
                updates.add(unset("customerID"));
            } else {
                updates.add(set("customerID", cube.getCustomerID()));
            }

            if (cube.getZone() == null) {
                updates.add(unset("zone"));
            } else {
                updates.add(set("zone", cube.getZone()));
            }

            if (cube.getSubZone() == null) {
                updates.add(unset("subZone"));
            } else {
                updates.add(set("subZone", cube.getSubZone()));
            }

            if (cube.getSettings() == null) {
                updates.add(unset("settings"));
            } else {
                // do not keep an empty string (convert it to null
                Map<String, Object> settings = cube.getSettings()
                    .entrySet()
                    .stream()
                    .filter(entry -> ((entry.getValue() instanceof String) && !(((String) entry.getValue()).isEmpty())))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                cube.setSettings(new BasicDBObject(settings));

                updates.add(set("settings", cube.getSettings()));
            }

            // put cube under group
            Query<Group> groupQuery = usersDatastore.find(Group.class);
            if (cube.getGroup_id() != null && !cube.getGroup_id().isEmpty()) {
                updates.add(set("group_id", cube.getGroup_id()));
                groupQuery.filter(eq("_id", new ObjectId(cube.getGroup_id())));
                Group group = groupQuery.first();
                if (group != null) {
                    updates.add(set("group", group.getName()));
                } else {
                    updates.add(unset("group"));
                }
            } else if (cube.getGroup() != null && !cube.getGroup().isEmpty()) {
                updates.add(set("group", cube.getGroup()));
                groupQuery.filter(eq("name", cube.getGroup()));
                Group group = groupQuery.first();
                if (group != null) {
                    updates.add(set("group_id", group.getId().toString()));
                } else {
                    updates.add(unset("group_id"));
                }
            }
            // ~put cube under group

            UpdateResult result = q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));
            if (result.getModifiedCount() == 1) {
                op.complete(cube);
            } else {
                op.fail(String.format("Unable to update EISScube with id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
