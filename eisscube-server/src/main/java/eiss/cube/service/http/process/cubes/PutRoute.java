package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import eiss.db.Cubes;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toMap;
import static javax.servlet.http.HttpServletResponse.*;

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
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id '%s' is not valid", id))
                    .end();
            return;
        }

        String json = context.getBodyAsString();
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
            q.filter(Filters.eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            if (cube.getName() == null) {
                updates.add(UpdateOperators.unset("name"));
            } else {
                updates.add(UpdateOperators.set("name", cube.getName()));
            }

            if (cube.getAddress() == null) {
                updates.add(UpdateOperators.unset("address"));
            } else {
                updates.add(UpdateOperators.set("address", cube.getAddress()));
            }

            if (cube.getCity() == null) {
                updates.add(UpdateOperators.unset("city"));
            } else {
                updates.add(UpdateOperators.set("city", cube.getCity()));
            }

            if (cube.getZipCode() == null) {
                updates.add(UpdateOperators.unset("zipCode"));
            } else {
                updates.add(UpdateOperators.set("zipCode", cube.getZipCode()));
            }

            if (cube.getCustomerID() == null) {
                updates.add(UpdateOperators.unset("customerID"));
            } else {
                updates.add(UpdateOperators.set("customerID", cube.getCustomerID()));
            }

            if (cube.getZone() == null) {
                updates.add(UpdateOperators.unset("zone"));
            } else {
                updates.add(UpdateOperators.set("zone", cube.getZone()));
            }

            if (cube.getSubZone() == null) {
                updates.add(UpdateOperators.unset("subZone"));
            } else {
                updates.add(UpdateOperators.set("subZone", cube.getSubZone()));
            }

            if (cube.getSettings() == null) {
                updates.add(UpdateOperators.unset("settings"));
            } else {
                // do not keep an empty string (convert it to null
                Map<String, Object> settings = cube.getSettings()
                    .entrySet()
                    .stream()
                    .filter(entry -> ((entry.getValue() instanceof String) && !(((String) entry.getValue()).isEmpty()))
                    ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                cube.setSettings(new BasicDBObject(settings));

                updates.add(UpdateOperators.set("settings", cube.getSettings()));
            }

            // put cube under group
            Query<Group> groupQuery = usersDatastore.find(Group.class);
            if (cube.getGroup_id() != null && !cube.getGroup_id().isEmpty()) {
                updates.add(UpdateOperators.set("group_id", cube.getGroup_id()));
                groupQuery.filter(Filters.eq("_id", new ObjectId(cube.getGroup_id())));
                Group group = groupQuery.first();
                if (group != null) {
                    updates.add(UpdateOperators.set("group", group.getName()));
                } else {
                    updates.add(UpdateOperators.unset("group"));
                }
            } else if (cube.getGroup() != null && !cube.getGroup().isEmpty()) {
                updates.add(UpdateOperators.set("group", cube.getGroup()));
                groupQuery.filter(Filters.eq("name", cube.getGroup()));
                Group group = groupQuery.first();
                if (group != null) {
                    updates.add(UpdateOperators.set("group_id", group.getId().toString()));
                } else {
                    updates.add(UpdateOperators.unset("group_id"));
                }
            }
            // ~put cube under group

            UpdateResult result = q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();
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
