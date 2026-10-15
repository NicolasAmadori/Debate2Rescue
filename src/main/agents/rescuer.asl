/* Initial goals */

!start.

/* Plans */

// Plan for starting the agent and joining the model and notifying the orchestrator.
+!start <-
	join_as_rescuer;
	.send(orchestrator, tell, free(rescuer)).

// Handling emergencies assigned by the orchestrator.
// Arrive at the emergency location, start the rescue, gather emergency info, and evaluate the situation.
+!handle(Id, X, Y)[source(orchestrator)] <-
	!go_to(X, Y);
	start_rescue(Id);
	?emergency_info(Id, Type, Severity, Victims);
	!evaluate(Id, Type, Severity, Victims).

// Requesting a helicopter for the emergency.
// TODO: change this to request other types of resources if needed.
+!evaluate(Id, Type, Severity, Victims) <-
	.send(orchestrator, tell, request(Id, helicopter, [Type, Severity, Victims])).

//When granted a helicopter for the emergency, the agent waits for the operation to end and then goes home.
+granted(Id, helicopter)[source(orchestrator)] <-
	-granted(Id, helicopter)[source(orchestrator)].

+emergency_resolved(Id) <-
    -emergency_resolved(Id);
    !go_home.

// If the agent is already at the target location, do nothing.
+!go_to(X, Y)[source(self)] : at(X, Y).

// If the agent is not at the target location, move towards it (internal action) and recursively check again.
+!go_to(X, Y)[source(self)] <-
    move_towards(X, Y);
    !go_to(X, Y).

+!go_home <-
	?station(X, Y);
	!go_to(X, Y);
	.send(orchestrator, tell, free(rescuer)).