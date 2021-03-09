package cube.json.messages.properties;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Property {

    private String id;
    private String name;
    private String label;
    private String description;

}
