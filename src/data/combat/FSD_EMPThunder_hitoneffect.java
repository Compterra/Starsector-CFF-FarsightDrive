package data.combat;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;


public class FSD_EMPThunder_hitoneffect implements OnHitEffectPlugin {
    private static final Color THUNDER_COLOR = new Color(163, 86, 91, 189);


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if ((float) Math.random() > 0f && !shieldHit && target instanceof ShipAPI) {

            float emp = projectile.getEmpAmount();
            float dam = projectile.getDamageAmount();

            engine.spawnEmpArc(projectile.getSource(), point, target, target,
                    DamageType.ENERGY,
                    dam,
                    emp,
                    100000f,
                    "tachyon_lance_emp_impact",
                    50f,
                    THUNDER_COLOR,
                    new Color(255,255,255,255)
            );

        }
    }
}
