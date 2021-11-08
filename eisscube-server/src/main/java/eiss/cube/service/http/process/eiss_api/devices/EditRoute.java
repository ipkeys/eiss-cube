package eiss.cube.service.http.process.eiss_api.devices;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.devices.Device;
import eiss.cube.json.messages.devices.DeviceRequest;
import eiss.cube.json.messages.devices.DeviceResponse;
import eiss.cube.json.messages.devices.Location;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

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
        String jsonBody = context.getBodyAsString();

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
        if (ObjectId.isValid(id)) {
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(Filters.eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            if (d.getName() == null) {
                updates.add(UpdateOperators.unset("name"));
            } else {
                updates.add(UpdateOperators.set("name", d.getName()));
            }

            if (d.getAddress() == null) {
                updates.add(UpdateOperators.unset("address"));
            } else {
                updates.add(UpdateOperators.set("address", d.getAddress()));
            }

            if (d.getCity() == null) {
                updates.add(UpdateOperators.unset("city"));
            } else {
                updates.add(UpdateOperators.set("city", d.getCity()));
            }

            if (d.getZipCode() == null) {
                updates.add(UpdateOperators.unset("zipCode"));
            } else {
                updates.add(UpdateOperators.set("zipCode", d.getZipCode()));
            }

            if (d.getCustomerID() == null) {
                updates.add(UpdateOperators.unset("customerID"));
            } else {
                updates.add(UpdateOperators.set("customerID", d.getCustomerID()));
            }

            if (d.getZone() == null) {
                updates.add(UpdateOperators.unset("zone"));
            } else {
                updates.add(UpdateOperators.set("zone", d.getZone()));
            }

            if (d.getSubZone() == null) {
                updates.add(UpdateOperators.unset("subZone"));
            } else {
                updates.add(UpdateOperators.set("subZone", d.getSubZone()));
            }

            if (d.getLocation() != null) {
                if (d.getLocation().getLat() == null) {
                    updates.add(UpdateOperators.unset("location.lat"));
                } else {
                    updates.add(UpdateOperators.set("location.lat", d.getLocation().getLat()));
                }

                if (d.getLocation().getLng() == null) {
                    updates.add(UpdateOperators.unset("location.lng"));
                } else {
                    updates.add(UpdateOperators.set("location.lng", d.getLocation().getLng()));
                }
            } else {
                updates.add(UpdateOperators.unset("location"));
            }

            if (d.getSettings() == null) {
                updates.add(UpdateOperators.unset("settings"));
            } else {
                updates.add(UpdateOperators.set("settings", d.getSettings()));
            }

            q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();

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
