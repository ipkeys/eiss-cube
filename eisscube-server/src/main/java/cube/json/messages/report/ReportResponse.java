package cube.json.messages.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportResponse {

    List<Power> usage = new ArrayList<>();

}
