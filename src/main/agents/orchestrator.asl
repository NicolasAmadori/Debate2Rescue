/*  Recap:
	Percepts: emergency(Id, X, Y), rescuer(R, X, Y), pilot(P, X, Y)

*/


/* Rules */

// D is the Manhattan distance between the cells (AX, AY) and (X, Y)
distance(AX, AY, X, Y, D) :- D = math.abs(AX - X) + math.abs(AY - Y).

/* Plans */

+emergency(Id, X, Y) <-
	!assign(Id, X, Y).

-emergency(Id, _, _) <-
	.abolish(assigned(_, Id));
	.abolish(granted(_, Id));
	.abolish(request(Id, _));
	.print("Emergency ", Id, " is over").

// a rescuer is free again, give it a waiting emergency, if any
+free(rescuer)[source(R)] <-
	-free(rescuer)[source(R)];
	+availableRescuer(R);
	!assign_waiting.

+free(pilot)[source(P)] <-
	-free(pilot)[source(P)];
	+availablePilot(P);
	!assign_resource.

// atomic so two emergencies arriving together cannot get the same rescuer
@assign[atomic]
+!assign(Id, X, Y) : not assigned(_, Id) & availableRescuer(_) <-
	.findall(d(D, R), availableRescuer(R) & rescuer(R, RX, RY) & distance(RX, RY, X, Y, D), Candidates);
	.min(Candidates, d(D, R));
	+assigned(R, Id);
	-availableRescuer(R);
	.print("Emergency ", Id, " assigned to ", R, " (distance ", D, ")");
	.send(R, achieve, handle(Id, X, Y)).

+!assign(Id, _, _) : assigned(_, Id).

+!assign(Id, _, _) <-
	.print("No free rescuer for emergency ", Id, ", it will wait").

// the emergency waiting for the longest time (lowest id) goes first
@assign_waiting[atomic]
+!assign_waiting <-
	.findall(Id, emergency(Id, _, _) & not assigned(_, Id), Waiting);
	if (Waiting \== []) {
		.min(Waiting, Id);
		?emergency(Id, X, Y);
		!assign(Id, X, Y);
	}.

// a rescuer asks for a helicopter, for now the first one asking gets it
// Sends type, severity and victims count
+request(Id, helicopter, [T, S, V]) <-
	!grant(Id).

// atomic so two requests arriving together cannot get the same pilot
@grant[atomic]
+!grant(Id) : request(Id, helicopter, [_, _ ,_])[source(R)] & availablePilot(_) & emergency(Id, X, Y) <-
	.findall(d(D, P), availablePilot(P) & pilot(P, PX, PY) & distance(PX, PY, X, Y, D), Candidates);
	.min(Candidates, d(D, P));
	-request(Id, helicopter, [_, _, _])[source(R)];
	-availablePilot(P);
	+granted(P, Id);
	.print("Helicopter ", P, " granted to ", R, " for emergency ", Id, " (distance ", D, ")");
	.send(R, tell, granted(Id, helicopter));
	.send(P, achieve, move_to_emergency(Id, X, Y)).

// no free pilot, the request stays in the belief base until one is free
+!grant(Id) <-
	.print("No free helicopter for emergency ", Id, ", the request will wait").

@assign_resource[atomic]
+!assign_resource <-
	.findall(Id, request(Id, helicopter, [_, _, _]), Waiting);
	if (Waiting \== []) {
		.min(Waiting, Id);
		!grant(Id);
	}.
