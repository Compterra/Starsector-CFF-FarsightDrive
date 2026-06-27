package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * 
 * 
 */
public class FSD_CrystalUnbinding extends BaseHullMod {
  
  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (isSMod(stats)) {
      stats.getSuppliesPerMonth().modifyMult(id + "_smod", 0.95f);
      stats.getSuppliesToRecover().modifyMult(id + "_smod", 0.95f);
    }
  }
  
  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
  }
  
  @Override
  public String getDescriptionParam(int index, HullSize hullSize) {
    return null;
  }
  
  @Override
  public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }
  
  @Override
  public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, 
                                        float width, boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    
    tooltip.addPara(
        "Unbinds the core matrix of Farsight Drive ships using procedures recovered from illegal %s-series blueprints.",
        pad, Misc.getTextColor(), Misc.getHighlightColor(), "[Detuned]");
    
    tooltip.addPara(
        "The readable portions suggest that removing the limiter grants the crystal more... freedom?",
        pads, Misc.getTextColor());
    

    tooltip.setParaSmallInsignia();
    tooltip.addPara(
        "All [Detuned]-series blueprints appear to require this as a prerequisite. We do not fully understand the cost. Shall we try it?",
        0f);
    
    tooltip.addPara(
        "- Technical Director",
        0f)
        .setAlignment(Alignment.RMID);
    
  }
  
  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) {
      return false;
    }
    
    if (!isFarsightDriveShip(ship)) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) {
      return "Requires Reflecting-Light Crystal first!";
    }
    
    if (!isFarsightDriveShip(ship)) {
      return "Can only be installed on Farsight Drive ships!";
    }
    
    return null;
  }
  
  /**
   */
  private boolean isFarsightDriveShip(ShipAPI ship) {
    if (ship == null || ship.getHullSpec() == null) {
      return false;
    }
    
    String manufacturer = ship.getHullSpec().getManufacturer();
    if (manufacturer != null && manufacturer.contains("Farsight Drive")) {
      return true;
    }
    
    String hullId = ship.getHullSpec().getHullId();
    return hullId != null && hullId.startsWith("FSD_");
  }
}

