package eiss.cube.service.http;

import eiss.cube.config.AppConfig;
import eiss.config.Config;
import eiss.cube.config.EissCubeConfig;
import eiss.jwt.ExpiredTokenException;
import eiss.jwt.Jwt;
import eiss.cube.service.http.process.api.ApiBuilder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Slf4j
public class Http extends AbstractVerticle {

    private EissCubeConfig cfg;
    private Router router;
    private ApiBuilder builder;
    private Jwt jwt;

    @Inject
    public Http(AppConfig cfg, ApiBuilder builder, Jwt jwt) {
        this.cfg = cfg.getEissCubeConfig();
        this.router = Router.router(getVertx());
        this.builder = builder;
        this.jwt = jwt;
    }

    @Override
    public void start() throws Exception {

        int port = Integer.valueOf(cfg.getHttpPort());

        HttpServerOptions options = new HttpServerOptions()
            .setPort(port)
            .setLogActivity(true)
            .setCompressionSupported(true);

        // build Routes based on annotations
        setupRoutes();

        HttpServer server = getVertx().createHttpServer(options);

        server
            .requestHandler(router)
            .listen(h -> {
                if (h.succeeded()) {
                    log.info("Start HTTP server to listen on port: {}", port);
                } else {
                    log.error("Failed to start HTTP server on port: {}", port);
                }
            });
    }

    @Override
    public void stop() throws Exception {
        log.info("Stop HTTP server");
    }

    private void setupRoutes() {

        router.route()
            .handler(CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader(HttpHeaderNames.CONTENT_TYPE.toString())
                .allowedHeader(HttpHeaderNames.AUTHORIZATION.toString())
                .exposedHeader("X-Total-Count")
            );

        router.route().handler(BodyHandler.create());
        // We need a cookie handler first
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        router.route("/cubes").handler(context -> {
            HttpServerResponse res = context.response();
            String auth = context.request().getHeader("Authorization");
            if (auth != null) {
                try {
                    jwt.decodeAuthHeader(Config.getInstance().getServerKey(), auth);

                    // allow access only for roles - "admin", "securityadmin" & "operator"
                    String role = jwt.getRole();
                    if (role.equalsIgnoreCase("admin") ||
                        role.equalsIgnoreCase("securityadmin") ||
                        role.equalsIgnoreCase("operator"))
                    {
                        // store user and role in session
                        Session s = context.session();

                        s.put("user", jwt.getUser());
                        s.put("group", jwt.getGroup());
                        s.put("role", role);

                        // Now call the next handler
                        context.next();
                    } else {
                        res
                            .setStatusCode(SC_UNAUTHORIZED)
                            .setStatusMessage("Unauthorized")
                            .end();
                    }

                } catch (ExpiredTokenException ex) {
                    res
                        .setStatusCode(SC_UNAUTHORIZED)
                        .setStatusMessage("Token expired")
                        .end();
                } catch (IllegalArgumentException ex) {
                    res
                        .setStatusCode(SC_UNAUTHORIZED)
                        .setStatusMessage("Invalid token")
                        .end();
                }

            } else {
                res
                    .setStatusCode(SC_UNAUTHORIZED)
                    .setStatusMessage("Unauthorized")
                    .end();
            }
        });

        builder.build(router);
    }

}
