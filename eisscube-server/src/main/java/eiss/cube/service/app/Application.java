package eiss.cube.service.app;

import eiss.cube.service.client.VEN;
import eiss.cube.service.http.Http;
import eiss.cube.service.tcp.Tcp;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Application {

    private Vertx vertx;
    private Http httpServer;
    private Tcp tcpServer;
    private VEN ven;

    private String httpDeploymentID = null;
    private String tcpDeploymentID = null;
    private String venDeploymentID = null;

    @Inject
    public Application(Vertx vertx, Http httpServer, Tcp tcpServer, VEN ven) {
        this.vertx = vertx;
        this.httpServer = httpServer;
        this.tcpServer = tcpServer;
        this.ven = ven;
    }

    public void start() throws Exception {
        vertx.deployVerticle(httpServer, res -> {
            if (res.succeeded()) {
                httpDeploymentID = res.result();
            }
        });

        vertx.deployVerticle(tcpServer, res -> {
            if (res.succeeded()) {
                tcpDeploymentID = res.result();
            }
        });

        vertx.deployVerticle(ven, res -> {
            if (res.succeeded()) {
                venDeploymentID = res.result();
            }
        });
    }

    public void stop() throws Exception {
        if (httpDeploymentID != null) {
            vertx.undeploy(httpDeploymentID);
        }

        if (tcpDeploymentID != null) {
            vertx.undeploy(tcpDeploymentID);
        }

        if (venDeploymentID != null) {
            vertx.undeploy(venDeploymentID);
        }
    }

}
