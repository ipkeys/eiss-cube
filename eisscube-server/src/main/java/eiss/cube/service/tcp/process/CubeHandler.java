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
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.Key;
import xyz.morphia.query.Query;
import xyz.morphia.query.UpdateOperations;
import xyz.morphia.query.UpdateResults;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Slf4j
public class CubeHandler implements Handler<NetSocket> {

    private static Map<String, String> clientMap = new HashMap<>();

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
            String to = json.getString("to");
            String command = json.getString("cmd");

            log.info("Sending to: {}, message: {}, id: {}", to, command, id);
            send(id, to, command);
        });
        eventBus.<JsonObject>consumer("eisscubetest", message -> {
            JsonObject json = message.body();

            String to = json.getString("to");
            String command = json.getString("cmd");

            log.info("Sending to: {}, message: {}", to, command);
            send(to, command);
        });

    }

    // send command to EISScube (save record for history)
    private void send(final String id, final String to, final String command) {
        Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
        q.criteria("_id").equal(new ObjectId(id));

        UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

        String writeHandlerID = clientMap.get(to);
        if (writeHandlerID != null) {
            Buffer outBuffer = Buffer.buffer();
            outBuffer.appendString(command).appendString("\0");
            eventBus.send(writeHandlerID, outBuffer);

            ops.set("status", "completed");
        } else {
            log.error("Client: {} disconnected", to);

            ops.set("status", "pending");
        }

        ops.set("updated", Instant.now());

        vertx.executeBlocking(future -> {
            UpdateResults result = datastore.update(q, ops);
            future.complete(result);
        }, res -> log.info("Served cube command id: {}", id));
    }

    // send command to EISScube (no save record)
    private void send(final String to, final String command) {
        String writeHandlerID = clientMap.get(to);
        if (writeHandlerID != null) {
            Buffer outBuffer = Buffer.buffer();
            outBuffer.appendString(command).appendString("\0");
            eventBus.send(writeHandlerID, outBuffer);
       } else {
            log.error("Client: {} disconnected", to);
        }
    }

    @Override
    public void handle(NetSocket socket) {

        socket.handler(buffer -> {
            String packet = buffer.getString(0, buffer.length());
            Arrays.stream(packet.split("\0")).forEach(message -> {
                if (!message.isEmpty()) {
                    log.info("Got from Client: {}", message);
                    // auth
                    if (message.contains("auth")) {
                        doAuth(socket, message);
                    } else
                    // ping-pong
                    if (message.equalsIgnoreCase("I")) {
                        doPing(socket);
                    } else
                    // other messages
                    {
                        parseMessage(socket, message);
                    }
                }
            });
        }).closeHandler(h -> goOffline(socket)).exceptionHandler(h -> {
            log.error("Socket problem: {}", h.getMessage());
            socket.close();
        });

        // let EISSCube 3 sec to establish connection and send "auth" request
        vertx.executeBlocking(future -> {
            new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        socket.write("auth\0");
                    }
                },3000);
            future.complete();
        }, res -> log.info("Client: {} is connected! Waiting for auth!", socket.writeHandlerID()));
    }

    private void doAuth(NetSocket socket, String message) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String deviceID = parts[1]; // SIM card number
            if (deviceID != null) {
                log.info("Client: {} with deviceID {}", socket.writeHandlerID(), deviceID);

                // TODO: check auth!!
                clientMap.put(deviceID, socket.writeHandlerID());

                SimpleDateFormat dateFormat = new SimpleDateFormat("z MM/dd/yyyy HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                // Send greeting for a new connection.
                String welcome =
                    "Welcome to EISScube Server!\n" +
                    "Server time is " + dateFormat.format(new Date()) + "\n\0";
                socket.write(welcome);

                updateCube(deviceID);
                getPendingCommands(deviceID);
            }
        }
    }

    private void updateCube(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            future.complete(cube);
        }, res -> {
            Instant timestamp = Instant.now();
            EISScube cube = (EISScube)res.result();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(randname.next()); // random name from dictionary
                cube.setOnline(Boolean.TRUE);
                cube.setTimeStarted(timestamp);
            } else {
                cube.setOnline(Boolean.TRUE);
                cube.setTimeStarted(timestamp);
            }
            saveEissCube(cube);
        });
    }

    private void saveEissCube(final EISScube cube) {
        vertx.executeBlocking(future -> {
            Key result = datastore.save(cube);
            future.complete(result);
        }, res -> log.info("Client: {} status is: Online", cube.getDeviceID()));
    }

    private void getPendingCommands(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            future.complete(cube);
        }, res_future -> {
            EISScube cube = (EISScube)res_future.result();
            if (cube != null) {
                Query<CubeCommand> qc = datastore.createQuery(CubeCommand.class);
                qc.and(
                    qc.criteria("cubeID").equal(cube.getId()),
                    qc.criteria("status").equal("pending")
                );
                qc.order("-created");

                vertx.executeBlocking(op -> {
                    List<CubeCommand> list = qc.asList();

                    list.forEach(cmd -> vertx.eventBus().send("eisscube", new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", deviceID)
                        .put("cmd", cmd.toString())));

                    op.complete();
                }, res_op -> log.debug("Got all pending commands for Client: {} ", deviceID));
            }
        });
    }

    private void doPing(final NetSocket socket) {
        socket.write("O\0");

        String deviceID = getDeviceID(socket.writeHandlerID());
        Instant timestamp = Instant.now();

        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
            .set("online", Boolean.TRUE)
            .set("lastPing", timestamp);

        vertx.executeBlocking(future -> {
            UpdateResults result = datastore.update(q, ops);
            future.complete(result);
        }, res -> log.info("Ping from Client: {} on: {}", deviceID, timestamp));
    }

    private void goOffline(final NetSocket socket) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        if (!deviceID.equalsIgnoreCase("Unknown")) {

            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("deviceID").equal(deviceID);

            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                .set("online", Boolean.FALSE);

            vertx.executeBlocking(future -> {
                UpdateResults result = datastore.update(q, ops);
                future.complete(result);
            }, res -> {
                log.debug("Client: {} status is: Offline", deviceID);
                clientMap.remove(deviceID);
            });

        } else {
            log.info("Socket for Unknown is closed");
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

        StringBuilder buf = new StringBuilder();

        buf.append(message);

        buf.append("\t").append(deviceID);

        // all reports reconrd contains 'ts' - timestamp
        if (message.contains("ts")) {
            buf.append(parseTimestampAndValue(message));
            saveReport(deviceID, message);
        }

        // status contains 'r', 'i', 'ss'
        if (message.contains("r") &&  message.contains("i") && message.contains("ss")) {
            saveStatus(deviceID, message);
        }

        log.info(buf.toString());
    }

    private String parseTimestampAndValue(String message) {
        StringBuilder buf = new StringBuilder();

        Instant date = null;
        String value = null;

        for (String part : message.split("&")) {
            if (part.startsWith("ts=")) {
                String timestamp = part.replace("ts=", "");
                date = Instant.ofEpochMilli(Long.valueOf(timestamp) * 1000);
            }
            if (part.startsWith("v=")) {
                value = part.replace("v=", "");
            }
            if (part.startsWith("dur=")) {
                value = part.replace("dur=", "");
            }
        }

        if (date != null) {
            buf.append("\t: ").append(date.toString());
        }

        if (value != null) {
            buf.append("\t: ").append(value);
        }

        return buf.toString();
    }

    private void saveReport(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            future.complete(cube);
        }, res_future -> {
            EISScube cube = (EISScube)res_future.result();
            if (cube != null) {
                vertx.executeBlocking(future -> {
                    Instant ts = null;
                    String v = null;
                    String type = "pulse"; // default

                    for (String part : message.split("&")) {
/*
                        if (part.startsWith("i=")) {
                            rid = deviceID + "_Meter_" + part.replace("i=", "");
                        }
*/
                        if (part.startsWith("ts=")) {
                            String timestamp = part.replace("ts=", "");
                            ts = Instant.ofEpochMilli(Long.valueOf(timestamp) * 1000);
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
                        cubeMeter.setCubeID(cube.getId().toString());
                        cubeMeter.setTimestamp(ts);
                        cubeMeter.setValue(Double.valueOf(v));
                        cubeMeter.setType(type);

                        Key result = datastore.save(cubeMeter);
                        future.complete(result);
                    } else {
                        future.fail(String.format("Cannot parse report from: %s", deviceID));
                    }
                }, res -> {
                    if (res.succeeded()) {
                        log.debug("Report from: {} saved in DB: {}", deviceID, res.result().toString());
                    } else {
                        log.debug("Report failed from: {} saved in DB: {}", deviceID, res.cause());
                    }
                });
            }
        });
    }

    // r=on&i=high&ss=3

    private void saveStatus(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            future.complete(cube);
        }, res_future -> {
            EISScube cube = (EISScube)res_future.result();
            if (cube != null) {
                vertx.executeBlocking(future -> {
                    String r = null;
                    String i = null;
                    String ss = null;

                    for (String part : message.split("&")) {
                        if (part.startsWith("r=")) {
                            r = part.replace("r=", "");
                        }
                        if (part.startsWith("i=")) {
                            i = part.replace("i=", "");
                        }
                        if (part.startsWith("ss=")) {
                            ss = part.replace("ss=", "");
                        }
                    }

                    if (r != null && i != null && ss != null) {
                        Query<CubeTest> qs = datastore.createQuery(CubeTest.class);
                        qs.criteria("cubeID").equal(cube.getId().toString());

                        UpdateOperations<CubeTest> ops = datastore.createUpdateOperations(CubeTest.class)
                            .set("r", Integer.valueOf(r))
                            .set("i", Integer.valueOf(i))
                            .set("ss", Integer.valueOf(ss));

                        datastore.update(qs, ops, true);
                        future.complete();
                    } else {
                        future.fail(String.format("Cannot update status of EISScube: %s", deviceID));
                    }
                }, res -> {
                    if (res.succeeded()) {
                        log.debug("Update status of EISScube: {}", deviceID);
                    } else {
                        log.debug("{}", res.cause());
                    }
                });
            }
        });
    }

}
