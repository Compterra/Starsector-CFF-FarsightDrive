package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_NightPulse implements OnHitEffectPlugin {
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if(projectile.getSource() !=null){
            ShipAPI source = projectile.getSource();
            if(KarmaAPI.hasKarmaData(source)) {
                float karma = KarmaAPI.getKarma(source);
                if (karma > 0.5f) {
                    if ((float) Math.random() > 0.75f && !shieldHit && target instanceof ShipAPI) {

                        float emp = 150f;
                        float dam = projectile.getDamageAmount() * 0.1f;

                        engine.spawnEmpArc(projectile.getSource(), point, target, target,
                                DamageType.ENERGY,
                                dam,
                                emp, // emp
                                100000f, // max range
                                "tachyon_lance_emp_impact",
                                20f, // thickness
                                projectile.getProjectileSpec().getFringeColor(),
                                projectile.getProjectileSpec().getCoreColor()
                        );
                        //engine.spawnProjectile(null, null, "plasma", point, 0, new Vector2f(0, 0));
                    }
                }
            }
        }
        if (target instanceof ShipAPI&&shieldHit) {
            ShipAPI ship = (ShipAPI) target;
            ship.getFluxTracker().increaseFlux(25,true);
        }
    }
}
