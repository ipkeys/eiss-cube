package cube.json.messages.setup;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Relay {

    private Boolean connected;
    private String contacts;
    private String label;
    private String description;

}
