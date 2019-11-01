package org.fogbowcloud.saps.engine.core.scheduler.selector;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class DefaultRoundRobin implements Selector {

	@Override
	public List<ImageTask> select(int count, Map<String, List<ImageTask>> tasks) {
		List<ImageTask> selectedTasks = new LinkedList<ImageTask>();
		List<String> usersForDelete = new LinkedList<String>();

		while (count > 0 && tasks.size() > 0) {
			for (String user : tasks.keySet()) {
				if (count > 0) {
					selectedTasks.add(tasks.get(user).remove(0));
					count--;
				}
				if (tasks.get(user).size() == 0)
					usersForDelete.add(user);
			}

			for (String user : usersForDelete)
				tasks.remove(user);
		}

		return selectedTasks;
	}

	@Override
	public String version() {
		return "Default Round Robin";
	}

}
