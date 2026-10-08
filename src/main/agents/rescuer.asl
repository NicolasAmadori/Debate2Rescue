/* Initial goals */

!start.

/* Plans */

// Plan for starting the agent and joining the model and notifying the orchestrator.
+!start <-
	join(rescuer);
	.send(orchestrator, tell, free(rescuer)).

// If the agent is already at the target location, do nothing.
+!go_to(X, Y)[source(orchestrator), source(self)] : at(X, Y).

// If the agent is not at the target location, move towards it (internal action) and recursively check again.
+!go_to(X, Y) <-
	move_towards(X, Y);
	!go_to(X, Y).