package eiss.cube.service.tcp.process;

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
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class CubeHandler implements Handler<NetSocket> {

    private static Map<String, String> clientMap = new HashMap<>();
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
            String command = json.getString("cmd");

            send(id, deviceID, command);
        });
        eventBus.<JsonObject>consumer("eisscubetest", message -> {
            JsonObject json = message.body();

            String deviceID = json.getString("to");
            String command = json.getString("cmd");

            sendNoStore(deviceID, command);
        });

    }

    // send command to EISScube (save record for history)
    private void send(final String id, final String deviceID, final String command) {
        vertx.executeBlocking(future -> {
            Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

            String writeHandlerID = clientMap.get(deviceID);
            if (writeHandlerID != null) {
                Buffer outBuffer = Buffer.buffer();
                outBuffer.appendString(command).appendString("\0");

                eventBus.send(writeHandlerID, outBuffer);

                ops.set("sent", Instant.now());
                ops.set("status", "Sending");
                datastore.update(q, ops);

                future.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
            } else {
                ops.set("status", "Pending");
                datastore.update(q, ops);

                future.fail(String.format("DeviceID: %s is OFFLINE. Pending message: %s", deviceID, command));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // send command to EISScube (no save record)
    private void sendNoStore(final String deviceID, final String command) {
        String writeHandlerID = clientMap.get(deviceID);
        if (writeHandlerID != null) {
            Buffer outBuffer = Buffer.buffer();
            outBuffer.appendString(command).appendString("\0");

            eventBus.send(writeHandlerID, outBuffer);

            log.info("DeviceID: {} is ONLINE. Sending message: {}", deviceID, command);
       } else {
            log.info("DeviceID: {} is OFFLINE. Dropped message: {}", deviceID, command);
        }
    }

    @Override
    public void handle(NetSocket socket) {

        final RecordParser parser = RecordParser.newDelimited("\0", h -> {
            String message = h.toString();
            if (message.contains("auth")) { // auth
                doAuth(socket, message);
            } else if (message.equalsIgnoreCase("I")) { // ping-pong
                doPing(socket);
            } else { // other messages
                parseMessage(socket, message);
            }
        });

        socket
            .handler(parser)
            .closeHandler(h -> {
                log.error("Socket closed");
                goOffline(socket);
            })
            .exceptionHandler(h -> {
                log.error("Socket problem: {}", h.getMessage());
                socket.close();
            });

        // let EISSCube 15 sec to establish connection and send "auth" request
        vertx.setTimer(15000, id -> {
            Buffer outBuffer = Buffer
                    .buffer()
                    .appendString("auth").appendString("\0");

            eventBus.send(socket.writeHandlerID(), outBuffer);

            log.info("Who is connected?...");
        });
    }

    private void doAuth(NetSocket socket, String message) {
        log.info("Authentication. Message: {}", message);

        String[] parts = message.split(" ");
        if (parts.length == 3) {
            String deviceID = parts[1]; // SIM card number
            String ss = parts[2]; // signal strength
            if (deviceID != null && ss != null) {
                String addr = socket.writeHandlerID();

                clientMap.put(deviceID, addr);

                // Send greeting for a new connection.
                Buffer outBuffer = Buffer
                        .buffer()
                        .appendString("Welcome to EISSCube Server!\n")
                        .appendString("Server time: ")
                        .appendString(df.format(Instant.now()))
                        .appendString("\n\n\0");

                eventBus.send(addr, outBuffer);

                updateCube(deviceID, ss);

                getPendingCommands(deviceID);
            }
        }
    }

    private void updateCube(String deviceID, String ss) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(randname.next()); // random name from dictionary
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setSignalStrength(Integer.valueOf(ss));
            cube.setTimeStarted(timestamp);
            cube.setLastPing(timestamp);

            datastore.save(cube);

            future.complete();
        }, res -> log.info("DeviceID: {} is ONLINE", deviceID));
    }

    private void getPendingCommands(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube != null) {
                Query<CubeCommand> qc = datastore.createQuery(CubeCommand.class);
                qc.and(
                    qc.criteria("cubeID").equal(cube.getId()),
                    qc.criteria("status").equal("Pending")
                );
                qc.order("-created");

                List<CubeCommand> list = qc.asList();

                list.forEach(cmd ->
                    eventBus.send("eisscube", new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", deviceID)
                        .put("cmd", cmd.toString()))
                );
            }

            future.complete();
        }, res_future -> log.info("DeviceID: {} served pending commands", deviceID));
    }

    private void doPing(final NetSocket socket) {
        String addr = socket.writeHandlerID();
        String deviceID = getDeviceID(addr);

        if (!deviceID.equalsIgnoreCase("Unknown")) {
            Instant timestamp = Instant.now();
            vertx.executeBlocking(future -> {

                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.criteria("deviceID").equal(deviceID);

                UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                        .set("online", Boolean.TRUE)
                        .set("lastPing", timestamp);

                datastore.update(q, ops);
                future.complete();
            }, res -> {
                Buffer outBuffer = Buffer
                        .buffer()
                        .appendString("O")
                        .appendString("\0");

                eventBus.send(addr, outBuffer);

                log.info("DeviceID: {} - Ping...Pong...", deviceID);
            });
        } else {
            log.info("DeviceID: Unknown - Ping...Pong...");
        }
    }

    private void goOffline(final NetSocket socket) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        if (!deviceID.equalsIgnoreCase("Unknown")) {
            vertx.executeBlocking(future -> {
                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.criteria("deviceID").equal(deviceID);

                UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                    .set("online", Boolean.FALSE);

                datastore.update(q, ops);
                future.complete();
            }, res -> {
                log.debug("DeviceID: {} is OFFLINE", deviceID);
                clientMap.remove(deviceID);
            });
        } else {
            log.info("DeviceID: Unknown - socket is closed");
        }
    }

    private String getDeviceID(final String writeHandlerID) {
        Optional<String> deviceID = clientMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equalsIgnoreCase(writeHandlerID))
            .map(Map.Entry::getKey).findFirst();

        return deviceID.orElse("Unknown");
    }

    private void parseMessage(final NetSocket socket, String message) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        log.info("DeviceID: {} Message: {}", deviceID, message);

        // acknowledgment contains 'ack=id' - CubeCommand id
        if (message.contains("ack=")) {
            acknowledgeCommand(deviceID, message);
        }

        // all reports contains 'rpt-ts' - report-timestamp
        if (message.contains("rpt-ts=")) {
            saveReport(deviceID, message);
        }

        // all statuses contains 'sts-ts' - status-timestamp
        if (message.contains("sts-ts=")) {
            saveStatus(deviceID, message);
        }

    }

    private void acknowledgeCommand(String deviceID, String message) {
        if (!message.contains("ack=test")) { // ignore test
            vertx.executeBlocking(future -> {
                String id = message.replace("ack=", "");

                if (ObjectId.isValid(id)) {
                    Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
                    q.criteria("_id").equal(new ObjectId(id));

                    UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);
                    ops.set("received", Instant.now());
                    ops.set("status", "Received");
                    datastore.update(q, ops);

                    future.complete(String.format("DeviceID: %s acknowledge the command id: %s", deviceID, id));
                } else {
                    future.fail(String.format("DeviceID: %s NOT acknowledge the command id: %s", deviceID, id));
                }
            }, res -> {
                if (res.succeeded()) {
                    log.info(res.result().toString());
                } else {
                    log.info(res.cause().getMessage());
                }
            });
        }
    }

    private void saveReport(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
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
                    future.complete(String.format("DeviceID: %s report saved", deviceID));
                } else {
                    future.fail(String.format("DeviceID: %s report NOT saved", deviceID));
                }
            } else {
                future.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // r=1&i=0&ss=3
    private void saveStatus(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
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
                    future.complete(String.format("DeviceID: %s status saved", deviceID));
                } else {
                    future.fail(String.format("DeviceID: %s status NOT saved", deviceID));
                }
            } else {
                future.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

}
