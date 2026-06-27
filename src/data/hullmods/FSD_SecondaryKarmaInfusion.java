package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Secondary Crystal Overgrowth - Secondary Karma Infusion
 * 
 * 
 * @author FarsightDrive Team
 */
public class FSD_SecondaryKarmaInfusion extends BaseHullMod {
  public static final Logger log = Global.getLogger(FSD_SecondaryKarmaInfusion.class);
  public static final boolean ENABLE_LOGGING = false;
  
  public static final String INFUSION_PROGRESS_KEY = "FSD_InfusionProgress";
  
  private static final String FLEET_MEMBER_ID_KEY = "FSD_FleetMemberId";
  
  // efficiencyconfig：infestation progress -> karma efficiency（0.0 - 0.5）
  public static final float MIN_EFFICIENCY = 0.0f;
  public static final float MAX_EFFICIENCY = 0.5f;
  
  private static final Map<HullSize, Float> ENTROPY_FIELD_RANGE = new HashMap<>();
  
  static {
    ENTROPY_FIELD_RANGE.put(HullSize.FRIGATE, 600f);
    ENTROPY_FIELD_RANGE.put(HullSize.DESTROYER, 900f);
    ENTROPY_FIELD_RANGE.put(HullSize.CRUISER, 1200f);
    ENTROPY_FIELD_RANGE.put(HullSize.CAPITAL_SHIP, 1500f);
  }
  
  public static final Map<HullSize, Float> INFUSION_INITIAL_KARMA = new HashMap<>();
  
  static {
    INFUSION_INITIAL_KARMA.put(HullSize.FRIGATE, 0.5f);
    INFUSION_INITIAL_KARMA.put(HullSize.DESTROYER, 0.4f);
    INFUSION_INITIAL_KARMA.put(HullSize.CRUISER, 0.3f);
    INFUSION_INITIAL_KARMA.put(HullSize.CAPITAL_SHIP, 0.15f);
  }
  
  public static final Map<HullSize, Float> INFUSION_KARMA_GAIN_MULT = new HashMap<>();
  
  static {
    INFUSION_KARMA_GAIN_MULT.put(HullSize.FRIGATE, 0.05f);
    INFUSION_KARMA_GAIN_MULT.put(HullSize.DESTROYER, 0.1f);
    INFUSION_KARMA_GAIN_MULT.put(HullSize.CRUISER, 0.15f);
    INFUSION_KARMA_GAIN_MULT.put(HullSize.CAPITAL_SHIP, 0.2f);
  }
  
  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
  }
  
  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    syncFleetMemberIdFromVariant(ship);
    
    syncInfusionProgressFromMemory(ship);
    
    if (!Global.getCombatEngine().isPaused() && !isInRefit(ship)) {
      initializeKarmaSystem(ship);
    }
  }
  
  /**
   */
  private void syncFleetMemberIdFromVariant(ShipAPI ship) {
    if (ship.getCustomData().containsKey(FLEET_MEMBER_ID_KEY)) {
      return;
    }
    
    if (ship.getVariant() != null) {
      for (String tag : ship.getVariant().getTags()) {
        if (tag.startsWith("fsd_fleet_member_id_")) {
          String memberId = tag.substring("fsd_fleet_member_id_".length());
          ship.setCustomData(FLEET_MEMBER_ID_KEY, memberId);
          
          if (ENABLE_LOGGING && log != null) {
            log.info(String.format(
                "[Secondary Crystal Overgrowth] ship %s found from variantFleetMember ID: %s",
                ship.getName(), memberId));
          }
          return;
        }
      }
    }
    
    if (ENABLE_LOGGING && log != null) {
      log.warn(String.format(
          "[Secondary Crystal Overgrowth] ship %s  variant has no FleetMember ID tag",
          ship.getName()));
    }
  }
  
  /**
   *  from globalMemorysynced infestation progress to ShipAPI
   * useFleetMember ID from globalMemoryreadprogress
   */
  private void syncInfusionProgressFromMemory(ShipAPI ship) {
    if (ship.getCustomData().containsKey(INFUSION_PROGRESS_KEY)) {
      return;
    }
    
    // getFleetMember ID
    Object memberIdObj = ship.getCustomData().get(FLEET_MEMBER_ID_KEY);
    if (!(memberIdObj instanceof String)) {
      if (ENABLE_LOGGING && log != null) {
        log.warn(String.format(
            "[Secondary Crystal Overgrowth] ship %s has no FleetMember ID，unable to readinfestation progress",
            ship.getName()));
      }
      ship.setCustomData(INFUSION_PROGRESS_KEY, 0f);
      return;
    }
    
    String memberId = (String) memberIdObj;
    
    //  from globalMemoryreadinfestation progress
    float progress = data.campaign.CrystalInfusionFleetListener.getInfusionProgressById(memberId);
    
    ship.setCustomData(INFUSION_PROGRESS_KEY, progress);
    
    if (ENABLE_LOGGING && log != null) {
      log.info(String.format(
          "[Secondary Crystal Overgrowth] ship %s (FleetMember ID: %s) synced infestation progress: %.2f%%",
          ship.getName(), memberId, progress * 100));
    }
  }
  
  /**
   */
  private float getInfusionProgressFromMemory() {
    try {
      com.fs.starfarer.api.campaign.CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
      if (fleet == null) {
        return 0f;
      }
      
      for (com.fs.starfarer.api.fleet.FleetMemberAPI member : 
           fleet.getFleetData().getMembersListCopy()) {
        if (member.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
          float progress = data.campaign.CrystalInfusionFleetListener.getInfusionProgressById(member.getId());
          if (progress > 0f) {
            return progress;
          }
        }
      }
    } catch (Exception e) {
      if (ENABLE_LOGGING && log != null) {
        log.warn("[Secondary Crystal Overgrowth] refit screen readinfestation progressfailed: " + e.getMessage());
      }
    }
    
    return 0f;
  }
  
  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null || engine.isPaused()) {
      return;
    }
    
    if (isInRefit(ship)) {
      return;
    }
    
    initializeCombatSystems(engine, ship);
    
    initializeKarmaSystem(ship);
    updateKarmaEfficiency(ship);
    
    processEntropyField(ship, engine, amount);
    processKarmaGain(ship);
    handleKarmaDissipation(ship, amount);
    
    if (ship == engine.getPlayerShip()) {
      displayInfusionStatus(ship, engine);
    }
  }
  
  /**
   */
  private void initializeCombatSystems(CombatEngineAPI engine, ShipAPI ship) {
    KarmaDamageReportListener.ensureRegistered(engine);
    
    String rendererKey = "FSD_SecondaryKarma_Renderer_" + ship.getId();
    if (!ship.getCustomData().containsKey(rendererKey)) {
      float entropyRange = getEntropyFieldRange(ship);
      
      data.hullmods.fsd_reflectlight_components.EntropyFieldState fieldState = 
          new data.hullmods.fsd_reflectlight_components.EntropyFieldState(ship, entropyRange, false);
      fieldState.setAnimationState(data.hullmods.fsd_reflectlight_components.AnimationState.FORMING);
      
      FSD_ReflectLight.EntropyFieldRenderer renderer = 
          new data.hullmods.FSD_ReflectLight().new EntropyFieldRenderer(fieldState, ship);
      
      engine.addLayeredRenderingPlugin(renderer);
      
      ship.setCustomData(rendererKey, renderer);
      ship.setCustomData("FSD_SecondaryKarma_EntropyRange", entropyRange);
      
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Secondary Crystal Overgrowth] ship %s initializationentropy fieldrenderer，range: %.0fsu (ship size: %s)，usesingle ring",
            ship.getName(), entropyRange, ship.getHullSize()));
      }
    }
  }
  
  /**
   */
  private static float getEntropyFieldRange(ShipAPI ship) {
    HullSize hullSize = ship.getHullSize();
    Float range = ENTROPY_FIELD_RANGE.get(hullSize);
    if (range != null) {
      return range;
    }
    // defaultvalue（ if  result does not has config）
    return 900f;
  }
  
  /**
   * initializationkarma system
   */
  private void initializeKarmaSystem(ShipAPI ship) {
    KarmaData karmaData = KarmaAPI.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    String initKey = "FSD_SecondaryKarma_Initialized";
    if (!ship.getCustomData().containsKey(initKey)) {
      HullSize hullSize = ship.getHullSize();
      Float initialKarmaObj = INFUSION_INITIAL_KARMA.get(hullSize);
      float initialKarma = (initialKarmaObj != null) ? initialKarmaObj : 0.3f;
      
      // useKarmaAPIsetInitial Karma
      KarmaAPI.setInitialKarmaByShipId(ship.getId(), initialKarma);
      
      ship.setCustomData(initKey, true);
      
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Secondary Crystal Overgrowth] for ship %s initializationkarma system，initialvalue: %.1f%%",
            ship.getName(), initialKarma * 100));
      }
    }
  }
  
  /**
   * updatekarma efficiency
   */
  private void updateKarmaEfficiency(ShipAPI ship) {
    // getinfestation progress
    float infusionProgress = getInfusionProgress(ship);
    
    float efficiency = MIN_EFFICIENCY + (MAX_EFFICIENCY - MIN_EFFICIENCY) * infusionProgress;
    float minEfficiency = 0.01f;
    efficiency = Math.max(minEfficiency, efficiency);
    
    KarmaAPI.addEfficiencyMultiplier(ship, "FSD_SecondaryKarmaInfusion", efficiency);
    
    // karmaBenefits = baseBenefits × efficiency
    HullSize hullSize = ship.getHullSize();
    Float baseGainMultObj = INFUSION_KARMA_GAIN_MULT.get(hullSize);
    float baseGainMult = (baseGainMultObj != null) ? baseGainMultObj : 0.1f;
    // Benefitsmultiplier = basemultiplier × efficiency
    float adjustedGainMult = baseGainMult * efficiency;
    KarmaAPI.addGainKarmaMultiplier(ship, "FSD_SecondaryKarmaInfusion", adjustedGainMult);
    
    ship.setCustomData("FSD_Karma_EfficiencyMultiplier", efficiency);
  }
  
  /**
   * getship infestation progress（0.0 - 1.0）
   */
  public static float getInfusionProgress(ShipAPI ship) {
    if (ship == null) {
      return 0f;
    }
    
    Object progress = ship.getCustomData().get(INFUSION_PROGRESS_KEY);
    if (progress instanceof Float) {
      return (Float) progress;
    }
    
    return 0f;
  }
  
  /**
   * setship infestation progress（0.0 - 1.0）
   */
  public static void setInfusionProgress(ShipAPI ship, float progress) {
    if (ship == null) {
      return;
    }
    
    progress = Math.max(0f, Math.min(1f, progress));
    ship.setCustomData(INFUSION_PROGRESS_KEY, progress);
  }
  
  /**
   */
  private void processEntropyField(ShipAPI ship, CombatEngineAPI engine, float amount) {
    // getentropy field range
    Object rangeObj = ship.getCustomData().get("FSD_SecondaryKarma_EntropyRange");
    if (!(rangeObj instanceof Float)) {
      return;
    }
    float entropyRange = (Float) rangeObj;
    
    // useFSD_ReflectLight EntropyFieldProcessorprocesskillreward
    data.hullmods.fsd_reflectlight_components.EntropyFieldState dummyState = 
        new data.hullmods.fsd_reflectlight_components.EntropyFieldState(
            ship, entropyRange, ship == engine.getPlayerShip());
    
    data.hullmods.fsd_reflectlight_components.EntropyFieldProcessor processor = 
        new data.hullmods.fsd_reflectlight_components.EntropyFieldProcessor();
    processor.detectAndProcessNearbyHulks(ship, dummyState);
  }
  
  /**
   */
  private void processKarmaGain(ShipAPI ship) {
    KarmaData karmaData = KarmaAPI.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    // getaccumulated damage
    float accumulatedDamage = karmaData.getKarmaDamageReport();
    if (accumulatedDamage <= 0) {
      return;
    }
    
    // getbase fluxcapacity
    float baseFlux = ship.getHullSpec().getFluxCapacity();
    if (baseFlux <= 0) {
      return;
    }
    
    float distanceMultiplier = calculateDistanceBonus(ship);
    
    // getkarmaBenefitsmultiplier
    float gainKarmaMultiplier = karmaData.getGainKarma();
    
    float efficiencyMultiplier = karmaData.getEfficiencyMultiplier();
    
    float baseKarmaGain = accumulatedDamage / baseFlux;
    float karmaToAdd = baseKarmaGain * distanceMultiplier * gainKarmaMultiplier * efficiencyMultiplier;
    
    // addkarma
    float oldKarma = karmaData.getKarma();
    karmaData.addKarma(karmaToAdd, KarmaType.COMBAT_GAIN);
    float newKarma = karmaData.getKarma();
    
    // clearaccumulated damage
    karmaData.setKarmaDamageReport(0f);
    
    if (ENABLE_LOGGING && log != null && karmaToAdd > 0.0001f) {
      String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
          ? ship.getName() 
          : ship.getHullSpec().getHullNameWithDashClass();
      log.info(String.format(
          "[Secondary Crystal-conversion] ship %s gained karma: %.2f%% → %.2f%% (+%.3f%%), efficiency=%.2f",
          shipName, oldKarma * 100, newKarma * 100, karmaToAdd * 100, efficiencyMultiplier));
    }
  }
  
  /**
   */
  private float calculateDistanceBonus(ShipAPI ship) {
    Object lastTargetObj = ship.getCustomData().get("FSD_ReflectLight_LastTarget");
    if (!(lastTargetObj instanceof ShipAPI)) {
      return 1.0f;
    }
    
    ShipAPI target = (ShipAPI) lastTargetObj;
    if (target == null || !target.isAlive() || target.isHulk()) {
      return 1.0f;
    }
    
    float dx = ship.getLocation().x - target.getLocation().x;
    float dy = ship.getLocation().y - target.getLocation().y;
    float distance = (float) Math.sqrt(dx * dx + dy * dy);
    
    final float DISTANCE_BONUS_RANGE = 1000f;
    final float DISTANCE_BONUS_MAX_RANGE = 500f;
    final float DISTANCE_BONUS_MAX = 0.2f;
    
    if (distance >= DISTANCE_BONUS_RANGE) {
      return 1.0f;
    }
    
    if (distance <= DISTANCE_BONUS_MAX_RANGE) {
      return 1.0f + DISTANCE_BONUS_MAX;
    }
    
    float ratio = (DISTANCE_BONUS_RANGE - distance) / (DISTANCE_BONUS_RANGE - DISTANCE_BONUS_MAX_RANGE);
    return 1.0f + (DISTANCE_BONUS_MAX * ratio);
  }
  
  /**
   */
  private void handleKarmaDissipation(ShipAPI ship, float amount) {
    KarmaData karmaData = KarmaAPI.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    float karma = karmaData.getKarma();
    if (karma <= 0 || !ship.areSignificantEnemiesInRange()) {
      return;
    }
    
    boolean isFiring = false;
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getChargeLevel() >= 1) {
        isFiring = true;
        break;
      }
    }
    
    String timerKey = "FSD_SecondaryKarma_CeaseFireTimer";
    Float ceaseFireTimer = 0f;
    if (ship.getCustomData().containsKey(timerKey)) {
      ceaseFireTimer = (Float) ship.getCustomData().get(timerKey);
    }
    
    HullSize hullSize = ship.getHullSize();
    float ceaseFireThreshold = getCeaseFireThreshold(hullSize);
    
    if (isFiring) {
      ceaseFireTimer = 0f;
      ship.setCustomData(timerKey, ceaseFireTimer);
    } else {
      ceaseFireTimer += amount;
      ship.setCustomData(timerKey, ceaseFireTimer);
      
      if (ceaseFireTimer >= ceaseFireThreshold) {
        float dissipationRate = getDissipationRate(hullSize);
        float dissipationMult = karmaData.getDissipationKarmaMult();
        float karmaLoss = dissipationRate * amount * dissipationMult;
        
        float oldKarma = karma;
        karmaData.reduceKarma(karmaLoss, KarmaType.PASSIVE_LOSS);
        float newKarma = karmaData.getKarma();
        
        if (ENABLE_LOGGING && log != null && karmaLoss > 0 && newKarma != oldKarma) {
          String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
              ? ship.getName() 
              : ship.getHullSpec().getHullNameWithDashClass();
          log.info(String.format(
              "[Secondary Crystal-dissipation] ship %s karma loss: %.1f%% → %.1f%%",
              shipName, oldKarma * 100, newKarma * 100));
        }
      }
    }
  }
  
  private float getCeaseFireThreshold(HullSize hullSize) {
    switch (hullSize) {
      case FRIGATE: return 2.0f;
      case DESTROYER: return 4.0f;
      case CRUISER: return 6.0f;
      case CAPITAL_SHIP: return 8.0f;
      default: return 4.0f;
    }
  }
  
  private float getDissipationRate(HullSize hullSize) {
    switch (hullSize) {
      case FRIGATE: return 0.05f;
      case DESTROYER: return 0.04f;
      case CRUISER: return 0.03f;
      case CAPITAL_SHIP: return 0.02f;
      default: return 0.04f;
    }
  }
  
  /**
   */
  private static boolean isInRefit(ShipAPI ship) {
    return ship.getOriginalOwner() == -1 && Global.getCurrentState() != GameState.COMBAT;
  }
  
  /**
   */
  private void displayInfusionStatus(ShipAPI ship, CombatEngineAPI engine) {
    float infusionProgress = getInfusionProgress(ship);
    
    float efficiency = MIN_EFFICIENCY + (MAX_EFFICIENCY - MIN_EFFICIENCY) * infusionProgress;
    float minEfficiency = 0.01f;
    efficiency = Math.max(minEfficiency, efficiency);
    
    // getdistance bonus
    float distanceBonus = calculateDistanceBonus(ship);
    
    String timerKey = "FSD_SecondaryKarma_CeaseFireTimer";
    float ceaseFireTimer = 0f;
    if (ship.getCustomData().containsKey(timerKey)) {
      ceaseFireTimer = (Float) ship.getCustomData().get(timerKey);
    }
    float ceaseFireThreshold = getCeaseFireThreshold(ship.getHullSize());
    float dissipationRate = getDissipationRate(ship.getHullSize());
    
    KarmaStatusDisplay.displaySecondaryKarmaStatus(
        ship, engine, 
        infusionProgress, efficiency,
        distanceBonus,
        ceaseFireTimer, ceaseFireThreshold, dissipationRate,
        "SecondaryKarma");
  }
  
  @Override
  public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }
  
  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    Color highlight = Misc.getHighlightColor();
    Color negative = Misc.getNegativeHighlightColor();
    Color positive = Misc.getPositiveHighlightColor();
    
    // getcurrentinfestation progress and efficiency
    float infusionProgress = 0f;
    float efficiency = 0f;
    if (ship != null) {
      // combat in ： from ShipAPI customData in read
      if (!isInRefit(ship)) {
        infusionProgress = getInfusionProgress(ship);
      } else {
        String memberId = null;
        if (ship.getVariant() != null) {
          for (String tag : ship.getVariant().getTags()) {
            if (tag.startsWith("fsd_fleet_member_id_")) {
              memberId = tag.substring("fsd_fleet_member_id_".length());
              break;
            }
          }
        }
        
        // useFleetMember ID from globalMemoryread
        if (memberId != null && !memberId.isEmpty()) {
          infusionProgress = data.campaign.CrystalInfusionFleetListener.getInfusionProgressById(memberId);
        } else {
          infusionProgress = getInfusionProgressFromMemory();
        }
      }
      
      efficiency = MIN_EFFICIENCY + (MAX_EFFICIENCY - MIN_EFFICIENCY) * infusionProgress;
    }
    
    tooltip.addPara(
        "A controlled Reflecting-Light Crystal infestation on a non-Farsight Drive hull.",
        pad);
    
    // displaycurrentinfestation progress and efficiency
    if (ship != null) {
      tooltip.addSectionHeading("Current Status", Alignment.MID, pad);
      tooltip.addPara(
          "Infusion progress: %s    Karma efficiency: %s",
          pads, Misc.getTextColor(), highlight, 
          String.format("%.1f%%", infusionProgress * 100),
          String.format("%.1f%%", efficiency * 100));
    }
    
    tooltip.addSectionHeading("Base Effects", Alignment.MID, pad);
    tooltip.addPara(
        "Every %s karma provides an additional %s flux dissipation, up to %s additional dissipation.",
        pads, Misc.getTextColor(), highlight, "1%", "5/10/15/20", "50%");
    
    tooltip.addSectionHeading("Karma Mechanics", Alignment.MID, pad);
    tooltip.addPara(
        "For every %s of base flux capacity dealt as damage (shield damage counts half), the ship gains proportional karma.\nKarma gain improves within %s of enemies, reaching maximum efficiency at %s for %s increased gain.",
        pads, Misc.getTextColor(), highlight, "10%", "1000su", "500su", "20%");
    
    // displaycurrent ship entropy field range
    String entropyRangeText = "600/900/1200/1500su";
    if (ship != null) {
      float currentRange = getEntropyFieldRange(ship);
      entropyRangeText = String.format("%.0fsu", currentRange);
    }
    
    tooltip.addPara(
        "The warship maintains an entropy field with radius %s (frigate 600 / destroyer 900 / cruiser 1200 / capital 1500).\nCasualties inside the field generate %s for the ship.\n %s ",
        pads, Misc.getTextColor(), highlight, entropyRangeText, "karma", "Only compatible special hullmods can spend stored karma.");
    
    tooltip.addSectionHeading("Karma From Kills and Disables", Alignment.MID, pad);
    float col1 = 180f;
    float col2 = 180f;
    tooltip.beginTable(
        Misc.getBasePlayerColor(),
        Misc.getDarkPlayerColor(),
        Misc.getBrightPlayerColor(),
        20f,
        true,
        true,
        new Object[] {"Destroyed Target Type", col1, "Karma Gain", col2});
    tooltip.addRow(Alignment.MID, highlight, "Frigate", Alignment.MID, positive, "10%");
    tooltip.addRow(Alignment.MID, highlight, "Destroyer", Alignment.MID, positive, "20%");
    tooltip.addRow(Alignment.MID, highlight, "Cruiser", Alignment.MID, positive, "30%");
    tooltip.addRow(Alignment.MID, highlight, "Capital", Alignment.MID, positive, "40%");
    tooltip.addTable("", 0, pad);
    
    tooltip.addSectionHeading("Efficiency Impact", Alignment.MID, pad);
    tooltip.addPara(
        "All karma gain is limited by infusion progress, to a maximum efficiency of %s.",
        pads, Misc.getTextColor(), negative, "50%");
  }
  
}

