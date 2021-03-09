package eiss.vertx;

import com.google.inject.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import static java.lang.Boolean.TRUE;

public class VertxProvider implements Provider<Vertx> {

    private static Vertx vertx;

    public Vertx get() {
        if (vertx == null) {
            vertx = Vertx.vertx(new VertxOptions()
                    .setWorkerPoolSize(40)
                    .setPreferNativeTransport(TRUE)
            );
        }
        return vertx;
    }

}
