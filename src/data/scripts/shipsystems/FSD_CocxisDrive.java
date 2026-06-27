package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.combat.ShipCommand;

import java.awt.*;

public class FSD_CocxisDrive  extends BaseShipSystemScript {

    private static final float DURATION = 5f;
    private static final float SPEED_BONUS = 0.5f;
    private static final float MOBILITY_BONUS = 3.0f;

    private float initialFlux = 0f;
    private float initialHull = 0f;
    private boolean systemActive = false;
    private Color color = new Color(36,212,255,255);
    private Color Ccolor = new Color(0, 255, 166,255);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        if(!ship.getCustomData().containsKey("FSD_Codetect"))ship.setCustomData("FSD_Codetect", true);

        if (state == State.ACTIVE && !systemActive) {
            initialFlux = ship.getCurrFlux();
            initialHull = ship.getHitpoints();
            systemActive = true;
        }
//        }

        stats.getMaxSpeed().modifyFlat(id, 150f);
        stats.getAcceleration().modifyPercent(id, MOBILITY_BONUS * 700f);
        stats.getDeceleration().modifyPercent(id, MOBILITY_BONUS * 700f);
        stats.getTurnAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getMaxTurnRate().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, 0.05f);
        stats.getCombatEngineRepairTimeMult().modifyMult(id, 0.05f);
        ship.getEngineController().fadeToOtherColor(this, color, Ccolor, 1f, 0.4f);
        ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
        ship.addAfterimage(ship.getEngineController().getFlameColorShifter().getCurr(), (float) 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 7.5f, 0, 0.075f, 0.1f, true, false, false);
        stats.getWeaponDamageTakenMult().modifyMult(id,0.5f);
        stats.getShieldDamageTakenMult().modifyMult(id,0.5f);
        stats.getArmorDamageTakenMult().modifyMult(id,0.5f);
        stats.getHullDamageTakenMult().modifyMult(id,0.5f);
        stats.getEngineDamageTakenMult().modifyMult(id,0.5f);
        stats.getMissileDamageTakenMult().modifyMult(id,0.5f);

        if (ship.getShield() != null) {
            ship.getShield().toggleOff();
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getCombatWeaponRepairTimeMult().unmodify(id);
        stats.getCombatEngineRepairTimeMult().unmodify(id);
        stats.getWeaponDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);
        stats.getMissileDamageTakenMult().unmodify(id);
        systemActive = false;
    }

//    @Override
//        }
//                    SPEED_BONUS * 100, MOBILITY_BONUS * 100), false);
//        }
//    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Maneuverability +300%", false);
        } else if (index == 1) {
            return new StatusData("+150 top speed", false);
        }else if (index == 2) {
            return new StatusData("20x repair speed", false);
        }
        return null;
    }
}
