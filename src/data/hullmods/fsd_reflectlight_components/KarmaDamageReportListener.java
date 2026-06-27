package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import data.hullmods.FSD_ReflectLight;
import org.apache.log4j.Logger;

public class KarmaDamageReportListener implements DamageListener {
  private static final Logger log = Global.getLogger(KarmaDamageReportListener.class);
  private static final boolean ENABLE_LOGGING = FSD_ReflectLight.ENABLE_DETAIL_LOGGING;
  private static final String REGISTERED_KEY = "FSD_KarmaDamageReportListenerRegistered";

  private static final float SHIELD_DAMAGE_MULT = 0.5f;

  public static void ensureRegistered(CombatEngineAPI engine) {
    if (engine == null) {
      return;
    }
    if (!engine.getListenerManager().hasListenerOfClass(KarmaDamageReportListener.class)) {
      engine.getListenerManager().addListener(new KarmaDamageReportListener());
    }
    engine.getCustomData().put(REGISTERED_KEY, Boolean.TRUE);
  }
  
  @Override
  public void reportDamageApplied(
      Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
    
    ShipAPI sourceShip = getSourceShip(source);
    if (sourceShip == null) {
      return;
    }
    
    if (!sourceShip.getVariant().hasHullMod("FSD_ReflectLight") 
        && !sourceShip.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
      return;
    }
    
    if (!(target instanceof ShipAPI)) {
      return;
    }
    
    ShipAPI targetShip = (ShipAPI) target;
    if (sourceShip.getOwner() == targetShip.getOwner()) {
      return;
    }
    
    KarmaManager manager = KarmaManager.getInstance();
    KarmaData karmaData = manager.getKarmaData(sourceShip);
    if (karmaData == null) {
      if (ENABLE_LOGGING && log != null) {
        log.warn("[KarmaDamageReportListener] ship " + sourceShip.getId() + " has no KarmaData");
      }
      return;
    }
    
    float effectiveDamage = 
        result.getTotalDamageToArmor() + 
        result.getDamageToHull() + 
        (result.getDamageToShields() * SHIELD_DAMAGE_MULT);
    
    if (effectiveDamage <= 0) {
      return;
    }
    
    karmaData.addDamageReport(effectiveDamage);
    
    sourceShip.setCustomData("FSD_ReflectLight_LastTarget", targetShip);
    
    if (ENABLE_LOGGING && log != null && 
        Global.getCombatEngine().getTotalElapsedTime(false) % 2.0f < 0.1f) {
      float baseFlux = sourceShip.getHullSpec().getFluxCapacity();
      log.info(String.format(
          "[Reflecting-Light Crystal-damage] ship %s accumulated damage: %.1f (equivalent to base flux%.1f%%)",
          getShipName(sourceShip),
          karmaData.getKarmaDamageReport(), 
          (karmaData.getKarmaDamageReport() / baseFlux) * 100));
    }
  }
  
  /**
   * getdamagesourceship
   */
  private ShipAPI getSourceShip(Object source) {
    if (source instanceof ShipAPI) {
      return (ShipAPI) source;
    } else if (source instanceof WeaponAPI) {
      WeaponAPI weapon = (WeaponAPI) source;
      return weapon.getShip();
    } else if (source instanceof DamagingProjectileAPI) {
      DamagingProjectileAPI projectile = (DamagingProjectileAPI) source;
      if (projectile.getSource() instanceof ShipAPI) {
        return (ShipAPI) projectile.getSource();
      }
    }
    return null;
  }
  
  /**
   * getship display name
   */
  private String getShipName(ShipAPI ship) {
    if (ship.getName() != null && !ship.getName().isEmpty()) {
      return ship.getName();
    }
    return ship.getHullSpec().getHullNameWithDashClass();
  }
}
