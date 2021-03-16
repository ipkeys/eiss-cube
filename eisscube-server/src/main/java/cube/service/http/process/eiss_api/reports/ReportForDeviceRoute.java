package cube.service.http.process.eiss_api.reports;

import com.google.gson.Gson;
import cube.input.Conversion;
import cube.json.messages.reports.Report;
import cube.json.messages.reports.ReportRequest;
import cube.json.messages.reports.ReportResponse;
import cube.service.http.process.meters.MeterRequest;
import cube.service.http.process.meters.MeterResponse;
import eiss.api.Api;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.time.Instant;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/report")
public class ReportForDeviceRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Gson gson;
    private final Conversion conversion;

    @Inject
    public ReportForDeviceRoute(Vertx vertx, Gson gson, Conversion conversion) {
        this.vertx = vertx;
        this.gson = gson;
        this.conversion = conversion;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    ReportRequest req = gson.fromJson(jsonBody, ReportRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        ReportResponse res = getMeterReport(req);
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

    private ReportResponse getMeterReport(ReportRequest req) {
        // tune up for ElectSolv portal (if needed!)
        MeterResponse meterResponse = new MeterResponse();
        MeterRequest meterRequest = copyFromReportRequest(req);

        conversion.process(meterRequest, meterResponse);

        return ReportResponse.builder()
            .usage(
                meterResponse.getUsage().stream().map(meter -> Report.builder().t(meter.getT()).v(meter.getV()).build()).collect(toList())
            ).build();
    }

    private MeterRequest copyFromReportRequest(ReportRequest req) {
        MeterRequest r = new MeterRequest();

        r.setCubeID(req.getDeviceID());
        r.setType(req.getType());
        r.setFrom(req.getFrom());
        r.setTo(req.getTo());
        r.setUtcOffset(req.getUtcOffset());
        r.setAggregation(req.getAggregation());
        r.setFactor(req.getFactor());
        r.setWatch(req.getWatch());
        r.setLoad(req.getLoad());
        r.setMeter(req.getMeter());
        r.setUnit(req.getUnit());

        return r;
    }
}
