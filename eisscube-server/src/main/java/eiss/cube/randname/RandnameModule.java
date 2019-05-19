package eiss.cube.randname;

import com.google.inject.AbstractModule;

public class RandnameModule extends AbstractModule {

    protected void configure() {
        bind(Dictionary.class).asEagerSingleton();
        bind(Randname.class).asEagerSingleton();
    }

}
