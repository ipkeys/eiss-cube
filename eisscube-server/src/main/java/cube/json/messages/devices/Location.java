package cube.json.messages.devices;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Location {

	public static Double defaultLat = 40.32444602981903;
	public static Double defaultLng = -74.07683856203221;

	private Double lat;
	private Double lng;

}
