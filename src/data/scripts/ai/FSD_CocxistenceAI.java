package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.ai.BasicShipAI;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class FSD_CocxistenceAI extends BasicShipAI {

    public static final Logger log = Global.getLogger(FSD_CocxistenceAI.class);
    private final ShipAPI ship;
    private ShipAPI lockedTarget;
    private final IntervalUtil targetSelectionInterval = new IntervalUtil(2f, 3f);

    private static final float MAX_TARGET_RANGE = 2000f;
    private static final float LOW_HULL_FOR_RETREAT = 0.2f;
    private static final float LOW_HULL_FOR_SHIELD = 0.33f;
    private static final float HIGH_FLUX_FOR_SHIELD = 0.66f;
    private static final float HIGH_DAMAGE_PROJECTILE = 1200f;

    public FSD_CocxistenceAI(ShipAPI ship) {
        super((Ship) ship, new ShipAIConfig());
        this.ship = ship;
    }

    @Override
    public void advance(float amount) {
        targetSelectionInterval.advance(amount);

        useSystemAggressively();

        if (isExterminateOrderActive()) {
            handleExterminateBehavior();
        } else {
            handleDefaultBehavior();
        }

        super.advance(amount);

        handleCustomShieldControl();
    }

    private void handleDefaultBehavior() {
        if (lockedTarget == null || !lockedTarget.isAlive() || isBackingOff()) {
            if (targetSelectionInterval.intervalElapsed()) {
                lockedTarget = findOurPriorityTarget();
            }
        }
        setTargetOverride((Ship) lockedTarget);
    }

    private void handleExterminateBehavior() {
        if (lockedTarget == null || !lockedTarget.isAlive()) {
            lockedTarget = findOurPriorityTarget();
        }
        setTargetOverride((Ship) lockedTarget);

        if (lockedTarget == null) return;

        if (ship.getHitpoints() / ship.getMaxHitpoints() < LOW_HULL_FOR_RETREAT) {
            getAIFlags().setFlag(AIFlags.BACKING_OFF, 2.0f);
            return;
        }

        boolean canRam = !lockedTarget.getHullSpec().hasTag("station");
        if (canRam) {
            Vector2f ramDestination = calculateRamDestination();
            if (ramDestination != null) {
                getAIFlags().setFlag(AIFlags.MOVEMENT_DEST, 2f, ramDestination);
                getAIFlags().setFlag(AIFlags.RUN_QUICKLY, 2f);
                getAIFlags().setFlag(AIFlags.MANEUVER_TARGET, 2f, lockedTarget);
            }
        }
    }

    private void handleCustomShieldControl() {
        if (ship.getShield() == null) return;

        boolean forceShieldOn = (ship.getHitpoints() / ship.getMaxHitpoints() < LOW_HULL_FOR_SHIELD) ||
                                checkForHighDamageProjectiles();

        getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);

        if (forceShieldOn) {
        } else {
            if (ship.getFluxTracker().getFluxLevel() > HIGH_FLUX_FOR_SHIELD) {
                getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 0.5f);
            }
        }
    }

    private void useSystemAggressively() {
        if (ship.getSystem() != null && ship.getSystem().getCooldownRemaining() <= 0 && !ship.getSystem().isActive()) {
            ship.useSystem();
        }
    }

    private ShipAPI findOurPriorityTarget() {
        ShipAPI bestTarget = null;
        float bestScore = -1f;

        for (ShipAPI potentialTarget : Global.getCombatEngine().getShips()) {
            if (potentialTarget.getOwner() == ship.getOwner() || !potentialTarget.isAlive() || potentialTarget.isFighter() || potentialTarget.isShuttlePod()) {
                continue;
            }

            float distance = Misc.getDistance(ship.getLocation(), potentialTarget.getLocation());
            if (distance > MAX_TARGET_RANGE) {
                continue;
            }

            float score = 0;
            switch (potentialTarget.getHullSize()) {
                case CAPITAL_SHIP: score = 4; break;
                case CRUISER:      score = 3; break;
                case DESTROYER:    score = 2; break;
                case FRIGATE:      score = 1; break;
                default:           score = 0; break;
            }

            if (score > 0) {
                score += (1 - (distance / MAX_TARGET_RANGE)) * 0.5f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = potentialTarget;
            }
        }
        return bestTarget;
    }

    private boolean checkForHighDamageProjectiles() {
        for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
            if (proj.getOwner() == ship.getOwner() || proj.getDamageAmount() < HIGH_DAMAGE_PROJECTILE) {
                continue;
            }
            if (Misc.getDistance(ship.getLocation(), proj.getLocation()) < ship.getCollisionRadius() + proj.getVelocity().length() * 1.5f) {
                return true;
            }
        }
        return false;
    }

    private boolean isBackingOff() {
        return getAIFlags() != null && getAIFlags().hasFlag(AIFlags.BACKING_OFF);
    }

    private boolean isExterminateOrderActive() {
        if (getAIFlags() != null && (getAIFlags().hasFlag(AIFlags.PURSUING) || getAIFlags().hasFlag(AIFlags.HARASS_MOVE_IN))) {
            return true;
        }
        CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(ship.getOwner());
        if (fm != null) {
            CombatTaskManagerAPI tm = fm.getTaskManager(ship.isAlly());
            if (tm != null) {
                CombatFleetManagerAPI.AssignmentInfo assignment = tm.getAssignmentFor(ship);
                if (assignment != null && (assignment.getType() == CombatAssignmentType.INTERCEPT || assignment.getType() == CombatAssignmentType.ASSAULT)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Vector2f calculateFleetCenter(int owner) {
        List<ShipAPI> fleet = new ArrayList<>();
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
            if (s.getOwner() == owner && s.isAlive() && !s.isFighter() && !s.isShuttlePod()) {
                fleet.add(s);
            }
        }
        if (fleet.isEmpty()) return null;

        Vector2f center = new Vector2f();
        for (ShipAPI member : fleet) {
            Vector2f.add(center, member.getLocation(), center);
        }
        center.scale(1f / fleet.size());
        return center;
    }

    private Vector2f calculateRamDestination() {
        if (lockedTarget == null) return null;

        Vector2f friendlyCenter = calculateFleetCenter(ship.getOwner());
        if (friendlyCenter == null) return null;

        Vector2f direction = Vector2f.sub(friendlyCenter, lockedTarget.getLocation(), new Vector2f());
        direction.normalise();

        float distanceBehind = 500f;
        Vector2f destination = new Vector2f(lockedTarget.getLocation());
        Vector2f offset = new Vector2f(direction);
        offset.scale(distanceBehind);
        Vector2f.add(destination, offset, destination);

        return destination;
    }
}