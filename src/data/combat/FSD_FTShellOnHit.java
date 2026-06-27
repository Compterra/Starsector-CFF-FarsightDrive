package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class FSD_FTShellOnHit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        ShipAPI ship=projectile.getSource();
        float chance = 0.1f;
        if(ship.getCustomData().containsKey("FSD_ReflectLight_Karma")) {
            chance = (float) ship.getCustomData().get("FSD_ReflectLight_Karma");
        }
        if(target instanceof ShipAPI){
            if(Math.random()<=chance+0.05f){
                projectile.getDamage().getModifier().modifyMult(ship.getId(),0);
                engine.applyDamage(target,point,110,DamageType.ENERGY,110,false,false,ship);
            }
        }
    }
}
