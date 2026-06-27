package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaManager;
import data.hullmods.fsd_reflectlight_components.KarmaType;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

public class FSD_RLIntranet extends BaseHullMod {
  public static final float RANGE_MULTIPLIER = 2.0f;
  public static final float KARMA_MULTIPLIER = 0.2f;

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    KarmaManager manager = KarmaManager.getInstance();
    manager.setKarmaGainEfficiency(ship, KARMA_MULTIPLIER);
    manager.setKarmaGainRange(ship, RANGE_MULTIPLIER);
    if (ship.getVariant().hasHullMod("converted_hangar")) {
      ship.getVariant().removeMod("converted_hangar");
    }
    if (ship.getVariant().hasHullMod("hardenedshieldemitter")) {
      ship.getVariant().removeMod("hardenedshieldemitter");
    }
    if (!ship.hasListenerOfClass(FSD_RLIntranet_listener.class)) {
      ship.addListener(new FSD_RLIntranet_listener(ship));
    }
  }

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (ship.getVariant().hasHullMod("converted_hangar")) return false;
    if (ship.getVariant().hasHullMod("hardenedshieldemitter")) return false;
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship.getVariant().hasHullMod("converted_hangar")) {
      return "Incompatible with Converted Hangar";
    }
    if (ship.getVariant().hasHullMod("hardenedshieldemitter")) {
      return "Incompatible with Stabilized Shields";
    }
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
    Color c = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    tooltip.addPara("Fleet-support architecture built around the Equilibrium-class crystal intranet.", pads);
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara("Phase entropy field range is multiplied by %s.\n", pads, Misc.getTextColor(), c, "x2");
    tooltip.addPara(
        "Fighters inside the phase entropy field repair an additional %s hull per second.\nWarships repair %s/%s/%s/%s hull per second depending on ship size.\n",
        pads, Misc.getTextColor(), c, "25", "2%", "1.5%", "1%", "0.5%");
    tooltip.addPara("Friendly units inside the entropy field gain %s weapon range and %s flux dissipation.", pads, Misc.getTextColor(), c, "10%", "10%");
    tooltip.addSectionHeading("Drawbacks", Alignment.MID, pad);
    tooltip.addPara(
        "Karma conversion efficiency is reduced to %s.\n",
        pads, Misc.getTextColor(), r, "x0.2 (20%)");
    tooltip.addPara(
        "The intranet repair effect consumes karma.\nEvery %s total allied hull repaired consumes %s karma.\nRepairs stop when karma reaches zero.\n",
        pads, Misc.getTextColor(), c, "100", "1%");
    tooltip.addPara("Because the intranet occupies much of the core area, %s.", pads, Misc.getTextColor(), r, "it is incompatible with Converted Hangar and Stabilized Shields");
  }

  public void advanceInCombat(ShipAPI ship, float amount) {
    if (!ship.isAlive() || ship.isHulk()) return;
    if (!ship.getCustomData().containsKey("FSD_RLIntranet_RepairTotal")) {
      ship.setCustomData("FSD_RLIntranet_RepairTotal", 0f);
    }
  }

  public static class FSD_RLIntranet_listener implements AdvanceableListener {
    private ShipAPI ship;
    public final String id = "FSD_RLIntranet_listener_effect";
    float RANGE_MODIFER = 10f;
    float DISSIPATION_MODIFER = 10f;
    private Map<String, ShipAPI> extensionCruisers = new HashMap<>();
    private final IntervalUtil cleanupTimer = new IntervalUtil(5f, 7f);
    private float entropyRangeSq = 0f;
    float time = 0f;
    float time_interv = 0.5f;

    private FSD_RLIntranet_listener(ShipAPI ship) {
      this.ship = ship;
      updateEntropyRangeCache();
    }

    private void updateEntropyRangeCache() {
      float rangeMultiplier = KarmaManager.getInstance().getKarmaData(ship).getGainKarmaRange();
      float range = FSD_ReflectLight.getEntropyFieldRange(ship, rangeMultiplier);
      entropyRangeSq = range * range;
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (!ship.getCustomData().containsKey("FSD_RLIntranet_RepairTotal")) {
        ship.setCustomData("FSD_RLIntranet_RepairTotal", 0f);
      }
      updateEntropyRangeCache();
      cleanupTimer.advance(amount);
      if (cleanupTimer.intervalElapsed()) {
        cleanupEffects();
      }
      time += amount;
      if (time >= time_interv) {
        time = 0f;
        Vector2f shipLoc = ship.getLocation();
        List<ShipAPI> inRangeShips = new ArrayList<>();
        List<ShipAPI> cruisersAndCapitals = new ArrayList<>();
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
          if (s == ship) continue;
          if (s.isHulk() || !s.isAlive()) continue;
          if (s.getOwner() != ship.getOwner()) continue;
          Vector2f targetLoc = s.getLocation();
          float dx = shipLoc.x - targetLoc.x;
          float dy = shipLoc.y - targetLoc.y;
          float distSq = dx * dx + dy * dy;
          if (distSq <= entropyRangeSq) {
            inRangeShips.add(s);
            if (s.getHullSize() == HullSize.CRUISER || s.getHullSize() == HullSize.CAPITAL_SHIP) {
              cruisersAndCapitals.add(s);
            }
          }
        }
        for (ShipAPI s : inRangeShips) {
          if (!s.hasListenerOfClass(FSD_RLIntranet_listener_effectset.class)) {
            s.addListener(new FSD_RLIntranet_listener_effectset(s, ship));
          } else {
            refreshEffectListener(s);
          }
        }
        extensionCruisers.clear();
        int count = 0;
        for (ShipAPI s : cruisersAndCapitals) {
          if (count >= 3) break;
          if (!s.hasListenerOfClass(FSD_RLIntranet_aboveCAset_listener.class)) {
            s.addListener(new FSD_RLIntranet_aboveCAset_listener(s, ship));
          } else {
            refreshExtensionListener(s);
          }
          extensionCruisers.put(s.getId(), s);
          count++;
        }
      }
    }

    private void refreshEffectListener(ShipAPI s) {
      List<FSD_RLIntranet_listener_effectset> listeners =
          s.getListeners(FSD_RLIntranet_listener_effectset.class);
      if (listeners != null && !listeners.isEmpty()) {
        for (FSD_RLIntranet_listener_effectset l : listeners) {
          l.notifyEffect();
        }
      }
    }

    private void refreshExtensionListener(ShipAPI s) {
      List<FSD_RLIntranet_aboveCAset_listener> listeners =
          s.getListeners(FSD_RLIntranet_aboveCAset_listener.class);
      if (listeners != null && !listeners.isEmpty()) {
        for (FSD_RLIntranet_aboveCAset_listener l : listeners) {
          l.notifyEffect();
        }
      }
    }

    private void cleanupEffects() {
      List<ShipAPI> allShips = Global.getCombatEngine().getShips();
      Vector2f shipLoc = ship.getLocation();
      for (ShipAPI s : allShips) {
        if (s.hasListenerOfClass(FSD_RLIntranet_listener_effectset.class)) {
          boolean shouldRemove = false;
          if (!s.isAlive() || s.isHulk()) {
            shouldRemove = true;
          } else {
            Vector2f targetLoc = s.getLocation();
            float dx = shipLoc.x - targetLoc.x;
            float dy = shipLoc.y - targetLoc.y;
            float distSq = dx * dx + dy * dy;
            boolean inMainRange = distSq <= entropyRangeSq;
            boolean inExtensionRange = isInExtensionRange(s);
            if (!inMainRange && !inExtensionRange) {
              shouldRemove = true;
            }
          }
          if (shouldRemove) {
            s.removeListenerOfClass(FSD_RLIntranet_listener_effectset.class);
          }
        }
        if (s.hasListenerOfClass(FSD_RLIntranet_aboveCAset_listener.class)) {
          boolean shouldRemove = false;
          if (!s.isAlive() || s.isHulk()) {
            shouldRemove = true;
          } else {
            Vector2f targetLoc = s.getLocation();
            float dx = shipLoc.x - targetLoc.x;
            float dy = shipLoc.y - targetLoc.y;
            float distSq = dx * dx + dy * dy;
            if (distSq > entropyRangeSq) {
              shouldRemove = true;
            }
          }
          if (!extensionCruisers.containsKey(s.getId())) {
            shouldRemove = true;
          }
          if (shouldRemove) {
            s.removeListenerOfClass(FSD_RLIntranet_aboveCAset_listener.class);
          }
        }
      }
    }

    private boolean isInExtensionRange(ShipAPI target) {
      for (ShipAPI extender : extensionCruisers.values()) {
        if (!extender.isAlive() || extender.isHulk()) continue;
        float extRange = (extender.getHullSize() == HullSize.CRUISER) ? 600f : 800f;
        float extRangeSq = extRange * extRange;
        Vector2f extenderLoc = extender.getLocation();
        Vector2f targetLoc = target.getLocation();
        float dx = extenderLoc.x - targetLoc.x;
        float dy = extenderLoc.y - targetLoc.y;
        float distSq = dx * dx + dy * dy;
        if (distSq <= extRangeSq) {
          return true;
        }
      }
      return false;
    }
  }

  public static class FSD_RLIntranet_listener_effectset implements AdvanceableListener {
    private ShipAPI ship;
    private ShipAPI sourceShip;
    public final String id = "FSD_RLIntranet_listener_effectset_effect";
    float HITPOINTRECOVER_FI = 25f;
    float HITPOINTRECOVER_BB = 0.005f;
    float HITPOINTRECOVER_CA = 0.01f;
    float HITPOINTRECOVER_DD = 0.015f;
    float HITPOINTRECOVER_FR = 0.02f;
    float effect = 1f;
    float RANGE_MODIFER = 10f;
    float DISSIPATION_MODIFER = 10f;
    float time = 0f;
    float time_interv = 2f;
    private boolean init = false;
    private boolean fighter_key = false;
    private float repairAmount = 0f;

    private FSD_RLIntranet_listener_effectset(ShipAPI ship, ShipAPI sourceShip) {
      this.ship = ship;
      this.sourceShip = sourceShip;
      initEffectMultiplier();
    }

    private void initEffectMultiplier() {
      if (init) return;
      init = true;
      switch (ship.getHullSize()) {
        case FRIGATE:
          effect = HITPOINTRECOVER_FR;
          break;
        case DESTROYER:
          effect = HITPOINTRECOVER_DD;
          break;
        case CRUISER:
          effect = HITPOINTRECOVER_CA;
          break;
        case CAPITAL_SHIP:
          effect = HITPOINTRECOVER_BB;
          break;
        case FIGHTER:
          effect = HITPOINTRECOVER_FI;
          fighter_key = true;
          break;
      }
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      initEffectMultiplier();
      float karma = 0f;
      if (sourceShip != null && sourceShip.isAlive()) {
        karma = KarmaManager.getInstance().getKarma(sourceShip);
      }
      ship.getMutableStats().getFluxDissipation().modifyPercent(id, DISSIPATION_MODIFER);
      ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_MODIFER);
      ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_MODIFER);
      if (karma <= 0) {
        time += amount;
        if (time >= time_interv) {
          ship.removeListenerOfClass(this.getClass());
          ship.getMutableStats().getFluxDissipation().unmodify(id);
          ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
          ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
        }
        return;
      }
      float repairThisFrame = 0f;
      if (ship.getHitpoints() < ship.getMaxHitpoints()) {
        if (fighter_key) {
          repairThisFrame = effect * amount;
          ship.setHitpoints(ship.getHitpoints() + repairThisFrame);
        } else {
          repairThisFrame = ship.getMaxHitpoints() * effect * amount;
          ship.setHitpoints(ship.getHitpoints() + repairThisFrame);
        }
      }
      if (repairThisFrame > 0) {
        repairAmount += repairThisFrame;
        if (repairAmount >= 100f && sourceShip != null) {
          float karmaUsed = (float) Math.floor(repairAmount / 100f) / 100f;
          KarmaManager.getInstance().reduceKarma(sourceShip, karmaUsed, KarmaType.ACTIVE_COST);
          repairAmount = repairAmount % 100f;
          float repairTotal = 0f;
          if (sourceShip.getCustomData().containsKey("FSD_RLIntranet_RepairTotal")) {
            repairTotal = (float) sourceShip.getCustomData().get("FSD_RLIntranet_RepairTotal");
          }
          repairTotal += karmaUsed * 100f * 100f;
          sourceShip.setCustomData("FSD_RLIntranet_RepairTotal", repairTotal);
        }
      }
      time += amount;
      if (time >= time_interv) {
        ship.removeListenerOfClass(this.getClass());
        ship.getMutableStats().getFluxDissipation().unmodify(id);
        ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
        ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
      }
    }

    public void notifyEffect() {
      time = 0f;
    }
  }

  public static class FSD_RLIntranet_aboveCAset_listener implements AdvanceableListener {
    private ShipAPI ship;
    private ShipAPI sourceShip;
    public final String id = "FSD_RLIntranet_aboveCAset_listener_effect";
    float RANGE_CA = 600f;
    float RANGE_BB = 800f;
    float range = 0f;
    float rangeSq = 0f;
    float time = 0f;
    float time_interv = 0.5f;
    float time2 = 0f;
    float time_interv2 = 2f;

    private FSD_RLIntranet_aboveCAset_listener(ShipAPI ship, ShipAPI sourceShip) {
      this.ship = ship;
      this.sourceShip = sourceShip;
      if (ship.getHullSize() == HullSize.CRUISER) {
        range = RANGE_CA;
      } else if (ship.getHullSize() == HullSize.CAPITAL_SHIP) {
        range = RANGE_BB;
      }
      rangeSq = range * range;
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive() || sourceShip == null || !sourceShip.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      time += amount;
      time2 += amount;
      if (time2 >= time_interv2) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (time >= time_interv) {
        time = 0f;
        Vector2f shipLoc = ship.getLocation();
        List<ShipAPI> inRangeShips = new ArrayList<>();
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
          if (s == ship || s == sourceShip) continue;
          if (!s.isAlive() || s.isHulk()) continue;
          if (s.getOwner() != ship.getOwner()) continue;
          Vector2f targetLoc = s.getLocation();
          float dx = shipLoc.x - targetLoc.x;
          float dy = shipLoc.y - targetLoc.y;
          float distSq = dx * dx + dy * dy;
          if (distSq <= rangeSq) {
            inRangeShips.add(s);
          }
        }
        for (ShipAPI s : inRangeShips) {
          if (!s.hasListenerOfClass(FSD_RLIntranet_listener_effectset.class)) {
            s.addListener(new FSD_RLIntranet_listener_effectset(s, sourceShip));
          } else {
            List<FSD_RLIntranet_listener_effectset> ls =
                s.getListeners(FSD_RLIntranet_listener_effectset.class);
            if (!ls.isEmpty()) {
              ls.get(0).notifyEffect();
            }
          }
        }
      }
    }

    public void notifyEffect() {
      time2 = 0f;
    }
  }
}
