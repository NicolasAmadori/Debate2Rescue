/*
    Variables:
        E -> emergency id
        O -> objection id
        R -> reason

    Facts to be handled Java-side:
        contender(E), severity(E, S), victims(E, V)
        attack(O, E1, E2, R): objection O by emergency E1 attacks the claim of E with reason R
        reject(O1, E1, O2, R1): objection O1 by emergency E1 rejects the objection O2 with reason R1

*/

/* Claims */
claim(E) : prolog(contender(E)) => grant(E).
% the resource goes to one emergency only, excluding the claims of younger emergencies
exclude(E1, E2) : grant(E1), prolog(contender(E2)), prolog(E1 < E2) -> -grant(E2).

/* Defeasible rules */
% an attack is valid if its reason really puts the attacker ahead of the target
objection(O) : prolog(attack(O, E1, E2, R)), prolog(beats(R, E1, E2)) => undercut(claim(E2)).

% a reject is valid if its reason puts the author ahead and is stronger than the rejected one
objection(O1) : prolog(reject(O1, E1, O2, R1)), prolog(objection_by(O2, E2, R2)), prolog(beats(R1, E1, E2)), prolog(stronger(R1, R2)) => undercut(objection(O2)).

/* Strict rules */

/* Preferences */
% Older emergencies should go first
% This is used as a tie-breaker, everything else should come from the debate
sup(claim(E1), claim(E2)) :- contender(E1), contender(E2), E1 < E2.


/* Reasons hierarchy, the higher the strength value, the stronger the reason */
strength(waiting, 5).
strength(severity, 4).
strength(trapped, 3).
strength(escalating, 2).
strength(victims, 1).

stronger(R1, R2) :- strength(R1, S1), strength(R2, S2), S1 > S2.

/* The objection O was made by emergency E with reason R */
objection_by(O, E, R) :- attack(O, E, _, R).
objection_by(O, E, R) :- reject(O, E, _, R).

/* Reason R puts emergency E1 ahead of E2. */
beats(waiting, E1, E2) :- waiting(E1, W1), waiting(E2, W2), W1 > W2.
beats(severity, E1, E2) :- severity(E1, S1), severity(E2, S2), S1 < S2.
beats(trapped, E1, E2) :- trapped(E1), \+ trapped(E2).
beats(escalating, E1, E2) :- escalating(E1), \+ escalating(E2).
beats(victims, E1, E2) :- victims(E1, V1), victims(E2, V2), V1 > V2.