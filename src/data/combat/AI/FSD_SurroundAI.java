package data.combat.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicTargeting;

import java.awt.*;

public class FSD_SurroundAI implements MissileAIPlugin, GuidedMissileAI {

    private final float MAX_SPEED;
    private float PRECISION_RANGE = 750;

    private final Color CORE_COLOR = new Color(200, 100, 225);
    private final Color FRINGE_COLOR = new Color(200, 100, 150, 128);

    private final float RANDOM_RANGE;

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    private boolean launch = true, arcing = false, circlingMode = false, kamikazeMode = false;
    private float eccm = 2,
            timer = 0,
            check = 0f,
            arcTimer = 0,
            arcRandomness = 1f,
            circlingRadius = 1000f,
            empCharge = 100,
            circlingTimer = 0f,
            straightLineTimer = 0f,
            lastAngularVelocity = 0f,
            direction = 1,
            phaseOffset = 0f,
            speedMultiplier = 1f;
    private int count = 0;
    private String missileId = "";

    //////////////////////
    //////////////////////

    public FSD_SurroundAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed();
        if (missile.getSource().getVariant().getHullMods().contains("eccm")) {
            eccm = 1;
        }
        PRECISION_RANGE = (float) Math.pow(2 * PRECISION_RANGE, 2);
        RANDOM_RANGE = (float) Math.random() * 50;

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        
        missileId = missile.getProjectileSpecId() + "_" + missile.hashCode();
        
        long seed = missileId.hashCode();
        seed = (seed < 0) ? -seed : seed;
        
        java.util.Random rng = new java.util.Random(seed);
        
        direction = rng.nextBoolean() ? 1f : -1f;
        
        phaseOffset = rng.nextFloat() * 360f;
        
        speedMultiplier = 0.8f + rng.nextFloat() * 0.4f;
    }
    
    /**
     */
    private float getEffectiveRadius(CombatEntityAPI entity) {
        if (entity instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) entity;
            float shieldRadius = ship.getShield() != null ? ship.getShield().getRadius() : 0f;
            return Math.max(shieldRadius, ship.getCollisionRadius());
        }
        return entity.getCollisionRadius();
    }

    //////////////////////
    //////////////////////

    @Override
    public void advance(float amount) {

        if(missile.getProjectileSpecId().contains("emp")) {
            missile.getEngineStats().getMaxSpeed().modifyMult(missile.getSource().getId(), 2.5f);
        }
        
        if (!kamikazeMode && 
            (missile.isFizzling() || 
             (missile.getFlightTime() > missile.getMaxFlightTime() * 0.9f && missile.getMaxFlightTime() > 0f) ||
             empCharge <= 0)) {
            kamikazeMode = true;
            circlingMode = false;
            
            engine.addHitParticle(
                    missile.getLocation(),
                    missile.getVelocity(),
                    100,
                    1.0f,
                    0.5f,
                    new Color(255, 50, 50, 200));
            
            missile.getSpriteAPI().setColor(new Color(255, 50, 50));
            
//            }
        }
        
        float currentAngularVelocity = Math.abs(missile.getAngularVelocity());
        float angularVelocityDiff = Math.abs(currentAngularVelocity - lastAngularVelocity);
        
        lastAngularVelocity = currentAngularVelocity;
        
        if (currentAngularVelocity < 10f && angularVelocityDiff < 5f) {
            straightLineTimer += amount;
        } else {
            straightLineTimer = 0f;
        }
        
        if (straightLineTimer > 2f && !kamikazeMode && target != null) {
            boolean movingAwayFromTarget = false;
            
            Vector2f toTarget = new Vector2f(
                    target.getLocation().x - missile.getLocation().x,
                    target.getLocation().y - missile.getLocation().y);
            Vector2f velocity = missile.getVelocity();
            
            if (toTarget.length() > 0 && velocity.length() > 0) {
                toTarget.normalise();
                Vector2f velDir = new Vector2f(velocity);
                velDir.normalise();
                
                float dotProduct = toTarget.x * velDir.x + toTarget.y * velDir.y;
                
                if (dotProduct < 0) {
                    movingAwayFromTarget = true;
                }
            }
            
            
            if ((missile.getFlightTime() > missile.getMaxFlightTime() * 0.7f && missile.getMaxFlightTime() > 0f) || 
                (movingAwayFromTarget && missile.getFlightTime() > missile.getMaxFlightTime() * 0.4f)) {
                kamikazeMode = true;
                circlingMode = false;
                
                engine.addHitParticle(
                        missile.getLocation(),
                        missile.getVelocity(),
                        100,
                        1.0f,
                        0.5f,
                        new Color(255, 50, 50, 200));
                
                missile.getSpriteAPI().setColor(new Color(255, 50, 50));
                
//                }
            }
            else {
                circlingMode = false;
                straightLineTimer = 0f;
                
                if (missile.getVelocity().length() < MAX_SPEED * 0.5f) {
                    Vector2f dir = new Vector2f(missile.getVelocity());
                    if (dir.length() > 0) {
                        dir.normalise();
                        dir.scale(MAX_SPEED * 0.6f);
                        missile.getVelocity().set(dir);
                    }
                }
                
//                }
                
                engine.addHitParticle(
                        missile.getLocation(),
                        missile.getVelocity(),
                        50,
                        0.5f,
                        0.5f,
                        new Color(50, 200, 50, 200));
            }
        }
        
        if (circlingMode && target != null) {
            float distanceToTarget = MathUtils.getDistance(missile.getLocation(), target.getLocation());
            float effectiveRadius = getEffectiveRadius(target);
            float expectedDistance = circlingRadius;
            
            if (distanceToTarget > expectedDistance * 1.5f) {
                circlingMode = false;
                
//                }
                
                engine.addHitParticle(
                        missile.getLocation(),
                        missile.getVelocity(),
                        50,
                        0.5f,
                        0.5f,
                        new Color(100, 200, 255, 200));
            }
        }
        
        if (target == null
                || (target instanceof ShipAPI
                && ((ShipAPI) target).isHulk())
                || !engine.isEntityInPlay(target)
                || target.getCollisionClass() == CollisionClass.NONE
        ) {
            int SEARCH_CONE = 360;
            setTarget(
                    MagicTargeting.pickMissileTarget(
                            missile,
                            MagicTargeting.targetSeeking.NO_RANDOM,
                            (int) missile.getWeapon().getRange(),
                            SEARCH_CONE,
                            0,
                            1,
                            1,
                            1,
                            1));
            missile.giveCommand(ShipCommand.ACCELERATE);
            if (Math.random() > 0.5) {
                direction = -direction;
            }
            circlingMode = false;
            return;
        }

        if (!circlingMode && target != null) {
            float effectiveRadius = getEffectiveRadius(target);
            float distanceToTarget = MathUtils.getDistance(missile.getLocation(), target.getLocation());
            if (distanceToTarget < effectiveRadius + 350f) {
                circlingMode = true;
                long seed = missileId.hashCode();
                seed = (seed < 0) ? -seed : seed;
                java.util.Random rng = new java.util.Random(seed);
                
                float randomOffset = 100f + rng.nextFloat() * 300f;
                circlingRadius = effectiveRadius + randomOffset;
                
                circlingTimer = 0f;
            }
        }

        boolean HEATSEEKER = false;
        if (!HEATSEEKER && empCharge <= 0 && !kamikazeMode) {
            return;
        }

        timer += amount;
        if (launch || timer >= check) {
            launch = false;
            timer -= check;
            float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());

            check = Math.min(0.5f, Math.max(0.1f, 2 * dist / PRECISION_RANGE));
            
            if (circlingMode && circlingTimer > 2f) {
                Vector2f toTarget = new Vector2f(
                        target.getLocation().x - missile.getLocation().x,
                        target.getLocation().y - missile.getLocation().y);
                Vector2f velocity = missile.getVelocity();
                
                if (toTarget.length() > 0 && velocity.length() > 0) {
                    toTarget.normalise();
                    Vector2f velDir = new Vector2f(velocity);
                    velDir.normalise();
                    
                    float dotProduct = toTarget.x * velDir.x + toTarget.y * velDir.y;
                    
                    if (dotProduct < -0.7f) {
                        Vector2f newVel = new Vector2f(
                                velocity.x * 0.8f + toTarget.x * velocity.length() * 0.2f,
                                velocity.y * 0.8f + toTarget.y * velocity.length() * 0.2f
                        );
                        missile.getVelocity().set(newVel);
                        
                        if (Math.random() < 0.1f && Global.getCombatEngine().isInCampaign() == false) {
                            engine.addHitParticle(
                                    missile.getLocation(),
                                    new Vector2f(),
                                    20,
                                    0.5f,
                                    0.2f,
                                    new Color(50, 150, 200));
                        }
                    }
                }
            }

            if (kamikazeMode && target != null) {
                lead = target.getLocation();
                
                return;
            } else if (!MathUtils.isWithinRange(missile, target, 500) && !circlingMode) {
                lead =
                        AIUtils.getBestInterceptPoint(
                                missile.getLocation(),
                                MAX_SPEED
                                        * eccm,
                                target.getLocation(),
                                target.getVelocity());
                if (lead == null) {
                    lead = target.getLocation();
                }
            } else {
                float traverse;

                float angleToMissile =
                        VectorUtils.getFacing(
                                VectorUtils.getDirectionalVector(target.getLocation(), missile.getLocation()));

                if (HEATSEEKER) {
                    traverse =
                            MathUtils.getShortestRotation(
                                    angleToMissile, target.getFacing() + 180);

                    if (traverse <= 0) {
                        direction = 1f;
                    } else {
                        direction = -1f;
                    }

                    float effectiveRadius = getEffectiveRadius(target);

                    float idealRadius =
                            (Math.max(
                                    effectiveRadius + 100,
                                    effectiveRadius / 3
                                            + 300))
                                    * ((float)
                                    FastTrig.sin(
                                            MathUtils.FPI
                                                    * Math.min(90, Math.abs(traverse) - 30)
                                                    / 180));

                    float fallbackRadius =
                            MathUtils.getDistance(missile.getLocation(), target.getLocation())
                                    - 100;

                    circlingRadius =
                            Math.min(
                                    circlingRadius,
                                    Math.max(
                                            idealRadius,
                                            fallbackRadius)
                            );
                } else {
                    float baseTraverse = angleToMissile + 25 * direction;
                    
                    float timeOffset = (float) (Math.sin(Math.toRadians(phaseOffset + timer * 20 * speedMultiplier)) * 15);
                    
                    traverse = baseTraverse + timeOffset;
                    
                    if (circlingMode) {
                    } else {
                        float effectiveRadius = getEffectiveRadius(target);
                        float randomOffset = 100f + (float)Math.random() * 100f;
                        circlingRadius = effectiveRadius + randomOffset;
                        circlingMode = true;
                    }
                }

                float targetAngle =
                        angleToMissile
                                - direction
                                * (Math.min(
                                25, Math.abs(traverse * 0.9f)));

                lead = MathUtils.getPoint(target.getLocation(), circlingRadius - RANDOM_RANGE, targetAngle);
            }
            if (lead == null) {
                lead = target.getLocation();
            }

            if (MathUtils.isWithinRange(missile, target, 250) && empCharge > 0) {
                arcing = true;
            } else {
                arcing = false;
                engine.addHitParticle(
                        missile.getLocation(),
                        missile.getVelocity(),
                        50 + 25 * (float) Math.random(),
                        0.5f,
                        0.1f,
                        FRINGE_COLOR);
            }
        }

        float aimAngle =
                MathUtils.getShortestRotation(
                        missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), lead));

        float OVERSHOT_ANGLE = 60;
        
        if (kamikazeMode) {
            missile.giveCommand(ShipCommand.ACCELERATE);
            
            missile.getSpriteAPI().setColor(new Color(255, 50, 50));
            
            missile.getEngineStats().getMaxSpeed().modifyFlat("kamikazeBoost", MAX_SPEED);
            missile.getEngineStats().getAcceleration().modifyFlat("kamikazeBoost", 2000f);
            missile.getEngineStats().getTurnAcceleration().modifyFlat("kamikazeBoost", 2000f);
            missile.getEngineStats().getMaxTurnRate().modifyFlat("kamikazeBoost", 100f);
            
            Vector2f targetDir = VectorUtils.getDirectionalVector(missile.getLocation(), lead);
            targetDir.normalise();
            float currentSpeed = missile.getVelocity().length();
            float newSpeed = Math.max(currentSpeed, MAX_SPEED * 1.5f);
            targetDir.scale(newSpeed);
            
            missile.getVelocity().set(targetDir);
            
            if (aimAngle < 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }
            
            if (Math.random() < 0.2f) {
                engine.addHitParticle(
                        missile.getLocation(),
                        new Vector2f(
                                (float)Math.random() * 20f - 10f,
                                (float)Math.random() * 20f - 10f),
                        3f + (float)Math.random() * 5f,
                        0.5f,
                        0.1f + (float)Math.random() * 0.2f,
                        new Color(255, 50, 50, 200));
            }
            
            if (Math.random() < 0.3f) {
                Vector2f velDir = new Vector2f(missile.getVelocity());
                if (velDir.length() > 0) {
                    velDir.normalise();
                    velDir.scale(-1f);
                    
                    float randX = (float)Math.random() * 10f - 5f;
                    float randY = (float)Math.random() * 10f - 5f;
                    
                    engine.addSmoothParticle(
                            missile.getLocation(),
                            new Vector2f(velDir.x * 20f + randX, velDir.y * 20f + randY),
                            5f + (float)Math.random() * 10f,
                            0.7f,
                            0.2f + (float)Math.random() * 0.3f,
                            new Color(255, 30, 30, 150));
                }
            }
            
            return;
        } else if (circlingMode) {
            circlingTimer += amount;
            
            if (Math.abs(aimAngle) < 30) {
                missile.giveCommand(ShipCommand.ACCELERATE);
                
                if (circlingTimer < 1.0f) {
                    float currentSpeed = missile.getVelocity().length();
                    float speedThreshold = MAX_SPEED * 0.6f;
                    
                    if (currentSpeed > speedThreshold) {
                        missile.giveCommand(ShipCommand.DECELERATE);
                        
                        float desiredSpeed = MAX_SPEED * 0.2f;
                        if (missile.getVelocity().length() > desiredSpeed) {
                            Vector2f newVel = new Vector2f(missile.getVelocity());
                            newVel.normalise();
                            newVel.scale(desiredSpeed);
                            missile.getVelocity().set(newVel);
                        }
                    }
                }
                else {
                    if (missile.getVelocity().length() > MAX_SPEED * 0.7f) {
                        Vector2f newVel = new Vector2f(missile.getVelocity());
                        newVel.normalise();
                        newVel.scale(MAX_SPEED * 0.7f);
                        missile.getVelocity().set(newVel);
                    }
                }
            } else if (Math.abs(aimAngle) > 120) {
                missile.giveCommand(ShipCommand.DECELERATE);
            }
        } else if (Math.abs(aimAngle) < OVERSHOT_ANGLE) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }


        float DAMPING = 0.1f;
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
        if (arcing) {
            empArcing(amount);
        }
    }

    void empArcing(float time) {
        IntervalUtil timer = new IntervalUtil(0.25f, 0.25f);
        if (target instanceof ShipAPI) {

            arcTimer += time;
            if (arcTimer >= arcRandomness) {
                arcTimer -= arcRandomness;
                float ARC_DELAY = 0.3f;
                arcRandomness = ARC_DELAY - 0.2f + ((float) Math.random() * 0.4f);
                empCharge--;
                if (Math.random() / 0.15
                        < ((ShipAPI) target).getFluxTracker().getHardFlux()
                        / ((ShipAPI) target).getFluxTracker().getMaxFlux()) {
//                            0,
//                            100,
//                            1000,
//                            "tachyon_lance_emp_impact",
//                            1 + 3 * (float) Math.random(),
//                            FRINGE_COLOR,
//                            CORE_COLOR);
                        if (count <= 1&&Math.random() < 0.005) {
                            engine.spawnProjectile(missile.getSource(), null, "FSD_flarelauncher", missile.getLocation(), 0, new Vector2f());
                            count++;
                        }
                } else {
//                            0,
//                            200,
//                            1000,
//                            "tachyon_lance_emp_impact",
//                            5 + 3 * (float) Math.random(),
//                            FRINGE_COLOR,
//                            CORE_COLOR);
                }
                if(Math.random() < 0.7) {
                    if (MagicRender.screenCheck(0.25f, missile.getLocation())) {
                        MagicLensFlare.createSharpFlare(
                                engine,
                                missile.getSource(),
                                missile.getLocation(),
                                3,
                                200,
                                0,
                                FRINGE_COLOR,
                                CORE_COLOR);
                    }
                }
            }
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
        circlingMode = false;
        kamikazeMode = false;
        
        if (target != null) {
            long seed = (missileId + target.hashCode()).hashCode();
            seed = (seed < 0) ? -seed : seed;
            
            java.util.Random rng = new java.util.Random(seed);
            
            if (rng.nextFloat() < 0.2f) {
                direction = -direction;
            }
            
            phaseOffset = (phaseOffset + rng.nextFloat() * 90f) % 360f;
            
            speedMultiplier = Math.max(0.7f, Math.min(1.3f, 
                                      speedMultiplier + (rng.nextFloat() * 0.2f - 0.1f)));
        }
    }

    public void init(CombatEngineAPI engine) {}


//
//
//
//
//        this.missile = missile;
//        this.maxSpeed = missile.getMaxSpeed() * SPEED_FACTOR;
//        this.engine = Global.getCombatEngine();
//        this.ship = launchingShip;
//
//        this.direction = (Math.random() > 0.5) ? 1 : -1;
//    }
//
//    @Override
//    }
//
//    @Override
//        this.target = target;
//        }
//    }
//
//    @Override
//
//
//
//        }
//
//
//        Vector2f leadPoint = calculateIdealPoint();
//
//
//
//    }
//
//        return target != null &&
//                !(target instanceof ShipAPI && ((ShipAPI) target).isHulk()) &&
//    }
//
//        // assigning a target if there is none or it got destroyed
//                || (target instanceof ShipAPI
//                && ((ShipAPI) target).isHulk()) // comment out this line to remove target reengagement
//                || !engine.isEntityInPlay(target)
//                || target.getCollisionClass() == CollisionClass.NONE // check for phasing ships
//        ) {
//            int SEARCH_CONE = 360;
//                            MagicTargeting.targetSeeking.NO_RANDOM,
//                            SEARCH_CONE,
//                            0,
//                            1,
//                            1,
//                            1,
//                            1));
//            // forced acceleration by default
//        }
//    }
//
//        float baseRadius = (target instanceof ShipAPI) ?
//                ((ShipAPI) target).getCollisionRadius() + BASE_SAFE_DISTANCE :
//                BASE_SAFE_DISTANCE;
//    }
//
//        Vector2f targetPos = target.getLocation();
//        float currentAngle = VectorUtils.getAngle(missile.getLocation(), targetPos);
//    }
//
//        float aimAngle = VectorUtils.getAngle(missile.getLocation(), leadPoint);
//        float angleDiff = MathUtils.getShortestRotation(missile.getFacing(), aimAngle);
//
//        } else if (angleDiff > TURN_DEAD_ZONE) {
//        }
//    }
//
//        float distanceToPoint = MathUtils.getDistance(missile.getLocation(), leadPoint);
//        } else {
//        }
//
//            Vector2f newVel = VectorUtils.resize(missile.getVelocity(), maxSpeed);
//        }
//
//        Vector2f velocity = missile.getVelocity();
//        Vector2f directionVector = VectorUtils.getDirectionalVector(missile.getLocation(), leadPoint);
//        float dotProduct = Vector2f.dot(velocity, directionVector);
//        }
//    }
//
//        float currentDistance = MathUtils.getDistance(missile, target);
//            circlingRadius += RADIUS_ADJUST_STEP;
//        }else {
//            circlingRadius -= RADIUS_ADJUST_STEP+10f;
//        }
//    }
}
