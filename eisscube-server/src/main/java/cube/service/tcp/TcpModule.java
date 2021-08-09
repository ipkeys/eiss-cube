package cube.service.tcp;

import com.google.inject.AbstractModule;
import cube.service.tcp.process.CubeHandler;

public class TcpModule extends AbstractModule {

    protected void configure() {
        bind(Tcp.class).asEagerSingleton();
        bind(CubeHandler.class).asEagerSingleton();
    }

}
