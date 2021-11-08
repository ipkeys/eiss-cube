package eiss.cube.json.messages.properties;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PropertyListResponse {

    private List<Property> properties = new ArrayList<>();
    private Long total;

}
