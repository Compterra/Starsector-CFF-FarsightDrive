package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

public class PredictiveQueryCache {
  private static class CacheEntry {
    Vector2f lastQueryPosition;
    float lastQueryRange;
    List<ShipAPI> cachedResult;
    float cacheTimestamp;
    Vector2f velocity;
    Vector2f predictedNextPosition;

    CacheEntry() {
      this.lastQueryPosition = new Vector2f();
      this.velocity = new Vector2f();
      this.predictedNextPosition = new Vector2f();
      this.cachedResult = new ArrayList<>();
    }

    boolean isValid(Vector2f currentPos, float range, float currentTime) {
      if (currentTime - cacheTimestamp > 0.05f) {
        return false;
      }
      if (Math.abs(range - lastQueryRange) > 100f) {
        return false;
      }
      float dx = currentPos.x - lastQueryPosition.x;
      float dy = currentPos.y - lastQueryPosition.y;
      float distSq = dx * dx + dy * dy;
      if (distSq > 900f) {
        return false;
      }
      if (cachedResult.isEmpty()) {
        return false;
      }
      int validCount = 0;
      for (ShipAPI ship : cachedResult) {
        if (ship != null && ship.isAlive() && !ship.isExpired()) {
          validCount++;
        }
      }
      float validRate = (float) validCount / cachedResult.size();
      return validRate >= 0.8f;
    }

    List<ShipAPI> getValidatedResult() {
      List<ShipAPI> validResult = new ArrayList<>();
      for (ShipAPI ship : cachedResult) {
        if (ship != null && ship.isAlive() && !ship.isExpired() && !ship.isHulk()) {
          validResult.add(ship);
        }
      }
      return validResult;
    }

    void update(
        Vector2f position, float range, List<ShipAPI> result, Vector2f velocity, float timestamp) {
      this.lastQueryPosition.set(position);
      this.lastQueryRange = range;
      this.cachedResult = new ArrayList<>(result);
      this.cacheTimestamp = timestamp;
      if (velocity != null) {
        this.velocity.set(velocity);
        this.predictedNextPosition.set(
            position.x + velocity.x * 0.017f, position.y + velocity.y * 0.017f);
      }
    }
  }

  private static final Map<String, CacheEntry> cache = new HashMap<>();
  private static int totalQueries = 0;
  private static int cacheHits = 0;
  private static int cacheMisses = 0;

  public static List<ShipAPI> getShipsInRangeCached(
      ShipAPI queryShip, Vector2f center, float range) {
    if (queryShip == null || Global.getCombatEngine() == null) {
      return SpatialGrid.getShipsInRange(center, range);
    }
    String shipId = queryShip.getId();
    float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
    totalQueries++;
    CacheEntry entry = cache.get(shipId);
    if (entry != null && entry.isValid(center, range, currentTime)) {
      cacheHits++;
      List<ShipAPI> validResult = entry.getValidatedResult();
      if (validResult.size() < entry.cachedResult.size() * 0.8f) {
        cacheMisses++;
        cacheHits--;
        List<ShipAPI> result = SpatialGrid.getShipsInRange(center, range);
        entry.update(center, range, result, queryShip.getVelocity(), currentTime);
        return result;
      }
      return validResult;
    }
    cacheMisses++;
    List<ShipAPI> result = SpatialGrid.getShipsInRange(center, range);
    if (entry == null) {
      entry = new CacheEntry();
      cache.put(shipId, entry);
    }
    entry.update(center, range, result, queryShip.getVelocity(), currentTime);
    return result;
  }

  public static ShipAPI findNearestEnemyCached(ShipAPI ship) {
    if (ship == null || Global.getCombatEngine() == null) {
      return null;
    }
    List<ShipAPI> nearbyShips = getShipsInRangeCached(ship, ship.getLocation(), 2000f);
    ShipAPI nearest = null;
    float minDistSq = Float.MAX_VALUE;
    Vector2f shipLoc = ship.getLocation();
    for (ShipAPI candidate : nearbyShips) {
      if (candidate == null || candidate == ship) continue;
      if (candidate.getOwner() == ship.getOwner()) continue;
      if (!candidate.isAlive()) continue;
      if (candidate.isHulk()) continue;
      if (candidate.isExpired()) continue;
      if (candidate.isFighter()) continue;
      Vector2f candidateLoc = candidate.getLocation();
      float dx = shipLoc.x - candidateLoc.x;
      float dy = shipLoc.y - candidateLoc.y;
      float distSq = dx * dx + dy * dy;
      if (distSq < minDistSq) {
        minDistSq = distSq;
        nearest = candidate;
      }
    }
    return nearest;
  }

  public static void removeCache(ShipAPI ship) {
    if (ship != null) {
      cache.remove(ship.getId());
    }
  }

  public static void clearAll() {
    cache.clear();
    totalQueries = 0;
    cacheHits = 0;
    cacheMisses = 0;
  }

  public static String getCacheStats() {
    if (totalQueries == 0) {
      return "No queries yet";
    }
    float hitRate = (float) cacheHits / totalQueries * 100f;
    return String.format(
        "Queries: %d, Hits: %d (%.1f%%), Misses: %d",
        totalQueries, cacheHits, hitRate, cacheMisses);
  }

  public static float getCacheHitRate() {
    if (totalQueries == 0) {
      return 0f;
    }
    return (float) cacheHits / totalQueries;
  }
}
