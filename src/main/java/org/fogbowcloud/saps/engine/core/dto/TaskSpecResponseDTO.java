package org.fogbowcloud.saps.engine.core.dto;

import org.fogbowcloud.saps.engine.core.task.Specification;

import java.io.Serializable;
import java.util.List;

public class TaskSpecResponseDTO implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String id;
    private Specification spec;
    private List<CommandResponseDTO> commands;
    private String image;

    public TaskSpecResponseDTO(String id, Specification spec, List<CommandResponseDTO> commands, String image) {
        this.id = id;
        this.spec = spec;
        this.commands = commands;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public Specification getSpec(){
        return spec;
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

    public void setSpec(Specification spec){
        this.spec = spec;
    }

    public void setCommands(List<CommandResponseDTO> commands){
        this.commands = commands;
    }

    public void setImage(String image){
        this.image = image;
    }

    @Override
    public String toString() {
        return "TaskSpecResponseDTO [id=" + id + ", spec=" + spec + ", commands="
                + commands + ", image=" + image + "]";
    }

}