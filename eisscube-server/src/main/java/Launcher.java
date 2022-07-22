import com.google.inject.Guice;
import com.google.inject.Injector;
import eiss.config.ConfigModule;
import eiss.db.DatastoreModule;
import eiss.cube.input.ConversionModule;
import eiss.app.Application;
import eiss.json.GsonModule;
import eiss.cube.randname.RandnameModule;
import eiss.http.HttpModule;
import eiss.cube.service.tcp.TcpModule;
import eiss.webclient.WebClientModule;
import eiss.vertx.VertxModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Launcher {

    private static final String appName = "EISScube Server";

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(
            new ConfigModule(),
            new VertxModule(),
            new HttpModule(),
            new TcpModule(),
            new DatastoreModule(),
            new GsonModule(),
            new RandnameModule(),
            new ConversionModule(),
            new WebClientModule()
        );

        Application app = injector.getInstance(Application.class);

        try {
            log.info("{}: Starting...", appName);
            app.start();
        } catch (Exception e) {
            log.error("Start failure: {}", e.getMessage());
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> {
                log.info("{}: Shutdown...", appName);
                try {
                    app.stop();
                } catch (Exception e) {
                    log.error("Shutdown failure: {}" + e.getMessage());
                    System.exit(1);
                }
            }
        ));
    }

}
