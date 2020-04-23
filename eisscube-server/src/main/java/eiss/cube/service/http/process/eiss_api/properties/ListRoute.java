package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyListRequest;
import eiss.cube.json.messages.properties.PropertyListResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/eiss-api/properties/list")
public class ListRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    PropertyListRequest req = gson.fromJson(jsonBody, PropertyListRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        PropertyListResponse res = getListOfProperties(req);
                        op.complete(gson.toJson(res));
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end((String)res.result());
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

        Query<CubeProperty> properties = datastore.createQuery(CubeProperty.class);

        // filter

        // projections

        // skip/limit
        FindOptions options = new FindOptions();
        Integer s = req.getStart();
        Integer l = req.getLimit();
        if (s != null && l != null) {
            options.skip(s).limit(l);
        }

        // get & convert
        properties.find(options).toList().forEach(p ->
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
