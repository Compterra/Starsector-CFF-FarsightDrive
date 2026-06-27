package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class FSD_system2 extends BaseShipSystemScript {

    private static final float DURATION = 5f;
    private static final float SPEED_BONUS = 0.5f;
    private static final float DAMAGE_BONUS = 0.1f;
    private static final float FLUX_PER_SHOT = 150f;
    private static final String SYNERGY_TAG = "FSD_Missile";

    private boolean activated = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
//        ShipAPI ship = null;
//            ShipAPI stats1 = (ShipAPI) stats;
//            }
//        }
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        if (ship == null) return;

        if (state == State.ACTIVE && !activated) {
            activated = true;

            reloadAllMissiles(ship);

            applySynergyBuffs(ship, id);

            ship.getFluxTracker().increaseFlux(FLUX_PER_SHOT * countSynergyMissiles(ship), false);
        }

        if (state == State.ACTIVE) {
            stats.getMissileMaxSpeedBonus().modifyPercent(id, SPEED_BONUS * 100f);
            stats.getMissileWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS * 100f);
        }
    }


    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if(activated){activated = false;}
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
    }

    private void reloadAllMissiles(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {

                int maxAmmo = weapon.getMaxAmmo();
                int currentAmmo = weapon.getAmmo();
                if (currentAmmo < maxAmmo) {
                    weapon.setAmmo(currentAmmo + 1);
                }
            }
        }
    }


    private void applySynergyBuffs(ShipAPI ship, String id) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            for(int i=0;i<ship.getAllWeapons().size();i++) {
            if (weapon.getSpec().hasTag(SYNERGY_TAG)&&weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                //ProjectileSpecAPI proj = (ProjectileSpecAPI) weapon.getSpec().getProjectileSpec();

                for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()){
                    if(proj.getWeapon().getShip().getId().equals("FSD_storm"))
                proj.getProjectileSpec().setMoveSpeed(
                        proj.getMoveSpeed() * (1 + SPEED_BONUS)
                );
                weapon.getDamage().setDamage(1 + DAMAGE_BONUS);
                }
            }
            }
        }
    }

    private int countSynergyMissiles(ShipAPI ship) {
        int count = 0;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSpec().hasTag(SYNERGY_TAG)) count++;
        }
        return count;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Biomass link protocol active", false);
        }
        if (index == 1) {
            return new StatusData(String.format("Missile speed +%.0f%% Damage +%.0f%%",
                    SPEED_BONUS * 100, DAMAGE_BONUS * 100), false);
        }
        return null;
    }

//    @Override
//    }
}
