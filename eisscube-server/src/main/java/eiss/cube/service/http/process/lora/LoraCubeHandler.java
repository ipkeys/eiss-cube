package eiss.cube.service.http.process.lora;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import eiss.cube.config.AppConfig;
import eiss.cube.config.LoraServerConfig;
import eiss.cube.db.Cube;
import eiss.models.cubes.CubeCommand;
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
            Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

            sendToLoraServer(id, deviceID, command);

            ops.set("sent", Instant.now());
            ops.set("status", "Sending");
            datastore.update(q, ops);

            op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
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

            sendToLoraServer("", deviceID, command);

            op.complete(String.format("DeviceID: %s is ONLINE. Sending message: %s", deviceID, command));
        }, res -> {
            if (res.succeeded()) {
                log.info("{}", res.result());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }


    private void sendToLoraServer(final String id, final String deviceID, final String command) {
        String URI = cfg.getUrl() + "/api/devices/" + deviceID + "/queue";

        String hexStr = "";
        if (command.contains("c=ron")) {
            hexStr = "AwEB";
        }
        if (command.contains("c=roff")) {
            hexStr = "AwAA";
        }

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
                            Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
                            q.criteria("_id").equal(new ObjectId(id));

                            UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

                            ops.set("received", Instant.now());
                            ops.set("status", "Received");
                            datastore.update(q, ops);
                        }
                    }
                } else {
                    log.error("Message: {}", res.cause().getMessage());
                }
            });

    }

}
