package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_Longinus_shot_everyframe;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class FSD_Longinus_shotOnHit implements OnHitEffectPlugin {
    private Vector2f direction = new Vector2f();
    private Vector2f adjustedPosition = new Vector2f();
    
    /**
     */
    private boolean isPointInBounds(ShipAPI ship, Vector2f point) {
        BoundsAPI bounds = ship.getExactBounds();
        if (bounds == null) {
            float distance = Misc.getDistance(point, ship.getLocation());
            return distance <= ship.getCollisionRadius();
        }
        
        List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();
        if (segments == null || segments.isEmpty()) {
            float distance = Misc.getDistance(point, ship.getLocation());
            return distance <= ship.getCollisionRadius();
        }
        
        int intersections = 0;
        
        for (BoundsAPI.SegmentAPI segment : segments) {
            Vector2f p1 = segment.getP1();
            Vector2f p2 = segment.getP2();
            
            if ((p1.y > point.y) != (p2.y > point.y)) {
                float xIntersection = (p2.x - p1.x) * (point.y - p1.y) / (p2.y - p1.y) + p1.x;
                
                if (point.x < xIntersection) {
                    intersections++;
                }
            }
        }
        
        return (intersections % 2) == 1;
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile,
                      CombatEntityAPI target,
                      Vector2f hitPoint,
                      boolean shieldHit,
                      ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (engine.isPaused()) return;

        if (shieldHit) {
            if (!(target instanceof ShipAPI)) return;
            
            ShipAPI targetShip = (ShipAPI) target;
            direction = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
            
            float stepSize = 5f;
            float maxPenetrationDistance = 250f;
            float currentDistance = 0f;
            Vector2f testPoint = new Vector2f(hitPoint.x, hitPoint.y);
            Vector2f finalPoint = null;
            
            while (currentDistance < maxPenetrationDistance) {
                currentDistance += stepSize;
                testPoint.x = hitPoint.x + direction.x * currentDistance;
                testPoint.y = hitPoint.y + direction.y * currentDistance;
                
                if (isPointInBounds(targetShip, testPoint)) {
                    finalPoint = new Vector2f(
                        testPoint.x + direction.x * 15f,
                        testPoint.y + direction.y * 15f
                    );
                    break;
                }
            }
            
            if (finalPoint == null) {
                CombatEntityAPI driftEntity = engine.spawnProjectile(
                        projectile.getSource(),
                        projectile.getWeapon(),
                        "FSD_Longinus",
                        hitPoint,
                        projectile.getFacing(),
                        projectile.getVelocity()
                );
                
                if (driftEntity instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI driftProj = (DamagingProjectileAPI) driftEntity;
                    driftProj.setCollisionClass(CollisionClass.NONE);
                    driftProj.setDamageAmount(0f);
                    
                    if (driftProj instanceof MissileAPI) {
                        MissileAPI driftMissile = (MissileAPI) driftProj;
                        driftMissile.setMaxFlightTime(3f + (float)(Math.random() * 2f));
                        driftMissile.setFizzleTime(1f);
                    }
                }
                
                return;
            }
            
            adjustedPosition = finalPoint;
        }

        if (!(target instanceof ShipAPI)) return;

        ShipAPI ship = (ShipAPI) target;

        if (ship.isFighter() || ship.isStation()) return;

        if (ship.isStationModule()) {
            ShipAPI parent = ship.getParentStation();
            if (parent != null) {
                ship = parent;
            }
        }

        if(!shieldHit) {
            direction = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
            
            float penetrationDepth = 20f;
            Vector2f testPoint = new Vector2f(
                hitPoint.x + direction.x * penetrationDepth,
                hitPoint.y + direction.y * penetrationDepth
            );
            
            boolean foundValidPosition = false;
            
            if (isPointInBounds(ship, testPoint)) {
                adjustedPosition = testPoint;
                foundValidPosition = true;
            } else {
                for (float depth = 15f; depth >= 5f; depth -= 5f) {
                    testPoint.x = hitPoint.x + direction.x * depth;
                    testPoint.y = hitPoint.y + direction.y * depth;
                    if (isPointInBounds(ship, testPoint)) {
                        adjustedPosition = testPoint;
                        foundValidPosition = true;
                        break;
                    }
                }
            }
            
            if (!foundValidPosition) {
                CombatEntityAPI driftEntity = engine.spawnProjectile(
                        projectile.getSource(),
                        projectile.getWeapon(),
                        "FSD_Longinus",
                        hitPoint,
                        projectile.getFacing(),
                        projectile.getVelocity()
                );
                
                if (driftEntity instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI driftProj = (DamagingProjectileAPI) driftEntity;
                    driftProj.setCollisionClass(CollisionClass.NONE);
                    driftProj.setDamageAmount(0f);
                    
                    if (driftProj instanceof MissileAPI) {
                        MissileAPI driftMissile = (MissileAPI) driftProj;
                        driftMissile.setMaxFlightTime(3f + (float)(Math.random() * 2f));
                        driftMissile.setFizzleTime(1f);
                    }
                }
                
                return;
            }
        }
        
        if (shieldHit) {
            engine.applyDamage(
                ship,
                adjustedPosition,
                1500f,
                DamageType.KINETIC,
                0f,
                true,
                false,
                projectile.getSource()
            );
        }
        
        ship.getMutableStats().getMaxSpeed().modifyMult("FSD_Longinus_slow", 0.75f);
        ship.getMutableStats().getAcceleration().modifyMult("FSD_Longinus_slow", 0.5f);
        ship.getMutableStats().getDeceleration().modifyMult("FSD_Longinus_slow", 0.5f);
        ship.getMutableStats().getTurnAcceleration().modifyMult("FSD_Longinus_slow", 0.5f);
        ship.getMutableStats().getMaxTurnRate().modifyMult("FSD_Longinus_slow", 0.5f);
        
        ship.setCustomData("FSD_Longinus_slow_start_time", engine.getTotalElapsedTime(false));
        
        if (projectile.getSource() != null) {
            ShipAPI sourceShip = projectile.getSource();
            sourceShip.setCustomData("FSD_Longinus_target", ship);
            sourceShip.setCustomData("FSD_Longinus_target_time", engine.getTotalElapsedTime(false));
        }

        FSD_Longinus_shot_everyframe plugin = (FSD_Longinus_shot_everyframe) engine.getCustomData().get("FSD_Longinus_plugin_instance");
        if (plugin == null) {
            plugin = new FSD_Longinus_shot_everyframe();
            engine.addLayeredRenderingPlugin(plugin);
            engine.getCustomData().put("FSD_Longinus_plugin_instance", plugin);
        }
        
        CombatEntityAPI spawnedEntity = engine.spawnProjectile(
                projectile.getSource(),
                projectile.getWeapon(),
                "FSD_Longinus_paylord",
                adjustedPosition,
                projectile.getFacing(),
                new Vector2f()
        );
        
        DamagingProjectileAPI spear = null;
        if (spawnedEntity instanceof DamagingProjectileAPI) {
            spear = (DamagingProjectileAPI) spawnedEntity;
            spear.setCollisionClass(CollisionClass.NONE);

            if (spear instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) spear;
                missile.setMaxFlightTime(15f);
                missile.setFlightTime(0f);
                missile.setFizzleTime(0.1f);
                missile.setNoGlowTime(0f);
                missile.setFadeTime(0.05f);
            }
        }
        
        plugin.addAttachedSpear(ship, adjustedPosition, projectile.getFacing(), spear, projectile.getSource());
        
        if (projectile.getSource() != null) {
            ShipAPI sourceShip = projectile.getSource();
            if (sourceShip.getSystem() != null && sourceShip.getSystem().isActive()) {
                engine.spawnEmpArcVisual(
                    adjustedPosition,
                    ship,
                    adjustedPosition,
                    ship,
                    10f,
                    new java.awt.Color(166, 11, 11, 255),
                    java.awt.Color.WHITE
                );
                
                engine.applyDamage(
                    ship,
                    adjustedPosition,
                    0f,
                    DamageType.ENERGY,
                    3000f,
                    true,
                    false,
                    projectile.getSource()
                );
                
                for (int i = 0; i < 3; i++) {
                    Vector2f randomPoint = Misc.getPointAtRadius(adjustedPosition, (float)(Math.random() * 100f));
                    engine.spawnEmpArcVisual(
                        adjustedPosition,
                        null,
                        randomPoint,
                        null,
                        5f,
                        new java.awt.Color(100, 165, 255, 200),
                        java.awt.Color.WHITE
                    );
                }
                
                Global.getSoundPlayer().playSound("system_emp_emitter_activate", 1f, 1f, adjustedPosition, new Vector2f());
            }
        }

    }
}
