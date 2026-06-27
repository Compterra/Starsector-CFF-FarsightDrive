package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Timer;
import java.util.TimerTask;

public class FSD_RecoverOrder extends BaseShipSystemScript {

    private static final IntervalUtil clock = new IntervalUtil(0.15f, 0.15f);
    private static final String MISSILE_WEAPON_ID = "FSD_RepairLight";

    private float disableTimer = 0f;
    private ShipAPI manualTarget = null;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (effectLevel>=1f) {
            for(int i=0;i<3;i++) {
                spawnRepairMissile(ship);
            }
        }

    }

    private void spawnRepairMissile(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || ship == null) return;
        WeaponAPI launcher = getLauncher(ship);
       // MissileAPI missile = null;


        if (launcher == null) {
            Global.getLogger(this.getClass()).error("Repair missile launcher not found");
            return;
        }

        final MissileAPI missile = (MissileAPI) engine.spawnProjectile(
                ship,
                launcher,
                MISSILE_WEAPON_ID,
                ship.getLocation(),
                ship.getFacing(),
                ship.getVelocity()
        );
        if (missile == null) {
            Global.getLogger(this.getClass()).error("Repair missile spawn failed");
            return;
        }
    }

    private WeaponAPI getLauncher(ShipAPI ship) {
        if (ship == null) return null;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon == null) continue;
            if ("FSD_RepairLight".equals(weapon.getSpec().getWeaponId())) {
                return weapon;
            }
        }

        return ship.getAllWeapons().get(0);
    }


    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }
}
