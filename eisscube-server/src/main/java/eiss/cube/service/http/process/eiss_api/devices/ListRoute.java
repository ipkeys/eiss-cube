package eiss.cube.service.http.process.eiss_api.devices;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.devices.Device;
import eiss.cube.json.messages.devices.DeviceListRequest;
import eiss.cube.json.messages.devices.DeviceListResponse;
import eiss.cube.json.messages.devices.Location;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/eiss-api/devices/list")
public class ListRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
                    DeviceListRequest req = gson.fromJson(jsonBody, DeviceListRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        DeviceListResponse res = getListOfDevices(req);
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
                    response.setStatusCode(SC_INTERNAL_SERVER_ERROR)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                .end();
        }
    }

    private DeviceListResponse getListOfDevices(DeviceListRequest req) {
        DeviceListResponse rc = new DeviceListResponse();

        Query<EISScube> cubes = datastore.find(EISScube.class);

        // filter by "customerID" property
        String customerID = req.getCustomerID();
        if (customerID != null && !customerID.isEmpty()) {
            cubes.filter(Filters.eq("customerID", customerID));
        }

        // projections

        // skip/limit
        FindOptions options = new FindOptions();
        Integer s = req.getStart();
        Integer l = req.getLimit();
        if (s != null && l != null) {
            options.skip(s).limit(l);
        }

        // get & convert
        cubes.iterator(options).toList().forEach(d -> {
            Double lat = Location.defaultLat;
            Double lng = Location.defaultLng;

            // default Location - IPKeys office
            if (d.getLocation() != null) {
                lat = d.getLocation().getLat();
                lng = d.getLocation().getLng();
            }

            rc.getDevices().add(
                Device.builder()
                    .id(d.getId().toString())
                    .ICCID(d.getDeviceID())
                    .name(d.getName())
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
        });

        // total number of records
        rc.setTotal(cubes.count());

        return rc;
    }

}
