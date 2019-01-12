package eiss.cube.service.http;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import eiss.jwt.Jwt;
import eiss.cube.service.http.process.api.ApiBuilder;

public class HttpModule extends AbstractModule {

    protected void configure() {
        bindConstant().annotatedWith(Names.named("api_package")).to("eiss.cube.service.http.process");

        bind(Http.class).asEagerSingleton();;
        bind(ApiBuilder.class).asEagerSingleton();
        bind(Jwt.class).asEagerSingleton();
    }

}
