package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import java.util.*;

public class FSD_XC_engine extends BaseHullMod {
  private float WeaponFeed1 = 0.01f;
  private float WeaponFeed2 = 0.02f;
  public static final float GROUND_BONUS = 350;
  public static final float SMOD_ZERO_FLUX_BOOST = 10f;
  public static final float SMOD_WEAPON_FEED_MULT = 1.25f;

  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    stats.getDynamic().getMod(Stats.FLEET_GROUND_SUPPORT).modifyFlat(id, GROUND_BONUS);
    if (isSMod(stats)) {
      stats.getZeroFluxSpeedBoost().modifyFlat(id + "_smod", SMOD_ZERO_FLUX_BOOST);
    }
  }

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    String id = ship.getId() + "_FSD_XC_engine";
    MutableShipStatsAPI stats = ship.getMutableStats();
    float sModMult = isSMod(ship) ? SMOD_WEAPON_FEED_MULT : 1f;
    if (ship.getSystem().isActive()) {
      stats
          .getBallisticRoFMult()
          .modifyMult(id, 1f + WeaponFeed2 * sModMult * (ship.getVelocity().length()) / 15);
      stats
          .getEnergyRoFMult()
          .modifyMult(id, 1f + WeaponFeed2 * sModMult * (ship.getVelocity().length()) / 15);
    } else {
      stats
          .getBallisticRoFMult()
          .modifyMult(id, 1f + WeaponFeed1 * sModMult * (ship.getVelocity().length()) / 15);
      stats
          .getEnergyRoFMult()
          .modifyMult(id, 1f + WeaponFeed1 * sModMult * (ship.getVelocity().length()) / 15);
    }
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
    tooltip.addPara(
        "Based on current speed, every %s speed increases weapon rate of fire by %s.\nWhile the ship system is active, every %s speed increases weapon rate of fire by %s.\n",
        pads, Misc.getTextColor(), y, "15", "1%", "10", "2%");
    tooltip.addPara("Increases ground raid strength by %s; stacks with other sources.", pads, Misc.getTextColor(), y, "350");
    tooltip.addPara("S-mod: tuned phasewave governors add %s zero-flux speed and increase speed-fed weapon cycling by %s.", pads, Misc.getTextColor(), y, "+10", "25%");
  }
}
