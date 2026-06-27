package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_FlowOnHit implements OnHitEffectPlugin {
    float HP;
    float damage;
    private Color FringeCOLOR = new Color(171, 8, 8, 255);
    private Color COLOR = new Color(246, 49, 49, 255);
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        damage = projectile.getDamage().getDamage();
        ShipAPI source = (ShipAPI) projectile.getSource();
        if(target instanceof ShipAPI){
            HP = target.getHitpoints();
            projectile.setDamageAmount(HP*0.04f);
            engine.spawnEmpArcPierceShields(source, point, target, target,
                    DamageType.ENERGY,
                    HP * 0.005f,
                    HP * 0.1f,
                    100000f,
                    "tachyon_lance_emp_impact",
                    20f,
                    FringeCOLOR,
                    COLOR);
            engine.spawnEmpArcPierceShields(source, point, target,target, DamageType.ENERGY, HP * 0.005f, HP * 0.1f, 100000f, "tachyon_lance_emp_impact", 20f, FringeCOLOR, COLOR);
            engine.spawnEmpArcPierceShields(source, point, target,target, DamageType.ENERGY, HP * 0.005f, HP * 0.1f, 100000f, "tachyon_lance_emp_impact", 20f, FringeCOLOR, COLOR);
            engine.spawnEmpArcPierceShields(source, point, target,target, DamageType.ENERGY, HP * 0.005f, HP * 0.1f, 100000f, "tachyon_lance_emp_impact", 20f, FringeCOLOR, COLOR);
//                        0f,
//                        HP * 0.05f,
//                        100000f,
//                        "tachyon_lance_emp_impact",
//                        20f,
//            }
        }

    }
}
