/* Initial goals */

!start.

/* Plans */

+!start <-
	join_as_pilot;
	.send(orchestrator, tell, free(pilot)).

+!move_to_emergency(Id, R, X, Y)[source(orchestrator)] <-
	!go_to(X, Y);
	?emergency_info(Id, _, Severity, Victims, Trapped, Escalating, Size, CollateralRisk);
	wait_steps(2*Severity + Victims);
	resolve_emergency(Id);
	.send(R, tell, emergency_resolved(Id));
	?station(SX, SY);
	!go_to(SX, SY);
	.send(orchestrator, tell, free(pilot)).

// Already at the target location
+!go_to(X, Y)[source(self)] : at(X, Y).

// Move towards the target location
+!go_to(X, Y)[source(self)] <-
	move_towards(X, Y);
	!go_to(X, Y).