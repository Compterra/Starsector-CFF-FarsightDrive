package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FSD_WingReplaceH extends BaseHullMod {
  private static final Map<String, String> reset = new HashMap<>();

  static {
    reset.put("FSD_broadsword_wing", "broadsword_wing");
    reset.put("FSD_xyphos_wing", "xyphos_wing");
    reset.put("FSD_sarissa_wing", "sarissa_wing");
    reset.put("FSD_thunder_wing", "thunder_wing");
    reset.put("FSD_claw_wing", "claw_wing");
    reset.put("FSD_talon_wing", "talon_wing");
    reset.put("FSD_wasp_wing", "wasp_wing");
    reset.put("FSD_hoplon_wing", "hoplon_wing");
    reset.put("FSD_dagger_wing", "dagger_wing");
    reset.put("FSD_piranha_wing", "piranha_wing");
    reset.put("FSD_trident_wing", "trident_wing");
    reset.put("FSD_perdition_wing", "perdition_wing");
    reset.put("FSD_cobra_wing", "cobra_wing");
    reset.put("FSD_longbow_wing", "longbow_wing");
    reset.put("FSD_mining_drone_wing", "mining_drone_wing");
    reset.put("FSD_spark_wing", "spark_wing");
    reset.put("FSD_lux_wing", "lux_wing");
  }

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (!ship.getVariant().hasHullMod("FSD_WingReplace")) {
      for (int i = 0; i < ship.getVariant().getWings().size(); i++) {
        if (reset.containsKey(ship.getVariant().getWingId(i))) {
          ship.getVariant().setWingId(i, reset.get(ship.getVariant().getWingId(i)));
        }
      }
      ship.getVariant().removeMod("FSD_WingReplaceH");
    }
    detect(ship, id);
  }

  public void detect(ShipAPI ship, String id) {
    if (ship.getOriginalOwner() < 0) {
      if (Global.getSector() != null
          && Global.getSector().getPlayerFleet() != null
          && Global.getSector().getPlayerFleet().getCargo() != null
          && Global.getSector().getPlayerFleet().getCargo().getStacksCopy() != null
          && !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()) {
        for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
          if (s.isFighterWingStack()) {
            FighterWingSpecAPI wingSpec = s.getFighterWingSpecIfWing();
            if (Objects.equals(reset.get(wingSpec.getId()), wingSpec.getId())) continue;
            if (reset.containsKey(wingSpec.getId())) {
              Global.getSector().getPlayerFleet().getCargo().removeStack(s);
              Global.getSector()
                  .getPlayerFleet()
                  .getCargo()
                  .addFighters(reset.get(wingSpec.getId()), (int) s.getSize());
            }
          }
        }
      }
      if (Global.getSector() != null
          && Global.getSector().getPlayerFleet() != null
          && Global.getSector().getPlayerFleet().getMarket() != null
          && Global.getSector().getPlayerFleet().getMarket().getSubmarketsCopy() != null
          && !Global.getSector().getPlayerFleet().getMarket().getSubmarketsCopy().isEmpty()) {
        for (SubmarketAPI s : Global.getSector().getPlayerFleet().getMarket().getSubmarketsCopy()) {
          if (s.getCargo() != null) {
            for (CargoStackAPI c : s.getCargo().getStacksCopy()) {
              if (c.isFighterWingStack()) {
                FighterWingSpecAPI wingSpec = c.getFighterWingSpecIfWing();
                if (Objects.equals(reset.get(wingSpec.getId()), wingSpec.getId())) continue;
                if (reset.containsKey(wingSpec.getId())) {
                  Global.getSector().getPlayerFleet().getCargo().removeStack(c);
                  Global.getSector()
                      .getPlayerFleet()
                      .getCargo()
                      .addFighters(reset.get(wingSpec.getId()), (int) c.getSize());
                }
              }
            }
          }
        }
      }
      if (Global.getSector() != null
          && Global.getSector().getPlayerFleet() != null
          && Global.getSector().getPlayerFleet().getInteractionTarget() != null
          && Global.getSector().getPlayerFleet().getInteractionTarget().getMarket() != null
          && Global.getSector()
                  .getPlayerFleet()
                  .getInteractionTarget()
                  .getMarket()
                  .getSubmarketsCopy()
              != null
          && !Global.getSector()
              .getPlayerFleet()
              .getInteractionTarget()
              .getMarket()
              .getSubmarketsCopy()
              .isEmpty()) {
        for (SubmarketAPI s :
            Global.getSector()
                .getPlayerFleet()
                .getInteractionTarget()
                .getMarket()
                .getSubmarketsCopy()) {
          if (s.getCargo() != null) {
            for (CargoStackAPI c : s.getCargo().getStacksCopy()) {
              if (c.isFighterWingStack()) {
                FighterWingSpecAPI wingSpec = c.getFighterWingSpecIfWing();
                if (Objects.equals(reset.get(wingSpec.getId()), wingSpec.getId())) continue;
                if (reset.containsKey(wingSpec.getId())) {
                  Global.getSector().getPlayerFleet().getCargo().removeStack(c);
                  Global.getSector()
                      .getPlayerFleet()
                      .getCargo()
                      .addFighters(reset.get(wingSpec.getId()), (int) c.getSize());
                }
              }
            }
          }
        }
      }
      if (Global.getSector() != null
          && Global.getSector().getPlayerFleet() != null
          && Global.getSector().getPlayerFleet().getContainingLocation() != null
          && Global.getSector().getPlayerFleet().getContainingLocation().getPlanets() != null
          && !Global.getSector().getPlayerFleet().getContainingLocation().getPlanets().isEmpty()) {
        for (PlanetAPI s :
            Global.getSector().getPlayerFleet().getContainingLocation().getPlanets()) {
          if (s != null
              && s.getMarket() != null
              && s.getMarket().getSubmarketsCopy() != null
              && !s.getMarket().getSubmarketsCopy().isEmpty()) {
            for (SubmarketAPI m : s.getMarket().getSubmarketsCopy()) {
              if (m.getCargo() != null) {
                for (CargoStackAPI c : m.getCargo().getStacksCopy()) {
                  if (c.isFighterWingStack()) {
                    FighterWingSpecAPI wingSpec = c.getFighterWingSpecIfWing();
                    if (Objects.equals(reset.get(wingSpec.getId()), wingSpec.getId())) continue;
                    if (reset.containsKey(wingSpec.getId())) {
                      Global.getSector().getPlayerFleet().getCargo().removeStack(c);
                      Global.getSector()
                          .getPlayerFleet()
                          .getCargo()
                          .addFighters(reset.get(wingSpec.getId()), (int) c.getSize());
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (Global.getSector() != null
          && Global.getSector().getPlayerFleet() != null
          && Global.getSector().getPlayerFleet().getContainingLocation() != null
          && Global.getSector().getPlayerFleet().getContainingLocation().getCustomEntities() != null
          && !Global.getSector()
              .getPlayerFleet()
              .getContainingLocation()
              .getCustomEntities()
              .isEmpty()) {
        for (CustomCampaignEntityAPI s :
            Global.getSector().getPlayerFleet().getContainingLocation().getCustomEntities()) {
          if (s != null
              && s.getMarket() != null
              && s.getMarket().getSubmarketsCopy() != null
              && !s.getMarket().getSubmarketsCopy().isEmpty()) {
            for (SubmarketAPI m : s.getMarket().getSubmarketsCopy()) {
              if (m.getCargo() != null) {
                for (CargoStackAPI c : m.getCargo().getStacksCopy()) {
                  if (c.isFighterWingStack()) {
                    FighterWingSpecAPI wingSpec = c.getFighterWingSpecIfWing();
                    if (Objects.equals(reset.get(wingSpec.getId()), wingSpec.getId())) continue;
                    String wingHull = wingSpec.getVariant().getHullSpec().getBaseHullId();
                    if (reset.containsKey(wingHull)) {
                      Global.getSector().getPlayerFleet().getCargo().removeStack(c);
                      Global.getSector()
                          .getPlayerFleet()
                          .getCargo()
                          .addFighters(reset.get(wingSpec.getId()), (int) c.getSize());
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}
}
