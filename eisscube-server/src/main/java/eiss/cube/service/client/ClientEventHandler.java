package eiss.cube.service.client;

import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.query.Query;
import eiss.client.api.EventHandler;
import eiss.cube.service.client.events.Event;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.EISScube;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

@Slf4j
public class ClientEventHandler implements EventHandler {

    private Map<String, Event> events = new HashMap<>();
    private Vertx vertx;
    private Datastore datastore;


    @Inject
    public ClientEventHandler(Vertx vertx, Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    private static String getEventId(Map<String, Object> event) {
        Object o = event.get("eventId");
        if (o == null || !String.class.isAssignableFrom(o.getClass())) {
            return null;
        }
        return (String)o;
    }

    public void oadrReceivedEvent(Map<String, Object> params) {
        String id = getEventId(params);
        if (id == null) return;

        Event e = events.getOrDefault(id, new Event());
        e.update(params);
        events.putIfAbsent(id, e);

        log.info(
                "\n=== Event received =========" +
                "\nID        : {}" +
                "\nstart     : {}" +
                "\nend       : {}" +
                "\nprogram   : {}" +
                "\nven       : {}" +
                "\nresources : {}" +
                "\n============================",
                id,
                e.getStart(),
                e.getEnd(),
                e.getProgram(),
                e.getVen(),
                String.join(",", e.getResources())
        );
    }

    public void oadrEventSignal(Map<String, Object> params) {
        String id = getEventId(params);
        if (id == null) return;

        Event e = events.getOrDefault(id, new Event());
        e.update(params);
        events.putIfAbsent(id, e);

        String signalType = e.getSignalType();
        String signalValue = e.getSignalValue();

        log.info(
                "\n=== Start signal ===========" +
                "\nID        : {}" +
                "\nstart     : {}" +
                "\nend       : {}" +
                "\nprogram   : {}" +
                "\nven       : {}" +
                "\nresources : {}" +
                "\n----------------------------" +
                "\ntype      : {}" +
                "\nvalue     : {}" +
                "\n============================",
                id,
                e.getStart(),
                e.getEnd(),
                e.getProgram(),
                e.getVen(),
                String.join(",", e.getResources()),
                signalType,
                signalValue
        );

        if (signalType.equalsIgnoreCase(Event.LEVEL)) {
            switch(signalValue.toLowerCase()) {
                case "normal":
                    break;
                case "moderate":
                case "high":
                case "special":
                    e.getResources().forEach(r -> sendCommandToCube(e.getVen(), r, "ron"));
                    break;
            }
        }
    }

    public void oadrEventModified(Map<String, Object> params) {

    }

    public void oadrSignalComplete(Map<String, Object> params) {
        String id = getEventId(params);
        if (id == null) return;

        Event e = events.getOrDefault(id, new Event());
        e.update(params);
        events.putIfAbsent(id, e);

        String signalType = e.getSignalType();

        log.info(
                "\n=== Stop signal ============" +
                "\nID        : {}" +
                "\nstart     : {}" +
                "\nend       : {}" +
                "\nprogram   : {}" +
                "\nven       : {}" +
                "\nresources : {}" +
                "\n----------------------------" +
                "\ntype      : {}" +
                "\n============================",
                id,
                e.getStart(),
                e.getEnd(),
                e.getProgram(),
                e.getVen(),
                String.join(",", e.getResources()),
                signalType
        );

        if (signalType.equalsIgnoreCase(Event.LEVEL)) {
            e.getResources().forEach(r -> sendCommandToCube(e.getVen(), r, "roff"));
        }
    }

    @Override
    public void oadrEventStatusChange(List activeSignals, List pendingSignals) {
        log.info("Event status change !!!");
    }

    @Override
    public void oadrEventComplete(Map<String, Object> params) {
        String id = getEventId(params);
        if (id == null) return;

        Event e = events.getOrDefault(id, new Event());
        e.update(params);
        events.putIfAbsent(id, e);

        log.info(
                "\n=== Event completed ========" +
                "\nID        : {}" +
                "\nstart     : {}" +
                "\nend       : {}" +
                "\nprogram   : {}" +
                "\nven       : {}" +
                "\nresources : {}" +
                "\n============================",
                id,
                e.getStart(),
                e.getEnd(),
                e.getProgram(),
                e.getVen(),
                String.join(",", e.getResources())
        );

        events.remove(id);
    }

    private void sendCommandToCube(String ven, String resource, String command) {

        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("name").equal(resource);

        vertx.executeBlocking(op -> {
            EISScube cube = q.get();
            if (cube != null) {
                CubeCommand cmd = new CubeCommand();
                cmd.setCubeID(cube.getId());
                cmd.setStatus("Created");
                cmd.setCreated(Instant.now());
                cmd.setCommand(command);

                try {
                    Key<CubeCommand> key = datastore.save(cmd);
                    cmd.setId((ObjectId)key.getId());

                    vertx.eventBus().send("eisscube", new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", cube.getDeviceID())
                        .put("cmd", cmd.toString()));

                    op.complete(cmd);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    op.fail("Unable to create a cube command");
                }
            } else {
                op.fail(String.format("Unable to find an EISScube for name: %s", resource));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info("Command sent: {}", res.result().toString());
            } else {
                log.error(res.cause().getMessage());
            }
        });
    }

}
