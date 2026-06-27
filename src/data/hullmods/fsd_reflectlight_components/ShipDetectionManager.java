package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShipDetectionManager {
  private static final Queue<ShipAPI> detectionQueue = new ConcurrentLinkedQueue<>();
  private static final Set<String> queuedShipIds = new HashSet<>();
  private static final int BASE_SHIPS_PER_FRAME = 10;
  private static int currentMaxShipsPerFrame = BASE_SHIPS_PER_FRAME;
  private static final int SHIP_COUNT_THRESHOLD_MEDIUM = 50;
  private static final int SHIP_COUNT_THRESHOLD_LARGE = 100;
  private static final float MIN_DETECTION_INTERVAL = 0.2f;
  private static final Map<String, Float> lastDetectionTimes = new HashMap<>();

  public static void initialize() {
    detectionQueue.clear();
    queuedShipIds.clear();
    lastDetectionTimes.clear();
    currentMaxShipsPerFrame = BASE_SHIPS_PER_FRAME;
  }

  public static void enqueueShip(ShipAPI ship) {
    if (ship == null || !ship.isAlive() || ship.isHulk()) {
      return;
    }
    if (Global.getCombatEngine() == null) return;
    String shipId = ship.getId();
    if (queuedShipIds.contains(shipId)) {
      return;
    }
    float gameTime = Global.getCombatEngine().getTotalElapsedTime(false);
    float lastDetection =
        lastDetectionTimes.containsKey(shipId) ? lastDetectionTimes.get(shipId) : 0f;
    boolean isMoving = ship.getVelocity().lengthSquared() > 1f;
    float interval = isMoving ? MIN_DETECTION_INTERVAL : MIN_DETECTION_INTERVAL * 2;
    if (gameTime - lastDetection >= interval) {
      detectionQueue.add(ship);
      queuedShipIds.add(shipId);
    }
  }

  public static List<ShipAPI> processQueue(float amount) {
    List<ShipAPI> shipsToDetect = new ArrayList<>();
    if (Global.getCombatEngine() == null) return shipsToDetect;
    int totalShips = Global.getCombatEngine().getShips().size();
    if (totalShips > SHIP_COUNT_THRESHOLD_LARGE) {
      currentMaxShipsPerFrame = BASE_SHIPS_PER_FRAME * 2;
    } else if (totalShips > SHIP_COUNT_THRESHOLD_MEDIUM) {
      currentMaxShipsPerFrame = BASE_SHIPS_PER_FRAME + 5;
    } else {
      currentMaxShipsPerFrame = BASE_SHIPS_PER_FRAME;
    }
    int processCount = Math.min(detectionQueue.size(), currentMaxShipsPerFrame);
    float gameTime = Global.getCombatEngine().getTotalElapsedTime(false);
    for (int i = 0; i < processCount; i++) {
      ShipAPI ship = detectionQueue.poll();
      if (ship == null) continue;
      queuedShipIds.remove(ship.getId());
      if (!ship.isAlive() || ship.isHulk()) {
        continue;
      }
      shipsToDetect.add(ship);
      lastDetectionTimes.put(ship.getId(), gameTime);
    }
    return shipsToDetect;
  }

  public static List<ShipAPI> processQueue(float amount, Object instance) {
    return processQueue(amount);
  }
}
