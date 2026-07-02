package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FSD_OverrunPosition extends BaseHullMod {
  private static Map<ShipAPI.HullSize, Float> HullSizeBonus =
      new HashMap<ShipAPI.HullSize, Float>();

  static {
    HullSizeBonus.put(ShipAPI.HullSize.FRIGATE, 1.1f);
    HullSizeBonus.put(ShipAPI.HullSize.DESTROYER, 1.2f);
    HullSizeBonus.put(ShipAPI.HullSize.CRUISER, 1.4f);
    HullSizeBonus.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.6f);
  }

  @Override
  public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (isSMod(stats)) {
      stats.getWeaponTurnRateBonus().modifyPercent(id + "_smod", 15f);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (!ship.hasListener(OverrunPosition_Listener.class)) {
      ship.addListener(new OverrunPosition_Listener());
    }
//    if (ship.getVariant().hasHullMod("dedicated_targeting_core")) {
//      ship.getVariant().removeMod("dedicated_targeting_core");
//    }
//    if (ship.getVariant().hasHullMod("targetingunit")) {
//      ship.getVariant().removeMod("targetingunit");
//    }
//    if (ship.getVariant().hasHullMod("advancedcore")) {
//      ship.getVariant().removeMod("advancedcore");
//    }
  }

  @Override
  public boolean shouldAddDescriptionToTooltip(
      ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip,
      ShipAPI.HullSize hullSize,
      ShipAPI ship,
      float width,
      boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    Color y = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    tooltip.addPara("Performs basic crystal guidance, letting the hull alter weapon-slot structures for greater range.", pads);
//    tooltip.addSectionHeading("Benefits", Alignment.MID, pads);
//    tooltip.addPara("All energy weapons gain %s base range;\n", pads, Misc.getTextColor(), y, "100");
//    tooltip.addPara(
//        "Further increases base range according to energy weapon flux cost;\nthe increase is the absolute value of %s, rounded up,\nup to %s base range through this method;\n",
//        pads, Misc.getTextColor(), y, "50*(1.0-current flux/damage ratio)", "200");
    tooltip.addPara(
        "During combat, range increases with karma: every %s accumulated karma increases base range by %s.",
        pads, Misc.getTextColor(), y, "1%", "2");

  }

  public void advanceInCombat(ShipAPI ship, float amount) {
    MutableShipStatsAPI stats = ship.getMutableStats();
    ShipAPI.HullSize hullSize = ship.getHullSize();
    String id = ship.getId() + "_FSD_OverrunPosition";
    float mult = (Float) HullSizeBonus.get(hullSize);
//    stats.getEnergyWeaponRangeBonus().modifyMult(id, mult);
//    stats.getVentRateMult().modifyMult(ship.getId(), 0.7f);
//    stats.getBallisticRoFMult().modifyMult(ship.getId(), 0.25f);
//    stats.getEnergyRoFMult().modifyMult(ship.getId(), 0.7f);
//    stats.getMissileRoFMult().modifyMult(ship.getId(), 0.7f);
//    stats.getMaxSpeed().modifyMult(id, 0.75f);
  }
  @Override
  public boolean isSModEffectAPenalty() {
    return true;
  }


  /**
   */
  private boolean hasKarmaSystem(ShipAPI ship) {
    return ship.getVariant().hasHullMod("FSD_ReflectLight") 
        || ship.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion");
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
//    if (ship.getVariant().hasHullMod("dedicated_targeting_core")) return false;
//    if (ship.getVariant().hasHullMod("targetingunit")) return false;
//    if (ship.getVariant().hasHullMod("advancedcore")) return false;
    if (!hasKarmaSystem(ship)) return false;
    return true;
  }

  public String getUnapplicableReason(ShipAPI ship) {
//    if (ship.getVariant().hasHullMod("dedicated_targeting_core")
//        || ship.getVariant().hasHullMod("targetingunit")
//        || ship.getVariant().hasHullMod("advancedcore")) {
//      return "Incompatible with other targeting systems!";
//    }
    if (!hasKarmaSystem(ship)) {
      return "Requires a ship with Reflecting-Light Crystal or Secondary Crystal Overgrowth!";
    }
    return null;
  }

  public static class OverrunPosition_Listener implements WeaponBaseRangeModifier {
    @Override
    public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
      return 0;
    }

    @Override
    public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
      return 1f;
    }

    @Override
    public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
      float bonus = 0;
      if (weapon.getSpec() == null) {
        return 0f;
      }
//      if (weapon.getSpec().getMountType() != WeaponAPI.WeaponType.ENERGY) {
//        return 0f;
//      }
//      if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.ENERGY) {
//        bonus = 100f;
//        float fluxRatio = weapon.getDamage().getDamage() / weapon.getFluxCostToFire();
//        if (fluxRatio <= 1f) {
//          bonus += Math.min((1f - fluxRatio) * 50f, 200f);
//        } else if (fluxRatio >= 1f) {
//          bonus += Math.min((fluxRatio - 1f) * 50f, 200f);
//        }

//      }
      if (KarmaAPI.getKarmaData(ship) != null) {
          float karma = KarmaAPI.getKarma(ship);
          boolean sMod = Boolean.TRUE.equals(ship.getCustomData().get("FSD_OverrunPosition_SMod"));
          bonus += (karma * (sMod ? 250f : 200f));
      }
      return bonus;
    }
  }
}
