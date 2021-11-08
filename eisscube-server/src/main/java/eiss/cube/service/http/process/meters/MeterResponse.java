package eiss.cube.service.http.process.meters;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MeterResponse {

    List<Meter> usage = new ArrayList<>();

}
