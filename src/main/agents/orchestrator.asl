/* Rules */

// a rescuer can take an emergency if it is free and we did not already give it one
available(R) :- free(R) & not assigned(R, _).

// D is the Manhattan distance between rescuer R and the cell (X, Y)
distance(R, X, Y, D) :- rescuer(R, RX, RY) & D = math.abs(RX - X) + math.abs(RY - Y).

/* Plans */

+emergency(Id, X, Y) <-
	.print("New emergency ", Id, " at (", X, ",", Y, ")");
	!assign(Id, X, Y).

-emergency(Id, _, _) <-
	.print("Emergency ", Id, " is over").

// a rescuer is free again, give it a waiting emergency, if any
+free(R) <-
	.abolish(assigned(R, _));
	!assign_waiting.

// atomic so two emergencies arriving together cannot get the same rescuer
@assign[atomic]
+!assign(Id, X, Y) : not assigned(_, Id) & available(_) <-
	.findall(d(D, R), available(R) & distance(R, X, Y, D), Candidates);
	.min(Candidates, d(D, R));
	+assigned(R, Id);
	.abolish(free(R));
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
