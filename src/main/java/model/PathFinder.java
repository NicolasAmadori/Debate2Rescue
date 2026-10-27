package model;

import jason.environment.grid.Location;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/***
 * Handles path planning and movement calculations for responders on the simulation grid.
 * Follows Bresenham straight lines when clear and uses simple pathfinding to navigate around obstacles,
 * caching paths for efficiency and avoiding loops.
 */
public class PathFinder {

  private final SimModel model;
  private final Map<String, List<Location>> cachedPaths = new HashMap<>();
  private final Map<String, Location> cachedTargets = new HashMap<>();

  public PathFinder(SimModel model) {
    this.model = model;
  }

  /***
   * Calculates the next position for a responder moving towards the given target.
   * Rescuers move by 1 cell, pilots move by up to 2 cells.
   *
   * @param responder the responder to move
   * @param target the target location
   * @return the new position
   */
  public Location nextPosition(Responder responder, Location target) {
    if (responder == null) {
      return null;
    }
    if (target == null) {
      return responder.position();
    }

    int cells = responder instanceof Pilot ? 2 : 1;
    Location current = responder.position();
    String agName = responder.name();

    for (int i = 0; i < cells; i++) {
      if (current.equals(target)) {
        clearState(agName);
        break;
      }
      Location next = stepTowards(agName, current, target);
      if (next.equals(current)) {
        break;
      }
      current = next;
    }

    if (current.equals(target)) {
      clearState(agName);
    }
    return current;
  }

  /***
   * Calculates a single step towards the target for a named agent.
   * Advances along a cached path if valid, otherwise computes a direct Bresenham line
   * or a simple obstacle detour.
   *
   * @param agName the name of the agent
   * @param current the current location
   * @param target the target location
   * @return the next location to move to
   */
  public Location stepTowards(String agName, Location current, Location target) {
    if (current == null || target == null || current.equals(target)) {
      return current;
    }

    // Clear cache if the target has changed
    if (agName != null) {
      if (!target.equals(cachedTargets.get(agName))) {
        clearState(agName);
        cachedTargets.put(agName, target);
      }

      // 1. Advance along cached path if valid and not blocked
      List<Location> path = cachedPaths.get(agName);
      if (path != null && !path.isEmpty()) {
        Location next = path.get(0);
        if (isTraversable(next, target) && isAdjacent(current, next)) {
          path.remove(0);
          if (path.isEmpty()) {
            cachedPaths.remove(agName);
          }
          return next;
        }
        cachedPaths.remove(agName);
      }
    }

    // 2. Find a new path (straight Bresenham line or obstacle detour)
    List<Location> path = findPath(current, target);
    if (path.isEmpty()) {
      return current;
    }

    Location next = path.remove(0);
    if (agName != null && !path.isEmpty()) {
      cachedPaths.put(agName, path);
    }
    return next;
  }

  /***
   * Calculates a single step towards the target without agent state caching.
   *
   * @param current the current location
   * @param target the target location
   * @return the next location to move to
   */
  public Location stepTowards(Location current, Location target) {
    return stepTowards(null, current, target);
  }

  /***
   * Discards blocked cells and finds the optimal step towards the target avoiding obstacles.
   *
   * @param agName the name of the agent
   * @param current the current location
   * @param target the target location
   * @return the next location to move to
   */
  public Location avoidObstacle(String agName, Location current, Location target) {
    return stepTowards(agName, current, target);
  }

  /***
   * Discards blocked cells and chooses the best move towards the target.
   *
   * @param current the current location
   * @param target the target location
   * @return the next location to move to
   */
  public Location avoidObstacle(Location current, Location target) {
    return stepTowards(null, current, target);
  }

  /***
   * Finds a path from start to target.
   * If the direct Bresenham line is obstacle-free, returns it immediately.
   * Otherwise, finds a detour around obstacles using BFS to avoid loops.
   *
   * @param start starting location
   * @param target destination location
   * @return a list of locations from start to destination (excluding start)
   */
  public List<Location> findPath(Location start, Location target) {
    if (start == null || target == null || start.equals(target)) {
      return Collections.emptyList();
    }

    // Fast path: direct Bresenham line if clear of obstacles
    List<Location> line = bresenham(start, target);
    if (isLineClear(line, target)) {
      return new ArrayList<>(line.subList(1, line.size()));
    }

    // Detour: BFS to find the shortest obstacle-free path without looping
    Queue<Location> queue = new ArrayDeque<>();
    Map<Location, Location> parent = new HashMap<>();
    queue.add(start);
    parent.put(start, null);

    Location closest = start;
    int closestDist = distanceSquared(start, target);

    while (!queue.isEmpty()) {
      Location curr = queue.poll();
      if (curr.equals(target)) {
        closest = curr;
        break;
      }

      int dist = distanceSquared(curr, target);
      if (dist < closestDist) {
        closestDist = dist;
        closest = curr;
      }

      for (Location next : getFreeNeighbors(curr, target)) {
        if (!parent.containsKey(next)) {
          parent.put(next, curr);
          queue.add(next);
        }
      }
    }

    // If target is unreachable and agent is already at the closest cell, stop.
    if (closest.equals(start)) {
      return Collections.emptyList();
    }

    List<Location> path = new ArrayList<>();
    for (Location at = closest; at != null && !at.equals(start); at = parent.get(at)) {
      path.add(0, at);
    }
    return path;
  }

  /***
   * Checks whether the entire Bresenham line of sight is clear of obstacles.
   */
  private boolean isLineClear(List<Location> line, Location target) {
    if (line.size() <= 1) {
      return true;
    }
    for (int i = 1; i < line.size(); i++) {
      if (!isTraversable(line.get(i), target)) {
        return false;
      }
    }
    return true;
  }

  /***
   * Returns all traversable neighbor cells adjacent to the current position.
   *
   * @param current the current location
   * @param target the target location
   * @return a list of traversable neighbor locations
   */
  public List<Location> getFreeNeighbors(Location current, Location target) {
    List<Location> free = new ArrayList<>();
    if (current == null) {
      return free;
    }
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) {
          continue;
        }
        Location candidate = new Location(current.x + dx, current.y + dy);
        if (isTraversable(candidate, target)) {
          free.add(candidate);
        }
      }
    }
    return free;
  }

  /***
   * Returns all traversable neighbor cells adjacent to the current position.
   *
   * @param current the current location
   * @return a list of traversable neighbor locations
   */
  public List<Location> getFreeNeighbors(Location current) {
    return getFreeNeighbors(current, null);
  }

  /***
   * Checks whether a cell is traversable.
   * Single point of truth: checks grid boundaries and obstacles.
   *
   * @param loc the location to check
   * @param target the target location
   * @return true if traversable, false otherwise
   */
  public boolean isTraversable(Location loc, Location target) {
    if (loc == null || !model.inGrid(loc)) {
      return false;
    }
    if (model.hasObject(SimModel.OBSTACLE, loc)) {
      return false;
    }
    // Future extension: avoid other emergencies apart from its own:
    // if (model.hasObject(SimModel.EMERGENCY, loc) && !loc.equals(target)) return false;
    return true;
  }

  /***
   * Checks whether a cell is traversable or not.
   *
   * @param loc the location to check
   * @return true if the cell is traversable, false otherwise
   */
  public boolean isTraversable(Location loc) {
    return isTraversable(loc, null);
  }

  /***
   * Generates a 2D line of grid cells connecting two locations using Bresenham's line algorithm.
   *
   * @param from the starting location
   * @param to the ending location
   * @return a list of locations representing the line from 'from' to 'to'
   */
  public static List<Location> bresenham(Location from, Location to) {
    List<Location> line = new ArrayList<>();
    if (from == null || to == null) {
      return line;
    }
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

  /***
   * Clears cached paths and target for a given agent.
   *
   * @param agName the agent's name
   */
  public void clearState(String agName) {
    if (agName == null) {
      return;
    }
    cachedPaths.remove(agName);
    cachedTargets.remove(agName);
  }

  /***
   * Clears all cached paths and targets.
   */
  public void clearState() {
    cachedPaths.clear();
    cachedTargets.clear();
  }

  /***
   * Returns an unmodifiable list of the currently cached path for an agent.
   *
   * @param agName the agent name
   * @return list of remaining path locations, or empty list if no cached path
   */
  public List<Location> getCachedPath(String agName) {
    if (agName == null) {
      return Collections.emptyList();
    }
    List<Location> path = cachedPaths.get(agName);
    return path == null ? Collections.emptyList() : List.copyOf(path);
  }

  private static boolean isAdjacent(Location a, Location b) {
    return a != null && b != null && Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1;
  }

  private static int distanceSquared(Location a, Location b) {
    int dx = a.x - b.x;
    int dy = a.y - b.y;
    return dx * dx + dy * dy;
  }
}
