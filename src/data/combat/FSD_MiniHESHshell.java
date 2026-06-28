package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_BloodEatEffect;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FSD_MiniHESHshell extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {
    public static float DAMAGE = 10;
    public float TOTAL_DAMAGE = 25;
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if(projectile.getSource() !=null) {
            ShipAPI source = projectile.getSource();
            if (KarmaAPI.hasKarmaData(source)) {
                float karma = KarmaAPI.getKarma(source);
                if (karma > 0.75f) {
                    if (!projectile.isFading()) {
                        if ((target instanceof ShipAPI)&&!shieldHit) {
                            Vector2f offset = new Vector2f();
                            Vector2f.sub(point, target.getLocation(), offset);
                            offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
                            FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(projectile, (ShipAPI) target, offset, 0.5f, 100, TOTAL_DAMAGE, 0.5f, true, true);
                            CombatEntityAPI e = engine.addLayeredRenderingPlugin(bloodEatEffect);
                        }
                    }
                }
            }
            if (!shieldHit && target instanceof ShipAPI) {
                dealArmorDamage(projectile, (ShipAPI) target, point, DAMAGE);
            }
        }
    }

    public static void dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue;

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) {
                    damMult = 1/15f;
                } else {
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = armorDamage * damMult * damageTypeMult;
                damage = Math.min(damage, armorInCell);
                if (damage <= 0) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }
}
