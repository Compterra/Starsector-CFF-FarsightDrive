package data.hullmods.FSD_LSW;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.awt.*;

public class FSD_LSweapon extends BaseHullMod {
  public static final Color color_1 = new Color(255, 50, 50, 203);
  static final String weapon_M = "FSD_LSShaftcannon_missile";
  static final String weapon_C = "FSD_LSShaftcannon";
  static final String weapon_B = "FSD_LSShaftcannon_beam";
  static final String hullmod_M = "FSD_LSShaftcannon_missile_hullmod";
  static final String hullmod_C = "FSD_LSShaftcannon_hullmod";
  static final String hullmod_B = "FSD_LSShaftcannon_beam_hullmod";
  static final String slotid = "FSD_LSW";

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {}

  @Override
  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    ShipVariantAPI v = stats.getVariant();
    if (v.getHullMods().contains(hullmod_M)
        || v.getHullMods().contains(hullmod_C)
        || v.getHullMods().contains(hullmod_B)) return;
    if (v.getWeaponId(slotid) == null) {
      v.addMod(hullmod_M);
      v.addWeapon(slotid, weapon_M);
      return;
    }
    switch (v.getWeaponId(slotid)) {
      case weapon_M:
        {
          v.addMod(hullmod_C);
          v.clearSlot(slotid);
          v.addWeapon(slotid, weapon_C);
          break;
        }
      case weapon_C:
        {
          v.addMod(hullmod_B);
          v.clearSlot(slotid);
          v.addWeapon(slotid, weapon_B);
          break;
        }
      case weapon_B:
        {
          v.addMod(hullmod_M);
          v.clearSlot(slotid);
          v.addWeapon(slotid, weapon_M);
          break;
        }
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

  @Override
  public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
    return null;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip,
      ShipAPI.HullSize hullSize,
      ShipAPI ship,
      float width,
      boolean isForModSpec) {}
}
