package model;

import config.Config;
import jason.environment.grid.Location;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/***
 * Handles path planning and movement calculations for responders on the simulation grid.
 * Moves following Bresenham paths and avoids obstacles.
 */
public class PathFinder {

  private final SimModel model;

  public PathFinder(SimModel model) {
    this.model = model;
  }

  /***
   * Calculates the next position for a responder moving towards the given target.
   * Rescuers move by 1 cell, pilots move by up to 2 cells.
   * @param responder the responder to move
   * @param target the target location
   * @return the new position
   */
  public Location nextPosition(Responder responder, Location target) {
    int cells = responder instanceof Pilot ? 2 : 1;
    Location current = responder.position();

    for (int i = 0; i < cells; i++) {
      if (current.equals(target)) {
        break;
      }
      Location next = stepTowards(current, target);
      if (next.equals(current)) {
        break;
      }
      current = next;
    }
    return current;
  }

  /***
   * Calculates a single step towards the target following Bresenham line or avoiding obstacles.
   * @param current the current location
   * @param target the target location
   * @return the next location to move to
   */
  private Location stepTowards(Location current, Location target) {
    List<Location> line = bresenham(current, target);
    if (line.size() > 1 && isTraversable(line.get(1))) {
      return line.get(1);
    }
    return avoidObstacle(current, target);
  }

  /***
   * Discards blocked neighbor cells and chooses the free one closer to the target.
   * @param current the current location
   * @param target the target location
   * @return the next location to move to, or the current location if no free neighbors are available
   */
  private Location avoidObstacle(Location current, Location target) {
    List<Location> freeNeighbors = getFreeNeighbors(current);
    return freeNeighbors.stream()
        .min(
            Comparator.comparingInt((Location loc) -> distanceSquared(loc, target))
                .thenComparingInt(loc -> Config.getManhattanDistance(loc, target)))
        .orElse(current);
  }

  /***
   * Returns all traversable neighbor cells adjacent to the current position.
   * @param current the current location
   * @return a list of traversable neighbor locations
   */
  public List<Location> getFreeNeighbors(Location current) {
    List<Location> free = new ArrayList<>();
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) {
          continue;
        }
        Location candidate = new Location(current.x + dx, current.y + dy);
        if (isTraversable(candidate)) {
          free.add(candidate);
        }
      }
    }
    return free;
  }

  /***
   * Checks whether a cell is traversable or not.
   * @param loc the location to check
   * @return true if the cell is traversable, false otherwise
   */
  public boolean isTraversable(Location loc) {
    return model.inGrid(loc) && !model.hasObject(SimModel.OBSTACLE, loc);
  }

  /***
   * Generates a 2D line of grid cells connecting two locations using Bresenham's line algorithm.
   * @param from the starting location
   * @param to the ending location
   * @return a list of locations representing the line from 'from' to 'to'
   */
  public static List<Location> bresenham(Location from, Location to) {
    List<Location> line = new ArrayList<>();
    int x0 = from.x, y0 = from.y;
    int x1 = to.x, y1 = to.y;
    int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;

    int x = x0, y = y0;
    while (true) {
      line.add(new Location(x, y));
      if (x == x1 && y == y1) {
        break;
      }
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x += sx;
      }
      if (e2 < dx) {
        err += dx;
        y += sy;
      }
    }
    return line;
  }

  private static int distanceSquared(Location a, Location b) {
    int dx = a.x - b.x;
    int dy = a.y - b.y;
    return dx * dx + dy * dy;
  }
}
