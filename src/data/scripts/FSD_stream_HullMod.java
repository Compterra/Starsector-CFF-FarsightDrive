package data.scripts;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.combat.FSD_Longinus_AmmoReloadListener;

public class FSD_stream_HullMod extends BaseHullMod {
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(FSD_Longinus_AmmoReloadListener.class)) {
            ship.addListener(new FSD_Longinus_AmmoReloadListener(ship));
        }
    }
    
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}

