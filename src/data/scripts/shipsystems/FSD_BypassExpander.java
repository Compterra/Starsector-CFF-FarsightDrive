package data.scripts.shipsystems;

//
//
//
//    @Override
//        ShipAPI ship = (ShipAPI) stats.getEntity();
//
//            removeProjectilesInRange(ship, RANGE, MAX_PROJECTILE_DAMAGE_THRESHOLD);
//
//        }
//
//
//    }
//
//    @Override
//
//
//    }
//
//
//
//        CombatEngineAPI engine = Global.getCombatEngine();
//        float reduceflux=0f;
//
//        List<MissileAPI> missiles = engine.getMissiles();
//            float distance = Misc.getDistance(ship.getLocation(), missile.getLocation());
//                reduceflux+=missile.getDamageAmount();
//            }
//        }
//
//        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
//                float distance = Misc.getDistance(ship.getLocation(), projectile.getLocation());
//                    reduceflux+=projectile.getDamageAmount();
//                }
//            }
//        }
//
//        float newflux=Math.max(0f,Math.max(ship.getFluxTracker().getCurrFlux(),ship.getFluxTracker().getHardFlux())-REDUCE_FLUX_MULT*reduceflux);
//        }else if(newflux>ship.getFluxTracker().getHardFlux()){
//        }
//    }
//}

import java.awt.Color;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import static zzz.com.fs.starfarer.prototype.junk.CombatStarfieldParticle.velocity;

public class FSD_BypassExpander extends BaseShipSystemScript {

    private static final float EVENT_HORIZON_SIZE = 10f;
    private static final float SPAGHETTIFY_SPEED = 150f;

    private static final int ACCRETION_PARTICLES = 6;
    private static final float PARTICLE_SIZE_MOD = 0.6f;

    private static final float CORE_GLOW_SCALE = 1.8f;
    private static final float GLOW_DURATION = 0.9f;

    private static final float DISTORTION_SCALE = 4.5f;
    private static final float DISTORTION_INTENSITY = 30f;

    private  final float MAX_PROJECTILE_DAMAGE_THRESHOLD = 250f;
    private  final float RANGE = 600f;
    private  final float REDUCE_FLUX_MULT = 1.33f;
    private boolean effectApplied = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (!effectApplied) {
            effectApplied = true;
            removeProjectilesInRange(ship);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        effectApplied = false;
    }

    private void removeProjectilesInRange(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float totalDamageIntercepted = 0f;

        List<MissileAPI> missiles = engine.getMissiles();
        for (MissileAPI missile : missiles) {
            if (missile.getOwner() == ship.getOwner()) continue;
            if (missile.getDamageAmount() >= MAX_PROJECTILE_DAMAGE_THRESHOLD) continue;
            if (Misc.getDistance(ship.getLocation(), missile.getLocation()) > RANGE) continue;

            createSingularityEffect(engine, missile.getLocation());

            totalDamageIntercepted += missile.getDamageAmount();
            engine.removeEntity(missile);
        }

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (projectile.getOwner() == ship.getOwner()) continue;
            if (projectile.getDamageAmount() >= MAX_PROJECTILE_DAMAGE_THRESHOLD) continue;
            if (Misc.getDistance(ship.getLocation(), projectile.getLocation()) > RANGE) continue;

            createSingularityEffect(engine, projectile.getLocation());

            totalDamageIntercepted += projectile.getDamageAmount();
            engine.removeEntity(projectile);
        }

        if (totalDamageIntercepted > 0) {
            float fluxReduction = REDUCE_FLUX_MULT * totalDamageIntercepted;
            ship.getFluxTracker().decreaseFlux(fluxReduction);
        }
    }

    private void createSingularityEffect(CombatEngineAPI engine, Vector2f epicenter) {
        engine.addHitParticle(
                epicenter,
                new Vector2f(),
                EVENT_HORIZON_SIZE * CORE_GLOW_SCALE,
                1.2f,
                GLOW_DURATION,
                new Color(80, 80, 255, 180)
        );

        for (int i = 0; i < ACCRETION_PARTICLES; i++) {
            float angle = (float) (Math.random() * 360);
            float distance = EVENT_HORIZON_SIZE * (1 + (float) Math.random());

            Vector2f startPos = Vector2f.add(
                    epicenter,
                    new Vector2f(
                            (float) (distance * Math.cos(Math.toRadians(angle))),
                            (float) (distance * Math.sin(Math.toRadians(angle)))
                    ),
                    null
            );

//            Vector2f velocity = Vector2f.sub(epicenter, startPos, null);

            float currentLength = velocity.length();
            if (currentLength > 0) {
                float minSpeed = SPAGHETTIFY_SPEED * 0.3f;
                velocity.scale(Math.max(minSpeed, SPAGHETTIFY_SPEED / currentLength));
            }


            engine.addNebulaSmokeParticle(
                    startPos,
                    velocity,
                    EVENT_HORIZON_SIZE * PARTICLE_SIZE_MOD,
                    1.5f,
                    0.4f,
                    0.6f,
                    GLOW_DURATION * 0.7f,
                    new Color(30, 30, 60, 120)
            );
        }

        RippleDistortion ripple = new RippleDistortion(epicenter, new Vector2f());
        ripple.setSize(EVENT_HORIZON_SIZE * DISTORTION_SCALE);
        ripple.setIntensity(DISTORTION_INTENSITY);
        ripple.setFrameRate(24f);
        ripple.setCurrentFrame(0f);
        ripple.fadeInSize(0.1f);
        ripple.fadeOutIntensity(0.5f);
        DistortionShader.addDistortion(ripple);
    }
}

