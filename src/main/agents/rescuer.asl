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
reasons([waiting, severity, collateral_risk, trapped, escalating, size, victims]).

// Mapping from boolean percept values (trapped, escalating) to debate facts
trapped_fact(true, [trapped]).
trapped_fact(false, []).
trapped_fact(_, []).

escalating_fact(true, [escalating]).
escalating_fact(false, []).
escalating_fact(_, []).

collateral_fact(true, [collateral_risk]).
collateral_fact(false, []).
collateral_fact(_, []).

/* Rules */

// My value V for reason R. The waiting value is the number of denials.
value(Id, waiting, N) :- denials(Id, N).
value(Id, severity, S) :- requesting(Id, _, F) & .member(severity(S), F).
value(Id, collateral_risk, 1) :- requesting(Id, _, F) & .member(collateral_risk, F).
value(Id, collateral_risk, 0) :- requesting(Id, _, F) & not .member(collateral_risk, F).
value(Id, trapped, 1) :- requesting(Id, _, F) & .member(trapped, F).
value(Id, trapped, 0) :- requesting(Id, _, F) & not .member(trapped, F).
value(Id, escalating, 1) :- requesting(Id, _, F) & .member(escalating, F).
value(Id, escalating, 0) :- requesting(Id, _, F) & not .member(escalating, F).
value(Id, size, S) :- requesting(Id, _, F) & .member(size(S), F).
value(Id, victims, V) :- requesting(Id, _, F) & .member(victims(V), F).

// I challenge only with a reason I really have, I am not trapped is not a reason
worth(waiting, N) :- N > 0.
worth(severity, _).
worth(collateral_risk, 1).
worth(trapped, 1).
worth(escalating, 1).
worth(size, S) :- S > 0.
worth(victims, V) :- V > 0.

// Emergency E has a claim in the debate for RT, who conceded has not
contender(RT, E) :- move(RT, _, E, claim).

// Emergency E revealed its value for reason R
revealed(RT, E, R) :- move(RT, _, E, challenge(_, R, _)).
revealed(RT, E, R) :- move(RT, _, E, answer(_, R, _)).

// Reason R was raised between A and B, by either of them
raised(RT, A, B, R) :- move(RT, _, A, challenge(B, R, _)).
raised(RT, A, B, R) :- move(RT, _, B, challenge(A, R, _)).

// The challenge M by A on reason R against my emergency is not answered
unanswered(RT, Id, M, A, R) :-
	move(RT, M, A, challenge(Id, R, _)) & contender(RT, A) & not revealed(RT, Id, R).

// The values I can reveal to answer the challenge M on R: my value for R first,
// then the other ones I did not reveal yet, they can make an exception hold
answers(RT, Id, M, R, [answer(M, R, V) | Others]) :-
	value(Id, R, V) &
	.findall(answer(M, R2, V2), reasons(Reasons) & .member(R2, Reasons) & R2 \== R & not revealed(RT, Id, R2) & value(Id, R2, V2), Others).

// The reasons I really have that were not raised between my emergency and T yet, the strongest first
challenges(RT, Id, T, Challenges) :-
	.findall(challenge(T, R, V), reasons(Reasons) & .member(R, Reasons) & not raised(RT, Id, T, R) & value(Id, R, V) & worth(R, V), Challenges).

// The moves of the debate, as arg.deliberate and arg.choose want them
moves(RT, Moves) :- .findall(move(M, E, C), move(RT, M, E, C), Moves).

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
	?emergency_info(Id, Type, Severity, Victims, Trapped, Escalating, Size, CollateralRisk);
	!evaluate(Id, Type, Severity, Victims, Trapped, Escalating, Size, CollateralRisk).

// Requesting the resource required by the emergency type.
+!evaluate(Id, Type, Severity, Victims, Trapped, Escalating, Size, CollateralRisk) : resource_for(Type, Resource) <-
	!make_request(Id, Resource, Severity, Victims, Trapped, Escalating, Size, CollateralRisk).

// Fallback: if the emergency type has no mapping, request a helicopter.
+!evaluate(Id, Type, Severity, Victims, Trapped, Escalating, Size, CollateralRisk) <-
	!make_request(Id, helicopter, Severity, Victims, Trapped, Escalating, Size, CollateralRisk).

+!make_request(Id, RT, Severity, Victims, Trapped, Escalating, Size, CollateralRisk) <-
	?trapped_fact(Trapped, FT);
	?escalating_fact(Escalating, FE);
	?collateral_fact(CollateralRisk, FC);
	.concat(FT, FE, FC, [severity(Severity), victims(Victims), size(Size)], Facts);
	+requesting(Id, RT, Facts);
	+denials(Id, 0);
	.send(orchestrator, tell, request(Id, RT)).

/* Debate */

// A move is played only if arg2p says it improves my position against the other emergency:
// first answer a challenge against me, then challenge T, otherwise nothing I can say helps and I withdraw.
+!your_turn(RT, T)[source(orchestrator)] : requesting(Id, RT, _) & moves(RT, Moves) <-
	if (unanswered(RT, Id, M, A, R) & answers(RT, Id, M, R, Answers) & arg.choose(Moves, Id, A, Answers, answer(M, R2, V))) {
		.send(orchestrator, answer, answer(RT, M, R2, V));
	} elif (challenges(RT, Id, T, Challenges) & arg.choose(Moves, Id, T, Challenges, challenge(T, AR, AV))) {
		.send(orchestrator, challenge, challenge(RT, T, AR, AV));
	} else {
		.send(orchestrator, concede, concede(RT));
	}.

+!your_turn(RT, _)[source(orchestrator)] <-
	.send(orchestrator, concede, concede(RT)).

// Atomic so the move is stored before the next turn is handled.
@relay_move[atomic]
+!kqml_received(orchestrator, Performative, move(RT, M, E, C), _) : .member(Performative, [argue, challenge, answer]) <-
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