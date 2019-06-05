package eiss.cube.service.tcp.process;

import dev.morphia.query.Sort;
import eiss.cube.randname.Randname;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeMeter;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
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
import dev.morphia.query.UpdateOperations;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class CubeHandler implements Handler<NetSocket> {

    //private static Map<String, String> clientMap = new HashMap<>();
    private static DateTimeFormatter df = DateTimeFormatter.ofPattern("z MM/dd/yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    private Vertx vertx;
    private EventBus eventBus;
    private Datastore datastore;
    private Randname randname;

    @Inject
    public CubeHandler(Vertx vertx, Datastore datastore, Randname randname) {
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
            Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

            //String writeHandlerID = clientMap.get(deviceID);
            if (socket != null && !socket.isEmpty()) {
                Buffer outBuffer = Buffer.buffer();
                outBuffer.appendString(command).appendString("\0");

                eventBus.send(socket, outBuffer);

                ops.set("sent", Instant.now());
                ops.set("status", "Sending");
                datastore.update(q, ops);

                op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
            } else {
                ops.set("status", "Pending");
                datastore.update(q, ops);

                op.fail(String.format("DeviceID: %s is OFFLINE. Pending message: %s", deviceID, command));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // send command to EISScube (no save record)
    private void sendNoStore(final String deviceID, final String socket, final String command) {
        //String writeHandlerID = clientMap.get(deviceID);
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
            Query<EISScube> q = datastore.createQuery(EISScube.class);
            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                .set("online", Boolean.FALSE);

            datastore.update(q, ops);

            op.complete();
        }, res -> log.info("Server is shutdown! Set status of all EISScubes to OFFLINE"));
    }

    @Override
    public void handle(NetSocket netSocket) {

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
            .closeHandler(h -> {
                log.error("Socket closed");
                goOffline(netSocket);
            })
            .exceptionHandler(h -> {
                log.error("Socket problem: {}", h.getMessage());
                netSocket.close();
            });

        // let EISSCube 15 sec to establish connection and send "auth" request
        vertx.setTimer(15000, id -> {
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

//                clientMap.put(deviceID, addr);

                // Send greeting for a new connection.
                Buffer outBuffer = Buffer
                        .buffer()
                        .appendString("Welcome to EISSCube Server!\n")
                        .appendString("Server time: ")
                        .appendString(df.format(Instant.now()))
                        .appendString("\n\n\0");

                eventBus.send(socket, outBuffer);

                updateCube(deviceID, socket, ss);

                getPendingCommands(deviceID);
            }
        }
    }

    private void updateCube(String deviceID, String socket, String ss) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(op -> {
            EISScube cube = q.first();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(randname.next()); // random name from dictionary
                cube.setSocket(socket);
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(Integer.valueOf(ss));
            cube.setTimeStarted(timestamp);
            cube.setLastPing(timestamp);
            cube.setSocket(socket);

            datastore.save(cube);

            op.complete();
        }, res -> log.info("DeviceID: {} is ONLINE", deviceID));
    }

    private void getPendingCommands(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.first();
            if (cube != null) {
                Query<CubeCommand> qc = datastore.createQuery(CubeCommand.class);
                qc.and(
                    qc.criteria("cubeID").equal(cube.getId()),
                    qc.criteria("status").equal("Pending")
                );
                qc.order(Sort.descending("created"));

                List<CubeCommand> list = qc.find().toList();
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

            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("socket").equal(socket);

            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                .set("online", Boolean.TRUE)
                .set("lastPing", Instant.now());

            datastore.update(q, ops);

            Buffer outBuffer = Buffer
                .buffer()
                .appendString("O")
                .appendString("\0");

            eventBus.send(socket, outBuffer);

            EISScube cube = q.first();
            op.complete(cube);
        }, res -> log.info("DeviceID: {} - Ping...Pong...", ((EISScube)res.result()).getDeviceID()));
    }

    private void goOffline(final NetSocket netSocket) {
        vertx.executeBlocking(op -> {
            String socket = netSocket.writeHandlerID();

            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("socket").equal(socket);

            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                .set("online", Boolean.FALSE);

            datastore.update(q, ops);

            EISScube cube = q.first();
            op.complete(cube);
        }, res -> log.debug("DeviceID: {} is OFFLINE", ((EISScube)res.result()).getDeviceID()));
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
                String deviceID = "Unknown";

                Query<EISScube> qc = datastore.createQuery(EISScube.class);
                qc.criteria("socket").equal(socket);

                EISScube cube = qc.first();
                if (cube != null) {
                    deviceID = cube.getDeviceID();
                }

                String id = message.replace("ack=", "");
                if (ObjectId.isValid(id)) {
                    Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
                    q.criteria("_id").equal(new ObjectId(id));

                    UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);
                    ops.set("received", Instant.now());
                    ops.set("status", "Received");
                    datastore.update(q, ops);

                    op.complete(String.format("DeviceID: %s acknowledge the command id: %s", deviceID, id));
                } else {
                    op.fail(String.format("DeviceID: %s NOT acknowledge the command id: %s", deviceID, id));
                }
            }, res -> {
                if (res.succeeded()) {
                    log.info(String.valueOf(res.result()));
                } else {
                    log.info(res.cause().getMessage());
                }
            });
        }
    }

    private void saveReport(String socket, String message) {
        vertx.executeBlocking(op -> {
            String deviceID ="Unknown";

            Query<EISScube> qc = datastore.createQuery(EISScube.class);
            qc.criteria("socket").equal(socket);

            EISScube cube = qc.first();
            if (cube != null) {
                Instant ts = null;
                String v = null;
                String type = "pulse"; // default

                for (String part : message.split("&")) {
                    if (part.startsWith("rpt-ts=")) {
                        String timestamp = part.replace("rpt-ts=", "");
                        ts = Instant.ofEpochSecond(Long.valueOf(timestamp));
                    }
                    if (part.startsWith("v=")) {
                        v = part.replace("v=", "");
                    }
                    if (part.startsWith("dur=")) {
                        type = "cycle";
                        v = part.replace("dur=", "");
                    }
                }

                if (ts != null && v != null) {
                    CubeMeter cubeMeter = new CubeMeter();
                    cubeMeter.setCubeID(cube.getId());
                    cubeMeter.setType(type);
                    cubeMeter.setTimestamp(ts);
                    cubeMeter.setValue(Double.valueOf(v));

                    datastore.save(cubeMeter);
                    op.complete(String.format("DeviceID: %s report saved", deviceID));
                } else {
                    op.fail(String.format("DeviceID: %s report NOT saved", deviceID));
                }
            } else {
                op.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // r=1&i=0&ss=3
    private void saveStatus(String socket, String message) {
        vertx.executeBlocking(op -> {
            String deviceID ="Unknown";

            Query<EISScube> qc = datastore.createQuery(EISScube.class);
            qc.criteria("socket").equal(socket);

            EISScube cube = qc.first();
            if (cube != null) {
                Instant ts = null;
                String r = null;
                String i = null;

                for (String part : message.split("&")) {
                    if (part.startsWith("sts-ts=")) {
                        String timestamp = part.replace("sts-ts=", "");
                        ts = Instant.ofEpochSecond(Long.valueOf(timestamp));
                    }
                    if (part.startsWith("r=")) {
                        r = part.replace("r=", "");
                    }
                    if (part.startsWith("i=")) {
                        i = part.replace("i=", "");
                    }
                }

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
                op.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(String.valueOf(res.result()));
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

}
