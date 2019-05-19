package eiss.cube.config;

import lombok.Getter;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;

@BindConfig(value = "app")
public class AppConfig {

    @InjectConfig(value = "host")
    @Getter
    private String host = "localhost";

    @InjectConfig(value = "port")
    @Getter
    private Integer port = 36000;

    @InjectConfig(value = "auth")
    @Getter
    private String auth = "12345678901234567890";

}
