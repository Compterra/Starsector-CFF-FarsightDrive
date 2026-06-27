package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

public class SpatialGrid {
  private static final float GRID_SIZE = 750f;
  private static final int SMALL_BATTLE_THRESHOLD = 25;
  private static final Map<GridKey, Set<ShipAPI>> gridMap = new HashMap<>();
  private static final Map<String, GridKey> shipGrids = new HashMap<>();
  private static float lastGlobalUpdateTime = 0f;
  private static final float BASE_UPDATE_INTERVAL = 1.0f;
  private static float currentUpdateInterval = BASE_UPDATE_INTERVAL;
  private static final int SHIP_COUNT_THRESHOLD_MEDIUM = 50;
  private static final int SHIP_COUNT_THRESHOLD_LARGE = 100;
  private static boolean initialized = false;

  private static class GridKey {
    final int x, y;

    GridKey(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GridKey)) return false;
      GridKey other = (GridKey) obj;
      return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
      return x * 31 + y;
    }

    @Override
    public String toString() {
      return "(" + x + "," + y + ")";
    }
  }

  public static void initialize() {
    if (initialized) return;
    try {
      gridMap.clear();
      shipGrids.clear();
      currentUpdateInterval = BASE_UPDATE_INTERVAL;
      if (Global.getCombatEngine() == null) {
        Global.getLogger(SpatialGrid.class).info("combat engine unavailable，SpatialGrid will  in initialize when combat starts");
        return;
      }
      initialized = true;
      List<ShipAPI> allShips = Global.getCombatEngine().getShips();
      for (ShipAPI ship : allShips) {
        if (ship == null || !ship.isAlive() || ship.isHulk()) {
          continue;
        }
        String shipId = ship.getId();
        GridKey currentGrid = getGridKey(ship.getLocation());
        Set<ShipAPI> cell = gridMap.get(currentGrid);
        if (cell == null) {
          cell = new HashSet<ShipAPI>();
          gridMap.put(currentGrid, cell);
        }
        cell.add(ship);
        shipGrids.put(shipId, currentGrid);
      }
      Global.getLogger(SpatialGrid.class).info("SpatialGridinitialization complete");
    } catch (Exception e) {
      Global.getLogger(SpatialGrid.class).error("SpatialGridinitialization failed：", e);
      initialized = false;
    }
  }

  public static GridKey getGridKey(Vector2f location) {
    int gridX = (int) Math.floor(location.x / GRID_SIZE);
    int gridY = (int) Math.floor(location.y / GRID_SIZE);
    return new GridKey(gridX, gridY);
  }

  public static List<GridKey> getGridsInRange(Vector2f center, float radius) {
    List<GridKey> grids = new ArrayList<>();
    int minX = (int) Math.floor((center.x - radius) / GRID_SIZE);
    int maxX = (int) Math.ceil((center.x + radius) / GRID_SIZE);
    int minY = (int) Math.floor((center.y - radius) / GRID_SIZE);
    int maxY = (int) Math.ceil((center.y + radius) / GRID_SIZE);
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        grids.add(new GridKey(x, y));
      }
    }
    return grids;
  }

  public static List<ShipAPI> getShipsInRange(Vector2f center, float radius) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null) return new ArrayList<>();
    List<ShipAPI> allShips = engine.getShips();
    int shipCount = allShips.size();
    if (shipCount <= SMALL_BATTLE_THRESHOLD) {
      return getShipsInRangeDirect(allShips, center, radius);
    }
    if (!initialized) {
      initialize();
      if (!initialized) {
        return getShipsInRangeDirect(allShips, center, radius);
      }
    }
    return getShipsInRangeGrid(center, radius);
  }

  private static List<ShipAPI> getShipsInRangeDirect(
      List<ShipAPI> allShips, Vector2f center, float radius) {
    List<ShipAPI> result = new ArrayList<>();
    float radiusSq = radius * radius;
    for (ShipAPI ship : allShips) {
      if (ship == null || ship.isShuttlePod()) {
        continue;
      }
      Vector2f shipLoc = ship.getLocation();
      float dx = center.x - shipLoc.x;
      float dy = center.y - shipLoc.y;
      float distSq = dx * dx + dy * dy;
      if (distSq <= radiusSq) {
        result.add(ship);
      }
    }
    return result;
  }

  private static List<ShipAPI> getShipsInRangeGrid(Vector2f center, float radius) {
    List<ShipAPI> result = new ArrayList<>();
    float radiusSquared = radius * radius;
    List<GridKey> grids = getGridsInRange(center, radius);
    Set<ShipAPI> processedShips = new HashSet<>();
    for (GridKey key : grids) {
      Set<ShipAPI> shipsInGrid = gridMap.get(key);
      if (shipsInGrid == null) continue;
      for (ShipAPI ship : shipsInGrid) {
        if (ship == null || !ship.isAlive() || processedShips.contains(ship)) continue;
        Vector2f shipLoc = ship.getLocation();
        float dx = center.x - shipLoc.x;
        float dy = center.y - shipLoc.y;
        float distSq = dx * dx + dy * dy;
        if (distSq <= radiusSquared) {
          result.add(ship);
          processedShips.add(ship);
        }
      }
    }
    if (Global.getCombatEngine() != null) {
      for (ShipAPI ship : Global.getCombatEngine().getShips()) {
        if (ship == null || !ship.isHulk() || processedShips.contains(ship)) continue;
        Vector2f shipLoc = ship.getLocation();
        float dx = center.x - shipLoc.x;
        float dy = center.y - shipLoc.y;
        float distSq = dx * dx + dy * dy;
        if (distSq <= radiusSquared) {
          result.add(ship);
          processedShips.add(ship);
        }
      }
    }
    return result;
  }

  public static void updateShipPosition(ShipAPI ship, boolean force) {
    if (!initialized) {
      return;
    }
    if (ship == null) return;
    if (!ship.isAlive() || ship.isHulk()) {
      removeShip(ship);
      return;
    }
    String shipId = ship.getId();
    GridKey currentGrid = getGridKey(ship.getLocation());
    GridKey oldGrid = shipGrids.get(shipId);
    if (!force && oldGrid != null && oldGrid.equals(currentGrid)) {
      return;
    }
    if (oldGrid != null) {
      Set<ShipAPI> oldCell = gridMap.get(oldGrid);
      if (oldCell != null) {
        oldCell.remove(ship);
        if (oldCell.isEmpty()) {
          gridMap.remove(oldGrid);
        }
      }
    }
    Set<ShipAPI> cell = gridMap.get(currentGrid);
    if (cell == null) {
      cell = new HashSet<ShipAPI>();
      gridMap.put(currentGrid, cell);
    }
    cell.add(ship);
    shipGrids.put(shipId, currentGrid);
  }

  public static void removeShip(ShipAPI ship) {
    if (!initialized) return;
    if (ship == null) return;
    String shipId = ship.getId();
    GridKey grid = shipGrids.remove(shipId);
    if (grid != null) {
      Set<ShipAPI> cell = gridMap.get(grid);
      if (cell != null) {
        Iterator<ShipAPI> it = cell.iterator();
        while (it.hasNext()) {
          ShipAPI s = it.next();
          if (s == null || s.getId().equals(shipId) || !s.isAlive() || s.isHulk()) {
            it.remove();
          }
        }
        if (cell.isEmpty()) {
          gridMap.remove(grid);
        }
      }
    }
  }

  public static void update(float amount) {
    if (!initialized) {
      initialize();
      return;
    }
    if (Global.getCombatEngine() == null) return;
    float gameTime = Global.getCombatEngine().getTotalElapsedTime(false);
    List<ShipAPI> allShips = Global.getCombatEngine().getShips();
    int shipCount = allShips.size();
    if (shipCount > SHIP_COUNT_THRESHOLD_LARGE) {
      currentUpdateInterval = BASE_UPDATE_INTERVAL * 1.5f;
    } else if (shipCount > SHIP_COUNT_THRESHOLD_MEDIUM) {
      currentUpdateInterval = BASE_UPDATE_INTERVAL * 1.2f;
    } else {
      currentUpdateInterval = BASE_UPDATE_INTERVAL;
    }
    if (gameTime - lastGlobalUpdateTime >= currentUpdateInterval) {
      Map<String, ShipAPI> shipIdMap = new HashMap<>(allShips.size());
      for (ShipAPI ship : allShips) {
        if (ship != null) {
          shipIdMap.put(ship.getId(), ship);
        }
      }
      for (ShipAPI ship : allShips) {
        if (ship != null) {
          String shipId = ship.getId();
          if (!ship.isAlive() || ship.isHulk()) {
            removeShip(ship);
            continue;
          }
          GridKey currentGrid = getGridKey(ship.getLocation());
          GridKey oldGrid = shipGrids.get(shipId);
          if (oldGrid != null && oldGrid.equals(currentGrid)) {
            continue;
          }
          if (oldGrid != null) {
            Set<ShipAPI> oldCell = gridMap.get(oldGrid);
            if (oldCell != null) {
              oldCell.remove(ship);
              if (oldCell.isEmpty()) {
                gridMap.remove(oldGrid);
              }
            }
          }
          Set<ShipAPI> cell = gridMap.get(currentGrid);
          if (cell == null) {
            cell = new HashSet<ShipAPI>();
            gridMap.put(currentGrid, cell);
          }
          cell.add(ship);
          shipGrids.put(shipId, currentGrid);
        }
      }
      List<String> invalidShipIds = new ArrayList<>();
      for (String shipId : shipGrids.keySet()) {
        ShipAPI ship = shipIdMap.get(shipId);
        if (ship == null || !ship.isAlive() || ship.isHulk()) {
          invalidShipIds.add(shipId);
        }
      }
      for (String shipId : invalidShipIds) {
        GridKey grid = shipGrids.remove(shipId);
        if (grid != null) {
          Set<ShipAPI> cell = gridMap.get(grid);
          if (cell != null) {
            Iterator<ShipAPI> iterator = cell.iterator();
            while (iterator.hasNext()) {
              ShipAPI s = iterator.next();
              if (s == null || s.getId().equals(shipId) || !s.isAlive() || s.isHulk()) {
                iterator.remove();
              }
            }
            if (cell.isEmpty()) {
              gridMap.remove(grid);
            }
          }
        }
      }
      lastGlobalUpdateTime = gameTime;
    }
  }
}
