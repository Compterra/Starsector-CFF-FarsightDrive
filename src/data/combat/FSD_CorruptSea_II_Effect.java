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

public class FSD_CorruptSea_II_Effect implements OnHitEffectPlugin {
    private static final Color THUNDER_COLOR = new Color(163, 86, 91, 189);


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if(projectile.isExpired()){
            engine.spawnProjectile(
                    engine.getPlayerShip(),
                    projectile.getWeapon(),
                    "FSD_CorruptSea",
                    point,
                    0f,
                    target.getVelocity());
        }
    }
}
