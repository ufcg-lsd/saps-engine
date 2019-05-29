package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;
import java.util.List;

public class TaskSpecResponseDTO implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String id;
    private List<CommandResponseDTO> commands;

    public TaskSpecResponseDTO(String id, /*String spec,*/ List<CommandResponseDTO> commands/*, String metadata*/) {
        this.id = id;
        //this.spec = spec;
        this.commands = commands;
        //this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public List<CommandResponseDTO> getCommands() {
        return commands;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setCommands(List<CommandResponseDTO> commands){
        this.commands = commands;
    }

    @Override
    public String toString() {
        return "TaskSpecResponseDTO [id=" + id + /*", spec=" + spec + */", commands="
                + commands /*+ ", metadata=" + metadata*/ + "]";
    }

}