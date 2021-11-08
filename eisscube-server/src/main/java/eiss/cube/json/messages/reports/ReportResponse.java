package eiss.cube.json.messages.reports;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportResponse {

    List<Report> usage;

}
