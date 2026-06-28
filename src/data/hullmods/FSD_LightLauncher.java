package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class FSD_LightLauncher extends BaseHullMod {
  private static boolean isEnabled = false;

  public void Onfire(ShipAPI ship, WeaponAPI weapon, float amount) {
    if (!ship.getCustomData().containsKey("FSD_LLcounter")) {
      ship.setCustomData("FSD_LLcounter", new IntervalUtil(7f, 7f));
    }
    if (ship.getCustomData().containsKey("FSD_LLcounter")) {
      IntervalUtil dur = (IntervalUtil) ship.getCustomData().get("FSD_LLcounter");
      isEnabled = ship.getSystem().isOn();
      if (isEnabled) {
        dur.advance(amount);
      }
      if (dur.intervalElapsed()) {
        isEnabled = false;
        weapon.setForceFireOneFrame(true);
        ship.getCustomData().remove("FSD_LLcounter");
      }
    }
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
    Color c = Misc.getHighlightColor();
    tooltip.addPara(
        "Uses the same principle as the Equilibrium-class carrier's energy recovery device, but miniaturization limits performance.\n" + "When the ship system activates, this device injects energy into the lightbolt exciter.\nAfter %s, stored energy reaches its limit and releases a lightbolt.",
        pads, Misc.getTextColor(), c, "7s");
  }

  public void advanceInCombat(ShipAPI ship, float amount) {
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getType() == WeaponAPI.WeaponType.DECORATIVE) {
        Onfire(ship, weapon, amount);
      }
    }
  }
}
