package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class CommandResponseDTO implements Serializable{
    private static final Logger LOGGER = Logger.getLogger(CommandResponseDTO.class);

    private static final long serialVersionUID = 5281647552435522413L;

    private String command;
    private String state;
    private String exitCode;

    public CommandResponseDTO(String command, String state){
        this.command = command;
        this.state = state;
    }

    public CommandResponseDTO(){}

    public CommandResponseDTO(String command, String state, String exitCode) {
        this.command = command;
        this.state = state;
        this.exitCode = exitCode;
    }

    public String getCommand() {
        return command;
    }

    public String getState() {
        return state;
    }

    public String getExitCode() {
        return exitCode;
    }

    public void setCommand(String command){
        this.command = command;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setExitCode(String exitCode){
        this.exitCode = exitCode;
    }

    @Override
    public String toString() {
        return "CommandResponseDTO [command=" + command + ", state=" + state +
                ", exitCode=" + exitCode + "]";
    }
}