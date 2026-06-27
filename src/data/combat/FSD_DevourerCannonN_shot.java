package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_DevourerCannonN_shot implements OnHitEffectPlugin {
    public static float DAMAGE = 0;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        DAMAGE = projectile.getDamageAmount();
        if (!shieldHit && target instanceof ShipAPI) {
            dealArmorDamage(projectile, (ShipAPI) target, point, DAMAGE);
        }
//            ShipAPI ship = (ShipAPI) target;
//        }
    }

    public static void dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI source = projectile.getSource();
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);
        float damageDealt = 0f;
        float baseHullDamageMulti = 0.08f;
        float armorThreshold = 0.5f;
        float selfHullThreshold = 0.5f;
        float damageMultiplier = 1f;
        boolean isSourceCritical = source != null &&
                (source.getHitpoints() / source.getMaxHitpoints() < selfHullThreshold);

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                float maxArmor = target.getArmorGrid().getMaxArmorInCell();;
                float armorInCell = grid.getArmorValue(cell[0] + i, cell[1] + j);
                float armorPercent = grid.getArmorRating();

                if (armorPercent < armorThreshold || armorPercent <= 0) {
                    float hullDamage = armorDamage * baseHullDamageMulti * damageTypeMult;

                    boolean isDouble = armorPercent <= 0 || isSourceCritical;
                    if (isDouble) {
                        hullDamage *= 2f;
                        engine.addHitParticle(point,
                                new Vector2f(),
                                50f,
                                1f,
                                0.5f,
                                new Color(255,100,100,255));
                    }

                    if (hullDamage > 0) {
                        if(target.getHullLevel()>=0.15f){
                        engine.applyDamage(target, point, hullDamage,
                                DamageType.ENERGY,
                                0f,
                                false,
                                true,
                                source);
                        }

                        if (source != null) {
                            float healAmount = Math.min(hullDamage,
                                    source.getMaxHitpoints() - source.getHitpoints());
                            source.setHitpoints(source.getHitpoints() + healAmount);

                        }
                    }
                }
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue;

                int cx = cell[0] + i;
                int cy = cell[1] + j;
                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;


    }}}}

