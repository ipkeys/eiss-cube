package eiss.webclient;

import com.google.inject.Provider;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
public class WebClientProvider implements Provider<WebClient> {

    private static WebClient webClient;
    private final Vertx vertx;

    @Inject
    public WebClientProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    public WebClient get() {
        if (webClient == null) {

            WebClientOptions options = new WebClientOptions()
                .setLogActivity(FALSE)
                .setTcpFastOpen(TRUE)
                .setTcpCork(TRUE)
                .setTcpQuickAck(TRUE)
                .setReusePort(TRUE);

            webClient = WebClient.create(vertx, options);
        }
        return webClient;
    }

}
