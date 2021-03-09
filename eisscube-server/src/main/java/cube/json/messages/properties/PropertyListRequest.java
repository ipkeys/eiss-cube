package cube.json.messages.properties;

import lombok.Data;

@Data
public class PropertyListRequest {

	private Integer start = 0;
	private Integer limit = 10;

}
