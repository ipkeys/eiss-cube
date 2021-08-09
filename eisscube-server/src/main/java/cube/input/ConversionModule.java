package cube.input;

import com.google.inject.AbstractModule;

public class ConversionModule extends AbstractModule {

    protected void configure() {
        bind(Conversion.class).asEagerSingleton();
    }

}
