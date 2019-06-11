package eiss.cube.config;

import lombok.Getter;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;

@BindConfig(value = "app", resolve = true)
public class AppConfig {

    @InjectConfig(value = "eissCube")
    @Getter
    private EissCubeConfig eissCubeConfig;

    @InjectConfig(value = "database")
    @Getter
    private DatabaseConfig databaseConfig;

    @InjectConfig(value = "randName")
    @Getter
    private RandNameConfig randNameConfig;

    @InjectConfig(value = "apiUser")
    @Getter
    private ApiUserConfig apiUserConfig;

}
