package cube.json.messages.devices;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceListResponse {

    private List<Device> devices = new ArrayList<>();
    private Long total;

}
