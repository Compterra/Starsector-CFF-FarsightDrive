package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static data.scripts.shipsystems.FSD_MineStrikeStats.*;

public class FSD_SpawnMineOnFire implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f entityLoc = weapon.getShip().getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTarget();
        spawnMine(weapon.getShip(), entityLoc);
    }
    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(mineLoc == null){
            return;
        }
        Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 150f + (float) Math.random() * 30f);
        //Vector2f currLoc = null;
        float start = (float) Math.random() * 360f;
        for (float angle = start; angle < start + 390; angle += 30f) {
            if (angle != start) {
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(50f + (float) Math.random() * 30f);
                currLoc = Vector2f.add(mineLoc, loc, new Vector2f());
            }
            for (MissileAPI other : Global.getCombatEngine().getMissiles()) {
                if (!other.isMine()) continue;

                float dist = Misc.getDistance(currLoc, other.getLocation());
                if (dist < other.getCollisionRadius() + 40f) {
                    currLoc = null;
                    break;
                }
            }
            if (currLoc != null) {
                break;
            }
        }
        if (currLoc == null) {
            currLoc = Misc.getPointAtRadius(mineLoc, 30f + (float) Math.random() * 30f);
        }


        //Vector2f currLoc = mineLoc;
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                WEAPON_ID,
                currLoc,
                (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
//			float extraDamageMult = source.getMutableStats().getMissileWeaponDamageMult().getModifiedValue();
        }


        float fadeInTime = 0.5f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));

        float liveTime = LIVE_TIME;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);

        Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
    }

    protected EveryFrameCombatPlugin createMissileJitterPlugin(final MissileAPI mine, final float fadeInTime) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) return;

                elapsed += amount;

                float jitterLevel = mine.getCurrentBaseAlpha();
                if (jitterLevel < 0.5f) {
                    jitterLevel *= 2f;
                } else {
                    jitterLevel = (1f - jitterLevel) * 2f;
                }

                float jitterRange = 1f - mine.getCurrentBaseAlpha();
                float maxRangeBonus = 50f;
                float jitterRangeBonus = jitterRange * maxRangeBonus;
                Color c = JITTER_UNDER_COLOR;
                c = Misc.setAlpha(c, 70);
                mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0, jitterRangeBonus);

                if (jitterLevel >= 1 || elapsed > fadeInTime) {
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        };
    }
}
