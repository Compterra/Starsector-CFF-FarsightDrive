package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KarmaStatusDisplay {
  
  private static final String STATUS_PREFIX = "FSD_Karma_";
  
  private static final String ICON_KARMA = "graphics/icons/hullsys/entropy_amplifier.png";
  private static final String ICON_FLUX = "graphics/icons/hullsys/damper_field.png";
  private static final String ICON_RATE = "graphics/icons/hullsys/targeting_feed.png";
  private static final String ICON_DISSIPATION = "graphics/icons/hullsys/high_energy_focus.png";
  private static final String ICON_INFUSION = "graphics/hullmods/FSD_Crystals.png";
  
  private KarmaStatusDisplay() {
  }
  
  
  public static void displayFullKarmaStatus(
      ShipAPI ship, CombatEngineAPI engine, 
      float distanceBonus,
      float ceaseFireTimer, float ceaseFireThreshold, float dissipationRate,
      String sourceId) {
    if (ship == null || engine == null) return;
    if (ship != engine.getPlayerShip()) return;
    
    KarmaData karmaData = KarmaAPI.getKarmaData(ship);
    if (karmaData == null) return;
    
    float karma = karmaData.getKarma();
    
    displayKarmaLevel(ship, engine, karmaData, sourceId, false);
    displayFluxBonus(ship, engine, karmaData, sourceId);
    displayConversionRateWithDistance(ship, engine, karmaData, distanceBonus, sourceId);
    displayDissipationStatus(engine, ship, ceaseFireTimer, ceaseFireThreshold, dissipationRate, karma, sourceId);
  }
  
  public static void displaySecondaryKarmaStatus(
      ShipAPI ship, CombatEngineAPI engine, 
      float infusionProgress, float efficiency,
      float distanceBonus,
      float ceaseFireTimer, float ceaseFireThreshold, float dissipationRate,
      String sourceId) {
    if (ship == null || engine == null) return;
    if (ship != engine.getPlayerShip()) return;
    
    KarmaData karmaData = KarmaAPI.getKarmaData(ship);
    if (karmaData == null) return;
    
    float karma = karmaData.getKarma();
    
    displayInfusionProgress(engine, infusionProgress, efficiency, sourceId);
    
    displayKarmaLevel(ship, engine, karmaData, sourceId, true);
    
    displayFluxBonus(ship, engine, karmaData, sourceId);
    
    displayConversionRateWithDistance(ship, engine, karmaData, distanceBonus, sourceId);
    
    displayDissipationStatus(engine, ship, ceaseFireTimer, ceaseFireThreshold, dissipationRate, karma, sourceId);
  }
  
  
  public static void displayKarmaLevel(
      ShipAPI ship, CombatEngineAPI engine, 
      KarmaData karmaData, String sourceId, boolean isLimited) {
    
    float karma = karmaData.getKarma();
    int karmaPercent = (int) (karma * 100);
    
    String karmaStatus = String.format("Karma: %d%%", karmaPercent);
    String karmaDetail;
    boolean negative = false;
    
    if (karma >= 0.8f) {
      karmaDetail = isLimited ? "Karma surging (limited)" : "Karma surging";
    } else if (karma >= 0.5f) {
      karmaDetail = isLimited ? "Karma abundant (limited)" : "Karma abundant";
    } else if (karma >= 0.3f) {
      karmaDetail = isLimited ? "Karma stable (limited)" : "Karma stable";
    } else if (karma >= 0.1f) {
      karmaDetail = "Karma low";
      negative = true;
    } else {
      karmaDetail = "Karma depleted";
      negative = true;
    }
    
    if (!isLimited) {
      float efficiencyMultiplier = karmaData.getEfficiencyMultiplier();
      if (efficiencyMultiplier < 0.99f) {
        int efficiencyPercent = (int) (efficiencyMultiplier * 100);
        karmaDetail += String.format(" (efficiency %d%%)", efficiencyPercent);
      }
    }
    
    engine.maintainStatusForPlayerShip(
        STATUS_PREFIX + sourceId + "_Karma",
        ICON_KARMA,
        isLimited ? "Secondary Karma System" : "Entropy Field Karma",
        karmaStatus + " - " + karmaDetail,
        negative);
  }
  
  public static void displayFluxBonus(
      ShipAPI ship, CombatEngineAPI engine, 
      KarmaData karmaData, String sourceId) {
    
    float karma = karmaData.getKarma();
    if (karma <= 0) return;
    
    HullSize hullSize = ship.getHullSize();
    float baseDissipation = ship.getHullSpec().getFluxDissipation();
    float karmaPercentFloat = karma * 100f;
    
    float bonusPerPercent = getBonusPerPercent(hullSize);
    float maxBonus = baseDissipation * 0.5f;
    float bonusDissipation = Math.min(bonusPerPercent * karmaPercentFloat, maxBonus);
    
    if (bonusDissipation > 0) {
      String fluxText = String.format("+%.0f flux dissipation", bonusDissipation);
      String fluxDetail = String.format("From karma (cap %.0f)", maxBonus);
      
      engine.maintainStatusForPlayerShip(
          STATUS_PREFIX + sourceId + "_FluxBonus",
          ICON_FLUX,
          "Karma Bonus",
          fluxText + " - " + fluxDetail,
          false);
    }
  }
  
  public static void displayConversionRateWithDistance(
      ShipAPI ship, CombatEngineAPI engine, 
      KarmaData karmaData, float distanceBonus, String sourceId) {
    
    float gainMultiplier = karmaData.getGainKarma();
    float efficiencyMult = karmaData.getEfficiencyMultiplier();
    float totalConversionRate = gainMultiplier * distanceBonus * efficiencyMult;
    
    String conversionText = String.format("Conversion rate: %.0f%%", totalConversionRate * 100f);
    String conversionDetail;
    
    if (efficiencyMult < 0.99f) {
      if (distanceBonus > 1.01f) {
        conversionDetail = String.format("Efficiency x%.0f%% + distance +%.0f%%", efficiencyMult * 100f, (distanceBonus - 1.0f) * 100f);
      } else {
        conversionDetail = String.format("Efficiency x%.0f%%", efficiencyMult * 100f);
      }
    } else if (distanceBonus > 1.01f) {
      conversionDetail = String.format("Distance bonus +%.0f%%", (distanceBonus - 1.0f) * 100f);
    } else {
      conversionDetail = "Base efficiency";
    }
    
    boolean isLowRate = totalConversionRate < 1.0f;
    
    engine.maintainStatusForPlayerShip(
        STATUS_PREFIX + sourceId + "_ConversionRate",
        ICON_RATE,
        "Karma Conversion Efficiency",
        conversionText + " - " + conversionDetail,
        isLowRate);
  }
  
  public static void displayConversionRate(
      ShipAPI ship, CombatEngineAPI engine, 
      KarmaData karmaData, String sourceId) {
    displayConversionRateWithDistance(ship, engine, karmaData, 1.0f, sourceId);
  }
  
  public static void displayInfusionProgress(
      CombatEngineAPI engine, 
      float infusionProgress, float efficiency, String sourceId) {
    
    int progressPercent = (int) (infusionProgress * 100);
    int efficiencyPercent = (int) (efficiency * 100);
    
    String statusText = String.format("Infusion progress: %d%%", progressPercent);
    String detailText = String.format("Karma efficiency: %d%%", efficiencyPercent);
    
    boolean isLow = infusionProgress < 0.3f;
    
    engine.maintainStatusForPlayerShip(
        STATUS_PREFIX + sourceId + "_Progress",
        ICON_INFUSION,
        "Secondary Crystal Overgrowth",
        statusText + " - " + detailText,
        isLow);
  }
  
  public static void displayDissipationStatus(
      CombatEngineAPI engine, ShipAPI ship,
      float ceaseFireTimer, float ceaseFireThreshold,
      float dissipationRate, float karma,
      String sourceId) {
    
    boolean isFiring = false;
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getChargeLevel() >= 1) {
        isFiring = true;
        break;
      }
    }
    
    if (isFiring || karma <= 0) return;
    
    if (ceaseFireTimer >= ceaseFireThreshold) {
      String dissText = "Karma dissipating";
      String dissDetail = String.format("-%.0f%% per second", dissipationRate * 100f);
      
      engine.maintainStatusForPlayerShip(
          STATUS_PREFIX + sourceId + "_Dissipation",
          ICON_DISSIPATION,
          "Ceasefire state",
          dissText + " - " + dissDetail,
          true);
    } else {
      float remaining = ceaseFireThreshold - ceaseFireTimer;
      String countdownText = String.format("Ceasefire %.1fs", ceaseFireTimer);
      String countdownDetail = String.format("Dissipation starts after %.1fs", remaining);
      
      engine.maintainStatusForPlayerShip(
          STATUS_PREFIX + sourceId + "_Dissipation",
          ICON_DISSIPATION,
          "Ceasefire state",
          countdownText + " - " + countdownDetail,
          false);
    }
  }
  
  
  private static float getBonusPerPercent(HullSize hullSize) {
    switch (hullSize) {
      case FRIGATE: return 5f;
      case DESTROYER: return 10f;
      case CRUISER: return 15f;
      case CAPITAL_SHIP: return 20f;
      default: return 5f;
    }
  }
}
