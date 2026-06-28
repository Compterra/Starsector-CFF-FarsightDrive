package data.combat;

import com.fs.starfarer.api.combat.*;

public class FSD_LSWMissileOnFire implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if(projectile instanceof MissileAPI){
            MissileAPI missile = (MissileAPI) projectile;
            missile.setMaxRange(missile.getMaxFlightTime()*missile.getMaxSpeed());
        }
    }
}
