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

    private long reportTimerID;
    private String auth;
    private Vertx vertx;
    private EventBus eventBus;
    private NetSocket socket;

    @Inject
    public CubeClientHandler(AppConfig cfg, Vertx vertx) {
        this.auth = cfg.getAuth();
        this.vertx = vertx;

        eventBus = vertx.eventBus();
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
            StringBuilder sb = new StringBuilder().append("Relay ON");
            logInfo(message, sb);
        }

        if (message.contains("c=roff")) {
            StringBuilder sb = new StringBuilder().append("Relay OFF");
            logInfo(message, sb);
        }

        if (message.contains("c=icp")) {
            StringBuilder sb = new StringBuilder().append("Input count PULSES");
            logInfo(message, sb);
            startReportingPulses(message);
        }

        if (message.contains("c=ioff")) {
            StringBuilder sb = new StringBuilder().append("Input STOP count");
            logInfo(message, sb);
            stopReporting();
        }
    }

    private void logInfo(String message, StringBuilder sb) {
        for (String part : message.split("&")) {
            if (part.contains("st=")) {
                String st = part.replace("st=", "");
                sb.append(" start time: ").append(Instant.ofEpochSecond(Long.valueOf(st)));
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

        // Will report in "each" seconds
        String idString = String.format("id=%s\0", id);
        reportTimerID = vertx.setPeriodic(Integer.valueOf(each) * 1000, v -> {
            double value = Math.random() * 100;
            String response = String.format("rpt-ts=%s&v=%4.2f&%s", Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond(), value, idString);
            socket.write(response);
        });
    }

    private void stopReporting() {
        vertx.cancelTimer(reportTimerID);
    }

/*
    private String reportStatus = "sts-ts=%s&r=%s&i=%s&ss=3";

    private void reportStatus(ChannelHandlerContext ctx) {

        String input1 = Math.round(Math.random()) == 0 ? "low" : "high";
        String input2 = Math.round(Math.random()) == 0 ? "low" : "high";


        String msg = String.format(reportStatus, relay1, relay2, input1, input2);
        ctx.writeAndFlush(Unpooled.copiedBuffer(msg + "\0", CharsetUtil.UTF_8));
    }
*/

}
