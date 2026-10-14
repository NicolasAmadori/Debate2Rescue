package model;

import jason.environment.grid.Location;

/** Generic Responder (rescuer/pilot) sealed interface to access common parameters and methods. */
public sealed interface Responder permits Rescuer, Pilot {

  String name();

  Location station();

  Location position();

  Responder moveTo(Location newPosition);
}
