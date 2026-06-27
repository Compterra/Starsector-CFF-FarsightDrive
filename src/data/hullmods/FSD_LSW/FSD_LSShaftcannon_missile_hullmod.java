package data.hullmods.FSD_LSW;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.awt.*;

public class FSD_LSShaftcannon_missile_hullmod extends BaseHullMod {
  public static final Color color_1 = new Color(255, 50, 50, 203);

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {}

  @Override
  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

  @Override
  public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
    return null;
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
    float pad = 3f;
    float opad = 10f;
    tooltip.addPara("Shows the currently installed weapon.", pad);
    tooltip.addSectionHeading("Equipment Status", Alignment.MID, pad);
    tooltip.addPara("Current equipment type: Anti-Air Missile", pad);
  }
}
