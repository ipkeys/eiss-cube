package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import eiss.cube.config.AppConfig;
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
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toMap;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/cubes/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private AppConfig cfg;
    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PutRoute(AppConfig cfg, Vertx vertx, Datastore datastore, Gson gson) {
        this.cfg = cfg;
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("id '%s' is not valid", id))
                .end();
            return;
        }

        String json = context.getBodyAsString();
        log.info("Update existing EISScube: {} by: {}", id, json);
        EISScube cube = gson.fromJson(json, EISScube.class);
        if (cube == null) {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("Unable to update EISScube with id: %s", id))
                .end();
            return;
        }

        vertx.executeBlocking(op -> {
            /*
            try {
               if (cube.getAddress() != null && cube.getCity() != null && cube.getZipCode() != null) {
                    String address = cube.getAddress() + ", " + cube.getCity() + " " + cube.getZipCode();

                    // use geocoding to get location by address
                    GeoApiContext geoContext = new GeoApiContext().setApiKey(cfg.getGoogleApiKey());

                    GeocodingResult[] results = GeocodingApi
                        .geocode(geoContext, address)
                        .await();

                    CubePoint location = new CubePoint();
                    location.setLat(results[0].geometry.location.lat);
                    location.setLng(results[0].geometry.location.lng);
                    ops.set("location", location);
                } else {
                    ops.unset("location");
                }
            } catch (Exception e) {
                op.fail(String.format("Unable to update EISScube: %s", e.getMessage()));
            }
            */
            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class);

            if (cube.getName() == null) {
                ops.unset("name");
            } else {
                ops.set("name", cube.getName());
            }

            if (cube.getAddress() == null) {
                ops.unset("address");
            } else {
                ops.set("address", cube.getAddress());
            }

            if (cube.getCity() == null) {
                ops.unset("city");
            } else {
                ops.set("city", cube.getCity());
            }

            if (cube.getZipCode() == null) {
                ops.unset("zipCode");
            } else {
                ops.set("zipCode", cube.getZipCode());
            }

            if (cube.getCustomerID() == null) {
                ops.unset("customerID");
            } else {
                ops.set("customerID", cube.getCustomerID());
            }

            if (cube.getZone() == null) {
                ops.unset("zone");
            } else {
                ops.set("zone", cube.getZone());
            }

            if (cube.getSubZone() == null) {
                ops.unset("subZone");
            } else {
                ops.set("subZone", cube.getSubZone());
            }

            if (cube.getSettings() == null) {
                ops.unset("settings");
            } else {
                // do not keep an empty string (convert it to null
                Map<String, Object> settings = cube.getSettings()
                    .entrySet()
                    .stream()
                    .filter(entry -> ((entry.getValue() instanceof String) && !(((String) entry.getValue()).isEmpty()))
                    ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                cube.setSettings(new BasicDBObject(settings));

                ops.set("settings", cube.getSettings());
            }

            UpdateResults result = datastore.update(q, ops);
            if (result.getUpdatedCount() == 1) {
                op.complete(cube);
            } else {
                op.fail(String.format("Unable to update EISScube with id: %s", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(SC_OK)
                    .end(gson.toJson(res.result()));
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(res.cause().getMessage())
                    .end();
            }
        });
    }

}
