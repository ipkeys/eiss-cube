package cube.config;

import com.google.inject.AbstractModule;
import net.jmob.guice.conf.core.ConfigurationModule;

import java.io.File;

public class ConfigModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ConfigurationModule().fromPath(new File("./")));
        requestInjection(AppConfig.class);
    }

}
