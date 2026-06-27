package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class KarmaManager implements KarmaSystemAPI {
  private static final Logger log = Global.getLogger(KarmaManager.class);
  private static final boolean ENABLE_LOGGING = false;
  private static KarmaManager instance;
  private final Map<String, KarmaData> karmaCache;
  private float lastCleanupTime;
  private static final float CLEANUP_INTERVAL = 5.0f;
  
  // ==================== extendedconfigAPI ====================
  // custominitial karmaconfig
  private final Map<ShipAPI.HullSize, Float> customInitialKarmaByHullSize;
  private final Map<String, Float> customInitialKarmaByHullId;
  private final Map<String, Float> customInitialKarmaByShipId;
  
  // customkarma gainconfig
  private final Map<ShipAPI.HullSize, Float> customKarmaGainMultByHullSize;
  private final Map<String, Float> customKarmaGainMultByHullId;
  
  // customkarmadecayconfig
  private final Map<ShipAPI.HullSize, Float> customKarmaDissipationMultByHullSize;
  private final Map<String, Float> customKarmaDissipationMultByHullId;

  private KarmaManager() {
    this.karmaCache = new HashMap<String, KarmaData>();
    this.lastCleanupTime = 0f;
    
    this.customInitialKarmaByHullSize = new HashMap<ShipAPI.HullSize, Float>();
    this.customInitialKarmaByHullId = new HashMap<String, Float>();
    this.customInitialKarmaByShipId = new HashMap<String, Float>();
    this.customKarmaGainMultByHullSize = new HashMap<ShipAPI.HullSize, Float>();
    this.customKarmaGainMultByHullId = new HashMap<String, Float>();
    this.customKarmaDissipationMultByHullSize = new HashMap<ShipAPI.HullSize, Float>();
    this.customKarmaDissipationMultByHullId = new HashMap<String, Float>();
  }

  public static KarmaManager getInstance() {
    if (instance == null) {
      instance = new KarmaManager();
    }
    return instance;
  }

  public KarmaData getKarmaData(ShipAPI ship) {
    if (ship == null) {
      return null;
    }
    String shipId = ship.getId();
    KarmaData data = karmaCache.get(shipId);
    if (data == null) {
      data = new KarmaData(ship);
      karmaCache.put(shipId, data);
      if (ENABLE_LOGGING && log != null) {
        log.info("[KarmaManager] for ship " + shipId + " created new KarmaData cache");
      }
    }
    return data;
  }

  public boolean hasKarmaData(ShipAPI ship) {
    return ship != null && karmaCache.containsKey(ship.getId());
  }

  public void removeKarmaData(ShipAPI ship) {
    if (ship != null) {
      karmaCache.remove(ship.getId());
    }
  }

  @Override
  public float getKarma(ShipAPI ship) {
    KarmaData data = getKarmaData(ship);
    return data != null ? data.getKarma() : 0f;
  }

  @Override
  public float getKarmaMax(ShipAPI ship) {
    KarmaData data = getKarmaData(ship);
    return data != null ? data.getKarmaMax() : 1f;
  }

  @Override
  public float getKarmaPercent(ShipAPI ship) {
    KarmaData data = getKarmaData(ship);
    if (data == null || data.getKarmaMax() == 0f) {
      return 0f;
    }
    return data.getKarma() / data.getKarmaMax();
  }

  @Override
  public boolean hasKarma(ShipAPI ship, float amount) {
    return getKarma(ship) >= amount;
  }

  @Override
  public void setKarma(ShipAPI ship, float karma, KarmaType type) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.setKarma(karma, type);
    }
  }

  @Override
  public float addKarma(ShipAPI ship, float amount, KarmaType type) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      return data.addKarma(amount, type);
    }
    return 0f;
  }

  @Override
  public float consumeKarma(ShipAPI ship, float amount, KarmaType type) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      float oldKarma = data.getKarma();
      float consumed = Math.min(oldKarma, amount);
      data.setKarma(oldKarma - consumed, type);
      return consumed;
    }
    return 0f;
  }

  @Override
  public float reduceKarma(ShipAPI ship, float amount, KarmaType type) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      return data.reduceKarma(amount, type);
    }
    return 0f;
  }

  @Override
  public void multiplyKarma(ShipAPI ship, float multiplier, KarmaType type) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.setKarma(data.getKarma() * multiplier, type);
    }
  }

  @Override
  public void setKarmaMax(ShipAPI ship, float max) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.modifyKarmaMaxMultiplier("KarmaManager_setKarmaMax", max);
    }
  }

  @Override
  public void modifyKarmaMax(ShipAPI ship, float multiplier) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      float newMax = data.getKarmaMax() * multiplier;
      data.modifyKarmaMaxMultiplier("KarmaManager_modifyKarmaMax", newMax);
    }
  }

  @Override
  public void setKarmaGainEfficiency(ShipAPI ship, float efficiency) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.addEfficiencyMultiplier("KarmaManager_setEfficiency", efficiency);
    }
  }

  @Override
  public void setKarmaDissipationRate(ShipAPI ship, float rate) {
    setKarmaDissipationRate(ship, rate, KarmaPriority.PRIORITY_NORMAL, "KarmaManager");
  }
  
  @Override
  public void setKarmaDissipationRate(ShipAPI ship, float rate, int priority, String source) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.setDissipationKarmaMult(rate, priority, source);
    }
  }

  @Override
  public void setKarmaGainRange(ShipAPI ship, float rangeMultiplier) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.addGainKarmaRangeMultiplier("KarmaManager_setRange", rangeMultiplier);
    }
  }

  @Override
  public void addKarmaChangeListener(ShipAPI ship, KarmaChangeListener listener) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.addListener(listener);
    }
  }

  @Override
  public void removeKarmaChangeListener(ShipAPI ship, KarmaChangeListener listener) {
    KarmaData data = getKarmaData(ship);
    if (data != null) {
      data.removeListener(listener);
    }
  }

  @Override
  public List<KarmaHistoryEntry> getKarmaHistory(ShipAPI ship, int count) {
    KarmaData data = getKarmaData(ship);
    if (data == null) {
      return new java.util.ArrayList<KarmaHistoryEntry>();
    }
    return data.getRecentHistory(count);
  }

  public void setKarma(ShipAPI ship, float karma) {
    setKarma(ship, karma, KarmaType.EXTERNAL);
  }

  public float addKarma(ShipAPI ship, float amount) {
    return addKarma(ship, amount, KarmaType.EXTERNAL);
  }

  public float reduceKarma(ShipAPI ship, float amount) {
    return reduceKarma(ship, amount, KarmaType.PASSIVE_LOSS);
  }

  public int cleanup(CombatEngineAPI engine) {
    if (engine == null) {
      return 0;
    }
    int removedCount = 0;
    Iterator<Map.Entry<String, KarmaData>> iter = karmaCache.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, KarmaData> entry = iter.next();
      KarmaData data = entry.getValue();
      ShipAPI ship = data.getShip();
      boolean shouldRemove = false;
      if (ship == null) {
        shouldRemove = true;
      } else if (ship.isHulk() || (!ship.isAlive() && ship.isExpired())) {
        shouldRemove = true;
      } else if (!engine.isEntityInPlay(ship)) {
        shouldRemove = true;
      }
      if (shouldRemove) {
        iter.remove();
        removedCount++;
      }
    }
    if (ENABLE_LOGGING && removedCount > 0 && log != null) {
      log.info("[KarmaManager] cleaned " + removedCount + "  invalid cache entries");
    }
    return removedCount;
  }

  public void autoManage(CombatEngineAPI engine, float amount) {
    if (engine == null) {
      return;
    }
    lastCleanupTime += amount;
    if (lastCleanupTime >= CLEANUP_INTERVAL) {
      cleanup(engine);
      lastCleanupTime = 0f;
    }
  }

  public void clear() {
    int cacheSize = karmaCache.size();
    karmaCache.clear();
    lastCleanupTime = 0f;
    if (ENABLE_LOGGING && log != null) {
      log.info("[KarmaManager] cleared " + cacheSize + "  combat temporary cache entries");
    }
  }

  public String getCacheStats() {
    int totalCache = karmaCache.size();
    return String.format("Total: %d (memory-only)", totalCache);
  }

  public int getCacheSize() {
    return karmaCache.size();
  }

  public static void resetInstance() {
    if (instance != null) {
      instance.clear();
      instance = null;
    }
  }
  
  // ==================== initial karmaconfigAPI ====================
  
  /**
   */
  public void setInitialKarmaByHullSize(ShipAPI.HullSize hullSize, float initialKarma) {
    customInitialKarmaByHullSize.put(hullSize, Math.max(0f, Math.min(1f, initialKarma)));
    if (ENABLE_LOGGING && log != null) {
      log.info("[KarmaManager] set hull size " + hullSize + " initial karma to: " + initialKarma);
    }
  }
  
  /**
   */
  public void setInitialKarmaByHullId(String hullId, float initialKarma) {
    customInitialKarmaByHullId.put(hullId, Math.max(0f, Math.min(1f, initialKarma)));
    if (ENABLE_LOGGING && log != null) {
      log.info("[KarmaManager] set Hull ID " + hullId + " initial karma to: " + initialKarma);
    }
  }
  
  /**
   */
  public void setInitialKarmaByShipId(String shipId, float initialKarma) {
    customInitialKarmaByShipId.put(shipId, Math.max(0f, Math.min(1f, initialKarma)));
    if (ENABLE_LOGGING && log != null) {
      log.info("[KarmaManager] setship instance " + shipId + " initial karma to: " + initialKarma);
    }
  }
  
  /**
   */
  public float getInitialKarmaFor(ShipAPI ship) {
    if (ship == null) {
      return 0.5f;
    }
    
    String shipId = ship.getId();
    if (customInitialKarmaByShipId.containsKey(shipId)) {
      return customInitialKarmaByShipId.get(shipId);
    }
    
    String hullId = ship.getHullSpec().getHullId();
    if (customInitialKarmaByHullId.containsKey(hullId)) {
      return customInitialKarmaByHullId.get(hullId);
    }
    
    ShipAPI.HullSize hullSize = ship.getHullSize();
    if (customInitialKarmaByHullSize.containsKey(hullSize)) {
      return customInitialKarmaByHullSize.get(hullSize);
    }
    
    return data.hullmods.FSD_ReflectLight.INITIAL_KARMA.containsKey(hullSize) ?
           data.hullmods.FSD_ReflectLight.INITIAL_KARMA.get(hullSize) : 0.5f;
  }
  
  /**
   */
  public void resetInitialKarmaByHullSize(ShipAPI.HullSize hullSize) {
    customInitialKarmaByHullSize.remove(hullSize);
  }
  
  /**
   */
  public void resetInitialKarmaByHullId(String hullId) {
    customInitialKarmaByHullId.remove(hullId);
  }
  
  /**
   */
  public void resetInitialKarmaByShipId(String shipId) {
    customInitialKarmaByShipId.remove(shipId);
  }
  
  /**
   */
  public void clearAllInitialKarmaConfigs() {
    customInitialKarmaByHullSize.clear();
    customInitialKarmaByHullId.clear();
    customInitialKarmaByShipId.clear();
    if (ENABLE_LOGGING && log != null) {
      log.info("[KarmaManager] cleared all custom initial karma config");
    }
  }
}
