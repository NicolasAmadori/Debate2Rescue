package model;

public enum EmergencyStatus {
  WAITING, // No rescuer is coming yet
  ASSIGNED, // A rescuer is on its way
  IN_PROGRESS // The rescuer reached the emergency
}
