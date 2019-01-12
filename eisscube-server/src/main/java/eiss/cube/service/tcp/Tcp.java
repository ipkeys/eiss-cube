package eiss.cube.service.tcp;

import eiss.cube.config.AppConfig;
import eiss.cube.service.tcp.process.CubeHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Tcp extends AbstractVerticle {

    private AppConfig cfg;
    private CubeHandler handler;

    @Inject
    public Tcp(AppConfig cfg, CubeHandler handler) {
        this.cfg = cfg;
        this.handler = handler;
    }

    @Override
    public void start() throws Exception {

        int port = cfg.getEissCubeConfig().getTcpPort();

        NetServerOptions options = new NetServerOptions()
            .setPort(port)
            .setTcpKeepAlive(Boolean.TRUE)
            .setLogActivity(Boolean.TRUE);

        NetServer server = getVertx().createNetServer(options);

        server
            .connectHandler(handler::handle)
            .listen(h -> {
                if (h.succeeded()) {
                    log.info("Start TCP server to listen on port: {}", port);
                } else {
                    log.error("Failed to start TCP server on port: {}", port);
                }
            });
    }

    @Override
    public void stop() throws Exception {
        log.info("Stop TCP server");
    }

}
