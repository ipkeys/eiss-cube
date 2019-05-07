package eiss.cube.service.client.events;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class tracks the current state of event map objects passed in
 * from the eissclient api. Each event can have multiple signals so this
 * tracks the current value of each signal type.
 *
 * The Condition objects of the EventStateManager are compared
 * against the current list of EventState objects.
 */
@Data
public class Event {
    private static final String ACTIVE = "active";
    private static final String PENDING = "pending";

    // signal types
    public static final String LEVEL = "level";
    public static final String SETPOINT = "setpoint";
    public static final String PRICE = "price";

    private Map<String, Object> event = null;
    private String program = null;
    private Instant start = null;
    private Instant end = null;
    private String state = PENDING;
    private String signalType = null;
    private String signalValue = null;
    private String ven = null;
    private List<String> resources = new ArrayList<>();

    public void update(Map<String, Object> event) {
        this.event = event;
        this.resources.clear();

        Object startObj = event.get("startTime");
        if (startObj != null && startObj.getClass().isAssignableFrom(Date.class)) {
            start = ((Date)startObj).toInstant();
            state = Instant.now().isBefore(start) ? PENDING : ACTIVE;
        }

        Object endObj = event.get("endTime");
        if (endObj != null && endObj.getClass().isAssignableFrom(Date.class)) {
            end = ((Date)endObj).toInstant();
        }

        // for received event, resources are under target.
        // IMPORTANT: 2b has a target with a Map containing resources and assets as keys.
        //            2a has a target that is a List with just the resourceIDs.
        Object targetObj = event.get("target");
        if (targetObj != null) {
            if (Map.class.isAssignableFrom(targetObj.getClass())) { // 2.0b received event
                Map target = (Map) targetObj;

                Object venObj = target.get("resource");
                if (String.class.isAssignableFrom(venObj.getClass())) {
                    ven = (String) venObj;
                }

                Object assetsObj = target.get("assets");
                if (assetsObj != null && List.class.isAssignableFrom(assetsObj.getClass())) {
                    List assets = (List) assetsObj;
                    for (Object r : assets) {
                        if (String.class.isAssignableFrom(r.getClass())) {
                            String rid = (String) r;
                            resources.add(rid);
                        }
                    }
                }
            } else if (List.class.isAssignableFrom(targetObj.getClass())) { // 2.0a received event
                List targets = (List) targetObj;
                for (Object o : targets) {
                    if (String.class.isAssignableFrom(o.getClass())) {
                        String rid = (String) o;
                        resources.add(rid);
                    }
                }
            }
        }

        // for signal changes resources are under resources key
        Object resourcesObj = event.get("resources");
        if (resourcesObj != null && List.class.isAssignableFrom(resourcesObj.getClass())) {
            List rs = (List) resourcesObj;
            for (Object r : rs) {
                if (String.class.isAssignableFrom(r.getClass())) {
                    String rid = (String) r;
                    resources.add(rid);
                }
            }
        }

        // program
        Object programObj = event.get("marketContext");
        if (programObj != null && String.class.isAssignableFrom(programObj.getClass())) {
            program = (String)programObj;
        }

        // signal type
        Object signalTypeObj = event.get("signalType");
        if (signalTypeObj != null && String.class.isAssignableFrom(signalTypeObj.getClass())) {
            signalType = (String)signalTypeObj;
        }

        // signal value
        Object signalValueObj = event.get("value");
        if (signalValueObj != null && String.class.isAssignableFrom(signalValueObj.getClass())) {
            signalValue = (String)signalValueObj;
        }

    }

}
