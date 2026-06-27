package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_Teleport extends BaseShipSystemScript {

    private Vector2f SEClocation = new Vector2f(0f, 0f);
    private float angle;
    private static final float FLUX_DISSIPATION = 500f;
    private static final float JUMP_DISTANCE = 500f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null || !ship.isAlive()) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI target = ship.getShipTarget();
        Vector2f jumpVector = calculateJumpVector(ship);
        Vector2f targetLocation = Vector2f.add(ship.getLocation(), jumpVector, null);
        if(effectLevel < 0.99f){
            angle = ship.getFacing();
            if (ship != engine.getPlayerShip()) {
                if (target != null) {
                    if (Misc.getDistance(targetLocation, target.getLocation()) < 2000) {
                        try {
                            float randomAngle = (float) (360 * Math.random());
                            float randomDistance = 450f;
                            float offsetX = (float) (randomDistance * Math.cos(Math.toRadians(randomAngle)));
                            float offsetY = (float) (randomDistance * Math.sin(Math.toRadians(randomAngle)));
                            if (SEClocation == null) {
                                SEClocation = new Vector2f(target.getLocation().x + offsetX, target.getLocation().y + offsetY);
                            } else {
                                SEClocation.x = target.getLocation().x + offsetX;
                                SEClocation.y = target.getLocation().y + offsetY;
                            }

                            float dx = target.getLocation().x - SEClocation.x;
                            float dy = target.getLocation().y - SEClocation.y;
                            angle = (float) Math.atan2(dy, dx);
                        } catch (Exception e) {
                            if (SEClocation == null) {
                                SEClocation = new Vector2f(targetLocation);
                            } else {
                                SEClocation.set(targetLocation);
                            }
                            Global.getLogger(FSD_Teleport.class).error("Error calculating SEClocation", e);
                        }
                    }
                }
            }
        }
        if (state == State.ACTIVE && effectLevel >= 0.99f) {
            angle = ship.getFacing();
            if (ship != engine.getPlayerShip()) {
                if (target != null) {
                    if (Misc.getDistance(targetLocation, target.getLocation()) < 2000) {
                        if (SEClocation != null) {
                            performTeleport(ship, SEClocation, angle);
                        } else {
                            performTeleport(ship, targetLocation, angle);
                        }
                    } else {
                        performTeleport(ship, targetLocation, angle);
                    }
                } else {
                    performTeleport(ship, targetLocation, angle);
                }
            }
            if (ship == engine.getPlayerShip()) {
                performTeleport(ship, targetLocation, angle);
            }

            if (ship.getFluxTracker().getCurrFlux() >= FLUX_DISSIPATION) {
                ship.getFluxTracker().decreaseFlux(FLUX_DISSIPATION);
            }
        }
    }
    
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        
        ShipAPI target = ship.getShipTarget();
        Vector2f jumpVector = calculateJumpVector(ship);
        Vector2f targetLocation = Vector2f.add(ship.getLocation(), jumpVector, null);
        CombatEngineAPI engine = Global.getCombatEngine();
        angle = ship.getFacing();
        
        if (ship != engine.getPlayerShip()) {
            if (target != null) {
                if (Misc.getDistance(targetLocation, target.getLocation()) < 2000) {
                    try {
                        float randomAngle = (float) (360 * Math.random());
                        float randomDistance = 450f;
                        float offsetX = (float) (randomDistance * Math.cos(Math.toRadians(randomAngle)));
                        float offsetY = (float) (randomDistance * Math.sin(Math.toRadians(randomAngle)));
                        
                        if (SEClocation == null) {
                            SEClocation = new Vector2f(target.getLocation().x + offsetX, target.getLocation().y + offsetY);
                        } else {
                            SEClocation.x = target.getLocation().x + offsetX;
                            SEClocation.y = target.getLocation().y + offsetY;
                        }

                        float dx = target.getLocation().x - SEClocation.x;
                        float dy = target.getLocation().y - SEClocation.y;
                        angle = -(float) Math.atan2(dy, dx);
                        ship.setFacing(angle);
                    } catch (Exception e) {
                        Global.getLogger(FSD_Teleport.class).error("Error calculating SEClocation in unapply", e);
                    }
                }
            }
        }
    }

    private Vector2f calculateJumpVector(ShipAPI ship) {
        float angle = ship.getFacing();
        return new Vector2f(
                (float) (JUMP_DISTANCE * Math.cos(Math.toRadians(angle))),
                (float) (JUMP_DISTANCE * Math.sin(Math.toRadians(angle)))
        );
    }

    private void performTeleport(ShipAPI ship, Vector2f target, float angle) {
        if (target == null) {
            Global.getLogger(FSD_Teleport.class).warn("Teleport target was null; using current ship location");
            target = new Vector2f(ship.getLocation());
        }
        
        spawnJumpEffect(ship.getLocation());

        ship.setCollisionClass(CollisionClass.NONE);

        Vector2f shipLoc = ship.getLocation();
        if (shipLoc != null) {
            shipLoc.set(target);
        } else {
            try {
                ship.getLocation().set(target);
            } catch (Exception e) {
                Global.getLogger(FSD_Teleport.class).error("Unable to set ship location", e);
            }
        }
        
        ship.setFacing(angle);

        ship.setCollisionClass(CollisionClass.SHIP);

        spawnJumpEffect(target);
    }

    private void spawnJumpEffect(Vector2f location) {
        if (location == null) return;
        
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.addHitParticle(
                location,
                new Vector2f(),
                200f,
                1f,
                0.5f,
                new Color(100,150,255,200)
        );
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("Quantum jump complete", false);
        if (index == 1) return new StatusData("Flux dissipated +500", true);
        return null;
    }

//    @Override
//    }
}
