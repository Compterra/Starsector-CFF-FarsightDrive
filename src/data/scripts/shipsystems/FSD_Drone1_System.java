package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_Drone1_System extends BaseShipSystemScript {


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        stats.getHullDamageTakenMult().modifyMult(id, 0.5f);
        stats.getFluxDissipation().modifyMult(id, 2f);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify();
        stats.getFluxDissipation().unmodify();
    }
}
