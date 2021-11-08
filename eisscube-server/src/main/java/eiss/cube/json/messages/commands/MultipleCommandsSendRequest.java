package eiss.cube.json.messages.commands;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MultipleCommandsSendRequest {

    private List<Command> commands = new ArrayList<>();

}
