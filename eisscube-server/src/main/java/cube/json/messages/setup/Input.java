package cube.json.messages.setup;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Input {

    private Boolean connected;
    private String signal;
    private String meter;
    private String unit;
    private Float factor;
    private String watch;
    private Float load;
    private String label;
    private String description;

}
