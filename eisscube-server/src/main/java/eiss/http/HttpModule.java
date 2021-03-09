package eiss.http;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import cube.service.http.process.lora.LoraCubeHandler;
import eiss.jwt.Jwt;
import eiss.api.ApiBuilder;

public class HttpModule extends AbstractModule {

    protected void configure() {
        bindConstant().annotatedWith(Names.named("api_package")).to("cube.service.http.process");

        bind(Http.class).asEagerSingleton();
        bind(ApiBuilder.class).asEagerSingleton();
        bind(Jwt.class).asEagerSingleton();

        bind(LoraCubeHandler.class).asEagerSingleton();
    }

}
