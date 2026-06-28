package data.combat;

import com.fs.starfarer.api.combat.*;

public class FSD_Cruise_OnFire implements OnFireEffectPlugin {
    @Override
    @SuppressWarnings("deprecation")
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
//        ShipAPI ship = weapon.getShip();
//        if (ship == null) return;
//        if(weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.ENERGY || weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.SYNERGY) {
//            ship.getFluxTracker().increaseFlux(2000f, false);
//            engine.removeEntity(projectile);
//            CombatEntityAPI entity = engine.spawnProjectile(ship, weapon, weapon.getId(), "FSD_Cruise_rocket", weapon.getFirePoint(1), weapon.getCurrAngle(), ship.getVelocity());
//            if(entity instanceof DamagingProjectileAPI){
//                ((DamagingProjectileAPI) entity).setHitpoints(400f);
//            }
//        }
    }
}
