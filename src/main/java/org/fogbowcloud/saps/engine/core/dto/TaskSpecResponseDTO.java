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
    private String image;

    public TaskSpecResponseDTO(String id, List<CommandResponseDTO> commands, String image) {
        this.id = id;
        this.commands = commands;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public List<CommandResponseDTO> getCommands() {
        return commands;
    }

    public String getImage(){
        return image;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setCommands(List<CommandResponseDTO> commands){
        this.commands = commands;
    }

    public void setImage(String image){
        this.image = image;
    }

    @Override
    public String toString() {
        return "TaskSpecResponseDTO [id=" + id + ", commands="
                + commands + ", image=" + image + "]";
    }

}