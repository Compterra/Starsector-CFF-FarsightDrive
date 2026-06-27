package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FSD_LSWmissile implements OnHitEffectPlugin{
    private static final float DETECTION_RANGE = 100f;
    private static final float FRAG_DAMAGE = 80f;
    private static final float EMP_DAMAGE = 60f;
    private static final float DAMAGE_BOOST = 0.10f;
    private final Color EXPLOSION_COLOR = new Color(255, 0, 0, 255);
    private final Color PARTICLE_COLOR = new Color(240, 200, 50, 255);
    private final Color EMPArcCoreColor = new Color(222, 244, 241, 131);
    private final Color EMPArcFringeColor = new Color(7, 99, 236, 255);
    private final int NUM_PARTICLES = 20;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if(target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (!ship.hasListenerOfClass(FSD_LSWMDebuff_Listener.class)) {
                ship.addListener(new FSD_LSWMDebuff_Listener(ship));
            }
        }
        DamagingExplosionSpec boom = new DamagingExplosionSpec(
                0.1f,
                DETECTION_RANGE,
                50,
                FRAG_DAMAGE,
                50,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                2,
                5,
                5,
                25,
                new Color(225,100,0),
                new Color(200,100,25)
        );
        boom.setDamageType(DamageType.FRAGMENTATION);
        boom.setShowGraphic(false);
        boom.setSoundSetId("explosion_flak");
        engine.spawnDamagingExplosion(boom, projectile.getSource(), projectile.getLocation());

        if(MagicRender.screenCheck(0.1f, projectile.getLocation())){
            engine.addHitParticle(
                    projectile.getLocation(),
                    new Vector2f(),
                    100,
                    1,
                    0.25f,
                    EXPLOSION_COLOR
            );
            for (int i=0; i<NUM_PARTICLES; i++){
                float axis = (float)Math.random()*360;
                float range = (float)Math.random()*100;
                engine.addHitParticle(
                        MathUtils.getPointOnCircumference(projectile.getLocation(), range/5, axis),
                        MathUtils.getPointOnCircumference(new Vector2f(), range, axis),
                        2+(float)Math.random()*2,
                        1,
                        1+(float)Math.random(),
                        PARTICLE_COLOR
                );
            }
            engine.applyDamage(
                    projectile,
                    projectile.getLocation(),
                    projectile.getHitpoints() * 2f,
                    DamageType.FRAGMENTATION,
                    0f,
                    false,
                    false,
                    projectile
            );
            for(CombatEntityAPI entity : AIUtils.getNearbyEnemies(projectile,DETECTION_RANGE)){
                engine.spawnEmpArc(projectile.getSource(),
                        projectile.getLocation(),
                        entity, entity,
                        DamageType.FRAGMENTATION,
                        0,
                        EMP_DAMAGE,
                        100000f,
                        "tachyon_lance_emp_impact",
                        10f,
                        EMPArcCoreColor,
                        EMPArcFringeColor);
            }
        }
    }
    public static class FSD_LSWMDebuff_Listener implements AdvanceableListener {

        private ShipAPI ship;
        private IntervalUtil timer = new IntervalUtil(10f,10f);
        public FSD_LSWMDebuff_Listener(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public void advance(float amount) {
//                    "wqrwrqwetr",
//                    10f,
//                    1f, 4f);
            timer.advance(amount);
            if(!timer.intervalElapsed()) {
                applyDebuff(ship);
            }else {
                removeDebuff(ship);
                ship.getListenerManager().removeListenerOfClass(FSD_LSWMDebuff_Listener.class);
            }
        }
        private void applyDebuff(ShipAPI ship) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            String id = ship.getId();
            stats.getEngineDamageTakenMult().modifyMult(id, 1+DAMAGE_BOOST);
            stats.getHullDamageTakenMult().modifyMult(id, 1+DAMAGE_BOOST);
            stats.getArmorDamageTakenMult().modifyMult(id, 1+DAMAGE_BOOST);
        }

        private void removeDebuff(ShipAPI ship) {
            MutableShipStatsAPI stats = ship.getMutableStats();

            stats.getEngineDamageTakenMult().unmodify();
            stats.getHullDamageTakenMult().unmodify();
            stats.getArmorDamageTakenMult().unmodify();
        }
    }
}

