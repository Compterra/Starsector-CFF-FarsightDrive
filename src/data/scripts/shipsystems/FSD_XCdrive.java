package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;


import java.awt.*;
import java.util.List;

import static com.fs.starfarer.api.combat.CollisionClass.NONE;
import static com.fs.starfarer.api.combat.CollisionClass.PROJECTILE_FIGHTER;

public class FSD_XCdrive extends BaseShipSystemScript {

    private static final float MOBILITY_BONUS = 1.0f;
    public static float SPEED_BONUS = 100f;
    private Color color = new Color(36,212,255,255);
    private Color Ccolor = new Color(0, 255, 166,255);
    public static float DAMAGE_MULT = 0.2f;
    private boolean SetAmmo = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel){
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(weapon.getId().contains("FSD_XCCannon")&&!SetAmmo){
                weapon.setAmmo(100);
                SetAmmo = true;
            }
        }
        stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
        stats.getAcceleration().modifyPercent(id, MOBILITY_BONUS * 700f);
        stats.getDeceleration().modifyPercent(id, MOBILITY_BONUS * 700f);
        stats.getTurnAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getMaxTurnRate().modifyPercent(id, MOBILITY_BONUS * 100f);
        ship.getEngineController().fadeToOtherColor(this, color, Ccolor, 1f, 0.4f);
        ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_MULT * effectLevel);
        stats.getShieldUpkeepMult().modifyMult(id, 0f);

        if (stats.getEntity() instanceof ShipAPI && false) {
            String key = ship.getId() + "_" + id;
            Object test = Global.getCombatEngine().getCustomData().get(key);
            if (state == State.IN) {
                if (test == null && effectLevel > 0.2f) {
                    Global.getCombatEngine().getCustomData().put(key, new Object());
                    ship.getEngineController().getExtendLengthFraction().advance(1f);
                    for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                        if (engine.isSystemActivated()) {
                            ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
                        }
                    }
                }
            } else {
                Global.getCombatEngine().getCustomData().remove(key);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
//        ShipAPI ship = (ShipAPI) stats.getEntity();
//            }
//        }
        SetAmmo = false;
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Improved maneuverability", false);
        }
        if (index == 1) {
            return new StatusData("+100 top speed", false);
        }
        if (index == 2) {
            return new StatusData("Shield damage taken -20%", false);
        }
        return null;
    }
}

