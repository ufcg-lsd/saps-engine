# Tasks Recovery Process

This process aims to identify and address possible inconsistencies in the status of tasks. This process is normally triggered when starting the Scheduler.

To identify inconsistencies, all tasks that are in a processing state (downloading, preprocessing or running) are checked.

Given that, we basically have two types of inconsistencies:
1. Task in processing state without execution id.
    - When the field corresponding to the execution id is empty or null.
2. Task in processing state with nonexistent execution id.
    - When the field corresponding to the execution identifier is valid but there is no execution corresponding to that identifier.

### Recovery Steps

We follow the following steps:
- We perform a search for label in the executions. Whereas the execution has a label defined as a concatenation between the Task ID and the state of the Task.
- If no execution is found, we revert the task status to the previous one.
- If an execution is found, we set in the Task the identification of that execution.
- If more than one execution is found, we choose the execution closest to the final state and set in the Task the identification of that execution.