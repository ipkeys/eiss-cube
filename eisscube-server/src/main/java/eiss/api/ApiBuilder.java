package eiss.api;

import com.google.inject.Injector;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import java.lang.reflect.Method;

@Slf4j
public class ApiBuilder {

    private final Injector injector;

    @Inject @Named("api_package")
    private String API_PACKAGE;

    @Inject
    public ApiBuilder(Injector injector) {
        this.injector = injector;
    }

    public void build(Router router) {

        Reflections reflections = new Reflections(API_PACKAGE);

        reflections.getTypesAnnotatedWith(Api.class).forEach(clazz -> {
            @SuppressWarnings("unchecked") Handler<RoutingContext> handler = (Handler<RoutingContext>) injector.getInstance(clazz);
            Path path = clazz.getAnnotation(Path.class);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                String friendlyRoute = path.value().replaceAll("\\{(.*?)\\}", ":$1");

                POST post = method.getAnnotation(POST.class);
                if (post != null) {
                    router.post(friendlyRoute).handler(handler);
                    log.debug("Route created for '{}' and method POST", friendlyRoute);
                    break;
                }

                GET get = method.getAnnotation(GET.class);
                if (get != null) {
                    router.get(friendlyRoute).handler(handler);
                    log.debug("Route created for '{}' and method GET", friendlyRoute);
                    break;
                }

                DELETE delete = method.getAnnotation(DELETE.class);
                if (delete != null) {
                    router.delete(friendlyRoute).handler(handler);
                    log.debug("Route created for '{}' and method DELETE", friendlyRoute);
                    break;
                }

                PUT put = method.getAnnotation(PUT.class);
                if (put != null) {
                    router.put(friendlyRoute).handler(handler);
                    log.debug("Route created for '{}' and method PUT", friendlyRoute);
                    break;
                }
            }
        });
    }

}
