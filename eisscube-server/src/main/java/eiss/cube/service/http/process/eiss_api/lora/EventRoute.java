package eiss.cube.service.http.process.eiss_api.lora;

import com.google.gson.Gson;
import eiss.models.cubes.CubeMeter;
import eiss.models.cubes.EISScube;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import eiss.cube.randname.Randname;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.CubeTest;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
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
    public EventRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson, Randname randname) {
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
            case "join":
                joinLORACube(json);
                break;
            case "up":
                upLORACube(json);
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

    private void joinLORACube(JsonObject json) {
        String deviceID = base64toHex(json.getString("devEUI"));
        String deviceName = json.getString("deviceName");
        Integer ss = json.getJsonArray("rxInfo").getJsonObject(0).getInteger("rssi");

        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(Filters.eq("deviceID", deviceID));

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(deviceName);
                cube.setSocket(null);
                cube.setDeviceType("l");
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(convertDBm(ss));
            cube.setTimeStarted(timestamp);
            cube.setLastPing(timestamp);

            datastore.save(cube);

            op.complete();
        }, res -> log.info("DeviceID: {} sent JOIN", deviceID));
    }



    private void upLORACube(JsonObject json) {
        String deviceID = base64toHex(json.getString("devEUI"));
        String deviceName = json.getString("deviceName");
        Integer ss = json.getJsonArray("rxInfo").getJsonObject(0).getInteger("rssi");

        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(Filters.eq("deviceID", deviceID));

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(deviceName);
                cube.setSocket(null);
                cube.setDeviceType("l");
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(convertDBm(ss));
            cube.setLastPing(timestamp);

            datastore.save(cube);

            // use combination of port & data and use timestamp from server
            Integer port = json.getInteger("fPort");
            String data = json.getString("objectJSON");

            if (port == 2) { // STATUS report
                if (data != null && !data.isEmpty()) {
                    JsonObject dataJSON = new JsonObject(data);
                    //doBusinessWithDevice(deviceID, dataJSON);
                }
            }
            if (port == 3) { // ICP report
                if (data != null && !data.isEmpty()) {
                    JsonObject dataJSON = new JsonObject(data);
                    savePulseReport(cube, dataJSON);
                }
            }
            if (port == 4) { // ICC report
                if (data != null && !data.isEmpty()) {
                    JsonObject dataJSON = new JsonObject(data);
                    saveCycleReport(cube, dataJSON);
                }
            }
            if (port == 5) { // TEST report
                if (data != null && !data.isEmpty()) {
                    JsonObject dataJSON = new JsonObject(data);
                    Instant ts = Instant.parse(json.getString("publishedAt")).truncatedTo(SECONDS);
                    dataJSON.put("ts", ts);

                    saveTestReport(cube, dataJSON);
                }
            }

            log.info("Data on port: {} payload: {}", port, data);

            op.complete(data);
        }, res -> log.info("DeviceID: {} sent UP", deviceID));
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

    private void saveTestReport(EISScube cube, JsonObject dataJSON) {
        CubeTest cubeTest = new CubeTest();
        cubeTest.setCubeID(cube.getId());
        cubeTest.setTimestamp(dataJSON.getInstant("ts"));
        cubeTest.setR(dataJSON.getInteger("r"));
        cubeTest.setI(dataJSON.getInteger("i"));

        datastore.save(cubeTest);
    }

    private void savePulseReport(EISScube cube, JsonObject dataJSON) {
        CubeMeter cubeMeter = new CubeMeter();
        cubeMeter.setCubeID(cube.getId());
        cubeMeter.setTimestamp(Instant.ofEpochSecond(dataJSON.getInteger("ts")));
        cubeMeter.setValue(dataJSON.getDouble("v"));
        cubeMeter.setType("p"); // count pulses

        datastore.save(cubeMeter);
    }

    private void saveCycleReport(EISScube cube, JsonObject dataJSON) {
        Instant ts = Instant.ofEpochSecond(dataJSON.getInteger("ts"));
        Double v = dataJSON.getDouble("dur");

        Query<CubeMeter> qm = datastore.find(CubeMeter.class);
        qm.filter(
            Filters.and(
                Filters.eq("cubeID", cube.getId()),
                Filters.eq("timestamp", ts)
            )
        );

        List<UpdateOperator> updates = new ArrayList<>();

        updates.add(UpdateOperators.set("type", "c"));
        updates.add(UpdateOperators.setOnInsert(Map.of("cubeID", cube.getId())));
        updates.add(UpdateOperators.setOnInsert(Map.of("timestamp", ts)));
        if (v > -1) { // interval is finished - set value = dur, if -1 - do not save it
            updates.add(UpdateOperators.set("value", v)); // do not update timestamp
        }

        qm.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute(new UpdateOptions().upsert(true));

        if (v > -1) { // after update of interval - fix the previous record
            fixNotFinishedCycleReport(cube.getId()); // finish unfinished interval - set to 1 minute
        }
    }

    private void fixNotFinishedCycleReport(ObjectId cubeID) {
        vertx.executeBlocking(op -> {
            Query<CubeMeter> q = datastore.find(CubeMeter.class);
            q.filter(
                Filters.and(
                    Filters.eq("cubeID", cubeID),
                    Filters.eq("type", "c"),
                    Filters.exists("value").not()
                )
            );

            UpdateOperator upd = UpdateOperators.set("value", 60);
            q.update(upd).execute();

            op.complete();
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }


    private void doBusinessWithDevice(String deviceID, JsonObject dataJSON) {
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(Filters.eq("deviceID", deviceID));
        q.filter(Filters.eq("deviceType", "l"));

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube != null) {
                Query<CubeReport> qr = datastore.find(CubeReport.class);
                qr.filter(Filters.eq("cubeID", cube.getId()));

                CubeReport cr = qr.first();
                if (cr != null) {
                    // do "cycle" report
                    if (cr.getType().equalsIgnoreCase("c")) {
                        String work_mode = dataJSON.getString("Work_mode");
                        if (work_mode != null && work_mode.equalsIgnoreCase("2ACI+2AVI")) { // MOD 1
                            Instant ts = Instant.now().truncatedTo(SECONDS);

                            String DI1_status = dataJSON.getString("DI1_status"); // "L" or "H"
                            String start_edge = cr.getEdge(); // "f" or "r"
                            // compare start edge and DI status and create an entry
                            if (DI1_status != null && start_edge != null) {
                                // Start cycle
                                if (
                                        (start_edge.equalsIgnoreCase("f") && DI1_status.equalsIgnoreCase("L"))
                                        ||
                                        (start_edge.equalsIgnoreCase("r") && DI1_status.equalsIgnoreCase("H"))
                                ) {
                                    if (cr.getTs() == null) { // cycle not started yet?
                                        qr.update(UpdateOperators.set("ts", ts)).execute();
                                        // create an open meter record with no value
                                        CubeMeter meter = new CubeMeter();
                                        meter.setCubeID(cube.getId());
                                        meter.setType("c");
                                        meter.setTimestamp(ts);
                                        meter.setValue(null); // no duration
                                        datastore.save(meter);
                                    }
                                }
                                // Finish cycle
                                if (
                                        (start_edge.equalsIgnoreCase("f") && DI1_status.equalsIgnoreCase("H"))
                                        ||
                                        (start_edge.equalsIgnoreCase("r") && DI1_status.equalsIgnoreCase("L"))
                                ) {
                                    if (cr.getTs() != null) { // cycle started and timestamp is marked?
                                        Duration dur = Duration.between(cr.getTs(), ts);
                                        // update an open record with duration
                                        Query<CubeMeter> qm = datastore.find(CubeMeter.class);
                                        qm.filter(
                                                Filters.and(
                                                        Filters.eq("cubeID", cube.getId()),
                                                        Filters.eq("type", "c"),
                                                        Filters.eq("timestamp", cr.getTs())
                                                )
                                        );
                                        qm.update(UpdateOperators.set("value", (double) dur.toSeconds())).execute();
                                        // remove start timestamp of cycle from report - waiting for the next
                                        qr.update(UpdateOperators.unset("ts")).execute();
                                    }
                                }
                            }
                        }
                    }

                    // do "pulses" report
                    if (cr.getType().equalsIgnoreCase("p")) {
                        String work_mode = dataJSON.getString("Work_mode");
/*
                        if (work_mode != null && work_mode.equalsIgnoreCase("Count mode 1")) { // MOD 3
                            Double lastCounter1 = cube.getLastCounter1();
                            if (lastCounter1 == null) { // start to count from 0
                                q.update(UpdateOperators.set("lastCounter1", (double) 0)).execute();
                                vertx.eventBus().send("loracubetest",
                                        new JsonObject()
                                                .put("to", cube.getDeviceID())
                                                .put("cmd", "CLRCOUNT")
                                );
                            } else {
                                Instant t = Instant.now().truncatedTo(SECONDS);
                                Double v = dataJSON.getDouble("Count1_times");

                                if (v != null) {
                                    if (v >= lastCounter1) { // keep report and
                                        // keep last counter for next report
                                        q.update(UpdateOperators.set("lastCounter1", v)).execute();
                                        // keep meter report
                                        CubeMeter meter = new CubeMeter();
                                        meter.setCubeID(cube.getId());
                                        meter.setType("p");
                                        meter.setTimestamp(t);
                                        meter.setValue(v - lastCounter1);
                                        datastore.save(meter);
                                    } else { // overflow? - reset to 0 and start again
                                        q.update(UpdateOperators.set("lastCounter1", (double) 0)).execute();
                                        vertx.eventBus().send("loracubetest",
                                                new JsonObject()
                                                        .put("to", cube.getDeviceID())
                                                        .put("cmd", "CLRCOUNT")
                                        );
                                    }
                                }
                            }
                        }
*/
                    }
                }

/*
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
*/
            }
            op.complete();
        }, res -> log.info("DeviceID: {} device data is handeled", deviceID));
    }

}
