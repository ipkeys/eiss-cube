package eiss.cube.service.tcp.process;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import eiss.cube.randname.Randname;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeMeter;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.updates.UpdateOperators.set;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
public class CubeHandler implements Handler<NetSocket> {

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("z MM/dd/yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final Vertx vertx;
    private final EventBus eventBus;
    private final Datastore datastore;
    private final Randname randname;

    @Inject
    public CubeHandler(Vertx vertx, @Cubes Datastore datastore, Randname randname) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.randname = randname;

        eventBus = vertx.eventBus();

        eventBus.<JsonObject>consumer("eisscube", message -> {
            JsonObject json = message.body();

            String id = json.getString("id");
            String deviceID = json.getString("to");
            String socket = json.getString("socket");
            String command = json.getString("cmd");

            send(id, deviceID, socket, command);
        });

        eventBus.<JsonObject>consumer("eisscubetest", message -> {
            JsonObject json = message.body();

            String deviceID = json.getString("to");
            String socket = json.getString("socket");
            String command = json.getString("cmd");

            sendNoStore(deviceID, socket, command);
        });
    }

    // send command to EISScube (save record for history)
    private void send(final String id, final String deviceID, final String socket, final String command) {
        vertx.executeBlocking(op -> {
            Query<CubeCommand> q = datastore.find(CubeCommand.class);
            q.filter(eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            if (socket != null && !socket.isEmpty()) {
                Buffer outBuffer = Buffer.buffer();
                outBuffer.appendString(command).appendString("\0");

                eventBus.send(socket, outBuffer);

                updates.add(set("sent", Instant.now()));
                updates.add(set("status", "Sending"));

                q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

                op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
            } else {
                updates.add(set("status", "Pending"));

                q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

                op.fail(String.format("DeviceID: %s is OFFLINE. Pending message: %s", deviceID, command));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("{}", res.result());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // send command to EISScube (no save record)
    private void sendNoStore(final String deviceID, final String socket, final String command) {
        if (socket != null && !socket.isEmpty()) {
            Buffer outBuffer = Buffer.buffer();
            outBuffer.appendString(command).appendString("\0");

            eventBus.send(socket, outBuffer);

            log.info("DeviceID: {} is ONLINE. Sending message: {}", deviceID, command);
       } else {
            log.info("DeviceID: {} is OFFLINE. Dropped message: {}", deviceID, command);
        }
    }

    public void setAllDevicesOffline() {
        vertx.executeBlocking(op -> {
            Query<EISScube> q = datastore.find(EISScube.class);
            List<UpdateOperator> updates = new ArrayList<>();

            updates.add(set("socket", ""));
            updates.add(set("online", FALSE));

            // update All documents
            q.update(new UpdateOptions().multi(TRUE), updates.toArray(UpdateOperator[]::new));

            op.complete();
        }, res -> log.info("Server was restarted! Set status of ALL devices to OFFLINE"));
    }

    @Override
    public void handle(final NetSocket netSocket) {

        final RecordParser parser = RecordParser.newDelimited("\0", h -> {
            String message = h.toString();
            if (message.contains("auth")) { // auth
                doAuth(netSocket, message);
            } else if (message.equalsIgnoreCase("I")) { // ping-pong
                doPing(netSocket);
            } else { // other messages
                parseMessage(netSocket, message);
            }
        });

        netSocket
            .handler(parser)
            .closeHandler(h ->
                log.info("Socket closed")
            )
            .exceptionHandler(h -> {
                log.info("Socket problem: {}", h.getMessage());
                goOffline(netSocket.writeHandlerID());
                netSocket.close();
            });

        // let EISSCube 10 sec to establish connection and send "auth" request
        vertx.setTimer(10000, id -> {
            Buffer outBuffer = Buffer.buffer().appendString("auth").appendString("\0");
            eventBus.send(netSocket.writeHandlerID(), outBuffer);
            log.info("Who is connected?...");
        });
    }

    private void doAuth(NetSocket netSocket, String message) {
        log.info("Authentication. Message: {}", message);

        String[] parts = message.split(" ");
        if (parts.length == 3) {
            String deviceID = parts[1]; // SIM card number
            String ss = parts[2]; // signal strength
            if (deviceID != null && ss != null) {
                String socket = netSocket.writeHandlerID();

                // Send greeting for a new connection.
                Buffer outBuffer = Buffer.buffer()
                    .appendString("Welcome to EISSCube Server!\n")
                    .appendString("Server time: ")
                    .appendString(df.format(Instant.now()))
                    .appendString("\n\n\0");

                eventBus.send(socket, outBuffer);

                updateCube(deviceID, socket, ss);

                getWaitingCommands(deviceID, "Pending");
                getWaitingCommands(deviceID, "Sending");
            }
        }
    }

    private void updateCube(String deviceID, String socket, String ss) {
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(eq("deviceID", deviceID));

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(randname.next()); // random name from dictionary
                cube.setSocket(socket);
                cube.setDeviceType("e");
            }

            Instant timestamp = Instant.now();

            cube.setOnline(TRUE);
            cube.setSignalStrength(Integer.valueOf(ss));
            cube.setTimeStarted(timestamp);
            cube.setLastPing(timestamp);
            cube.setSocket(socket);
            cube.setDeviceType("e");

            datastore.save(cube);

            op.complete();
        }, res -> log.info("DeviceID: {} is ONLINE", deviceID));
    }

    private void getWaitingCommands(String deviceID, String status) {
        Query<EISScube> q = datastore.find(EISScube.class);
        q.filter(eq("deviceID", deviceID));

        vertx.executeBlocking(future -> {
            EISScube cube = q.first();
            if (cube != null) {
                Query<CubeCommand> qc = datastore.find(CubeCommand.class);
                qc.filter(
                    and(
                        eq("cubeID", cube.getId()),
                        eq("status", status)
                    )
                );
                FindOptions o = new FindOptions();
                o.sort(Sort.descending("created"));

                List<CubeCommand> list = qc.iterator(o).toList();
                list.forEach(cmd ->
                    eventBus.send("eisscube", new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", cube.getDeviceID())
                        .put("socket", cube.getSocket())
                        .put("cmd", cmd.toString()))
                );
            }

            future.complete();
        }, res_future -> log.info("DeviceID: {} served pending commands", deviceID));
    }

    private void doPing(final NetSocket netSocket) {
        vertx.executeBlocking(op -> {
            String socket = netSocket.writeHandlerID();

            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("socket", socket));

            EISScube cube = q.first();
            if (cube != null) {
                List<UpdateOperator> updates = new ArrayList<>();

                updates.add(set("online", TRUE));
                updates.add(set("lastPing", Instant.now()));

                q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

                Buffer outBuffer = Buffer.buffer()
                    .appendString("O")
                    .appendString("\0");

                eventBus.send(socket, outBuffer);

                op.complete(cube.getDeviceID());
            } else {
                op.fail(String.format("Socket: %s is not belong to EISSCube", socket));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("DeviceID: {} - Ping...Pong...", res.result());
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }

    private void goOffline(final String socket) {
        vertx.executeBlocking(op -> {
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("socket", socket));

            EISScube cube = q.first();
            if (cube != null) {
                List<UpdateOperator> updates = new ArrayList<>();

                updates.add(set("online", FALSE));

                q.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

                op.complete(cube.getDeviceID());
            } else {
                op.fail(String.format("Socket: %s is not belong to EISSCube", socket));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("DeviceID: {} is OFFLINE", res.result());
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }

    private void parseMessage(final NetSocket netSocket, String message) {
        String socket = netSocket.writeHandlerID();

        // acknowledgment contains 'ack=id' - CubeCommand id
        if (message.contains("ack=")) {
            acknowledgeCommand(socket, message);
        }

        // all reports contains 'rpt-ts' - report-timestamp
        if (message.contains("rpt-ts=")) {
            saveReport(socket, message);
        }

        // all statuses contains 'sts-ts' - status-timestamp
        if (message.contains("sts-ts=")) {
            saveStatus(socket, message);
        }
    }

    private void acknowledgeCommand(String socket, String message) {
        if (!message.contains("ack=test")) { // ignore test
            vertx.executeBlocking(op -> {
                Query<EISScube> q = datastore.find(EISScube.class);
                q.filter(eq("socket", socket));

                EISScube cube = q.first();
                if (cube != null) {
                    String deviceID = cube.getDeviceID();

                    String id = message.replace("ack=", "");
                    if (ObjectId.isValid(id)) {
                        UpdateResult result = datastore.find(CubeCommand.class)
                            .filter(eq("_id", new ObjectId(id)))
                            .update(new UpdateOptions(),
                                set("received", Instant.now()),
                                set("status", "Received")
                            );
                        log.debug("{}", result);
                        op.complete(String.format("DeviceID: %s acknowledge the command id: %s", deviceID, id));
                    } else {
                        op.fail(String.format("DeviceID: %s NOT acknowledge the command id: %s", deviceID, id));
                    }
                } else {
                    op.fail(String.format("DeviceID: not found on socket: %s", socket));
                }
            }, res -> {
                if (res.succeeded()) {
                    log.info("{}", res.result());
                } else {
                    log.info(res.cause().getMessage());
                }
            });
        }
    }

    private void saveReport(String socket, String message) {
        vertx.executeBlocking(op -> {
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(Filters.eq("socket", socket));

            EISScube cube = q.first();
            if (cube != null) {
                Instant ts = null;
                String v = null;
                String type = "p"; // pulse - default

                for (String part : message.split("&")) {
                    if (part.startsWith("rpt-ts=")) {
                        String timestamp = part.replace("rpt-ts=", "");
                        ts = Instant.ofEpochSecond(Long.parseLong(timestamp));
                    }
                    if (part.startsWith("v=")) {
                        v = part.replace("v=", "");
                    }
                    if (part.startsWith("dur=")) {
                        type = "c"; // cycle
                        v = part.replace("dur=", "");
                    }
                }

                String deviceID = cube.getDeviceID();

                if (ts != null && v != null) {
                    Query<CubeMeter> qm = datastore.find(CubeMeter.class);
                    qm.filter(
                        and(
                            eq("cubeID", cube.getId()),
                            eq("timestamp", ts)
                        )
                    );

                    List<UpdateOperator> updates = new ArrayList<>();

                    updates.add(UpdateOperators.setOnInsert(Map.of("cubeID", cube.getId())));
                    updates.add(UpdateOperators.setOnInsert(Map.of("timestamp", ts)));
                    updates.add(set("type", type));
                    if (!v.equalsIgnoreCase("z")) { // interval is finished - set value = dur
                        updates.add(set("value", Double.valueOf(v)));
                    }

                    qm.update(new UpdateOptions().upsert(TRUE), updates.toArray(UpdateOperator[]::new));

                    if (!v.equalsIgnoreCase("z")) { // after update of interval - fix the previous record
                        fixNotFinishedCycleReport(cube.getId()); // finish unfinished interval - set to 1 minute
                    }
                    op.complete(String.format("DeviceID: %s report saved", deviceID));
                } else {
                    op.fail(String.format("DeviceID: %s report NOT saved", deviceID));
                }
            } else {
                op.fail(String.format("DeviceID: not found on socket: %s", socket));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }

    private void fixNotFinishedCycleReport(ObjectId cubeID) {
        vertx.executeBlocking(op -> {
            Query<CubeMeter> q = datastore.find(CubeMeter.class);
            q.filter(
                and(
                    eq("cubeID", cubeID),
                    eq("type", "c"),
                    exists("value").not()
                )
            );
            q.update(new UpdateOptions(), set("value", 60));

            op.complete();
        }, res -> log.info("fixNotFinishedCycleReport for {}", cubeID));
    }

    // r=1&i=0&ss=3
    private void saveStatus(String socket, String message) {
        vertx.executeBlocking(op -> {
            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(Filters.eq("socket", socket));

            EISScube cube = q.first();
            if (cube != null) {
                Instant ts = null;
                String r = null;
                String i = null;

                for (String part : message.split("&")) {
                    if (part.startsWith("sts-ts=")) {
                        String timestamp = part.replace("sts-ts=", "");
                        ts = Instant.ofEpochSecond(Long.parseLong(timestamp));
                    }
                    if (part.startsWith("r=")) {
                        r = part.replace("r=", "");
                    }
                    if (part.startsWith("i=")) {
                        i = part.replace("i=", "");
                    }
                }

                String deviceID = cube.getDeviceID();

                if (ts != null && r != null && i != null) {
                    CubeTest cubeTest = new CubeTest();
                    cubeTest.setCubeID(cube.getId());
                    cubeTest.setTimestamp(ts);
                    cubeTest.setR(Integer.valueOf(r));
                    cubeTest.setI(Integer.valueOf(i));

                    datastore.save(cubeTest);
                    op.complete(String.format("DeviceID: %s status saved", deviceID));
                } else {
                    op.fail(String.format("DeviceID: %s status NOT saved", deviceID));
                }
            } else {
                op.fail(String.format("DeviceID: not found on socket: %s", socket));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }

}
