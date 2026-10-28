package model;

import config.Config;

public enum Resource {
  HELICOPTER(Config.ICONS_BASE_PATH + "helicopter.png"),
  FIRE_TRUCK(Config.ICONS_BASE_PATH + "fire_truck.png"),
  AMBULANCE(Config.ICONS_BASE_PATH + "ambulance.png"),
  POLICE_CAR(Config.ICONS_BASE_PATH + "police_car.png"),
  EXCAVATOR(Config.ICONS_BASE_PATH + "excavator.png"),
  POLICE_HELICOPTER(Config.ICONS_BASE_PATH + "police_helicopter.png"),
  ARMY(Config.ICONS_BASE_PATH + "army.png"),
  BOMB_SQUAD(Config.ICONS_BASE_PATH + "bomb_squad.png"),
  HAZMAT_TEAM(Config.ICONS_BASE_PATH + "hazmat_team.png");

  public final String path;

  private Resource(String path) {
    this.path = path;
  }
}
