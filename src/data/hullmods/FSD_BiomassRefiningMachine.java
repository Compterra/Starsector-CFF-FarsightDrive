package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.HashMap;

public class FSD_BiomassRefiningMachine extends BaseHullMod {
  private static HashMap<String, IntervalUtil> FSD_Missile_intervals =
      new HashMap<String, IntervalUtil>();

  static {
    FSD_Missile_intervals.put("FSD_Decay", new IntervalUtil(30f, 30f));
    FSD_Missile_intervals.put("FSD_Weepingblood", new IntervalUtil(20f, 20f));
    FSD_Missile_intervals.put("FSD_Proliferation", new IntervalUtil(30f, 30f));
    FSD_Missile_intervals.put("FSD_CorruptSea", new IntervalUtil(20f, 20f));
    FSD_Missile_intervals.put("FSD_Cruise", new IntervalUtil(10f, 10f));
  }

  private HashMap<Integer, HashMap<WeaponAPI, Boolean>> FSD_Missile_start =
      new HashMap<Integer, HashMap<WeaponAPI, Boolean>>();
  private int FSD_Missile_count = 0;

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getSpec().getTags().contains("FSD_Missile")) {
        if (ship.getCaptain() == null) {
          weapon.setMaxAmmo(weapon.getSpec().getMaxAmmo());
        }
        if (ship.getCaptain() != null
            && ship.getCaptain().getStats().hasSkill("missile_specialization")
            && ship.getVariant().getHullMods().contains("missleracks")) {
          weapon.setMaxAmmo(weapon.getSpec().getMaxAmmo() * 2);
        }
        if (ship.getCaptain() != null
            && ship.getVariant().getHullMods().contains("missleracks")
            && !ship.getCaptain().getStats().hasSkill("missile_specialization")) {
          weapon.setMaxAmmo(weapon.getSpec().getMaxAmmo());
        }
      }
    }
  }

  public void advanceInCombat(ShipAPI ship, float amount) {
    if (!ship.getCustomData().containsKey("FSD_Missile_timers")) {
      for (WeaponAPI weapon : ship.getAllWeapons()) {
        String weaponId = weapon.getId();
        if (FSD_Missile_intervals.containsKey(weaponId)) {
          float maxInterval = FSD_Missile_intervals.get(weaponId).getMaxInterval();
          weapon.getAmmoTracker().setAmmoPerSecond(1 / maxInterval);
        }
      }
      ship.setCustomData("FSD_Missile_timers", false);
    }
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
    Color y = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    Color g = Misc.getPositiveHighlightColor();
    tooltip.addPara("Allows Farsight Drive missile systems to recover ammunition. Recovery timing is shown %s.", pads, Misc.getTextColor(), y, "after compatible weapons are installed");
    tooltip.addPara("Farsight Drive missile systems cannot increase ammunition capacity through %s.", pads, Misc.getTextColor(), r, "Expanded Missile Racks");
    tooltip.addSectionHeading("Affected Weapons", Alignment.MID, pad);
    float col1 = 180f;
    float col2 = 180f;
    tooltip.beginTable(
        Misc.getBasePlayerColor(),
        Misc.getDarkPlayerColor(),
        Misc.getBrightPlayerColor(),
        20f,
        true,
        true,
        new Object[] {"Weapon Name", col1, "Recovery Time", col2});
    HashMap<String, String> FSD_Missile_desc = new HashMap<String, String>();
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      for (String key : FSD_Missile_intervals.keySet()) {
        if (weapon.getId().equals(key)) {
          FSD_Missile_desc.put(weapon.getId(), weapon.getDisplayName());
        }
      }
    }
    for (String key : FSD_Missile_desc.keySet()) {
      tooltip.addRow(
          Alignment.MID,
          y,
          FSD_Missile_desc.get(key),
          Alignment.MID,
          g,
          "" + FSD_Missile_intervals.get(key).getMaxInterval() + "s/shot");
    }
    tooltip.addTable("", 0, pad);
  }
}
