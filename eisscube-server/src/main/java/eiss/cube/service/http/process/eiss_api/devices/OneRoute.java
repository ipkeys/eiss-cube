package eiss.cube.service.http.process.eiss_api.devices;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.devices.Device;
import eiss.cube.json.messages.devices.DeviceIdRequest;
import eiss.cube.json.messages.devices.DeviceResponse;
import eiss.cube.json.messages.devices.Location;
import eiss.cube.service.http.process.api.Api;
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
@Path("/eiss-api/devices/id")
public class OneRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public OneRoute(Vertx vertx, Datastore datastore, Gson gson) {
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
                    DeviceIdRequest req = gson.fromJson(jsonBody, DeviceIdRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        DeviceResponse res = getDevice(req);
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

    private DeviceResponse getDevice(DeviceIdRequest req) {
        DeviceResponse rc = new DeviceResponse();

        String id = req.getId();
        if (ObjectId.isValid(id)) {
            Query<EISScube> q = datastore.createQuery(EISScube.class);

            // filter
            q.criteria("_id").equal(new ObjectId(id));

            // projections

            // get
            EISScube d = q.first();
            if (d != null) {
                Double lat = Location.defaultLat;
                Double lng = Location.defaultLng;

                // default Location - IPKeys office
                if (d.getLocation() != null) {
                    lat = d.getLocation().getLat();
                    lng = d.getLocation().getLng();
                }

                rc.setDevice(
                    Device.builder()
                        .id(d.getId().toString())
                        .deviceID(d.getDeviceID())
                        .online(d.getOnline())
                        .timeStarted(d.getTimeStarted())
                        .lastPing(d.getLastPing())
                        .signalStrength(d.getSignalStrength())
                        .address(d.getAddress())
                        .city(d.getCity())
                        .zipCode(d.getZipCode())
                        .customerID(d.getCustomerID())
                        .zone(d.getZone())
                        .subZone(d.getSubZone())
                        .location(Location.builder().lat(lat).lng(lng).build())
                        .settings(d.getSettings())
                    .build()
                );
            }
        }

        return rc;
    }


}
