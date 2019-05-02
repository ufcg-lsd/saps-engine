package org.fogbowcloud.saps.engine.core.command;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class Command implements Serializable{
	private static final Logger LOGGER = Logger.getLogger(Command.class);
	
	private static final long serialVersionUID = 5281647552435522413L;

	public enum Type {
		LOCAL, REMOTE, EPILOGUE
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
	
	public State getState() {
		return this.state;
	}

	public Command clone() {
		return null;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject command = new JSONObject();
			command.put("command", this.getCommand());
			command.put("type", this.getType().toString());
			command.put("state", this.getState().toString());
			return command;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSON from command", e);
			return null;
		}
	}

	public static Command fromJSON(JSONObject commandJSON) {
		Command command = new Command(commandJSON.optString("command"), 
				Type.valueOf(commandJSON.optString("type")));
		command.setState(State.valueOf(commandJSON.optString("state")));
		return command;
	}
}
