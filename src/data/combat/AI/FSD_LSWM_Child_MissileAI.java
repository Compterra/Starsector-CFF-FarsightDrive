package data.combat.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.DistortionEntity;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class FSD_LSWM_Child_MissileAI implements MissileAIPlugin, GuidedMissileAI{
    private MissileAPI missile;
    private CombatEntityAPI targetship;
    public CombatEntityAPI target;
    public FSD_LSWM_Child_MissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    @Override
    public void advance(float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();



//            float AngleChange = AngleDetect(missile, LowestHull);
//            if (AngleChange > 10) missile.giveCommand(ShipCommand.TURN_RIGHT);
//            else if (AngleChange < -10) missile.giveCommand(ShipCommand.TURN_LEFT);
//            else if (AngleChange > -10 && AngleChange < 10) missile.giveCommand(ShipCommand.ACCELERATE);

        if(!missile.getCustomData().containsKey("LSWM_Parent")) return;

        MissileAPI parent = (MissileAPI) missile.getCustomData().get("LSWM_Parent");

        if(parent == null || parent.isFizzling() || parent.isFading() || parent.isExpired()) {
            CombatEntityAPI newTarget = AIUtils.getNearestEnemy(missile);
            if(newTarget == null) return;

            setTarget(newTarget);

            if (isTargetInFront(missile, newTarget)) {
                missile.giveCommand(ShipCommand.ACCELERATE);
            } else {
                float turnDirection = getTurnDirection(missile, newTarget);
                if (turnDirection > 0) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                } else {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                }
            }
        } else {
            setTarget(parent);

            if (isTargetInFront(missile, parent)) {
                missile.giveCommand(ShipCommand.ACCELERATE);
            } else {
                float turnDirection = getTurnDirection(missile, parent);
                if (turnDirection > 0) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                } else {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                }
            }
        }
        missile.giveCommand(ShipCommand.ACCELERATE);
//        update(amount, missile.getLocation(), missile.getVelocity(), LowestHull.getLocation());



        if (target != null && Misc.getDistance(missile.getLocation(), target.getLocation()) > 300) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
        else {
            missile.giveCommand(ShipCommand.DECELERATE);
        }
    }
    public boolean isTargetInFront(MissileAPI missile, CombatEntityAPI target) {
        Vector2f missileLoc = missile.getLocation();
        Vector2f targetLoc = target.getLocation();

        float facing = missile.getFacing();
        Vector2f missileDir = new Vector2f(
                (float)Math.cos(Math.toRadians(facing)),
                (float)Math.sin(Math.toRadians(facing))
        );

        Vector2f toTarget = new Vector2f(targetLoc.x - missileLoc.x, targetLoc.y - missileLoc.y);
        toTarget.normalise();

        float dot = missileDir.x * toTarget.x + missileDir.y * toTarget.y;

        return dot > Math.cos(1/9f * Math.PI);
    }

    public float getTurnDirection(MissileAPI missile, CombatEntityAPI target) {
        Vector2f missileLoc = missile.getLocation();
        Vector2f targetLoc = target.getLocation();

        float facing = missile.getFacing();
        Vector2f missileDir = new Vector2f(
                (float)Math.cos(Math.toRadians(facing)),
                (float)Math.sin(Math.toRadians(facing))
        );

        Vector2f toTarget = new Vector2f(targetLoc.x - missileLoc.x, targetLoc.y - missileLoc.y);
        toTarget.normalise();

        float cross = missileDir.x * toTarget.y - missileDir.y * toTarget.x;

        return -cross;
    }

    public float AngleDetect (MissileAPI missile, CombatEntityAPI targetship){
        Vector2f missileLoc = missile.getLocation();
        Vector2f targetLoc = targetship.getLocation();
        float CurrAngle = missile.getFacing();
        float TargetAngle = (float) Math.toDegrees(Math.atan2(targetLoc.y - missileLoc.y, targetLoc.x - missileLoc.x));
        return CurrAngle - TargetAngle;
    }
    public void update(float amount, Vector2f currentPosition, Vector2f currentVelocity, Vector2f targetPosition) {
        if (targetPosition == null) return;

        Vector2f awayFromTarget = new Vector2f(currentPosition.x - targetPosition.x, currentPosition.y - targetPosition.y);
        float awayLength = awayFromTarget.length();
        if (awayLength == 0) {
            awayFromTarget = new Vector2f((float)Math.random() - 0.5f, (float)Math.random() - 0.5f);
        }

        Vector2f desiredDir = (Vector2f) awayFromTarget.normalise();

        float currentAngle = missile.getFacing();

        float targetAngle = (float) Math.toDegrees(Math.atan2(desiredDir.y, desiredDir.x));
        if (targetAngle < 0) targetAngle += 360;

        float angleDiff = targetAngle - currentAngle;
        if (angleDiff > 180) {
            angleDiff -= 360;
        } else if (angleDiff < -180) {
            angleDiff += 360;
        }

        float maxTurnRate = 100;

        if (Math.abs(angleDiff) > 30f) {
            if (angleDiff > 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }
        }

        missile.giveCommand(ShipCommand.ACCELERATE);
    }





    private float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

}
