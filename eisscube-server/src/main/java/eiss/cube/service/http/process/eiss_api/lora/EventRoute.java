package eiss.cube.service.http.process.eiss_api.lora;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import eiss.cube.db.Cube;
import eiss.cube.randname.Randname;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
import eiss.models.cubes.LORAcube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eiss.utils.reactadmin.ParamName.ASC;
import static eiss.utils.reactadmin.ParamName.END;
import static eiss.utils.reactadmin.ParamName.FILTER;
import static eiss.utils.reactadmin.ParamName.ORDER;
import static eiss.utils.reactadmin.ParamName.SORT;
import static eiss.utils.reactadmin.ParamName.START;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/lora")
public class EventRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;
    private final Randname randname;

    @Inject
    public EventRoute(Vertx vertx, @Cube Datastore datastore, Gson gson, Randname randname) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
        this.randname = randname;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();


        String event = request.getParam("event");
        JsonObject json = context.getBodyAsJson();
        log.info("event: {}, body: {}", event, json.encodePrettily());

        switch (event) {
            case "up":
                upLORACube(json);
                break;
            case "join":
                joinLORACube(json);
                break;
            default:
                break;
        }

        response.setStatusCode(SC_OK).end();
    }

    private static String base64toHex(String base64str) {
        byte[] contentBytes = Base64.getDecoder().decode(base64str);
        BigInteger no = new BigInteger(1, contentBytes);
        return no.toString(16);
    }

    private void upLORACube(JsonObject json) {
        String deviceID = base64toHex(json.getString("devEUI"));
        String deviceName = json.getString("deviceName");
        Integer ss = json.getJsonArray("rxInfo").getJsonObject(0).getInteger("rssi");

        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(op -> {
            LORAcube cube = q.first();
            if (cube == null) {
                cube = new LORAcube();
                cube.setDeviceID(deviceID);
                cube.setName(deviceName);
                cube.setSocket(null);
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(convertDBm(ss));
            cube.setLastPing(timestamp);
            cube.setSocket(null);

            datastore.save(cube);

            String data = json.getString("objectJSON");
            if (data != null && !data.isEmpty()) {
                JsonObject dataJSON = new JsonObject(data);
                doBusinessWithDevice(deviceID, dataJSON);
            }

            op.complete();
        }, res -> {
            if (res.succeeded()) {
                log.info("DeviceID: {} 'up' device data is served", deviceID);
            }
        });
    }

    private void joinLORACube(JsonObject json) {
        String deviceID = base64toHex(json.getString("devEUI"));
        String deviceName = json.getString("deviceName");
        Integer ss = json.getJsonArray("rxInfo").getJsonObject(0).getInteger("rssi");

        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(op -> {
            LORAcube cube = q.first();
            if (cube == null) {
                cube = new LORAcube();
                cube.setDeviceID(deviceID);
                cube.setName(deviceName);
                cube.setSocket(null);
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(convertDBm(ss));
            cube.setTimeStarted(timestamp);
            cube.setSocket(null);

            datastore.save(cube);

            op.complete();
        }, res -> log.info("DeviceID: {} is ONLINE", deviceID));
    }

    public static boolean between(int i, int minValueInclusive, int maxValueExlusive) {
        return (i >= minValueInclusive && i < maxValueExlusive);
    }

    /*
        0 - < -105 dBm or unknown
        1 - < -93 dBm
        2 - < -81 dBm
        3 - < -69 dBm
        4 - < -57 dBm
        5 - >= -57 dBm
    */
    private int convertDBm(Integer ss) {
        int rc = 0;

        if (between(ss, -105,-93)) {
            rc = 1;
        }
        if (between(ss, -93,-81)) {
            rc = 2;
        }
        if (between(ss, -81,-69)) {
            rc = 3;
        }
        if (between(ss, -69,-57)) {
            rc = 4;
        }
        if (ss >= -57) {
            rc = 5;
        }

        return rc;
    }

    private void doBusinessWithDevice(String deviceID, JsonObject dataJSON) {
        Query<LORAcube> q = datastore.createQuery(LORAcube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(op -> {
            LORAcube cube = q.first();
            if (cube != null) {
                Query<CubeReport> qr = datastore.createQuery(CubeReport.class);
                qr.field("cubeID").equal(cube.getId());

                CubeReport cr = qr.first();
                if (cr != null) {
                    if (cr.getType().equalsIgnoreCase("p")) {
                        // do "count pulses" report
                    }
                    if (cr.getType().equalsIgnoreCase("c")) {
                        // do "count cycle" report
                    }
                }


                String work_mode = dataJSON.getString("Work_mode");
                if (work_mode != null && work_mode.equalsIgnoreCase("2ACI+2AVI")) {
                    String r = "0";
                    String RO1_status = dataJSON.getString("RO1_status");
                    if (RO1_status != null && RO1_status.equalsIgnoreCase("ON")) {
                        r = "1";
                    }

                    String i = "0";
                    String DI1_status = dataJSON.getString("DI1_status");
                    if (DI1_status != null && DI1_status.equalsIgnoreCase("H")) {
                        i = "1";
                    }

                    CubeTest cubeTest = new CubeTest();
                    cubeTest.setCubeID(cube.getId());
                    cubeTest.setTimestamp(Instant.now());
                    cubeTest.setR(Integer.valueOf(r));
                    cubeTest.setI(Integer.valueOf(i));

                    datastore.save(cubeTest);
                }

            }
            op.complete();
        }, res -> log.info("DeviceID: {} device data is handeled", deviceID));
    }

}
