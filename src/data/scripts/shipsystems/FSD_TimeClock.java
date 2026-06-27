package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class FSD_TimeClock extends BaseShipSystemScript {

    private static final float DURATION = 5f;
    private static final float SPEED_BONUS = 0.5f;
    private static final float MOBILITY_BONUS = 1.0f;

    private float initialFlux = 0f;
    private float initialHull = 0f;
    private boolean systemActive = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE && !systemActive) {
            initialFlux = ship.getCurrFlux();
            initialHull = ship.getHitpoints();
            systemActive = true;
        }
        if (systemActive) {
            float Flux = ship.getFluxTracker().getCurrFlux();
            ship.getFluxTracker().setCurrFlux(initialFlux);
            ship.setHitpoints(initialHull);
        }

        stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS * 100f);
        stats.getAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getDeceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getTurnAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getMaxTurnRate().modifyPercent(id, MOBILITY_BONUS * 100f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        systemActive = false;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Stable thrust: flux/hull locked", false);
        }
        if (index == 1) {
            return new StatusData(String.format("Speed +%.0f%% Maneuverability +%.0f%%",
                    SPEED_BONUS * 100, MOBILITY_BONUS * 100), false);
        }
        return null;
    }


//        return systemActive ? DURATION : 0f;
//    }
//
//
//        return systemActive ? 1f : 0f;
//    }
}
