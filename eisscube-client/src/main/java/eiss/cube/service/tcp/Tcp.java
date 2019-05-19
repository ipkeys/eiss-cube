package eiss.cube.service.tcp;

import eiss.cube.config.AppConfig;
import eiss.cube.service.tcp.process.CubeClientHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.*;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static java.lang.Boolean.TRUE;

@Slf4j
public class Tcp extends AbstractVerticle {

    private AppConfig cfg;
    private CubeClientHandler handler;
    private NetClient client;

    @Inject
    public Tcp(AppConfig cfg, CubeClientHandler handler) {
        this.cfg = cfg;
        this.handler = handler;
    }

    @Override
    public void start() throws Exception {
        String host = cfg.getHost();
        int port = cfg.getPort();

        NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(1_200_000) // 20 min
                .setReconnectAttempts(5)
                .setReconnectInterval(300_000) // 5 min
                .setLogActivity(TRUE);

        client = vertx.createNetClient(options);

        client.connect(port, host, res -> {
            if (res.succeeded()) {
                log.info("Start TCP client - connected to {} on port: {}", host, port);
                NetSocket socket = res.result();
                handler.setNetSocket(socket);

                final RecordParser parser = RecordParser.newDelimited("\0", h -> {
                    String message = h.toString();
                    log.info("Received: {}", message);

                    if (message.contains("auth")) { // auth
                        handler.authClient();
                    } else {
                        handler.parseMessage(message);
                    }
                });

                socket.handler(parser);

                socket.closeHandler(h -> {
                    log.info("Socket closed");
                });
            } else {
                log.info("Failed to connect: {}", res.cause().getMessage());
            }
        });

    }

    @Override
    public void stop() throws Exception {
        log.info("Stop TCP client");
        client.close();
    }

}
