/*  Recap:
	Percepts: emergency(Id, X, Y), rescuer(R, X, Y), pilot(P, RT, X, Y)

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
	.abolish(request(Id, _, _));
	.print("Emergency ", Id, " is over").

// a rescuer is free again, give it a waiting emergency, if any
+free(rescuer)[source(R)] <-
	-free(rescuer)[source(R)];
	+availableRescuer(R);
	!assign_waiting.

+free(pilot)[source(P)] : pilot(P, RT, _, _) <-
	-free(pilot)[source(P)];
	+availableResource(RT, P);
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

// a rescuer asks for a resource, for now the first one asking gets it
// Sends type, severity and victims count
+request(Id, RT, [T, S, V]) <-
	!grant(Id, RT).

// atomic so two requests arriving together cannot get the same pilot
@grant[atomic]
+!grant(Id, RT) : request(Id, RT, [_, _ ,_])[source(R)] & availableResource(RT, _) & emergency(Id, X, Y) <-
	.findall(d(D, P), availableResource(RT, P) & pilot(P, RT, PX, PY) & distance(PX, PY, X, Y, D), Candidates);
	.min(Candidates, d(D, P));
	-request(Id, RT, [_, _, _])[source(R)];
	-availableResource(RT, P);
	+granted(P, Id);
	.print("Resource type ", RT, " with pilot ", P, " granted to ", R, " for emergency ", Id, " (distance ", D, ")");
	.send(R, tell, granted(Id, RT));
	.send(P, achieve, move_to_emergency(Id, X, Y)).

// no free pilot, the request stays in the belief base until one is free
+!grant(Id, RT) <-
	.print("No free resource for emergency ", Id, ", the request will wait").

@assign_resource[atomic]
+!assign_resource <-
	.findall(Id, request(Id, RT, [_, _, _]) & availableResource(RT, _), Waiting);
	if (Waiting \== []) {
		.min(Waiting, Id);
		!grant(Id, _);
	}.
