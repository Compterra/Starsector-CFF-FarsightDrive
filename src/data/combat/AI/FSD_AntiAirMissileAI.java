package data.combat.AI;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class FSD_AntiAirMissileAI implements MissileAIPlugin {
    private final MissileAPI missile;
    private final CombatEngineAPI engine;

    public FSD_AntiAirMissileAI(MissileAPI missile, CombatEngineAPI engine) {
        this.missile = missile;
        this.engine = engine;
    }

    @Override
    public void advance(float amount) {
        if (engine.isPaused()) return;

        CombatEntityAPI target = findTarget();
        if (target == null) return;

        Vector2f targetLocation = target.getLocation();
        Vector2f missileLocation = missile.getLocation();
        Vector2f directionToTarget = Vector2f.sub(targetLocation, missileLocation, null);

        directionToTarget.normalise();
        directionToTarget.scale(missile.getMaxSpeed() * amount);

        Vector2f newVelocity = Vector2f.add(missile.getVelocity(), directionToTarget, null);
        missile.getVelocity().set(newVelocity);

        missile.setFacing((float) Math.toDegrees(Math.atan2(newVelocity.y, newVelocity.x)));
    }

    private CombatEntityAPI findTarget() {
        // Find the nearest target within a certain range with priority of enemy fighters > enemy ships
        float searchRange = 1000f;

        CombatEntityAPI targetFighter = null;
        CombatEntityAPI targetShip = null;

        float distanceFighter = Float.MAX_VALUE;
        float distanceShip = Float.MAX_VALUE;

        List<ShipAPI> ships = engine.getShips();

        for (ShipAPI entity : ships) {
            float distance = Vector2f.sub(entity.getLocation(), missile.getLocation(), null).length();

            if (entity.getOwner() != missile.getOwner() && !entity.isHulk()) {
                if (entity.getHullSize() == ShipAPI.HullSize.FIGHTER) {
                    if (distance < distanceFighter && distance <= searchRange) {
                        distanceFighter = distance;
                        targetFighter = entity;
                    }
                } else {
                    if (distance < distanceShip && distance <= searchRange) {
                        distanceShip = distance;
                        targetShip = entity;
                    }
                }
            }
        }

        if (targetFighter != null) return targetFighter;
        return targetShip;
    }
}