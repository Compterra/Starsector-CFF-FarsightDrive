package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_RepairLightOnHit implements OnHitEffectPlugin {
    private CombatEngineAPI engine;
    public static float RepairAmount = 50;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
//        if(target instanceof ShipAPI) {
//            if (target.getOwner() == projectile.getOwner()) {
//                if (target.getHullLevel() >= 1) {
//                    return;
//                } else {
//                    target.setHitpoints(Math.min(target.getHitpoints() + 300, target.getMaxHitpoints()));
//                    engine.addFloatingDamageText(point, 300, new Color(0, 255, 34), target, null);
//                    RepairArmor(projectile, (ShipAPI) target, point, RepairAmount);
//                }
//            } else {
//                engine.applyDamage(target, point, 300, DamageType.HIGH_EXPLOSIVE, 600, false, false, null);
//            }
//        }
    }

    public static void RepairArmor(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float armorValue = grid.getMaxArmorInCell();

        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = armorDamage * damMult * damageTypeMult;
                damage = Math.min(armorValue, armorInCell+damage);
                if (damage >= armorValue) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
//                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }
}
