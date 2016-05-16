package org.fogbowcloud.scheduler.core.model;

import java.io.Serializable;

public class Command implements Serializable{

	private static final long serialVersionUID = 5281647552435522413L;

	public enum Type {
		PROLOGUE, REMOTE, EPILOGUE
	}

	public enum State {
		UNSTARTED, RUNNING, FINISHED, FAILED
	}

	private String command;
	private final Type type;
	private State state = State.UNSTARTED;

	public Command(String command, Type type) {
		this.command = command;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public String getCommand() {
		return command;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Command clone() {
		return null;
	};
}
