package data.scripts.shipsystems;

import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import com.fs.starfarer.api.combat.*;

import java.awt.*;

public class FSD_Fortress extends BaseShipSystemScript {

    private static final float SHIELD_DAMAGE_MULT = 0.25f;
    private static final float WEAPON_FIRE_RATE_MULT = 0.5f;
    private static final String SYSTEM_ID = "fsd_shield_focus";

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        stats.getShieldDamageTakenMult().modifyMult(SYSTEM_ID, SHIELD_DAMAGE_MULT);

        stats.getBallisticRoFMult().modifyMult(SYSTEM_ID, WEAPON_FIRE_RATE_MULT);
        stats.getEnergyRoFMult().modifyMult(SYSTEM_ID, WEAPON_FIRE_RATE_MULT);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().unmodify(SYSTEM_ID);
        stats.getBallisticRoFMult().unmodify(SYSTEM_ID);
        stats.getEnergyRoFMult().unmodify(SYSTEM_ID);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Shield damage x" + SHIELD_DAMAGE_MULT, false);
        }
        if (index == 1) {
            return new StatusData("Weapon fire rate x" + WEAPON_FIRE_RATE_MULT, true);
        }
        return null;
    }
}
