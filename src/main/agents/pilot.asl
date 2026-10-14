/* Initial goals */

!start.

/* Plans */

+!start <-
	join(pilot);
	.send(orchestrator, tell, free(pilot)).

// Already at the target location
+!go_to(X, Y)[source(orchestrator), source(self)] : at(X, Y).

// Move towards the target location
+!go_to(X, Y)[source(orchestrator), source(self)] <-
	move_towards(X, Y);
	!go_to(X, Y).