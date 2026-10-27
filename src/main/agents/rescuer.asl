/* Initial beliefs */

/* Emergency type to required resource mapping */
resource_for(fire, fire_truck).
resource_for(medical, ambulance).
resource_for(hostage, police_car).
resource_for(aggression, police_car).
resource_for(hazmat, hazmat_team).
resource_for(bomb, bomb_squad).
resource_for(armed_person, army).
resource_for(disaster, excavator).
resource_for(missing, police_helicopter).
resource_for(evacuation, helicopter).

// Order in which the reasons are played in a debate, which reason wins is decided by arg2p
reasons([waiting, severity, trapped, escalating, victims]).

// Mapping from boolean percept values (trapped, escalating) to debate facts
trapped_fact(true, [trapped]).
trapped_fact(false, []).
trapped_fact(_, []).

escalating_fact(true, [escalating]).
escalating_fact(false, []).
escalating_fact(_, []).

/* Rules */

// My value V for reason R. The waiting value is the number of denials.
value(Id, waiting, N) :- denials(Id, N).
value(Id, severity, S) :- requesting(Id, _, F) & .member(severity(S), F).
value(Id, trapped, 1) :- requesting(Id, _, F) & .member(trapped, F).
value(Id, trapped, 0) :- requesting(Id, _, F) & not .member(trapped, F).
value(Id, escalating, 1) :- requesting(Id, _, F) & .member(escalating, F).
value(Id, escalating, 0) :- requesting(Id, _, F) & not .member(escalating, F).
value(Id, victims, V) :- requesting(Id, _, F) & .member(victims(V), F).

// I attack only with a reason I really have, I am not trapped is not a reason
worth(waiting, N) :- N > 0.
worth(severity, _).
worth(trapped, 1).
worth(escalating, 1).
worth(victims, V) :- V > 0.

// F are the facts claimed for emergency E in the debate for RT
facts(RT, E, F) :- move(RT, _, E, claim(F)).

// M attacks my claim, or rejects one of my rejects, and I did not reject it yet
// the moves of who conceded do not count, its claim is gone
against_me(RT, Id, M) :-
	move(RT, M, A, attack(Id, _)) & facts(RT, A, _) & not move(RT, _, Id, reject(M, _)).
against_me(RT, Id, M) :-
	move(RT, M, A, reject(T, _)) & facts(RT, A, _) & move(RT, T, Id, _) & not move(RT, _, Id, reject(M, _)).

// R rejects move M: my emergency is ahead of its author's for R, which is stronger than the reason of M
// the first R found is the strongest
defense(RT, Id, M, R) :-
	move(RT, M, A, C) & reason(C, RM) & facts(RT, Id, F) & facts(RT, A, FA) &
	beats(R, F, FA) & stronger(R, RM).

// I did not attack the winning emergency W yet, my emergency is ahead of it for R
// the first R found is the strongest
can_attack(RT, Id, W, R) :-
	facts(RT, W, FW) & not move(RT, _, Id, attack(W, _)) & facts(RT, Id, F) & beats(R, F, FW).

/* Initial goals */

!start.

/* Plans */

// Plan for starting the agent and joining the model and notifying the orchestrator.
+!start <-
	join_as_rescuer;
	.send(orchestrator, tell, free(rescuer)).

// Handling emergencies assigned by the orchestrator.
// Arrive at the emergency location, start the rescue, gather emergency info, and evaluate the situation.
+!handle(Id, X, Y)[source(orchestrator)] <-
	!go_to(X, Y);
	start_rescue(Id);
	?emergency_info(Id, Type, Severity, Victims, Trapped, Escalating);
	!evaluate(Id, Type, Severity, Victims, Trapped, Escalating).

// Requesting the resource required by the emergency type.
+!evaluate(Id, Type, Severity, Victims, Trapped, Escalating) : resource_for(Type, Resource) <-
	!make_request(Id, Resource, Severity, Victims, Trapped, Escalating).

// Fallback: if the emergency type has no mapping, request a helicopter.
+!evaluate(Id, Type, Severity, Victims, Trapped, Escalating) <-
	!make_request(Id, helicopter, Severity, Victims, Trapped, Escalating).

+!make_request(Id, RT, Severity, Victims, Trapped, Escalating) <-
	?trapped_fact(Trapped, FT);
	?escalating_fact(Escalating, FE);
	.concat(FT, FE, [severity(Severity), victims(Victims)], Facts);
	+requesting(Id, RT, Facts);
	+denials(Id, 0);
	.send(orchestrator, tell, request(Id, RT)).

/* Debate */

// First turn: claim the resource with the facts of the emergency.
+!your_turn(RT, _)[source(orchestrator)] : requesting(Id, RT, _) & not facts(RT, Id, _) & claim_facts(Id, RT, F) <-
	.send(orchestrator, argue, claim(RT, F)).

// A move against me that I cannot reject: my claim cannot win, withdraw from the debate.
+!your_turn(RT, _)[source(orchestrator)] : requesting(Id, RT, _) & against_me(RT, Id, M) & not defense(RT, Id, M, _) <-
	.send(orchestrator, concede, concede(RT)).

// Defend against a move with the strongest reason.
+!your_turn(RT, _)[source(orchestrator)] : requesting(Id, RT, _) & against_me(RT, Id, M) & defense(RT, Id, M, R) <-
	.send(orchestrator, reject, reject(RT, M, R)).

// Attack the winning emergency with the strongest reason.
+!your_turn(RT, W)[source(orchestrator)] : requesting(Id, RT, _) & can_attack(RT, Id, W, R) <-
	.send(orchestrator, attack, attack(RT, W, R)).

// Nothing new to say.
+!your_turn(RT, _)[source(orchestrator)] <-
	.send(orchestrator, tell, pass(RT)).

// Atomic so the move is stored before the next turn is handled.
@relay_move[atomic]
+!kqml_received(orchestrator, Performative, move(RT, M, E, C), _) : .member(Performative, [argue, attack, reject]) <-
	+move(RT, M, E, C).

// Who concedes withdraws its claim, so its moves and the ones against it stop counting.
@relay_concede[atomic]
+!kqml_received(orchestrator, concede, move(RT, _, E, concede), _) <-
	.abolish(move(RT, _, E, claim)).

// Denied the resource: the request waits for the next debate.
+denied(Id, RT)[source(orchestrator)] : denials(Id, N) <-
	-denied(Id, RT)[source(orchestrator)];
	.abolish(move(RT, _, _, _));
	-denials(Id, N);
	+denials(Id, N + 1).

// When granted the resource, the agent waits for the operation to end and then goes home.
+granted(Id, RT)[source(orchestrator)] <-
	-granted(Id, RT)[source(orchestrator)];
	.abolish(move(RT, _, _, _));
	-requesting(Id, RT, _);
	-denials(Id, _).

+emergency_resolved(Id) <-
    -emergency_resolved(Id);
    !go_home.

// If the agent is already at the target location, do nothing.
+!go_to(X, Y)[source(self)] : at(X, Y).

// If the agent is not at the target location, move towards it (internal action) and recursively check again.
+!go_to(X, Y)[source(self)] <-
    move_towards(X, Y);
    !go_to(X, Y).

+!go_home <-
	?station(X, Y);
	!go_to(X, Y);
	.send(orchestrator, tell, free(rescuer)).