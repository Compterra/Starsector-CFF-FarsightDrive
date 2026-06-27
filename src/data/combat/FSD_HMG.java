package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class FSD_HMG implements OnFireEffectPlugin {
    private float time = 0.025f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI ship =weapon.getShip();
        Vector2f firepoint = weapon.getFirePoint(0);
        //float facing = weapon.getCurrAngle()*weapon.getCurrSpread();
        String weaponid = weapon.getId();
        engine.getElapsedInLastFrame();
        Vector2f location = projectile.getLocation();
        Vector2f vel = projectile.getVelocity();
        Vector2f shipVel = projectile.getSource().getVelocity();
        vel = new Vector2f(vel.x - shipVel.x, vel.y - shipVel.y);
        vel.scale(0.33f);
        for (float h = 0; h < 1; h += 0.25f) {
            float half = projectile.getWeapon().getCooldown() * h;
            location = new Vector2f(vel.x * half + location.x, vel.y * half + location.y);
            float facing = projectile.getFacing() + weapon.getCurrSpread() * ((float) Math.random() - 0.5f)*1.5f;
            //SelectChainAmmo(projectile.getSource(), engine, projectile, location, facing, 1);
            time =time-engine.getElapsedInLastFrame();
            if(weapon.getChargeLevel() >= 1.0f&&time<=0f){
                DamagingProjectileAPI proj = (DamagingProjectileAPI) engine.spawnProjectile(ship, projectile.getWeapon(), weaponid, location, facing, projectile.getVelocity());
                time = 0.025f;
            }
        }
        engine.removeEntity(projectile);

    }

//    @Override
//
//
//    }


}
