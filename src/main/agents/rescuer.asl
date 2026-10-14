/* Initial goals */

!start.

/* Plans */

// Plan for starting the agent and joining the model and notifying the orchestrator.
+!start <-
	join(rescuer);
	.send(orchestrator, tell, free(rescuer)).

// Handling emergencies assigned by the orchestrator.
// Arrive at the emergency location, start the rescue, gather emergency info, and evaluate the situation.
+!handle(Id, X, Y)[source(orchestrator)] <-
	!go_to(X, Y);
	start_rescue(Id);
	?emergency_info(Id, Type, Severity, Victims);
	!evaluate(Id, Type, Severity, Victims).

// If the agent is already at the target location, do nothing.
+!go_to(X, Y)[source(S)] : (S == self | S == orchestrator) & at(X, Y).

// If the agent is not at the target location, move towards it (internal action) and recursively check again.
+!go_to(X, Y)[source(S)] : (S == self | S == orchestrator) <-
    move_towards(X, Y);
    !go_to(X, Y).