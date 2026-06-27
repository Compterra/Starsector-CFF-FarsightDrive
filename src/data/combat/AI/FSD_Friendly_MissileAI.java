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


public class FSD_Friendly_MissileAI implements MissileAIPlugin, GuidedMissileAI{
    private MissileAPI missile;
    private CombatEntityAPI targetship;
    public CombatEntityAPI target;
    public FSD_Friendly_MissileAI(MissileAPI missile, ShipAPI launchingShip) {
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
        //List<ShipAPI> ships = engine.getShips();
        List<ShipAPI> ships = AIUtils.getNearbyAllies(missile, 5000);
        Iterator<ShipAPI> iterator = ships.iterator();
        while (iterator.hasNext()) {
            ShipAPI ship = iterator.next();
            if (ship.getHullSize().ordinal() < ShipAPI.HullSize.FRIGATE.ordinal()) {
                iterator.remove();
            }
        }

        ShipAPI LowestHull = null;
        Map<String, ShipAPI> Repairable = new HashMap<>();

        for (ShipAPI ship : ships) {
            if (ship.getHullLevel() <= 1) {
                Repairable.put(ship.getId(), ship);
            }
        }

        if (Repairable.isEmpty()) {
            if (ships.size() > 1) {
                LowestHull = ships.get(1);
            } else if (!ships.isEmpty()) {
                LowestHull = ships.get(0);
            }
        } else {
            for (ShipAPI ship : Repairable.values()) {
                if (LowestHull == null || ship.getHullLevel() < LowestHull.getHullLevel()) {
                    LowestHull = ship;
                }
            }
        }

        if (LowestHull == null) {
            LowestHull = AIUtils.getNearestShip(missile);
        }

        if (LowestHull != null) {
            setTarget(LowestHull);
//            float AngleChange = AngleDetect(missile, LowestHull);
//            if (AngleChange > 10) missile.giveCommand(ShipCommand.TURN_RIGHT);
//            else if (AngleChange < -10) missile.giveCommand(ShipCommand.TURN_LEFT);
//            else if (AngleChange > -10 && AngleChange < 10) missile.giveCommand(ShipCommand.ACCELERATE);

            if (isTargetInFront(missile, LowestHull)) {
                missile.giveCommand(ShipCommand.ACCELERATE);
            } else {
                float turnDirection = getTurnDirection(missile, LowestHull);
                if (turnDirection > 0) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                } else {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                }
            }
            missile.giveCommand(ShipCommand.ACCELERATE);
//            update(amount, missile.getLocation(), missile.getVelocity(), LowestHull.getLocation());
        }


        if (target != null && Misc.getDistance(missile.getLocation(), target.getLocation()) > 1500) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
//        float dist = target.getCollisionRadius() * 0.75f;
//        if(MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation()) < dist * dist && target!=null){
//            Vector2f loc=missile.getLocation();
//            DistortionEntity newDistortion = new DistortionEntity();
//            newDistortion.setGlobalTimer(0.25f, 0.25f, 0.1f);
//            newDistortion.setInnerFull(0.7f, 0.7f);
//            newDistortion.setInnerHardness(0.8f);
//            newDistortion.setSizeIn(64, 64);
//            newDistortion.setPowerIn(0);
//            newDistortion.setPowerFull(1);
//            newDistortion.setPowerOut(0);
//            newDistortion.setSizeFull(32, 32);
//            newDistortion.setSizeOut(16, 16);
//            newDistortion.setLocation(loc);
//            CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, newDistortion);
//            RepairArmor(missile, (ShipAPI) target,missile.getLocation(),50f);
//            target.setHitpoints(Math.min(target.getHitpoints() + 300, target.getMaxHitpoints()));
//            engine.addNebulaSmokeParticle(missile.getLocation(), new Vector2f(),30f,1.1f,0.15f,0.2f,0.3f,new Color(3, 228, 32, 166));
//            Global.getCombatEngine().addFloatingDamageText(loc, 300, new Color(135, 241, 150), missile, null);
//            Global.getCombatEngine().removeEntity(missile);
//        }

    }
    public static void RepairArmor(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float armorValue = grid.getMaxArmorInCell();

        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = armorDamage * damMult * damageTypeMult;
                damage = Math.min(armorValue, armorInCell+damage);
                if (damage >= armorValue) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
//                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
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

        return dot > Math.cos(1/18f * Math.PI);
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
