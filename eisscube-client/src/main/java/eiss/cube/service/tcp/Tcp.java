package eiss.cube.service.tcp;

import eiss.cube.config.AppConfig;
import eiss.cube.service.tcp.process.CubeClientHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.net.*;
import io.vertx.core.parsetools.RecordParser;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
public class Tcp extends AbstractVerticle {

    private String host;
    private int port;

    private CubeClientHandler handler;
    private NetClient client;

    private final AtomicReference<NetSocket> socket = new AtomicReference<>();
    private final Semaphore connectLock = new Semaphore(1);
    private final AtomicBoolean closeRequested = new AtomicBoolean(false);

    @Inject
    public Tcp(AppConfig cfg, CubeClientHandler handler) {
        this.host = cfg.getHost();
        this.port = cfg.getPort();
        this.handler = handler;
    }

    @Override
    public void start() throws Exception {
        NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(1_200_000) // 20 min
                .setReconnectAttempts(4) // initial 4 attempts and try to reconnect after additional 1 minute
                .setReconnectInterval(60_000) // 1 min
                .setLogActivity(FALSE);

        client = vertx.createNetClient(options);

        vertx.executeBlocking(op -> {
            log.info("Start TCP client - connect to {}:{}", host, port);
            connect();
            op.complete();
        }, res -> {

        });
    }

    @Override
    public void stop() throws Exception {
        log.info("Stop TCP client");
        if (isConnected()) {
            closeRequested.set(true);
            socket.get().close();
        }
        client.close();
    }

    private void connect() {
        // Block until connected
        doConnect();
        try {
            connectLock.acquire ();
            connectLock.release ();
        } catch (InterruptedException e) {
            log.debug(e.getMessage());
            Thread.currentThread().interrupt();
        }

        if (isConnected ()) {
            log.info("Connection established!");
        } else {
            log.error("Unable to connect to {}:{}", host, port);
        }
    }

    private boolean isConnected () {
        return socket.get () != null;
    }

    private void doConnect() {
        if (!connectLock.tryAcquire()) {
            log.info ("Connect attempt in progress...");
            return;
        }

        client.connect(port, host, res -> {
            if (res.succeeded()) {
                NetSocket socket = res.result();
                handler.setNetSocket(socket);

                final RecordParser parser = RecordParser.newDelimited("\0", h -> {
                    String message = h.toString();
                    if (message.contains("Welcome")) {
                        // On welcome - just print
                        log.info("\n{}", message);
                    } else if (message.contains("auth")) {
                        handler.authClient();
                    } else {
                        handler.parseMessage(message);
                    }
                });

                socket.handler(parser);
                socket.closeHandler(reconnectOnClose());
                socket.exceptionHandler(exceptionHandler());

                this.socket.set(socket);

                connectLock.release();
            } else {
                log.error("Failed: {}", res.cause().getMessage());
                connectLock.release ();
                reconnect();
            }
        });
    }

    private void reconnect() {
        socket.set(null);
        vertx.setTimer(60_000, v -> doConnect());
    }

    private Handler<Throwable> exceptionHandler () {
        return cause -> {
            log.warn("Exception {}:{}", cause.getClass().getSimpleName(), cause.getMessage());
            reconnect ();
        };
    }

    private Handler<Void> reconnectOnClose () {
        return event -> {
            if (!closeRequested.get ()) {
                log.warn("Socket closed, attempting to reconnect...");
                reconnect ();
            }
        };
    }

}
