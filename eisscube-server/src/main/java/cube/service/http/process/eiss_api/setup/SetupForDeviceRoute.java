package cube.service.http.process.eiss_api.setup;

import com.google.gson.Gson;
import cube.db.Cube;
import cube.json.messages.setup.Input;
import cube.json.messages.setup.Relay;
import cube.json.messages.setup.Setup;
import cube.json.messages.setup.SetupRequest;
import cube.json.messages.setup.SetupResponse;
import cube.models.CubeSetup;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
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
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/setup")
public class SetupForDeviceRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public SetupForDeviceRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.gson = gson;
        this.datastore = datastore;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    SetupRequest req = gson.fromJson(jsonBody, SetupRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        SetupResponse res = getSetup(req);
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

    private SetupResponse getSetup(SetupRequest req) {
        SetupResponse rc = new SetupResponse();

        String id = req.getDeviceID();
        if (ObjectId.isValid(id)) {
            Query<CubeSetup> setup = datastore.find(CubeSetup.class);
            setup.filter(Filters.eq("cubeID", new ObjectId(req.getDeviceID())));

            CubeSetup c = setup.first();
            if (c != null) {
                rc.setSetup(
                    Setup.builder()
                        .deviceID(c.getCubeID().toString())

                        .relay(Relay.builder()
                            .connected(c.getRelay().getConnected())
                            .contacts(c.getRelay().getContacts())
                            .label(c.getRelay().getLabel())
                            .description(c.getRelay().getDescription())
                        .build())

                        .input(Input.builder()
                            .connected(c.getInput().getConnected())
                            .signal(c.getInput().getSignal())
                            .meter(c.getInput().getMeter())
                            .unit(c.getInput().getUnit())
                            .factor(c.getInput().getFactor())
                            .label(c.getInput().getLabel())
                            .description(c.getInput().getDescription())
                        .build())

                    .build()
                );
            }
        }

        return rc;
    }

}
