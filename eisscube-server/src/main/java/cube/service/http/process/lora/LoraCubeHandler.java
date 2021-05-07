package cube.service.http.process.lora;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import cube.config.AppConfig;
import cube.config.LoraServerConfig;
import cube.db.Cube;
import cube.models.CubeCommand;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LoraCubeHandler {

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("z MM/dd/yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final LoraServerConfig cfg;
    private final Vertx vertx;
    private final EventBus eventBus;
    private final Datastore datastore;
    private final WebClient client;

    @Inject
    public LoraCubeHandler(AppConfig cfg, Vertx vertx, @Cube Datastore datastore, WebClient client) {
        this.cfg = cfg.getLoraServerConfig();
        this.vertx = vertx;
        this.datastore = datastore;
        this.client = client;

        eventBus = vertx.eventBus();

        eventBus.<JsonObject>consumer("loracube", message -> {
            JsonObject json = message.body();

            String id = json.getString("id");
            String deviceID = json.getString("to");
            String command = json.getString("cmd");

            send(id, deviceID, command);
        });

        eventBus.<JsonObject>consumer("loracubetest", message -> {
            JsonObject json = message.body();

            String deviceID = json.getString("to");
            String command = json.getString("cmd");

            sendNoStore(deviceID, command);
        });
    }

    // send command to LORAcube (save record for history)
    private void send(final String id, final String deviceID, final String command) {
        vertx.executeBlocking(op -> {
            Query<CubeCommand> q = datastore.find(CubeCommand.class);
            q.filter(Filters.eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            updates.add(UpdateOperators.set("sent", Instant.now()));
            updates.add(UpdateOperators.set("status", "Sending"));

            UpdateResult result = q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();
            if (result.getModifiedCount() == 1) {
                try {
                    sendToLoraDevice(id, deviceID, q.first()); // working from DB object, not a command string
                } catch (InterruptedException ignored) {
                }
                op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
            } else {
                op.fail(String.format("Cannot update status for command for DeviceID: %s. Message is: %s", deviceID, command));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("{}", res.result());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // send command to LORAcube (no save record)
    private void sendNoStore(final String deviceID, final String command) {
        vertx.executeBlocking(op -> {

            if (command.equalsIgnoreCase("CLRCOUNT")) {
                byte[] CLRCOUNT = {(byte) 0xA6, 0x01};
                String hexStr = Base64.getEncoder().encodeToString(CLRCOUNT);
                sendToLoraServer("", deviceID, hexStr);
            }

            op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
        }, res -> {
            if (res.succeeded()) {
                log.info("{}", res.result());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    private void sendToLoraDevice(final String id, final String deviceID, final CubeCommand cmd) throws InterruptedException {
        if (cmd != null) {
            // RESET device
            if (cmd.getCommand().equalsIgnoreCase("reboot")) {
                byte[] REBOOT = {0x04, (byte) 0xff};
                String hexStr = Base64.getEncoder().encodeToString(REBOOT);
                sendToLoraServer(id, deviceID, hexStr);
            }
            // RO1 (relay) ON
            if (cmd.getCommand().equalsIgnoreCase("ron")) {
                byte[] RON = {0x03, 0x01, 0x01};
                String hexStr = Base64.getEncoder().encodeToString(RON);
                sendToLoraServer(id, deviceID, hexStr);
            }
            // RO1 (relay) OFF
            if (cmd.getCommand().equalsIgnoreCase("roff")) {
                byte[] ROFF = {0x03, 0x00, 0x00};
                String hexStr = Base64.getEncoder().encodeToString(ROFF);
                sendToLoraServer(id, deviceID, hexStr);
            }
            // Count Pulses on DI1
            if (cmd.getCommand().equalsIgnoreCase("icp")) {
                String hexStr;
                // Step 1 - set TRAMSMIT interval
                int sec = cmd.getCompleteCycle();
                byte b1 = (byte) ((sec & 0xff0000) >> 16);
                byte b2 = (byte) ((sec & 0x00ff00) >> 8);
                byte b3 = (byte) (sec & 0x0000ff);
                byte[] TRANSMIT = {0x01, b1, b2, b3};
                hexStr = Base64.getEncoder().encodeToString(TRANSMIT);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 2 - set TRIGGER level
                String tr = cmd.getTransition();
                byte edge = (byte) (tr.equalsIgnoreCase("f") ? 0x00 : 0x01); // 0: falling edge; 1: rising edge
                byte[] TRIGGER = {0x09, 0x01, edge, 0x00, 0x32}; // hardcoded to 50 msec
                hexStr = Base64.getEncoder().encodeToString(TRIGGER);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 3 - set MOD = 2
                byte[] MOD2 = {0x0A, 0x02};
                hexStr = Base64.getEncoder().encodeToString(MOD2);
                sendToLoraServer(id, deviceID, hexStr);
            }
            // Count Cycles on DI1
            if (cmd.getCommand().equalsIgnoreCase("icc")) {
                String hexStr;
                // Step 1 - set TRAMSMIT interval to hardcoded 30 minutes
                int sec = 1800;
                byte b1 = (byte) ((sec & 0xff0000) >> 16);
                byte b2 = (byte) ((sec & 0x00ff00) >> 8);
                byte b3 = (byte) (sec & 0x0000ff);
                byte[] TRANSMIT = {0x01, b1, b2, b3};
                hexStr = Base64.getEncoder().encodeToString(TRANSMIT);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 2 - enable DI1 as trigger
                byte[] DTRI = {(byte) 0xAA, 0x02, 0x01, 0x00};
                hexStr = Base64.getEncoder().encodeToString(DTRI);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 3 - set TRIGGER level
                byte[] TRIGGER = {0x09, 0x01, 0x02, 0x00, 0x64}; // falling and raising edge(for MOD=1) with debounceing to 100 msec
                hexStr = Base64.getEncoder().encodeToString(TRIGGER);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 4 - enable tigger MOD 6
                byte[] ADDMOD6 = {0x0A, 0x06, 0x01};
                hexStr = Base64.getEncoder().encodeToString(ADDMOD6);
                sendToLoraServer(id, deviceID, hexStr);

                TimeUnit.SECONDS.sleep(2);

                // Step 5 - set MOD = 1
                byte[] MOD1 = {0x0A, 0x01};
                hexStr = Base64.getEncoder().encodeToString(MOD1);
                sendToLoraServer(id, deviceID, hexStr);
            }

        }

    }

    private void sendToLoraServer(final String id, final String deviceID, final String hexStr) {
        String URI = cfg.getUrl() + "/api/devices/" + deviceID + "/queue";

        JsonObject payload = new JsonObject();
        payload.put("confirmed", true);
        payload.put("data", hexStr);
        payload.put("fPort", 2); // Application port = 2

        client
            .postAbs(URI)
            .putHeader("Grpc-Metadata-Authorization", cfg.getApiToken())
            .sendJsonObject(new JsonObject().put("deviceQueueItem", payload), res -> {
                if (res.succeeded()) {
                    HttpResponse<Buffer> response = res.result();
                    log.info("Status code: {}, message: {}", response.statusCode(), response.statusMessage());
                    if (response.statusCode() == 200) {
                        log.info("done");
                        if (!id.isEmpty()) {
                            Query<CubeCommand> q = datastore.find(CubeCommand.class);
                            q.filter(Filters.eq("_id", new ObjectId(id)));

                            List<UpdateOperator> updates = new ArrayList<>();
                            updates.add(UpdateOperators.set("received", Instant.now()));
                            updates.add(UpdateOperators.set("status", "Received"));

                            q.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();
                        }
                    }
                } else {
                    log.error("Message: {}", res.cause().getMessage());
                }
            });

    }

}
