/*
    Facts to be handled Java-side:
        contender(E), severity(E, S), victims(E, V)
        attack(M, A, E, R): objection M by emergency A attacks the claim of E with reason R
        reject(M, A, T, R): objection M by emergency A rejects the objection T with reason R

*/


/* Defeasible rules */
claim(E) : prolog(contender(E)) => grant(E).

% an attack is valid if its reason really puts the attacker ahead of the target
objection(M) : prolog(attack(M, A, E, R)), prolog(beats(R, A, E)) => undercut(claim(E)).

% a reject is valid if its reason puts the author ahead and is stronger than the rejected one
objection(M) : prolog(reject(M, A, T, R)), prolog(objection_by(T, B, RT)), prolog(beats(R, A, B)), prolog(stronger(R, RT)) => undercut(objection(T)).


/* Strict rules */
exclude(E1, E2) : grant(E1), prolog(contender(E2)), prolog(E1 \== E2) -> -grant(E2).


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

/* The objection M was made by emergency A with reason R */
objection_by(M, A, R) :- attack(M, A, _, R).
objection_by(M, A, R) :- reject(M, A, _, R).

/* Reason R puts emergency E1 ahead of E2. */
beats(waiting, E1, E2) :- waiting(E1, W1), waiting(E2, W2), W1 > W2.
beats(severity, E1, E2) :- severity(E1, S1), severity(E2, S2), S1 < S2.
beats(trapped, E1, E2) :- trapped(E1), \+ trapped(E2).
beats(escalating, E1, E2) :- escalating(E1), \+ escalating(E2).
beats(victims, E1, E2) :- victims(E1, V1), victims(E2, V2), V1 > V2.