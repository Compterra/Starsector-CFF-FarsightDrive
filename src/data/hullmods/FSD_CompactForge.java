package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import org.magiclib.util.MagicIncompatibleHullmods;

public class FSD_CompactForge extends BaseHullMod {
  private static final float MISSILE_DAMAGE_REDUCTION = 0.4f;
  private static final float SMOD_MISSILE_DAMAGE_REDUCTION = 0.3f;
  private static final float MISSILE_ROF_BONUS = 0.6f;
  private static final float SMOD_MISSILE_AMMO_REGEN_MULT = 1.1f;
  private static final Set<String> EXCLUDED_MISSILE_IDS = new HashSet<>();

  static {
    EXCLUDED_MISSILE_IDS.add("squall");
    EXCLUDED_MISSILE_IDS.add("hurricane");
    EXCLUDED_MISSILE_IDS.add("annihilator");
    EXCLUDED_MISSILE_IDS.add("pilum");
  }

  private static final Set<String> INCOMPATIBLE_HULLMODS = new HashSet<>();

  static {
    INCOMPATIBLE_HULLMODS.add("missleracks");
    INCOMPATIBLE_HULLMODS.add("eccm");
    INCOMPATIBLE_HULLMODS.add("expanded_deck_crew");
    INCOMPATIBLE_HULLMODS.add("auxiliarythrusters");
  }

  @Override
  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {
    stats.getMissileRoFMult().modifyMult(id, 1f + MISSILE_ROF_BONUS);
    stats.getMissileAmmoRegenMult().modifyMult(id, 1f + MISSILE_ROF_BONUS);
    if (isSMod(stats)) {
      stats.getMissileAmmoRegenMult().modifyMult(id + "_smod", SMOD_MISSILE_AMMO_REGEN_MULT);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    for (String hullmodId : INCOMPATIBLE_HULLMODS) {
      if (ship.getVariant().getHullMods().contains(hullmodId)) {
        MagicIncompatibleHullmods.removeHullmodWithWarning(
            ship.getVariant(), hullmodId, "FSD_CompactForge");
      }
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    if (ship == null || !ship.isAlive()) return;
    String id = ship.getId() + "_FSD_CompactForge";
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getType() == WeaponType.MISSILE) {
        String weaponId = weapon.getId();
        weapon.getDamage().getModifier().unmodify(id);
        if (!EXCLUDED_MISSILE_IDS.contains(weaponId)) {
          float damageReduction = isSMod(ship) ? SMOD_MISSILE_DAMAGE_REDUCTION : MISSILE_DAMAGE_REDUCTION;
          weapon.getDamage().getModifier().modifyMult(id, 1f - damageReduction);
        }
      }
    }
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (ship == null) return false;
    for (String hullmodId : INCOMPATIBLE_HULLMODS) {
      if (ship.getVariant().getHullMods().contains(hullmodId)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship == null) return null;
    for (String hullmodId : INCOMPATIBLE_HULLMODS) {
      if (ship.getVariant().getHullMods().contains(hullmodId)) {
        String hullmodName = Global.getSettings().getHullModSpec(hullmodId).getDisplayName();
        return "Incompatible" + hullmodName;
      }
    }
    return null;
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
    Color negative = Misc.getNegativeHighlightColor();
    tooltip.addPara("The Night Dew-class sacrifices extensive modification space to mount a powerful active forge.", pads);
    tooltip.addSectionHeading("Drawbacks", Alignment.MID, pad);
    tooltip.addPara(
        "1. Missile weapon damage is reduced by %s (does not apply to Short Spear LRM, Proliferation MIRV, Antimatter SRM, or Resonator MRM), but missile reload time is reduced by %s.\n",
        pad, highlight, "40%", "60%");
    tooltip.addPara(
        "2. Incompatible with %s, %s, %s, and %s.", pad, negative, "Expanded Missile Racks", "Electronic Countermeasures", "Converted Hangar", "Auxiliary Thrusters");
  }
}
