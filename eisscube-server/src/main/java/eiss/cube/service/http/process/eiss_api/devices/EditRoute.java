package eiss.cube.service.http.process.eiss_api.devices;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.devices.Device;
import eiss.cube.json.messages.devices.DeviceRequest;
import eiss.cube.json.messages.devices.DeviceResponse;
import eiss.cube.json.messages.devices.Location;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
    public EditRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
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
                        op.complete(gson.toJson(res));
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end((String)res.result());
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
            Query<EISScube> q = datastore.createQuery(EISScube.class);

            // filter
            q.criteria("_id").equal(new ObjectId(id));

            // projections

            // update
            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class);

            if (d.getName() == null) ops.unset("name"); else ops.set("name", d.getName());
            if (d.getAddress() == null) ops.unset("address"); else ops.set("address", d.getAddress());
            if (d.getCity() == null) ops.unset("city"); else ops.set("city", d.getCity());
            if (d.getZipCode() == null) ops.unset("zipCode"); else ops.set("zipCode", d.getZipCode());
            if (d.getCustomerID() == null) ops.unset("customerID"); else ops.set("customerID", d.getCustomerID());
            if (d.getZone() == null) ops.unset("zone"); else ops.set("zone", d.getZone());
            if (d.getSubZone() == null) ops.unset("subZone"); else ops.set("subZone", d.getSubZone());
            if (d.getLocation() != null) {
                if (d.getLocation().getLat() == null) ops.unset("location.lat"); else ops.set("location.lat", d.getLocation().getLat());
                if (d.getLocation().getLng() == null) ops.unset("location.lng"); else ops.set("location.lng", d.getLocation().getLng());
            } else {
                ops.unset("location");
            }
            if (d.getSettings() == null) ops.unset("settings"); else ops.set("settings", d.getSettings());

            datastore.update(q, ops);

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
