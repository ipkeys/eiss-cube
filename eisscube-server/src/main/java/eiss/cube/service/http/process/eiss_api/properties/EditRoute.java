package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/eiss-api/properties/edit")
public class EditRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public EditRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
                    PropertyRequest req = gson.fromJson(jsonBody, PropertyRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        PropertyResponse res = editProperty(req);
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
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private PropertyResponse editProperty(PropertyRequest req) {
        PropertyResponse rc = new PropertyResponse();

        String id = req.getProperty().getId();
        if (isValid(id)) {
            Query<CubeProperty> property = datastore.find(CubeProperty.class);
            property.filter(eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            updates.add(set("name", req.getProperty().getName()));
            updates.add(set("label", req.getProperty().getLabel()));
            updates.add(set("description", req.getProperty().getDescription()));

            property.update(new UpdateOptions(), updates.toArray(UpdateOperator[]::new));

            // get an updated version
            CubeProperty p = property.first();
            if (p != null) {
                rc.setProperty(
                    Property.builder()
                        .id(p.getId().toString())
                        .name(p.getName())
                        .label(p.getLabel())
                        .description(p.getDescription())
                    .build()
                );
            }
        }

        return rc;
    }

}
