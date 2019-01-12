package eiss.cube.service.app;

import eiss.cube.service.http.Http;
import eiss.cube.service.tcp.Tcp;
import io.vertx.core.Vertx;

import javax.inject.Inject;

public class Application {

    private Vertx vertx;
    private Http httpServer;
    private Tcp tcpServer;

    private String httpDeploymentID = null;
    private String tcpDeploymentID = null;

    @Inject
    public Application(Vertx vertx, Http httpServer, Tcp tcpServer) {
        this.vertx = vertx;
        this.httpServer = httpServer;
        this.tcpServer = tcpServer;
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
    }

    public void stop() throws Exception {
        if (httpDeploymentID != null) {
            vertx.undeploy(httpDeploymentID);
        }
        if (tcpDeploymentID != null) {
            vertx.undeploy(tcpDeploymentID);
        }
    }

}
