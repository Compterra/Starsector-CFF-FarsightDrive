package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class FSD_HMGOnHit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        ShipAPI ship = projectile.getSource();
        if(!(target instanceof ShipAPI)){
            engine.applyDamage(target,point, projectile.getBaseDamageAmount()*2f,projectile.getDamageType(),0,false,false,ship );
        }
        if(target instanceof ShipAPI){
            if(((ShipAPI) target).getHullSpec().getHullSize() == ShipAPI.HullSize.FIGHTER){
                engine.applyDamage(target,point, projectile.getBaseDamageAmount()*2f,projectile.getDamageType(),0,false,false,ship );
            }
        }
    }
}
