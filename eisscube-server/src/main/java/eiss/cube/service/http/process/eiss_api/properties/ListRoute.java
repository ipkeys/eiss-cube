package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyListRequest;
import eiss.cube.json.messages.properties.PropertyListResponse;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/eiss-api/properties/list")
public class ListRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    PropertyListRequest req = gson.fromJson(jsonBody, PropertyListRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        PropertyListResponse res = getListOfProperties(req);
                        op.complete(res);
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(gson.toJson(res.result()));
                } else {
                    response.setStatusCode(SC_INTERNAL_SERVER_ERROR)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private PropertyListResponse getListOfProperties(PropertyListRequest req) {
        PropertyListResponse rc = new PropertyListResponse();

        Query<CubeProperty> properties = datastore.find(CubeProperty.class);

        FindOptions options = new FindOptions();
        Integer s = req.getStart();
        Integer l = req.getLimit();
        if (s != null && l != null) {
            options.skip(s).limit(l);
        }

        // get & convert
        properties.iterator(options).toList().forEach(p ->
            rc.getProperties().add(
                Property.builder()
                    .id(p.getId().toString())
                    .name(p.getName())
                    .label(p.getLabel())
                    .description(p.getDescription())
                .build()
            )
        );

        // total number of records
        rc.setTotal(properties.count());

        return rc;
    }

}
