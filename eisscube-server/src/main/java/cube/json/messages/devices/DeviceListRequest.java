package cube.json.messages.devices;

import lombok.Data;

@Data
public class DeviceListRequest {

	private String customerID;
	private Integer start = 0;
	private Integer limit = 10;

}
