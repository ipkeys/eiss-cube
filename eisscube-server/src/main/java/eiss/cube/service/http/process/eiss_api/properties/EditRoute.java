package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyIdRequest;
import eiss.cube.json.messages.properties.PropertyRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/properties/edit")
public class EditRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public EditRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
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
                    PropertyRequest req = gson.fromJson(jsonBody, PropertyRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        PropertyResponse res = editProperty(req);
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
        if (ObjectId.isValid(id)) {
            Query<CubeProperty> property = datastore.createQuery(CubeProperty.class);

            // filter
            property.criteria("_id").equal(new ObjectId(id));

            // projections

            // update
            UpdateOperations<CubeProperty> ops = datastore.createUpdateOperations(CubeProperty.class)
                    .set("name", req.getProperty().getName())
                    .set("label", req.getProperty().getLabel())
                    .set("description", req.getProperty().getDescription());

            datastore.update(property, ops);

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
