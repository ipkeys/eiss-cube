package cube.json.messages.setup;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Setup {

    private String deviceID;
    private Relay relay;
    private Input input;

}
