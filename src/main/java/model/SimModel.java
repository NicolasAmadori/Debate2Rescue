package model;

import jason.environment.grid.Location;
import java.util.List;

public interface SimModel {

  /***
   * Getter method
   * @return the list of locations of the stations on the grid.
   */
  List<Location> getStations();
}
