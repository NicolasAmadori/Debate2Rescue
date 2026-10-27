/*  Recap:
	Percepts: emergency(E, X, Y), rescuer(R, X, Y), pilot(P, RT, X, Y)

	Order is a list of contenders c(E, R)
*/

// steps for the request collection
window_steps(5).

// id of the last debate started
debate_count(0).

// id of the last move made in a debate
move_count(0).

/* Rules */

// D is the Bresenham distance from (AX, AY) to (X, Y), obstacles are not considered
distance(AX, AY, X, Y, D) :- D = math.max(math.abs(AX - X), math.abs(AY - Y)).

// Labels are the labels arg2p gives to the claims
labels(RT, Labels) :-
	.findall(move(M, A, C), move(RT, M, A, C), Moves) &
	arg.deliberate(Moves, Labels).

// X is ahead of E
ahead(E, X, Labels) :- .member(label(rank(_, X, E), L), Labels) & L \== out.

// Reason is why the winner is winning
winning_reason(Labels, Reason) :-
	.findall(C, .member(label(C, in), Labels) & (C = rank(_, _, _) | C = life_first(_, _) | C = no_time(_, _)), Reason).

// E challenges who is ahead of it, otherwise the winning emergency W
target(E, _, Labels, X) :- ahead(E, X, Labels).
target(E, W, _, W) :- E \== W.

/* Plans */

+emergency(E, X, Y) <-
	!assign(E, X, Y).

-emergency(E, _, _) <-
	.abolish(assigned(_, E));
	.abolish(granted(_, E));
	.abolish(request(E, _));
	.print("Emergency ", E, " is over").

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
+!assign(E, X, Y) : not assigned(_, E) & availableRescuer(_) <-
	.findall(d(D, R), availableRescuer(R) & rescuer(R, RX, RY) & distance(RX, RY, X, Y, D), Candidates);
	.min(Candidates, d(D, R));
	+assigned(R, E);
	assign(R, E);
	-availableRescuer(R);
	.print("Emergency ", E, " assigned to ", R, " (distance ", D, ")");
	.send(R, achieve, handle(E, X, Y)).

+!assign(E, _, _) : assigned(_, E).

+!assign(E, _, _) <-
	.print("No free rescuer for emergency ", E, ", it will wait").

// the emergency waiting for the longest time (lowest id) goes first
@assign_waiting[atomic]
+!assign_waiting <-
	.findall(E, emergency(E, _, _) & not assigned(_, E), Waiting);
	if (Waiting \== []) {
		.min(Waiting, E);
		?emergency(E, X, Y);
		!assign(E, X, Y);
	}.

/* Resource requests */

// a window or a debate is open for the type, the request joins the window or waits for the next debate
+request(_, RT) : window(RT) | debate(RT, _).

@request[atomic]
+request(E, RT) <-
	+window(RT);
	.print("Window opened for ", RT, " by request ", E);
	!!close_window(RT).

+!close_window(RT) : window_steps(S) <-
	wait_steps(S);
	-window(RT);
	.findall(E, request(E, RT), Requests);
	.print("Window closed for ", RT, ", requests: ", Requests);
	!deliberate(RT).

// with one request the resource is granted, with more a debate starts
@deliberate[atomic]
+!deliberate(RT) : not window(RT) & not debate(RT, _) & availableResource(RT, _) & request(_, RT) <-
	.findall(c(E, R), request(E, RT)[source(R)], Contenders);
	.sort(Contenders, Order);
	.count(availableResource(RT, _), Free);
	.length(Order, N);
	if (N <= Free) {
		for (.member(c(E, _), Order)) {
			!grant(E, RT);
		}
	} else {
		?debate_count(LastDId);
		DId = LastDId + 1;
		-+debate_count(DId);
		+debate_id(RT, DId);
		+debate(RT, Order);
		log_debate_start(DId, RT, Order);
		.print("Debate #", DId, " for ", RT, " between ", Order);
		for (.member(c(E, R), Order)) {
			!add_claim(RT, E, R);
		}
		!next_turn(RT);
	}.

// a window or a debate is open, no pilot of the type is free or no request is waiting
+!deliberate(_).

// create claims from the requests and forward to contenders
+!add_claim(RT, E, R) : debate(RT, Order) & debate_id(RT, DId) & move_count(N) <-
	-+move_count(N + 1);
	+move(RT, N + 1, E, claim);
	log_debate_move(DId, N + 1, R, E, claim, claim);
	for (.member(c(_, CR), Order)) {
		.send(CR, argue, move(RT, N + 1, E, claim));
	}.

/* Debate */

// the turn goes to the first contender whose claim is not accepted, until the others concede
+!next_turn(RT) : debate(RT, Order) & debate_id(RT, DId) & labels(RT, Labels) <-
	if (.member(label(claim(W), in), Labels)) {
		?winning_reason(Labels, Reason);
		log_debate_status(DId, W, Reason);
		.print(RT, " debate #", DId, ": emergency ", W, " is winning, because of: ", Reason);
	} else {
		// arg2p accepted no claim, the oldest emergency is winning and it can speak too
		.min(Order, c(W, _));
		log_debate_status(DId, W, tie_breaker_oldest);
		.print(RT, " debate #", DId, ": no claim accepted, emergency ", W, " is the oldest");
	}
	if (.member(c(E, R), Order) & not .member(label(claim(E), in), Labels) & target(E, W, Labels, T)) {
		!give_turn(RT, E, R, T);
	} else {
		!conclude(RT, W);
	}.

// T is the emergency E should challenge
+!give_turn(RT, E, R, T) <-
	+turn(RT, E, R);
	.send(R, achieve, your_turn(RT, T)).

// who just had the turn goes behind the others
+!end_turn(RT, E, R) : debate(RT, Order) <-
	-turn(RT, E, R);
	.delete(c(E, R), Order, Others);
	.concat(Others, [c(E, R)], NewOrder);
	-debate(RT, _);
	+debate(RT, NewOrder).

// the attacker challenges the claim of emergency E on Reason, revealing its Value
+!kqml_received(R, challenge, challenge(RT, E, Reason, Value), _) : turn(RT, _, R) <-
	!record_move(RT, challenge, challenge(E, Reason, Value)).

// the target of the challenge M answers it, revealing its Value for Reason
+!kqml_received(R, answer, answer(RT, M, Reason, Value), _) : turn(RT, _, R) <-
	!record_move(RT, answer, answer(M, Reason, Value)).

// the contender withdraws and waits for the next debate
// without its claim its moves stop counting and the winner may change
+!kqml_received(R, concede, concede(RT), _) : turn(RT, E, R) & debate(RT, Order) & debate_id(RT, DId) & move_count(N) <-
	-+move_count(N + 1);
	log_debate_move(DId, N + 1, R, E, concede, "withdrew claim");
	.print(RT, " debate #", DId, ", move ", N + 1, ": ", R, " (emergency ", E, ") concedes");
	-turn(RT, E, R);
	.abolish(move(RT, _, E, claim));
	.delete(c(E, R), Order, Others);
	-debate(RT, _);
	+debate(RT, Others);
	for (.member(c(_, OR), Others)) {
		.send(OR, concede, move(RT, N + 1, E, concede));
	}
	.send(R, tell, denied(E, RT));
	!next_turn(RT).

// stores the move of who has the turn and relays it to the contenders with the same performative
+!record_move(RT, Performative, Content) : turn(RT, E, R) & debate(RT, Order) & debate_id(RT, DId) & move_count(N) <-
	-+move_count(N + 1); // replaces move_count
	+move(RT, N + 1, E, Content);
	log_debate_move(DId, N + 1, R, E, Performative, Content);
	.print(RT, " debate #", DId, ", move ", N + 1, ": ", R, " (emergency ", E, ") ", Performative, " ", Content);
	for (.member(c(_, CR), Order)) {
		.send(CR, Performative, move(RT, N + 1, E, Content));
	}
	!end_turn(RT, E, R);
	!next_turn(RT).

// the losers wait for the next debate, the winner gets the resource
+!conclude(RT, W) : debate(RT, Order) & debate_id(RT, DId) & labels(RT, Labels) <-
	if (.member(label(claim(W), in), Labels)) {
		?winning_reason(Labels, Reason);
	} else {
		Reason = tie_breaker_oldest;
	}
	.findall(L, .member(c(L, _), Order) & L \== W, Losers);
	log_debate_end(DId, RT, W, Losers, Reason);
	.print(RT, " debate #", DId, " won by emergency ", W);
	for (.member(c(L, LR), Order) & L \== W) {
		.send(LR, tell, denied(L, RT));
	}
	.abolish(move(RT, _, _, _));
	-debate(RT, _);
	-debate_id(RT, _);
	!grant(W, RT);
	!deliberate(RT).

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
+!grant(E, RT) : request(E, RT)[source(R)] & availableResource(RT, _) & emergency(E, X, Y) <-
	.findall(d(D, P), availableResource(RT, P) & pilot(P, RT, PX, PY) & distance(PX, PY, X, Y, D), Candidates);
	.min(Candidates, d(D, P));
	-request(E, RT)[source(R)];
	-availableResource(RT, P);
	+granted(P, E);
	.print("Resource type ", RT, " with pilot ", P, " granted to ", R, " for emergency ", E, " (distance ", D, ")");
	.send(R, tell, granted(E, RT));
	.send(P, achieve, move_to_emergency(E, R, X, Y)).

// no free pilot, the request stays in the belief base until one is free
+!grant(E, RT) <-
	.print("No free resource for emergency ", E, ", the request will wait").
