package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.hullmods.FSD_ReflectLight;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KarmaData {
  private float karma;
  
  // karma cap - accumulatemechanism（takemaximum value）
  private float karmaMaxBase;
  private float karmaMax;
  private final Map<String, Float> karmaMaxModifiers;
  
  private float gainKarmaBase;
  private float gainKarma;
  private final Map<String, Float> gainKarmaMultipliers;
  
  private float efficiencyBase;
  private float efficiencyMultiplier;
  private final Map<String, Float> efficiencyMultipliers;
  
  private float gainKarmaRangeBase;
  private float gainKarmaRange;
  private final Map<String, Float> gainKarmaRangeMultipliers;
  
  private int gainKarmaCount;
  private float dissipationKarmaMult;
  private int dissipationPriority;
  private String dissipationSource;
  private float karmaDamageReport;
  private float totalRepair;
  private float overflowKarma;
  private final ShipAPI ship;
  private float initialKarma;
  private float combatGainedKarma;
  private float killGainedKarma;
  private float passiveLostKarma;
  private float activeConsumedKarma;
  private float externalModifiedKarma;
  private final List<KarmaHistoryEntry> history;
  private static final int MAX_HISTORY = 50;
  private final List<KarmaChangeListener> listeners;
  private final Map<Float, Boolean> thresholdStates;

  public KarmaData(ShipAPI ship) {
    this.ship = ship;
    
    // use KarmaManager  config API getinitial karma（supportcustomconfig）
    this.karma = KarmaManager.getInstance().getInitialKarmaFor(ship);
    
    // initializationkarma cap（accumulatemechanism）
    this.karmaMaxBase = 1.0f;
    this.karmaMax = 1.0f;
    this.karmaMaxModifiers = new HashMap<String, Float>();
    
    this.gainKarmaBase = 1.0f;
    this.gainKarma = 1.0f;
    this.gainKarmaMultipliers = new HashMap<String, Float>();
    
    this.efficiencyBase = 1.0f;
    this.efficiencyMultiplier = 1.0f;
    this.efficiencyMultipliers = new HashMap<String, Float>();
    
    this.gainKarmaRangeBase = 1.0f;
    this.gainKarmaRange = 1.0f;
    this.gainKarmaRangeMultipliers = new HashMap<String, Float>();
    
    this.gainKarmaCount = 0;
    
    this.dissipationKarmaMult = 1.0f;
    this.dissipationPriority = KarmaPriority.PRIORITY_NORMAL;
    this.dissipationSource = "default";
    this.karmaDamageReport = 0f;
    this.totalRepair = 0f;
    this.overflowKarma = 0f; // initializationoverflow karma
    
    this.initialKarma = this.karma; // recordinitial karma
    this.combatGainedKarma = 0f;
    this.killGainedKarma = 0f;
    this.passiveLostKarma = 0f;
    this.activeConsumedKarma = 0f;
    this.externalModifiedKarma = 0f;
    
    this.history = new ArrayList<KarmaHistoryEntry>();
    this.listeners = new ArrayList<KarmaChangeListener>();
    this.thresholdStates = new HashMap<Float, Boolean>();
    
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
      FSD_ReflectLight.log.info("[KarmaData] for ship " + ship.getId() + " creatememory-onlydata，initial karma=" + this.karma);
    }
  }

  public float getKarma() {
    return karma;
  }

  public float getKarmaMax() {
    return karmaMax;
  }

  public float getGainKarma() {
    return gainKarma;
  }

  public float getGainKarmaRange() {
    return gainKarmaRange;
  }

  public int getGainKarmaCount() {
    return gainKarmaCount;
  }

  public float getDissipationKarmaMult() {
    return dissipationKarmaMult;
  }
  
  public int getDissipationPriority() {
    return dissipationPriority;
  }
  
  public String getDissipationSource() {
    return dissipationSource;
  }

  public float getKarmaDamageReport() {
    return karmaDamageReport;
  }

  public float getEfficiencyMultiplier() {
    return efficiencyMultiplier;
  }

  public float getTotalRepair() {
    return totalRepair;
  }

  public ShipAPI getShip() {
    return ship;
  }

  public void setKarma(float karma, KarmaType type) {
    float oldKarma = this.karma;
    float newKarma = Math.max(0f, Math.min(this.karmaMax, karma));
    
    if (karma > this.karmaMax && karma > oldKarma) {
      float overflow = karma - this.karmaMax;
      this.overflowKarma += overflow;
      
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
            ? ship.getName() 
            : ship.getHullSpec().getHullNameWithDashClass();
        FSD_ReflectLight.log.info(String.format(
            "[Karma overflow] ship %s Karma overflow: %.3f, accumulated overflow: %.3f",
            shipName, overflow, this.overflowKarma));
      }
    }
    
    if (oldKarma == newKarma) {
      return;
    }
    float delta = newKarma - oldKarma;
    this.karma = newKarma;
    recordKarmaSource(delta, type);
    addHistory(oldKarma, newKarma, delta, type);
    notifyListeners(oldKarma, newKarma, delta, type);
    checkThresholds(oldKarma, newKarma);
  }

  public void setKarma(float karma) {
    setKarma(karma, KarmaType.EXTERNAL);
  }

  
  /**
   * add or updatekarma capmodifier（accumulatemechanism：takemaximum value）
   * @param source modifiersourceid
   */
  public void modifyKarmaMaxMultiplier(String source, float value) {
    karmaMaxModifiers.put(source, value);
    updateKarmaMax();
  }
  
  /**
   * removekarma capmodifier
   */
  public void removeKarmaMaxMultiplier(String source) {
    if (karmaMaxModifiers.remove(source) != null) {
      updateKarmaMax();
    }
  }
  
  /**
   */
  private void updateKarmaMax() {
    float maxValue = karmaMaxBase;
    for (Float modifier : karmaMaxModifiers.values()) {
      if (modifier > maxValue) {
        maxValue = modifier;
      }
    }
    this.karmaMax = maxValue;
  }
  
  
  /**
   * @param source modifiersourceid
   */
  public void addGainKarmaMultiplier(String source, float multiplier) {
    gainKarmaMultipliers.put(source, multiplier);
    updateGainKarma();
  }
  
  /**
   * removekarma gainmultiplier
   */
  public void removeGainKarmaMultiplier(String source) {
    if (gainKarmaMultipliers.remove(source) != null) {
      updateGainKarma();
    }
  }
  
  /**
   */
  private void updateGainKarma() {
    float result = gainKarmaBase;
    for (Float multiplier : gainKarmaMultipliers.values()) {
      result *= multiplier;
    }
    this.gainKarma = result;
  }
  
  
  /**
   * @param source modifiersourceid
   */
  public void addEfficiencyMultiplier(String source, float multiplier) {
    efficiencyMultipliers.put(source, multiplier);
    updateEfficiencyMultiplier();
  }
  
  /**
   * removeefficiencymultiplier
   */
  public void removeEfficiencyMultiplier(String source) {
    if (efficiencyMultipliers.remove(source) != null) {
      updateEfficiencyMultiplier();
    }
  }
  
  /**
   */
  private void updateEfficiencyMultiplier() {
    float result = efficiencyBase;
    for (Float multiplier : efficiencyMultipliers.values()) {
      result *= multiplier;
    }
    this.efficiencyMultiplier = result;
  }
  
  
  /**
   * @param source modifiersourceid
   */
  public void addGainKarmaRangeMultiplier(String source, float multiplier) {
    gainKarmaRangeMultipliers.put(source, multiplier);
    updateGainKarmaRange();
  }
  
  /**
   * removerangemultiplier
   */
  public void removeGainKarmaRangeMultiplier(String source) {
    if (gainKarmaRangeMultipliers.remove(source) != null) {
      updateGainKarmaRange();
    }
  }
  
  /**
   */
  private void updateGainKarmaRange() {
    float result = gainKarmaRangeBase;
    for (Float multiplier : gainKarmaRangeMultipliers.values()) {
      result *= multiplier;
    }
    this.gainKarmaRange = result;
  }

  public void setGainKarmaCount(int gainKarmaCount) {
    this.gainKarmaCount = gainKarmaCount;
  }

  @Deprecated
  public void setDissipationKarmaMult(float dissipationKarmaMult) {
    setDissipationKarmaMult(dissipationKarmaMult, KarmaPriority.PRIORITY_NORMAL, "legacy");
  }
  
  public boolean setDissipationKarmaMult(float dissipationKarmaMult, int priority, String source) {
    if (priority < this.dissipationPriority) {
      if (FSD_ReflectLight.ENABLE_PRIORITY_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info(String.format(
            "[Priority reject] ship %s: %s (priority %d) was blocked by %s (priority %d)",
            ship.getId(), source, priority, this.dissipationSource, this.dissipationPriority));
      }
      return false;
    }
    
    float oldMult = this.dissipationKarmaMult;
    int oldPriority = this.dissipationPriority;
    String oldSource = this.dissipationSource;
    
    this.dissipationKarmaMult = dissipationKarmaMult;
    this.dissipationPriority = priority;
    this.dissipationSource = source;
    
    if (FSD_ReflectLight.ENABLE_PRIORITY_LOGGING && FSD_ReflectLight.log != null) {
      FSD_ReflectLight.log.info(String.format(
          "[Priority update] ship %s: %.2f (%s, P%d) → %.2f (%s, P%d)",
          ship.getId(), oldMult, oldSource, oldPriority, 
          dissipationKarmaMult, source, priority));
    }
    
    return true;
  }
  
  public void resetDissipationKarmaMult() {
    this.dissipationKarmaMult = 1.0f;
    this.dissipationPriority = KarmaPriority.PRIORITY_NORMAL;
    this.dissipationSource = "reset";
  }

  public void setKarmaDamageReport(float karmaDamageReport) {
    this.karmaDamageReport = karmaDamageReport;
  }

  public void setTotalRepair(float totalRepair) {
    this.totalRepair = totalRepair;
  }

  public float addKarma(float amount, KarmaType type) {
    float oldKarma = this.karma;
    setKarma(this.karma + amount, type);
    return this.karma - oldKarma;
  }

  public float addKarma(float amount) {
    return addKarma(amount, KarmaType.EXTERNAL);
  }

  public float reduceKarma(float amount, KarmaType type) {
    float oldKarma = this.karma;
    setKarma(this.karma - amount, type);
    return oldKarma - this.karma;
  }

  public float reduceKarma(float amount) {
    return reduceKarma(amount, KarmaType.PASSIVE_LOSS);
  }

  public void incrementGainKarmaCount() {
    setGainKarmaCount(this.gainKarmaCount + 1);
  }

  public void decrementGainKarmaCount(int amount) {
    setGainKarmaCount(Math.max(0, this.gainKarmaCount - amount));
  }

  public void addDamageReport(float damage) {
    setKarmaDamageReport(this.karmaDamageReport + damage);
  }

  public void reduceDamageReport(float damage) {
    setKarmaDamageReport(Math.max(0f, this.karmaDamageReport - damage));
  }

  public void addTotalRepair(float amount) {
    setTotalRepair(this.totalRepair + amount);
  }

  public void resetTotalRepair() {
    setTotalRepair(0f);
  }

  private void recordKarmaSource(float delta, KarmaType type) {
    switch (type) {
      case INITIAL:
        initialKarma += delta;
        break;
      case COMBAT_GAIN:
        combatGainedKarma += delta;
        break;
      case KILL_GAIN:
        killGainedKarma += delta;
        break;
      case PASSIVE_LOSS:
        passiveLostKarma += Math.abs(delta);
        break;
      case ACTIVE_COST:
        activeConsumedKarma += Math.abs(delta);
        break;
      case EXTERNAL:
        externalModifiedKarma += delta;
        break;
    }
  }

  private void addHistory(float oldValue, float newValue, float delta, KarmaType type) {
    try {
      float timestamp =
          Global.getCombatEngine() != null
              ? Global.getCombatEngine().getTotalElapsedTime(false)
              : 0f;
      history.add(new KarmaHistoryEntry(timestamp, oldValue, newValue, delta, type, null));
      while (history.size() > MAX_HISTORY) {
        history.remove(0);
      }
    } catch (Exception e) {
    }
  }

  private void notifyListeners(float oldKarma, float newKarma, float delta, KarmaType type) {
    if (listeners.isEmpty()) {
      return;
    }
    List<KarmaChangeListener> listenersCopy = new ArrayList<KarmaChangeListener>(listeners);
    for (KarmaChangeListener listener : listenersCopy) {
      try {
        listener.onKarmaChanged(ship, oldKarma, newKarma, delta, type);
      } catch (Exception e) {
      }
    }
  }

  private void checkThresholds(float oldKarma, float newKarma) {
    if (listeners.isEmpty()) {
      return;
    }
    float[] thresholds = {0.25f, 0.5f, 0.75f, 1.0f};
    for (float threshold : thresholds) {
      Boolean wasAbove = thresholdStates.get(threshold);
      boolean isAbove = newKarma >= threshold;
      if (wasAbove == null || wasAbove != isAbove) {
        thresholdStates.put(threshold, isAbove);
        ThresholdType type = isAbove ? ThresholdType.REACHED : ThresholdType.DROPPED;
        List<KarmaChangeListener> listenersCopy = new ArrayList<KarmaChangeListener>(listeners);
        for (KarmaChangeListener listener : listenersCopy) {
          try {
            listener.onKarmaThresholdReached(ship, threshold, type);
          } catch (Exception e) {
          }
        }
      }
    }
  }

  public float getInitialKarma() {
    return initialKarma;
  }

  public float getCombatGainedKarma() {
    return combatGainedKarma;
  }

  public float getKillGainedKarma() {
    return killGainedKarma;
  }

  public float getPassiveLostKarma() {
    return passiveLostKarma;
  }

  public float getActiveConsumedKarma() {
    return activeConsumedKarma;
  }

  public float getExternalModifiedKarma() {
    return externalModifiedKarma;
  }

  public List<KarmaHistoryEntry> getHistory() {
    return new ArrayList<KarmaHistoryEntry>(history);
  }

  public List<KarmaHistoryEntry> getRecentHistory(int count) {
    int startIndex = Math.max(0, history.size() - count);
    return new ArrayList<KarmaHistoryEntry>(history.subList(startIndex, history.size()));
  }

  public void addListener(KarmaChangeListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public void removeListener(KarmaChangeListener listener) {
    listeners.remove(listener);
  }

  public void clearListeners() {
    listeners.clear();
  }
  
  
  /**
   * getcurrentaccumulate overflow karma
   * @return overflow karmaamount
   */
  public float getOverflowKarma() {
    return overflowKarma;
  }
  
  /**
   * consumeoverflow karma
   * @return actualconsume amount
   */
  public float consumeOverflowKarma(float amount) {
    float consumed = Math.min(amount, overflowKarma);
    overflowKarma -= consumed;
    
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null && consumed > 0) {
      String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
          ? ship.getName() 
          : ship.getHullSpec().getHullNameWithDashClass();
      FSD_ReflectLight.log.info(String.format(
          "[Overflow karma consumption] ship %s consumeoverflow karma: %.3f, remaining: %.3f",
          shipName, consumed, this.overflowKarma));
    }
    
    return consumed;
  }
  
  /**
   */
  public void resetOverflowKarma() {
    this.overflowKarma = 0f;
  }
  
  /**
   * setoverflow karma
   * @param amount overflow karmaamount
   */
  public void setOverflowKarma(float amount) {
    this.overflowKarma = Math.max(0f, amount);
  }
}
