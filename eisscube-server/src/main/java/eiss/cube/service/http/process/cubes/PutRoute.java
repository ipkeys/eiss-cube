package eiss.cube.service.http.process.cubes;

import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import eiss.cube.config.AppConfig;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubePoint;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.query.Query;
import xyz.morphia.query.UpdateOperations;
import xyz.morphia.query.UpdateResults;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

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

        ObjectId oid = new ObjectId(id);

        String json = context.getBodyAsString();
        log.info("Update existing EISScube: id={} by: {}", id, json);

        EISScube cube = gson.fromJson(json, EISScube.class);
        if (cube != null) {
            Query<EISScube> query = datastore.createQuery(EISScube.class).field("_id").equal(oid);
            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class);

            if (cube.getSimCard() == null) {
                ops.unset("simCard");
            } else {
                ops.set("simCard", cube.getSimCard());
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

            vertx.executeBlocking(op -> {
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

                    UpdateResults result = datastore.update(query, ops);
                    if (result.getUpdatedCount() == 1) {
                        op.complete(cube);
                    } else {
                        op.fail(String.format("Unable to update EISScube with id: %s", id));
                    }
                } catch (Exception e) {
                    op.fail(String.format("Unable to update EISScube: %s", e.getMessage()));
                }
            }, res -> {
                if (res.succeeded()) {
                    response
                        .putHeader("content-type", "application/json")
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
                } else {
                    response
                        .setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response
                .setStatusCode(SC_BAD_REQUEST)
                .setStatusMessage(String.format("Unable to update EISScube with id: %s", id))
                .end();
        }
    }

}
