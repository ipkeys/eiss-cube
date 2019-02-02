package eiss.cube.config;

import lombok.Getter;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;

@BindConfig(value = "app")
public class AppConfig {

    @InjectConfig(value = "eissCubeConfig")
    @Getter
    private EissCubeConfig eissCubeConfig;

    @InjectConfig(value = "databaseConfig")
    @Getter
    private DatabaseConfig databaseConfig;

    @InjectConfig(value = "googleApiKey")
    @Getter
    private String googleApiKey;

    @InjectConfig(value = "randNameConfig")
    @Getter
    private RandNameConfig randNameConfig;

}
