package eiss.cube.service.tcp.process;

import eiss.cube.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public class CubeClientHandler {

    private long relayTimerID = -1;
    private long reportTimerID = -1;
    private long statusTimerID = -1;
    private final String auth;
    private final Vertx vertx;
    private NetSocket socket;

    private int r = 0;
    private int i = 0;

    @Inject
    public CubeClientHandler(AppConfig cfg, Vertx vertx) {
        this.auth = cfg.getAuth();
        this.vertx = vertx;

        EventBus eventBus = vertx.eventBus();
 /*       eventBus.<JsonObject>consumer("eisscube", message -> {
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
*/
    }

    public void setNetSocket(NetSocket socket) {
        this.socket = socket;
    }

    public void authClient() {
        log.info("\nHello server! I am: {}", auth);
        String response = "auth " + auth + " 5\0";
        socket.write(response);
    }

    public void parseMessage(String message) {
        log.info("Got a command:\n{}", message);

        // Step 1 - acknowledge receiving of command
        if (message.contains("id=")) {
            acknowledgeCommand(message);
        }
        // ~Step 1 - acknowledge receiving of command

        //Step 2 - do some real work
        if (message.contains("c=ron")) {
            r = 1;
            StringBuilder sb = new StringBuilder().append("Relay ON");
            logInfo(message, sb);
            // stop previous timer
            stopRelayCycling();
        }

        if (message.contains("c=roff")) {
            r = 0;
            StringBuilder sb = new StringBuilder().append("Relay OFF");
            logInfo(message, sb);
            // stop previous timer
            stopRelayCycling();
        }

        if (message.contains("c=rcyc")) {
            StringBuilder sb = new StringBuilder().append("Relay CYCLE");
            logInfo(message, sb);
            startRelayCycles(message);
        }

        if (message.contains("c=icp")) {
            StringBuilder sb = new StringBuilder().append("Input count PULSES");
            logInfo(message, sb);
            startReportingPulses(message);
        }

        if (message.contains("c=icc")) {
            StringBuilder sb = new StringBuilder().append("Input count CYCLES");
            logInfo(message, sb);
            startReportingCycles(message);
        }

        if (message.contains("c=status")) {
            StringBuilder sb = new StringBuilder().append("Report STATUS");
            logInfo(message, sb);
            startReportingStatus(message);
        }

        if (message.contains("c=ioff")) {
            StringBuilder sb = new StringBuilder().append("Input STOP count");
            logInfo(message, sb);
            stopReporting();
            stopReportingStatus();
        }

    }

    private void logInfo(String message, StringBuilder sb) {
        for (String part : message.split("&")) {
            if (part.contains("st=")) {
                String st = part.replace("st=", "");
                sb.append(" start time: ").append(Instant.ofEpochSecond(Long.parseLong(st)));
            }
            if (part.contains("dur=")) {
                String dur = part.replace("dur=", "");
                sb.append(" for duration: ").append(dur).append(" seconds");
            }
        }
        log.info(sb.toString());
    }

    private void acknowledgeCommand(String message) {
        for (String part : message.split("&")) {
            if (part.contains("id=")) {
                String id = part.replace("id=", "");
                log.info("Acknowledged:\n{}", id);
                String response = "ack=" + id + "\0";
                socket.write(response);
            }
        }
    }

    private void startReportingPulses(String message) {
        String id = "";
        String each = "60";

        for (String part : message.split("&")) {
            if (part.contains("id=")) {
                id = part.replace("id=", "");
            }
            if (part.contains("each=")) {
                each = part.replace("each=", "");
            }
        }

        // stop previous timer
        stopReporting();
        // Will report in "each" seconds
        String idString = String.format("id=%s\0", id);
        vertx.setPeriodic(Integer.parseInt(each) * 1000L, v -> {
            reportTimerID = v;
            double value = Math.random() * 100;
            String response = String.format("rpt-ts=%s&v=%4.2f&%s", Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond(), value, idString);
            socket.write(response);
        });
    }

    private void startRelayCycles(String message) {
        String id = "";
        String each = "60";
        String pct = "50";

        for (String part : message.split("&")) {
            if (part.contains("id=")) {
                id = part.replace("id=", "");
            }
            if (part.contains("each=")) {
                each = part.replace("each=", "");
            }
            if (part.contains("pct=")) {
                pct = part.replace("pct=", "");
            }
        }

        final long off = Long.parseLong(each) * Long.parseLong(pct) / 100L;

        // stop previous timer
        stopRelayCycling();
        // Will report in "each" seconds
        String idString = String.format("id=%s\0", id);
        vertx.setPeriodic(Long.parseLong(each) * 1000L, v -> {
            relayTimerID = v;
            r = 1;
            i = 0;
            StringBuilder sbOn = new StringBuilder().append("Relay ON");
            logInfo(message, sbOn);

            vertx.setTimer(off * 1000L, vv -> {
                r = 0;
                i = 1;
                StringBuilder sbOff = new StringBuilder().append("Relay OFF");
                logInfo(message, sbOff);
            });
        });
    }

    private void startReportingCycles(String message) {
        String id = "";
        String edge = "r";

        for (String part : message.split("&")) {
            if (part.contains("id=")) {
                id = part.replace("id=", "");
            }
            if (part.contains("edge=")) {
                edge = part.replace("edge=", "");
            }
        }

        // stop previous timer
        stopReporting();
        // Will report in "each" seconds
        String idString = String.format("id=%s\0", id);
        String each = "1800"; // each 30 min
        vertx.setPeriodic(Long.parseLong(each) * 1000L, v -> {
            reportTimerID = v;
            final long timestamp = Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
            String value = "600"; // 10 min ON, 20 min OFF
            String startCycle = String.format("rpt-ts=%s&dur=z&%s", timestamp, idString); // dur=z - means that cycle started and in progress
            socket.write(startCycle);
            vertx.setTimer(Long.parseLong(value) * 1000L, vv -> {
                String finishCycle = String.format("rpt-ts=%s&dur=%s&%s", timestamp, value, idString);
                socket.write(finishCycle);
            });
        });
    }

    private void startReportingStatus(String message) {
        String id = "";
        String each = "60";

        for (String part : message.split("&")) {
            if (part.contains("id=")) {
                id = part.replace("id=", "");
            }
            if (part.contains("each=")) {
                each = part.replace("each=", "");
            }
        }

        // stop previous timer
        stopReportingStatus();
        // Will report in "each" seconds
        String idString = String.format("id=%s\0", id);
        vertx.setPeriodic(Long.parseLong(each) * 1000L, v -> {
            statusTimerID = v;
            final long timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
            String statusReport = String.format("sts-ts=%s&r=%s&i=%s&ss=3&%s", timestamp, r, i, idString);
            socket.write(statusReport);

            StringBuilder sb = new StringBuilder().append(statusReport);
            logInfo(message, sb);
        });
    }


    private void stopRelayCycling() {
        if (relayTimerID != -1) {
            vertx.cancelTimer(relayTimerID);
        }
    }
    private void stopReporting() {
        if (reportTimerID != -1) {
            vertx.cancelTimer(reportTimerID);
        }
    }
    private void stopReportingStatus() {
        if (statusTimerID != -1) {
            vertx.cancelTimer(statusTimerID);
        }
    }

}
