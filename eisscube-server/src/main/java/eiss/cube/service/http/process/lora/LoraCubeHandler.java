package eiss.cube.service.http.process.lora;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.config.AppConfig;
import eiss.config.LoraServerConfig;
import eiss.cube.utils.CubeCommandToJson;
import eiss.models.cubes.CubeCommand;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.db.Cubes;
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
import java.util.List;

@Slf4j
public class LoraCubeHandler {

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("z MM/dd/yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final LoraServerConfig cfg;
    private final Vertx vertx;
    private final Datastore datastore;
    private final WebClient client;

    @Inject
    public LoraCubeHandler(AppConfig cfg, Vertx vertx, @Cubes Datastore datastore, WebClient client) {
        this.cfg = cfg.getLoraServerConfig();
        this.vertx = vertx;
        this.datastore = datastore;
        this.client = client;

        EventBus eventBus = vertx.eventBus();

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

            CubeCommand cmd = new CubeCommand(command);
            try {
                sendToLoraDevice("", deviceID, cmd);
            } catch (InterruptedException ignored) {
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
            JsonObject payload = new JsonObject();
            payload.put("confirmed", false); // TODO: use in future - confirmation from device
            payload.put("fPort", 2); // Application port = 2
            payload.put("jsonObject", CubeCommandToJson.convert(cmd));

            sendToLoraServer(id, deviceID, payload);
        }
    }

    private void sendToLoraServer(final String id, final String deviceID, final JsonObject payload) {
        String URI = cfg.getUrl() + "/api/devices/" + deviceID + "/queue";

        log.info("API Payload: {}", payload.encodePrettily());

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
