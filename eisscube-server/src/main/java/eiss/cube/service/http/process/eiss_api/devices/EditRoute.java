package eiss.cube.service.http.process.eiss_api.devices;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.devices.Device;
import eiss.cube.json.messages.devices.DeviceRequest;
import eiss.cube.json.messages.devices.DeviceResponse;
import eiss.cube.json.messages.devices.Location;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/eiss-api/devices/edit")
public class EditRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public EditRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    DeviceRequest req = gson.fromJson(jsonBody, DeviceRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        DeviceResponse res = editDevice(req);
                        op.complete(res);
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(gson.toJson(res.result()));
                } else {
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private DeviceResponse editDevice(DeviceRequest req) {
        DeviceResponse rc = new DeviceResponse();

        Device d = req.getDevice();

        String id = req.getDevice().getId();
        if (isValid(id)) {
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            if (d.getName() == null) {
                updates.add(unset("name"));
            } else {
                updates.add(set("name", d.getName()));
            }

            if (d.getAddress() == null) {
                updates.add(unset("address"));
            } else {
                updates.add(set("address", d.getAddress()));
            }

            if (d.getCity() == null) {
                updates.add(unset("city"));
            } else {
                updates.add(set("city", d.getCity()));
            }

            if (d.getZipCode() == null) {
                updates.add(unset("zipCode"));
            } else {
                updates.add(set("zipCode", d.getZipCode()));
            }

            if (d.getCustomerID() == null) {
                updates.add(unset("customerID"));
            } else {
                updates.add(set("customerID", d.getCustomerID()));
            }

            if (d.getZone() == null) {
                updates.add(unset("zone"));
            } else {
                updates.add(set("zone", d.getZone()));
            }

            if (d.getSubZone() == null) {
                updates.add(unset("subZone"));
            } else {
                updates.add(set("subZone", d.getSubZone()));
            }

            if (d.getLocation() != null) {
                if (d.getLocation().getLat() == null) {
                    updates.add(unset("location.lat"));
                } else {
                    updates.add(set("location.lat", d.getLocation().getLat()));
                }

                if (d.getLocation().getLng() == null) {
                    updates.add(unset("location.lng"));
                } else {
                    updates.add(set("location.lng", d.getLocation().getLng()));
                }
            } else {
                updates.add(unset("location"));
            }

            if (d.getSettings() == null) {
                updates.add(unset("settings"));
            } else {
                updates.add(set("settings", d.getSettings()));
            }

            q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

            // get an updated version
            EISScube ud = q.first();
            if (ud != null) {

                Double lat = Location.defaultLat;
                Double lng = Location.defaultLng;

                // default Location - IPKeys office
                if (d.getLocation() != null) {
                    lat = ud.getLocation().getLat();
                    lng = ud.getLocation().getLng();
                }

                rc.setDevice(
                    Device.builder()
                        .id(ud.getId().toString())
                        .ICCID(ud.getDeviceID())
                        .name(ud.getName())
                        .online(ud.getOnline())
                        .timeStarted(ud.getTimeStarted())
                        .lastPing(ud.getLastPing())
                        .signalStrength(ud.getSignalStrength())
                        .address(ud.getAddress())
                        .city(ud.getCity())
                        .zipCode(ud.getZipCode())
                        .customerID(ud.getCustomerID())
                        .zone(ud.getZone())
                        .subZone(ud.getSubZone())
                        .location(Location.builder().lat(lat).lng(lng).build())
                        .settings(ud.getSettings())
                    .build()
                );
            }
        }

        return rc;
    }

}
