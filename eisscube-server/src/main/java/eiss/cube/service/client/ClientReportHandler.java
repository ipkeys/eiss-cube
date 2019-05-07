package eiss.cube.service.client;

import eiss.client.api.EventHandler;
import eiss.client.api.ReportHandler;
import eiss.client.api.VenConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ClientReportHandler implements ReportHandler {

    @Override
    public float getReportSample(String meterId, String reportType, boolean deltaReportingFlag) {
        log.info("getReportSample for meterId = " + meterId + ", reportType = " + reportType + ", deltaReportingFlag = " + deltaReportingFlag);
        return 0.0f;
    }

    @Override
    public void startReporting(String reportRequestId, String meterId, List<String> reportTypes, int sampleRateSeconds) {
        log.info("startReporting: request ID " + reportRequestId + " sample rate " + sampleRateSeconds);
    }

    @Override
    public void endReporting(String reportRequestId, String meterId) {
        log.info("endReporting: request ID " + reportRequestId);
    }

}
