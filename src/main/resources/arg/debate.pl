/*
    Facts to be handled Java-side:
        contender(E), severity(E, S), victims(E, V)
*/


/* Defeasible rules */
claim(E) : prolog(contender(E)) => grant(E).


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