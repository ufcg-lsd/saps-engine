package org.fogbowcloud.scheduler.core.model;

public class Command {

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
