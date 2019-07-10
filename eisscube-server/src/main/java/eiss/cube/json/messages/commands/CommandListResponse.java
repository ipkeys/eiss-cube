package eiss.cube.json.messages.commands;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandListResponse {

    private List<Command> commands = new ArrayList<>();
    private Long total;

}
