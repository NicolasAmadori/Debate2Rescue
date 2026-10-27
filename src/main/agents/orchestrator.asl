/*  Recap:
	Percepts: emergency(Id, X, Y), rescuer(R, X, Y), pilot(P, RT, X, Y)

	Order is a list of contenders c(Id, R)
*/

// steps for the request collection
window_steps(5).

// id of the last move made in a debate
move_count(0).

/* Rules */

// D is the Bresenham distance from (AX, AY) to (X, Y), obstacles are not considered
distance(AX, AY, X, Y, D) :- D = math.max(math.abs(AX - X), math.abs(AY - Y)).

// arg2p accepts the claim of emergency W, Labels are the labels of all the claims and objections
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
	.count(availableResource(RT, _), Free);
	.length(Order, N);
	if (N <= Free) {
		for (.member(c(Id, _), Order)) {
			!grant(Id, RT);
		}
	} else {
		.print("Debate for ", RT, " between ", Order);
		+debate(RT, Order);
		for (.member(c(Id, _), Order)) {
			!add_claim(RT, Id);
		}
		!next_turn(RT);
	}.

// a window or a debate is open, no pilot of the type is free or no request is waiting
+!deliberate(_).

// create claims from teh requests and forward to contenders
+!add_claim(RT, Id) : debate(RT, Order) & move_count(N) <-
	-+move_count(N + 1);
	+move(RT, N + 1, Id, claim);
	for (.member(c(_, R), Order)) {
		.send(R, argue, move(RT, N + 1, Id, claim));
	}.

/* Debate */

// the turn goes to the first losing contender who did not concede
+!next_turn(RT) : debate(RT, Order) <-
	if (winning(RT, W, Labels)) {
		.print(RT, " debate labels: ", Labels);
	} else {
		// arg2p accepted no claim, the oldest emergency is winning
		.min(Order, c(W, _));
		.print(RT, " debate: no claim accepted, emergency ", W, " is the oldest");
	}
	if (.member(c(Id, R), Order) & Id \== W & not passed(RT, Id)) {
		!give_turn(RT, Id, R, W);
	} else {
		// everyone has passed
		!conclude(RT, W);
	}.

// W is the emergency currently winning
+!give_turn(RT, Id, R, W) <-
	+turn(RT, Id, R);
	.send(R, achieve, your_turn(RT, W)).

// who just had the turn goes behind the others
+!end_turn(RT, Id, R) : debate(RT, Order) <-
	-turn(RT, Id, R);
	.delete(c(Id, R), Order, Others);
	.concat(Others, [c(Id, R)], NewOrder);
	-debate(RT, _);
	+debate(RT, NewOrder).

// the attacker challenges the claim of emergency E on Reason, revealing its Value
+!kqml_received(R, challenge, challenge(RT, E, Reason, Value), _) : turn(RT, _, R) <-
	!record_move(RT, challenge, challenge(E, Reason, Value)).

// the target of the challenge M answers it, revealing its Value for Reason
+!kqml_received(R, answer, answer(RT, M, Reason, Value), _) : turn(RT, _, R) <-
	!record_move(RT, answer, answer(M, Reason, Value)).

// the contender withdraws and waits for the next debate
// without its claim its moves stop counting and the winner may change, so who passed can speak again
+!kqml_received(R, concede, concede(RT), _) : turn(RT, Id, R) & debate(RT, Order) & move_count(N) <-
	-+move_count(N + 1);
	.print(RT, " debate, move ", N + 1, ": ", R, " (emergency ", Id, ") concedes");
	-turn(RT, Id, R);
	.abolish(move(RT, _, Id, claim));
	.abolish(passed(RT, _));
	.delete(c(Id, R), Order, Others);
	-debate(RT, _);
	+debate(RT, Others);
	for (.member(c(_, OR), Others)) {
		.send(OR, concede, move(RT, N + 1, Id, concede));
	}
	.send(R, tell, denied(Id, RT));
	!next_turn(RT).

// the contender has nothing to say for now
+pass(RT)[source(R)] : turn(RT, Id, R) <-
	-pass(RT)[source(R)];
	.print(RT, " debate: ", R, " (emergency ", Id, ") passes");
	+passed(RT, Id);
	!end_turn(RT, Id, R);
	!next_turn(RT).

// stores the move of who has the turn and relays it to the contenders with the same performative
// the move may change the winner, so who passed can speak again
+!record_move(RT, Performative, Content) : turn(RT, Id, R) & debate(RT, Order) & move_count(N) <-
	-+move_count(N + 1); // replaces move_count
	+move(RT, N + 1, Id, Content);
	.print(RT, " debate, move ", N + 1, ": ", R, " (emergency ", Id, ") ", Performative, " ", Content);
	for (.member(c(_, CR), Order)) {
		.send(CR, Performative, move(RT, N + 1, Id, Content));
	}
	.abolish(passed(RT, _));
	!end_turn(RT, Id, R);
	!next_turn(RT).

// the losers wait for the next debate, the winner gets the resource
+!conclude(RT, W) : debate(RT, Order) <-
	.print(RT, " debate won by emergency ", W);
	for (.member(c(L, LR), Order) & L \== W) {
		.send(LR, tell, denied(L, RT));
	}
	.abolish(move(RT, _, _, _));
	.abolish(passed(RT, _));
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
