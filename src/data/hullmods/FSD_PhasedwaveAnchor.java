package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;

public class FSD_PhasedwaveAnchor extends BaseHullMod {
  public static float BURN_BONUS = 1;
  public static final float SENSOR_PROFILE = 200f;

  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (stats.getVariant().hasHullMod("augmentedengines")) BURN_BONUS = 2;
    stats.getDynamic().getMod(Stats.FLEET_BURN_BONUS).modifyFlat(id, BURN_BONUS);
    stats.getSensorProfile().modifyFlat(id, SENSOR_PROFILE);
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
    tooltip.addPara(
        "Stabilizes the fleet drive bubble in hyperspace, increasing fleet burn by %s.\nMultiple stabilizers stack. If the ship also has %s, fleet burn is increased by an additional %s.",
        pads, Misc.getTextColor(), y, "1", "Enhanced Drive Field", "1");
  }
}
