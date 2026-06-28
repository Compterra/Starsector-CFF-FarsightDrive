package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FSD_HarmonicFluxWheel extends BaseHullMod {
  private static final float LARGE_WEAPON_RANGE_BONUS = 200f;
  private static final float SMOD_LARGE_WEAPON_RANGE_BONUS = 250f;
  private static final float HIGH_FLUX_THRESHOLD = 0.67f;
  private static final float LOW_FLUX_THRESHOLD = 0.33f;
  private static final float DAMAGE_FOR_KARMA = 1000f;
  private static final float ENERGY_SOFT_FLUX_PERCENT = 0.5f;
  private static final float SMOD_ENERGY_SOFT_FLUX_PERCENT = 0.35f;
  private static final float SMOD_KARMA_COST_MULT = 1.5f;
  private static final float SMOD_RETURN_RATE_MULT = 1.25f;
  private static final float SMOD_VENT_MANEUVERABILITY_BONUS = 1.0f;
  private static final float SMOD_SPEED_BONUS_MULTIPLIER = 1.0f;
  private static final Map<String, Float> RETURN_RATE = new HashMap<>();

  static {
    RETURN_RATE.put("LOW", 300f);
    RETURN_RATE.put("MEDIUM", 200f);
    RETURN_RATE.put("HIGH", 100f);
  }

  private static final Map<String, Float> KARMA_COST = new HashMap<>();

  static {
    KARMA_COST.put("LOW", 0.02f);
    KARMA_COST.put("MEDIUM", 0.01f);
    KARMA_COST.put("HIGH", 0.005f);
  }

  private static final float VENT_MANEUVERABILITY_BONUS = 2.0f;
  private static final float SPEED_BONUS_MULTIPLIER = 2.0f;

  @Override
  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (!ship.getCustomData().containsKey("FSD_HarmonicFluxWheel_Karma")) {
      ship.setCustomData("FSD_HarmonicFluxWheel_Karma", 1.0f);
    }
    if (!ship.getCustomData().containsKey("FSD_HarmonicFluxWheel_Buffer")) {
      ship.setCustomData("FSD_HarmonicFluxWheel_Buffer", 0f);
    }
    ship.setCustomData("FSD_HarmonicFluxWheel_SMod", isSMod(ship));
    if (!ship.hasListenerOfClass(HarmonicListener.class)) {
      ship.addListener(new HarmonicListener(ship));
    }
    if (!ship.hasListenerOfClass(HarmonicRangeModifier.class)) {
      ship.addListener(new HarmonicRangeModifier());
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {}

  @Override
  public boolean isSModEffectAPenalty() {
    return true;
  }

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
    Color highlight = Misc.getHighlightColor();
    tooltip.addPara("One of the Coaxial-class cores, allegedly based on an Eridani power with extremely advanced flux-control technology.", pads);
    tooltip.addPara("All large energy weapons gain %s base range and maximum firing accuracy.", pad, highlight, "200");
    tooltip.addPara(
        "While ship flux is below %s, external damage dealt accumulates extra karma: every %s damage provides %s karma.", pad, highlight, "67%", "1000", "1%");
    tooltip.addPara(
        "Soft flux from energy weapons is partially delayed: %s is applied immediately, while the remaining %s enters a buffer equal to the ship's base flux capacity and returns as hard flux by consuming karma.",
        pad, highlight, "50%", "50%");
    tooltip.addPara("Flux level, karma cost, and return rate:", pad);
    tooltip.beginTable(Color.GRAY, Color.BLACK, Color.BLACK, 20f, "Flux level", "Karma cost per 100 flux", "Return rate");
    tooltip.addRow(highlight, "Flux level < 33%", "2%", "300 points/sec");
    tooltip.addRow(highlight, "33% <= flux level < 67%", "1%", "200 points/sec");
    tooltip.addRow(highlight, "Flux level >= 67%", "0.5%", "100 points/sec");
    tooltip.addTable("", 0, pad);
    tooltip.addPara(
        "When the ship vents, it gains %s maneuverability. Buffered flux is cleared immediately, and top speed is boosted by total buffered flux: (buffered flux / max flux capacity) * top speed *"
            + " %s",
        pad, highlight, "200%", "2");
  }

  public static class HarmonicRangeModifier implements WeaponRangeModifier {
    @Override
    public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
      return 0;
    }

    @Override
    public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
      return 1f;
    }

    @Override
    public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
      if (weapon.getSize() == WeaponSize.LARGE) {
        Object sMod = ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod");
        return Boolean.TRUE.equals(sMod) ? SMOD_LARGE_WEAPON_RANGE_BONUS : LARGE_WEAPON_RANGE_BONUS;
      }
      return 0f;
    }
  }

  public static class HarmonicListener implements AdvanceableListener, DamageListener {
    private ShipAPI ship;
    private float lastKarma = 1.0f;
    private float damageAccumulated = 0f;
    private final String KARMA_KEY = "FSD_HarmonicFluxWheel_Karma";
    private final String BUFFER_KEY = "FSD_HarmonicFluxWheel_Buffer";
    private boolean isVenting = false;
    private float originalManeuverability = 0f;
    private float originalMaxSpeed = 0f;
    private IntervalUtil checkTimer = new IntervalUtil(0.1f, 0.1f);

    public HarmonicListener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public void advance(float amount) {
      if (!ship.isAlive() || Global.getCombatEngine() == null) return;
      if (!ship.getCustomData().containsKey(KARMA_KEY)) {
        ship.setCustomData(KARMA_KEY, 1.0f);
      }
      if (!ship.getCustomData().containsKey(BUFFER_KEY)) {
        ship.setCustomData(BUFFER_KEY, 0f);
      }
      float karma = (float) ship.getCustomData().get(KARMA_KEY);
      float bufferFlux = (float) ship.getCustomData().get(BUFFER_KEY);
      float fluxLevel = ship.getFluxLevel();
      float maxFlux = ship.getMaxFlux();
      float currentFlux = ship.getCurrFlux();
      if (bufferFlux > 0 && karma > 0) {
        String fluxState = getFluxState(fluxLevel);
        boolean sMod = Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod"));
        float returnRate = RETURN_RATE.get(fluxState) * (sMod ? SMOD_RETURN_RATE_MULT : 1f) * amount;
        float karmaCost = (returnRate / 100f) * KARMA_COST.get(fluxState) * (sMod ? SMOD_KARMA_COST_MULT : 1f);
        if (karma >= karmaCost) {
          float maxReturnPossible = Math.min(returnRate, bufferFlux);
          maxReturnPossible = Math.min(maxReturnPossible, maxFlux - currentFlux);
          if (maxReturnPossible > 0) {
            karma -= (maxReturnPossible / 100f) * KARMA_COST.get(fluxState);
            bufferFlux -= maxReturnPossible;
            ship.getFluxTracker().increaseFlux(maxReturnPossible, true);
          }
        }
      }
      boolean nowVenting = ship.getFluxTracker().isVenting();
      if (nowVenting != isVenting) {
        if (nowVenting) {
          originalManeuverability = ship.getMutableStats().getAcceleration().getBaseValue();
          originalMaxSpeed = ship.getMutableStats().getMaxSpeed().getBaseValue();
          ship.getMutableStats()
              .getAcceleration()
              .modifyMult(KARMA_KEY, 1f + (Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod")) ? SMOD_VENT_MANEUVERABILITY_BONUS : VENT_MANEUVERABILITY_BONUS));
          ship.getMutableStats()
              .getDeceleration()
              .modifyMult(KARMA_KEY, 1f + (Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod")) ? SMOD_VENT_MANEUVERABILITY_BONUS : VENT_MANEUVERABILITY_BONUS));
          ship.getMutableStats()
              .getTurnAcceleration()
              .modifyMult(KARMA_KEY, 1f + (Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod")) ? SMOD_VENT_MANEUVERABILITY_BONUS : VENT_MANEUVERABILITY_BONUS));
          ship.getMutableStats()
              .getMaxTurnRate()
              .modifyMult(KARMA_KEY, 1f + (Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod")) ? SMOD_VENT_MANEUVERABILITY_BONUS : VENT_MANEUVERABILITY_BONUS));
          float speedBonus = (bufferFlux / maxFlux) * originalMaxSpeed * (Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod")) ? SMOD_SPEED_BONUS_MULTIPLIER : SPEED_BONUS_MULTIPLIER);
          ship.getMutableStats().getMaxSpeed().modifyFlat(KARMA_KEY, speedBonus);
          bufferFlux = 0f;
        } else {
          ship.getMutableStats().getAcceleration().unmodify(KARMA_KEY);
          ship.getMutableStats().getDeceleration().unmodify(KARMA_KEY);
          ship.getMutableStats().getTurnAcceleration().unmodify(KARMA_KEY);
          ship.getMutableStats().getMaxTurnRate().unmodify(KARMA_KEY);
          ship.getMutableStats().getMaxSpeed().unmodify(KARMA_KEY);
        }
        isVenting = nowVenting;
      }
      if (checkTimer.intervalElapsed()) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
          if (weapon.getSize() == WeaponSize.LARGE) {
            String weaponId = weapon.getId() + "_" + KARMA_KEY;
            ship.getMutableStats().getWeaponTurnRateBonus().modifyPercent(weaponId, 50f);
          }
        }
      }
      ship.setCustomData(KARMA_KEY, karma);
      ship.setCustomData(BUFFER_KEY, bufferFlux);
    }

    private String getFluxState(float fluxLevel) {
      if (fluxLevel < LOW_FLUX_THRESHOLD) return "LOW";
      else if (fluxLevel < HIGH_FLUX_THRESHOLD) return "MEDIUM";
      else return "HIGH";
    }

    @Override
    public void reportDamageApplied(
        Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
      if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI)) return;
      ShipAPI sourceShip = (ShipAPI) source;
      if (sourceShip != ship) return;
      ShipAPI targetShip = (ShipAPI) target;
      if (targetShip.getOwner() == ship.getOwner()) return;
      float karma = (float) ship.getCustomData().get(KARMA_KEY);
      float fluxLevel = ship.getFluxLevel();
      if (fluxLevel < HIGH_FLUX_THRESHOLD) {
        float damage =
            result.getTotalDamageToArmor() + result.getDamageToHull() + result.getDamageToShields();
        damageAccumulated += damage;
        if (damageAccumulated >= DAMAGE_FOR_KARMA) {
          float karmaGain = (damageAccumulated / DAMAGE_FOR_KARMA) * 0.01f;
          karma = Math.min(1.0f, karma + karmaGain);
          damageAccumulated = damageAccumulated % DAMAGE_FOR_KARMA;
          ship.setCustomData(KARMA_KEY, karma);
        }
      }
      float shieldDamage = result.getDamageToShields();
      if (shieldDamage > 0) {
        float bufferFlux = (float) ship.getCustomData().get(BUFFER_KEY);
        float maxFlux = ship.getMaxFlux();
        float softFluxGenerated = shieldDamage * 1.5f;
        if (softFluxGenerated > 0) {
          boolean sMod = Boolean.TRUE.equals(ship.getCustomData().get("FSD_HarmonicFluxWheel_SMod"));
          float immediateFluxPercent = sMod ? SMOD_ENERGY_SOFT_FLUX_PERCENT : ENERGY_SOFT_FLUX_PERCENT;
          float fluxToBuffer = softFluxGenerated * (1f - immediateFluxPercent);
          ship.getFluxTracker().decreaseFlux(fluxToBuffer);
          bufferFlux = Math.min(bufferFlux + fluxToBuffer, maxFlux);
          ship.setCustomData(BUFFER_KEY, bufferFlux);
        }
      }
    }
  }
}
