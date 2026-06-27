package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaManager;
import data.hullmods.fsd_reflectlight_components.KarmaType;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

public class FSD_InductionStand extends BaseHullMod {
  @Deprecated public static float range = 1600f;

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (ship.getVariant().hasHullMod("FSD_InductionStand")
        && ship.getVariant().hasHullMod("shield_shunt")) {
      ship.getVariant().removeMod("shield_shunt");
    }
    if (ship.getVariant().hasHullMod("FSD_InductionStand")
        && ship.getVariant().hasHullMod("hardenedshieldemitter")) {
      ship.getVariant().removeMod("hardenedshieldemitter");
    }
    if (!ship.getCustomData().containsKey("FSD_InductionStand_TotalDamage")) {
      ship.setCustomData("FSD_InductionStand_TotalDamage", 0f);
    }
    if (!ship.hasListenerOfClass(FSD_InductionStand_listener.class)) {
      ship.addListener(new FSD_InductionStand_listener(ship));
    }
  }

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    return null;
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {}

  @Override
  public boolean shouldAddDescriptionToTooltip(
      HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    Color y = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    tooltip.addPara("Core sustainment equipment for the Coexistence-class assault battleship.", pads);
    tooltip.addSectionHeading("Effect", Alignment.MID, pad);
    tooltip.addPara(
        "Within the entropy field, %s of every %s hull lost by nearby ships is transferred to the Coexistence-class.\nTransfer efficiency increases by %s while the ship system is active.\n",
        pads, Misc.getTextColor(), y, "1000", "25%", "200%");
//    tooltip.addPara(
//        "The Coexistence-class loses %s karma per second;\nwhen hull is above %s, it loses %s hull per second to maintain karma until peak time ends;\n",
//        pads, Misc.getTextColor(), y, "1%", "50%", "100");
    tooltip.addPara(
        "Incompatible with %s and %s.\n",
//                "The field fails when the ship reaches %s peak time.",
            pads, Misc.getTextColor(), r, "Stabilized Shields", "Shield Shunt");
  }

  public void advanceInCombat(ShipAPI ship, float amount) {
    if (!ship.isAlive() || ship.isHulk()) return;
  }

  public static class FSD_InductionStand_listener implements AdvanceableListener {
    private ShipAPI ship;
    public final String id = "FSD_InductionStand_listener_effect";
    float EFFECT_COMMON = 5000f;
    float EFFECT_USESYSTEM = 500f;
    float time = 0f;
    float time_interv = 0.5f;
    private IntervalUtil DetectTimer = new IntervalUtil(1f, 1f);
    public static float IS_RCMULT = 2f;
    private float TOTAL_REPAIR = 0;
    private float rangeSq;
    private final IntervalUtil cleanupTimer = new IntervalUtil(5f, 7f);
    private boolean RC;

    private FSD_InductionStand_listener(ShipAPI ship) {
      this.ship = ship;
      updateRangeCache();
    }

    private void updateRangeCache() {
      float currentRange = 1500f;
      rangeSq = currentRange * currentRange;
    }
    
    private void setCustomDissipationRate(ShipAPI ship) {
      KarmaManager.getInstance().setKarmaDissipationRate(
          ship, 
          0f, 
          data.hullmods.fsd_reflectlight_components.KarmaPriority.PRIORITY_HIGH,
          "FSD_InductionStand_Custom"
      );
    }
    
    private void handleCustomKarmaDissipation(float amount) {
      data.hullmods.fsd_reflectlight_components.KarmaData karmaData = 
          KarmaManager.getInstance().getKarmaData(ship);
      if (karmaData == null) {
        return;
      }
      
      float karma = karmaData.getKarma();
      if (karma <= 0) {
        return;
      }
      
      float karmaLoss = 0.01f * amount;
      karmaData.reduceKarma(karmaLoss, data.hullmods.fsd_reflectlight_components.KarmaType.PASSIVE_LOSS);
    }

    @Override
    public void advance(float amount) {
      IntervalUtil Clock = new IntervalUtil(1f, 1f);
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      Clock.advance(amount);
//      if (ship.getPeakTimeRemaining() > 0) {
//        if (ship.getHullLevel() >= 0.5f) {
//          KarmaManager.getInstance().setKarmaDissipationRate(
//              ship,
//              0f,
//              data.hullmods.fsd_reflectlight_components.KarmaPriority.PRIORITY_HIGH,
//              "FSD_InductionStand_Prevent"
//          );
//          DetectTimer.advance(amount);
//          if (DetectTimer.intervalElapsed()) {
//            ship.setHitpoints(HitpointDecrease(100f));
//          }
//        } else {
//          setCustomDissipationRate(ship);
//          handleCustomKarmaDissipation(amount);
//        }
//      } else {
//        setCustomDissipationRate(ship);
//        handleCustomKarmaDissipation(amount);
//      }
//      if (Clock.intervalElapsed()) {
//        KarmaDecrease(0.01f);
//      }
      if (!ship.getCustomData().containsKey("FSD_InductionStand_Recover")) {
        ship.setCustomData("FSD_InductionStand_Recover", false);
      }
      if (!ship.getCustomData().containsKey("FSD_InductionStand_TotalRecover")) {
        ship.setCustomData("FSD_InductionStand_TotalRecover", 0f);
      }
      updateRangeCache();
      cleanupTimer.advance(amount);
      if (cleanupTimer.intervalElapsed()) {
        cleanupExpiredListeners();
      }
      time += amount;
      if (time >= time_interv) {
        time = 0f;
        Vector2f shipLoc = ship.getLocation();
        List<ShipAPI> inRangeShips = new ArrayList<>();
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
          if (s.getHullSize().equals(HullSize.FIGHTER)) continue;
          if (s.isHulk() || !s.isAlive()) continue;
          if (s == ship) continue;
          Vector2f targetLoc = s.getLocation();
          float dx = shipLoc.x - targetLoc.x;
          float dy = shipLoc.y - targetLoc.y;
          float distSq = dx * dx + dy * dy;
          if (distSq <= rangeSq) {
            inRangeShips.add(s);
          }
        }
        for (ShipAPI s : inRangeShips) {
          if (!s.hasListenerOfClass(FSD_InductionStand_listener_effectset.class)) {
            s.addListener(new FSD_InductionStand_listener_effectset(s, ship));
          } else {
            List<FSD_InductionStand_listener_effectset> ls =
                s.getListeners(FSD_InductionStand_listener_effectset.class);
            if (ls.size() > 0) {
              ls.get(0).notifyEffect();
            }
          }
        }
      }
    }

    private void KarmaDecrease(float percent) {
      KarmaManager manager = KarmaManager.getInstance();
      float currentKarma = manager.getKarma(ship);
      float karmaMax = manager.getKarmaMax(ship);
      float decreaseAmount = percent * karmaMax;
      manager.reduceKarma(ship, decreaseAmount, KarmaType.PASSIVE_LOSS);
    }

    private float GetTotalDamage(float damage) {
      Object damageObj = ship.getCustomData().get("FSD_InductionStand_TotalDamage");
      float CurDamage = (damageObj instanceof Float) ? (Float) damageObj : 0f;
      return CurDamage + damage;
    }

    private float DamageCountDecrease(float damage) {
      Object damageObj = ship.getCustomData().get("FSD_InductionStand_TotalDamage");
      float CurDamage = (damageObj instanceof Float) ? (Float) damageObj : 0f;
      return CurDamage - damage;
    }

    private void cleanupExpiredListeners() {
      if (Global.getCombatEngine() == null) return;
      Vector2f shipLoc = ship.getLocation();
      for (ShipAPI s : Global.getCombatEngine().getShips()) {
        if (s.hasListenerOfClass(FSD_InductionStand_listener_effectset.class)) {
          boolean shouldRemove = false;
          if (s.isHulk() || !s.isAlive()) {
            shouldRemove = true;
          } else {
            Vector2f targetLoc = s.getLocation();
            float dx = shipLoc.x - targetLoc.x;
            float dy = shipLoc.y - targetLoc.y;
            float distSq = dx * dx + dy * dy;
            if (distSq > rangeSq) {
              shouldRemove = true;
            }
          }
          if (shouldRemove) {
            s.removeListenerOfClass(FSD_InductionStand_listener_effectset.class);
          }
        }
      }
    }

    public void notifyrecover(float f) {
      float MaxHP = ship.getMaxHitpoints();
      float REAL_REPAIR = 0;
      int count = 0;
      Object rcObj = ship.getCustomData().get("FSD_InductionStand_Recover");
      RC = (rcObj instanceof Boolean) ? (Boolean) rcObj : false;
      Object totalRepairObj = ship.getCustomData().get("FSD_InductionStand_TotalRecover");
      TOTAL_REPAIR = (totalRepairObj instanceof Float) ? (Float) totalRepairObj : 0f;
      float karma = KarmaManager.getInstance().getKarma(ship);
      ship.setCustomData("FSD_InductionStand_TotalDamage", GetTotalDamage(f));
      float effect = 1f;
      float maxPeakTime = 1800f;
      float peakTimePercent = 0;
      if (ship.getPeakTimeRemaining() > 0 && maxPeakTime > 0) {
        peakTimePercent = ship.getPeakTimeRemaining() / maxPeakTime;
      }
      if (peakTimePercent <= 0.0f) {
        return;
      }
      if (!RC) {
        if (TOTAL_REPAIR <= MaxHP * IS_RCMULT) {
          float ResidualHP = MaxHP * IS_RCMULT - TOTAL_REPAIR;
          Object damageObj = ship.getCustomData().get("FSD_InductionStand_TotalDamage");
          float currentTotalDamage = (damageObj instanceof Float) ? (Float) damageObj : 0f;
          if (ship.getSystem() != null
              && ship.getSystem().getEffectLevel() >= 0
              && currentTotalDamage >= 500f) {
            REAL_REPAIR =
                Math.max((int) (currentTotalDamage / EFFECT_USESYSTEM), 0f) * effect * 250f;
            ship.setCustomData("FSD_InductionStand_TotalDamage", DamageCountDecrease(500f));
          }
          if (ship.getSystem() != null
              && ship.getSystem().getEffectLevel() <= 0
              && currentTotalDamage >= 5000f) {
            REAL_REPAIR = Math.max((int) (currentTotalDamage / EFFECT_COMMON), 0f) * effect * 250f;
            ship.setCustomData("FSD_InductionStand_TotalDamage", DamageCountDecrease(1000f));
          }
          REAL_REPAIR = Math.min(REAL_REPAIR, ResidualHP);
          TOTAL_REPAIR += REAL_REPAIR;
          ship.setHitpoints(Math.min(ship.getHitpoints() + REAL_REPAIR, ship.getMaxHitpoints()));
        }
      }
      if (TOTAL_REPAIR >= MaxHP * IS_RCMULT) {
        ship.setCustomData("FSD_InductionStand_Recover", true);
        ship.setCustomData("FSD_InductionStand_TotalRecover", TOTAL_REPAIR);
      }
    }

    private float HitpointDecrease(float num) {
      float CurrentHP = ship.getHitpoints();
      return (CurrentHP - num);
    }
  }

  public static class FSD_InductionStand_listener_effectset
      implements AdvanceableListener, DamageListener {
    private ShipAPI ship;
    private ShipAPI sourceship;
    private boolean RC = false;
    public final String id = "FSD_2_listener_effectset_effect";
    float time = 0f;
    float time_interv = 1.2f;
    private IntervalUtil RCtimer = new IntervalUtil(120f, 120f);
    private float lastReportedHullPoints = 0f;

    private FSD_InductionStand_listener_effectset(ShipAPI ship, ShipAPI sourceship) {
      this.ship = ship;
      this.sourceship = sourceship;
      this.lastReportedHullPoints = ship.getHitpoints();
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (sourceship != null && sourceship.getCustomData() != null) {
        RC = (boolean) sourceship.getCustomData().get("FSD_InductionStand_Recover");
      }
      time += amount;
      if (time >= time_interv) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (ship.getHitpoints() <= 200f) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (RC && sourceship != null) {
        RCtimer.advance(amount);
        if (RCtimer.intervalElapsed()) {
          sourceship.setCustomData("FSD_InductionStand_Recover", false);
          sourceship.setCustomData("FSD_InductionStand_TotalRecover", 0);
        }
      }
    }

    public void notifyEffect() {
      time = 0f;
    }

    @Override
    public void reportDamageApplied(
        Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
      if (target != ship || sourceship == null) return;
      if (target != null && ((ShipAPI) target).getHullSize() == HullSize.FIGHTER) return;
      if (result.getDamageToHull() > 0) {
        float hullDamage = result.getDamageToHull();
        float currentHull = ship.getHitpoints();
        lastReportedHullPoints = currentHull;
        if (!sourceship.hasListenerOfClass(FSD_InductionStand_listener.class)) return;
        List<FSD_InductionStand_listener> listeners =
            sourceship.getListeners(FSD_InductionStand_listener.class);
        for (FSD_InductionStand_listener l : listeners) {
          l.notifyrecover(hullDamage);
        }
      }
    }
  }
}
