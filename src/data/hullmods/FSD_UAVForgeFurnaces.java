package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;

public class FSD_UAVForgeFurnaces extends BaseHullMod {

  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (isSMod(stats)) {
      stats.getFighterRefitTimeMult().modifyMult(id + "_smod", 0.9f);
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    MutableShipStatsAPI stats = ship.getMutableStats();
    String id = ship.getId() + "_FSD_UAVForgeFurnaces";
    if(ship.getVariant().hasHullMod("FSD_WingReplace")){
        stats.getFighterRefitTimeMult().modifyMult(id, 0.85f);
    }
    boolean hasKarmaSystem = ship.getVariant().hasHullMod("FSD_ReflectLight") 
        || ship.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion");
    if(hasKarmaSystem) {
        ExtraDeploy(ship);
    }
    if(!ship.getVariant().hasHullMod("FSD_UAVForge") && !hasKarmaSystem) {
        stats.getFighterRefitTimeMult().modifyMult(id, 0.9f);
    }

//      FighterDebuff(ship);

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
    tooltip.addPara("A general-purpose fighter preparation system sold by Farsight Drive, with additional tuning options for FSD-pattern craft.", pads, Misc.getTextColor(), r, "");
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara(
        "All fighter replacement time reduced by %s.\n  *If %s is also installed, replacement time is reduced by an additional %s.\n  *When installed on a Reflecting-Light Crystal ship, allows deployment of %s additional fighter.",
        pads, Misc.getTextColor(), y, "10%","Crystal Forge Conversion", "5%", "1");
//    tooltip.addPara(
//        pads, Misc.getTextColor(), r, "30%", "20%", "50%");
  }

  public void ExtraDeploy(ShipAPI ship) {
      String ID = ship.getId() + "_FSD_UAVForgeFurnaces";
      String TAG = "FSD_Wing";
      if(ship.getOriginalOwner()==-1){
          return; //supress in refit
      }

      boolean allDeployed=true, ranOnce=false;

      for(FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
          if (bay.getWing() != null) {
              ranOnce = true;
              if (bay.getWing().getSpec().hasTag(TAG)) {

                  FighterWingSpecAPI wingSpec = bay.getWing().getSpec();
                  int deployed = bay.getWing().getWingMembers().size();
                  int maxTotal = wingSpec.getNumFighters() + 1;
                  int actualAdd = maxTotal - deployed;

                  if (actualAdd > 0) {
                      bay.setExtraDeployments(actualAdd);
                      bay.setExtraDeploymentLimit(maxTotal);
                      bay.setExtraDuration(9999999);
                      allDeployed = false;
                  } else {
                      bay.setExtraDeployments(0);
                      bay.setExtraDeploymentLimit(0);
                      bay.setFastReplacements(0);
                  }

                  if (ship.getMutableStats().getFighterRefitTimeMult().getPercentStatMod(ID) == null && actualAdd != 0) {
                      //instantly add all the required fighters upon deployment
                      bay.setFastReplacements(actualAdd);
                  }

                  //debug
//                    Global.getCombatEngine().addFloatingText(
//                            bay.getWeaponSlot().computePosition(ship),
//                            "add= "+bay.getExtraDeployments()+" max= "+bay.getExtraDeploymentLimit()+" fast= "+bay.getFastReplacements(),
//                            10, Color.ORANGE, ship, 1, 1);
              }
              if (ship.getMutableStats().getFighterRefitTimeMult().getPercentStatMod(ID)==null && allDeployed && ranOnce){
                  //used as a check to add all the extra fighters upon deployment
                  ship.getMutableStats().getFighterRefitTimeMult().modifyPercent(ID, 1);
              }
          }
      }
  }

//  public void FighterDebuff(ShipAPI ship) {
//    for (FighterWingAPI wingAPI : ship.getAllWings()) {
//      for (ShipAPI wingMember : wingAPI.getWingMembers()) {
//        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
//          if (bay.getWing() == null) continue;
//          if (wingMember.getHullSpec().getMinCrew() > 0) {
//            if (!range) {
//              rangeMod = wingAPI.getRange();
//              range = true;
//            }
//            wingMember.getWing().getSource();
//            int total = wingAPI.getSpec().getNumFighters();
//            int actual = total - wingAPI.getSpec().getNumFighters();
//            wingMember.getWing().getSource().setExtraDeploymentLimit(total);
//            wingMember.getWing().getSource().setExtraDeployments(0);
//            wingMember.getWing().getSource().setExtraDuration(9999f);
//            wingAPI.getSpec().setRange(rangeMod * 0.7f);
//            wingMember
//                .getMutableStats()
//                .getHullDamageTakenMult()
//                .modifyMult(wingMember.getId(), 1.2f);
//            wingMember.getMutableStats().getEmpDamageTakenMult().modifyMult(wingMember.getId(), 2f);
//          }
//        }
//      }
//    }
//  }
}
