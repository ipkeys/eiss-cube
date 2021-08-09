package cube.json.messages.commands;

import lombok.Data;

@Data
public class CommandListForDeviceRequest {

	private String deviceID;
	private Integer start = 0;
	private Integer limit = 10;

}
