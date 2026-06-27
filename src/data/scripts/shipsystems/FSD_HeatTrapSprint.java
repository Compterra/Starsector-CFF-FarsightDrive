package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;


import java.awt.*;


public class FSD_HeatTrapSprint extends BaseShipSystemScript {

    private static final Color COLOR = new Color(26, 118, 255, 255);


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        stats.getMaxTurnRate().modifyMult(id,2f);
        stats.getTurnAcceleration().modifyMult(id,1.5f);
        stats.getMaxSpeed().modifyMult(id, 2f);
        stats.getAcceleration().modifyMult(id, 1.5f);

        ship.addAfterimage(COLOR, (float) 0, 0, new Vector2f(0, 0).x, new Vector2f(0, 0).y, 4f, 0, 0.1f, 0.2f, true, false, false);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxTurnRate().unmodifyMult(id);
        stats.getTurnAcceleration().unmodifyMult(id);
        stats.getAcceleration().unmodifyMult(id);
        stats.getMaxSpeed().unmodifyMult(id);
    }
}
