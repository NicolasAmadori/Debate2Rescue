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

// Reasons hierarchy, the higher the strength value, the stronger the reason
strength(waiting, 5).
strength(severity, 4).
strength(trapped, 3).
strength(escalating, 2).
strength(victims, 1).

/* Rules */

stronger(R1, R2) :- strength(R1, S1) & strength(R2, S2) & S1 > S2.

// Reason R puts emergency F1 ahead of F2.
// The order must match the priority of the reasons hierarchy.
beats(waiting, F1, F2) :- .member(waiting(W1), F1) & .member(waiting(W2), F2) & W1 > W2.
beats(severity, F1, F2) :- .member(severity(S1), F1) & .member(severity(S2), F2) & S1 < S2.
beats(trapped, F1, F2) :- .member(trapped, F1) & not .member(trapped, F2).
beats(escalating, F1, F2) :- .member(escalating, F1) & not .member(escalating, F2).
beats(victims, F1, F2) :- .member(victims(V1), F1) & .member(victims(V2), F2) & V1 > V2.

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
	.send(orchestrator, tell, request(Id, Resource, [Type, Severity, Victims, Trapped, Escalating])).

// Fallback: if the emergency type has no mapping, request a helicopter.
+!evaluate(Id, Type, Severity, Victims, Trapped, Escalating) <-
	.send(orchestrator, tell, request(Id, helicopter, [Type, Severity, Victims, Trapped, Escalating])).

// When granted a resource for the emergency, the agent waits for the operation to end and then goes home.
+granted(Id, Resource)[source(orchestrator)] <-
	-granted(Id, Resource)[source(orchestrator)].

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