package eiss.cube.service.tcp;

import eiss.cube.config.AppConfig;
import eiss.cube.config.EissCubeConfig;
import eiss.cube.service.tcp.process.CubeHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
public class Tcp extends AbstractVerticle {

    private EissCubeConfig cfg;
    private CubeHandler handler;
    private NetServer server;

    @Inject
    public Tcp(AppConfig cfg, CubeHandler handler) {
        this.cfg = cfg.getEissCubeConfig();
        this.handler = handler;
    }

    @Override
    public void start() throws Exception {

        int port = Integer.valueOf(cfg.getTcpPort());

        NetServerOptions options = new NetServerOptions()
            .setPort(port)
            .setTcpKeepAlive(TRUE)
            .setLogActivity(FALSE);

        server = getVertx().createNetServer(options);

        server
            .connectHandler(handler)
            .listen(h -> {
                if (h.succeeded()) {
                    handler.setAllDevicesOffline();
                    log.info("Start TCP server to listen on port: {}", port);
                } else {
                    log.error("Failed to start TCP server on port: {}", port);
                }
            });
    }

    @Override
    public void stop() throws Exception {
        server.close(h -> log.info("Stop TCP server"));
    }

}
