/* Initial goals */

!start.

/* Plans */

+!start <-
	join(pilot);
	.send(orchestrator, tell, free(pilot)).

+!move_to_emergency(Id, X, Y)[source(orchestrator)] <-
	!go_to(X, Y);
	?emergency_info(Id, _, Severity, Victims);
	wait_steps(2*Severity + Victims);
	resolve_emergency(Id);
	?station(SX, SY);
	!go_to(SX, SY);
	.abolish(emergency_info(_, _, _, _)); // remove possible residual unresolved emergency info
	.send(orchestrator, tell, free(pilot)).

// Already at the target location
+!go_to(X, Y)[source(orchestrator), source(self)] : at(X, Y).

// Move towards the target location
+!go_to(X, Y)[source(orchestrator), source(self)] <-
	move_towards(X, Y);
	!go_to(X, Y).