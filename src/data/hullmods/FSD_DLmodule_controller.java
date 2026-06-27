package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class FSD_DLmodule_controller extends BaseHullMod {
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(ship.isStationModule()) {
            if (ship.getParentStation() != null) {
                float facingRate = getFacing(ship.getParentStation());
                ShipAPI parentStation = ship.getParentStation();
                ship.setFacing(parentStation.getFacing() - 10f * facingRate);
            }
        }
    }


  private float getFacing(ShipAPI parent) {
    return parent.getAngularVelocity() / parent.getMaxTurnRate();
  }
}
