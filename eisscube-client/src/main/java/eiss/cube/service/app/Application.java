package eiss.cube.service.app;

import eiss.cube.service.tcp.Tcp;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Application {

    private Vertx vertx;
    private Tcp tcpClient;

    private String tcpDeploymentID = null;

    @Inject
    public Application(Vertx vertx, Tcp tcpClient) {
        this.vertx = vertx;
        this.tcpClient = tcpClient;
    }

    public void start() throws Exception {
        vertx.deployVerticle(tcpClient, res -> {
            if (res.succeeded()) {
                tcpDeploymentID = res.result();
            }
        });
    }

    public void stop() throws Exception {
        if (tcpDeploymentID != null) {
            vertx.undeploy(tcpDeploymentID);
        }
    }

}
