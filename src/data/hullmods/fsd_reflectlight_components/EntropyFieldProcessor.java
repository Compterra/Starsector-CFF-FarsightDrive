package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.hullmods.FSD_ReflectLight;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class EntropyFieldProcessor {
  private final java.util.Map<String, java.util.Set<String>> shipProcessedMap =
      new java.util.HashMap<String, java.util.Set<String>>();
  private final java.util.Map<String, java.util.Map<String, Boolean>> shipTargetHulkStateMap =
      new java.util.HashMap<String, java.util.Map<String, Boolean>>();
  
  // killrewardconfig
  private static final float HARD_FLUX_DISSIPATE_PER_LEVEL = 0.05f; // per level5%hard flux dissipated
  private static final float SPEED_BOOST_MULTIPLIER = 1.33f;
  private static final float SPEED_BOOST_DURATION = 10.0f; // duration10s
  
  private final Map<String, Float> speedBoostEndTimeMap = new HashMap<>();

  public void detectAndProcessNearbyHulks(ShipAPI ship, EntropyFieldState fieldState) {
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.startOperation("detectAndProcessNearbyHulks");
    }
    String hullmodId = "FSD_ReflectLight";
    if (ship == null
        || !ship.isAlive()
        || ship.isHulk()
        || !ship.getVariant().getHullMods().contains(hullmodId)) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
        PerformanceMonitor.endOperation("detectAndProcessNearbyHulks");
      }
      return;
    }
    KarmaManager manager = KarmaManager.getInstance();
    KarmaData karmaData = manager.getKarmaData(ship);
    if (karmaData == null) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
        PerformanceMonitor.endOperation("detectAndProcessNearbyHulks");
      }
      return;
    }
    float entropyRange = FSD_ReflectLight.getEntropyFieldRange(ship, karmaData.getGainKarmaRange());
    java.util.Set<String> processedIds = shipProcessedMap.get(ship.getId());
    if (processedIds == null) {
      processedIds = new java.util.HashSet<String>();
      shipProcessedMap.put(ship.getId(), processedIds);
    }
    java.util.Map<String, Boolean> targetHulkState = shipTargetHulkStateMap.get(ship.getId());
    if (targetHulkState == null) {
      targetHulkState = new java.util.HashMap<String, Boolean>();
      shipTargetHulkStateMap.put(ship.getId(), targetHulkState);
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.startOperation("getShipsInRange_SpatialGrid");
    }
    List<ShipAPI> shipsInRange = SpatialGrid.getShipsInRange(ship.getLocation(), entropyRange);
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.endOperation("getShipsInRange_SpatialGrid");
    }
    int checkedShips = 0;
    int processedHulks = 0;
    for (ShipAPI target : shipsInRange) {
      checkedShips++;
      if (target == null || target == ship) continue;
      if (target.getHullSize() == HullSize.FIGHTER) continue;
      if (target.getOwner() == ship.getOwner()) continue;
      if (target.getVariant().hasHullMod("FSD_ReflectLight")) continue;
      String targetId = target.getId();
      Boolean wasHulkObj = targetHulkState.get(targetId);
      boolean wasHulk = wasHulkObj != null ? wasHulkObj.booleanValue() : false;
      boolean isHulk = target.isHulk();
      if (!wasHulk && isHulk && !processedIds.contains(targetId)) {
        processedIds.add(targetId);
        processedHulks++;
        
        // killkarmagetmechanism
        HullSize targetHullSize = getEffectiveHullSize(target);
        Float baseKarmaGain = FSD_ReflectLight.KARMA_GAIN_MULT.get(targetHullSize);
        if (baseKarmaGain == null) {
          baseKarmaGain = FSD_ReflectLight.KARMA_GAIN_MULT.get(HullSize.FRIGATE);
        }
        
        float efficiencyMultiplier = karmaData.getEfficiencyMultiplier();
        float finalKarmaGain = baseKarmaGain * efficiencyMultiplier;
        
        karmaData.addKarma(finalKarmaGain, KarmaType.KILL_GAIN);
        
        // calculationkillrewardlevel (1-4)
        int killLevel = getHullSizeLevel(targetHullSize);
        
        float hardFluxDissipate = killLevel * HARD_FLUX_DISSIPATE_PER_LEVEL;
        float maxFlux = ship.getMaxFlux();
        float hardFluxToRemove = maxFlux * hardFluxDissipate;
        float currentHardFlux = ship.getFluxTracker().getHardFlux();
        float actualRemoved = Math.min(hardFluxToRemove, currentHardFlux);
        
        if (actualRemoved > 0) {
          ship.getFluxTracker().decreaseFlux(actualRemoved);
        }
        
        float currentTime = com.fs.starfarer.api.Global.getCombatEngine().getTotalElapsedTime(false);
        speedBoostEndTimeMap.put(ship.getId(), currentTime + SPEED_BOOST_DURATION);
        
        if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
          FSD_ReflectLight.log.info(
              String.format(
                  "[Reflecting-Light Crystal-kill] ship %s kill  %s (%s level%d), gained karma: +%.1f%% (base%.1f%% × efficiency%.2f), current karma: %.1f%%, hard flux dissipated: %.1f (%.1f%%), speed buff: %ds",
                  ship.getId(), 
                  target.getId(),
                  targetHullSize.toString(),
                  killLevel,
                  finalKarmaGain * 100,
                  baseKarmaGain * 100,
                  efficiencyMultiplier,
                  karmaData.getKarma() * 100,
                  actualRemoved,
                  hardFluxDissipate * 100,
                  (int)SPEED_BOOST_DURATION));
        }
      }
      targetHulkState.put(targetId, Boolean.valueOf(isHulk));
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.endOperation("processShipsInRange_HulkProcessing");
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING
        && processedHulks > 0
        && FSD_ReflectLight.log != null) {
      FSD_ReflectLight.log.info(
          String.format(
              "[FSD][EntropyFieldProcessor] ship ID=%s checked%d ship(s) ship, processed%d ship(s) wreck. final karma=%.2f",
              ship.getId(), checkedShips, processedHulks, karmaData.getKarma()));
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.endOperation("detectAndProcessNearbyHulks");
    }
  }

  /**
   */
  public void applySpeedBoost(ShipAPI ship) {
    if (ship == null || !ship.isAlive() || ship.isHulk()) {
      speedBoostEndTimeMap.remove(ship.getId());
      return;
    }
    
    Float endTime = speedBoostEndTimeMap.get(ship.getId());
    if (endTime == null) {
      return;
    }
    
    float currentTime = com.fs.starfarer.api.Global.getCombatEngine().getTotalElapsedTime(false);
    
    if (currentTime >= endTime) {
      speedBoostEndTimeMap.remove(ship.getId());
      MutableShipStatsAPI stats = ship.getMutableStats();
      String modId = ship.getId() + "_EntropyKillSpeedBoost";
      stats.getMaxSpeed().unmodify(modId);
      stats.getAcceleration().unmodify(modId);
      stats.getDeceleration().unmodify(modId);
      stats.getTurnAcceleration().unmodify(modId);
      stats.getMaxTurnRate().unmodify(modId);
    } else {
      MutableShipStatsAPI stats = ship.getMutableStats();
      String modId = ship.getId() + "_EntropyKillSpeedBoost";
      stats.getMaxSpeed().modifyMult(modId, SPEED_BOOST_MULTIPLIER);
      stats.getAcceleration().modifyMult(modId, SPEED_BOOST_MULTIPLIER);
      stats.getDeceleration().modifyMult(modId, SPEED_BOOST_MULTIPLIER);
      stats.getTurnAcceleration().modifyMult(modId, SPEED_BOOST_MULTIPLIER);
      stats.getMaxTurnRate().modifyMult(modId, SPEED_BOOST_MULTIPLIER);
    }
  }
  
  /**
   * getship level (1-4)
   */
  private int getHullSizeLevel(HullSize hullSize) {
    switch (hullSize) {
      case FRIGATE:
        return 1;
      case DESTROYER:
        return 2;
      case CRUISER:
        return 3;
      case CAPITAL_SHIP:
        return 4;
      default:
        return 1;
    }
  }

  private HullSize getEffectiveHullSize(ShipAPI ship) {
    if (ship.isCapital()) {
      return HullSize.CAPITAL_SHIP;
    }
    if (ship.isCruiser()) {
      return HullSize.CRUISER;
    }
    if (ship.isDestroyer()) {
      return HullSize.DESTROYER;
    }
    if (ship.isFrigate()) {
      return HullSize.FRIGATE;
    }
    return ship.getHullSize();
  }
}
