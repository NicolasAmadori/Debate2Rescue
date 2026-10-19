/*  Recap:
	Percepts: emergency(Id, X, Y), rescuer(R, X, Y), pilot(P, RT, X, Y)

*/

// steps for the request collection
window_steps(5).

// id of the last move made in a debate
move_count(0).

/* Rules */

// D is the Manhattan distance between the cells (AX, AY) and (X, Y)
distance(AX, AY, X, Y, D) :- D = math.abs(AX - X) + math.abs(AY - Y).

// arg2p accepts the claim of emergency W, Labels are the labels of all the claims
winning(RT, W, Labels) :-
	.findall(move(M, A, C), move(RT, M, A, C), Moves) &
	arg.deliberate(Moves, Labels) &
	.member(label(claim(W), in), Labels).

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

+free(pilot)[source(P)] : pilot(P, RT, _, _) <-
	-free(pilot)[source(P)];
	+availableResource(RT, P);
	!deliberate(RT).

// atomic so two emergencies arriving together cannot get the same rescuer
@assign[atomic]
+!assign(Id, X, Y) : not assigned(_, Id) & availableRescuer(_) <-
	.findall(d(D, R), availableRescuer(R) & rescuer(R, RX, RY) & distance(RX, RY, X, Y, D), Candidates);
	.min(Candidates, d(D, R));
	+assigned(R, Id);
	assign(R, Id);
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

/* Resource requests */

// a window or a debate is open for the type, the request joins the window or waits for the next debate
+request(_, RT) : window(RT) | debate(RT, _).

@request[atomic]
+request(Id, RT) <-
	+window(RT);
	.print("Window opened for ", RT, " by request ", Id);
	!!close_window(RT).

+!close_window(RT) : window_steps(S) <-
	wait_steps(S);
	-window(RT);
	.findall(Id, request(Id, RT), Requests);
	.print("Window closed for ", RT, ", requests: ", Requests);
	!deliberate(RT).

// with one request the resource is granted, with more a debate starts
@deliberate[atomic]
+!deliberate(RT) : not window(RT) & not debate(RT, _) & availableResource(RT, _) & request(_, RT) <-
	.findall(c(Id, R), request(Id, RT)[source(R)], Contenders);
	.sort(Contenders, Order);
	if (Order = [c(Id, _)]) {
		!grant(Id, RT);
	} else {
		.print("Debate for ", RT, " between ", Order);
		+debate(RT, Order);
		!next_turn(RT);
	}.

// a window or a debate is open, no pilot of the type is free or no request is waiting
+!deliberate(_).

/* Debate */

// claims phase: the oldest contender without a claim gets the turn
+!next_turn(RT) : debate(RT, Order) & .member(c(Id, R), Order) & not move(RT, _, Id, claim(_)) <-
	+turn(RT, Id, R);
	.send(R, achieve, your_turn(RT, none)).

// all the claims are in, arg2p decides
+!next_turn(RT) : winning(RT, W, Labels) <-
	.print(RT, " debate labels: ", Labels);
	!conclude(RT, W).

// arg2p accepted no claim, the oldest emergency wins
+!next_turn(RT) : debate(RT, [c(W, _) | _]) <-
	.print(RT, " debate: no claim accepted, the oldest emergency wins");
	!conclude(RT, W).

+!kqml_received(R, argue, claim(RT, Facts), _) : turn(RT, Id, R) & move_count(N) <-
	-turn(RT, Id, R);
	-+move_count(N + 1); // replaces move_count
	+move(RT, N + 1, Id, claim(Facts));
	.print(RT, " debate, move ", N + 1, ": ", R, " (emergency ", Id, ") argues ", Facts);
	!next_turn(RT).

// the losers wait for the next debate, the winner gets the resource
+!conclude(RT, W) : debate(RT, Order) <-
	.print(RT, " debate won by emergency ", W);
	for (.member(c(L, LR), Order) & L \== W) {
		.send(LR, tell, denied(L, RT));
	}
	.abolish(move(RT, _, _, _));
	-debate(RT, _);
	!grant(W, RT);
	!deliberate(RT).

// atomic so two requests arriving together cannot get the same pilot
@grant[atomic]
+!grant(Id, RT) : request(Id, RT)[source(R)] & availableResource(RT, _) & emergency(Id, X, Y) <-
	.findall(d(D, P), availableResource(RT, P) & pilot(P, RT, PX, PY) & distance(PX, PY, X, Y, D), Candidates);
	.min(Candidates, d(D, P));
	-request(Id, RT)[source(R)];
	-availableResource(RT, P);
	+granted(P, Id);
	.print("Resource type ", RT, " with pilot ", P, " granted to ", R, " for emergency ", Id, " (distance ", D, ")");
	.send(R, tell, granted(Id, RT));
	.send(P, achieve, move_to_emergency(Id, R, X, Y)).

// no free pilot, the request stays in the belief base until one is free
+!grant(Id, RT) <-
	.print("No free resource for emergency ", Id, ", the request will wait").
