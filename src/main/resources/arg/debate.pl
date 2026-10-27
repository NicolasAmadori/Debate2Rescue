/*
    Variables:
        E -> emergency id
        R -> reason
        V -> value

    Facts to be handled Java-side:
        contender(E): emergency E claims the resource
        val(E, R, V): the rescuer of E revealed value V for reason R
        challenged(E1, E2, R): emergency E1 challenged emergency E2 on reason R

*/

/* Claims */
claim(E) : prolog(contender(E)) => grant(E).
% the resource goes to one emergency only, excluding the claims of younger emergencies
exclude(E1, E2) : grant(E1), prolog(contender(E2)), prolog(E1 < E2) -> -grant(E2).

/* Evidence: E1 is ahead of E2 on reason R */
% both values are known and E1 has the better one, on a reason that must have been raised between E1 and E2
compare(R, E1, E2) : val(E1, R, V1), val(E2, R, V2), prolog(raised(E1, E2, R)), prolog(beats(R, V1, V2)) => ahead(R, E1, E2).

% a challenge holds as long as the target does not reveal its value for the reason
challenge(R, E1, E2) : val(E1, R, V1), prolog(challenged(E1, E2, R)) => ahead(R, E1, E2).

% revealing the value answers the challenge, from now on compare decides
answer(R, E2, E1) : val(E2, R, V2), prolog(challenged(E1, E2, R)) => undercut(challenge(R, E1, E2)).

/* Principles */
% being ahead on a reason puts E1 over E2
rank(R, E1, E2) : ahead(R, E1, E2) => over(E1, E2).

% E1 and E2 cannot be over each other
asym(E1, E2) : over(E1, E2) -> -over(E2, E1).

% who is behind someone does not get the resource
beaten(E1, E2) : over(E1, E2) => -grant(E2).

/* Preferences */
% being behind someone is stronger than the claim alone
sup(beaten(_, _), claim(_)).

% being ahead on a stronger reason wins over being ahead on a weaker one
sup(rank(R1, _, _), rank(R2, _, _)) :- stronger(R1, R2).

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

/* Reason R was raised between E1 and E2, by either of them */
raised(E1, E2, R) :- challenged(E1, E2, R).
raised(E1, E2, R) :- challenged(E2, E1, R).

/* Value V1 is better than V2 for reason R */
beats(waiting, V1, V2) :- V1 > V2.
beats(severity, V1, V2) :- V1 < V2.
% 1 if present, 0 otherwise
beats(trapped, V1, V2) :- V1 > V2.
beats(escalating, V1, V2) :- V1 > V2.
beats(victims, V1, V2) :- V1 > V2.