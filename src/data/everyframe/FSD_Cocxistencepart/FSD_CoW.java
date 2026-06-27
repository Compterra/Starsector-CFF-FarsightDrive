package data.everyframe.FSD_Cocxistencepart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class FSD_CoW implements EveryFrameWeaponEffectPlugin {
    private boolean FSD_LSdetect = false;
    private float X,FPX;
    private float Y,FPY;

    private WeaponAPI weapon1;
    private ShipAPI ship;

    private ShipAPI engineDrone;
    private EngineSlotAPI droneEngineSlot;

    private ShipEngineControllerAPI controller;
    private float CONTRAIL_DUR = 0,CONTRAIL_WIDTH = 0;

    private float DIFFX,DIFFY,DIFFA;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip().getCustomData().containsKey("FSD_Codetect") && !FSD_LSdetect) {
            ship = null;
            ship = weapon.getShip();
            FSD_LSdetect = true;
            X = weapon.getSprite().getCenterX();
            Y = weapon.getSprite().getCenterY();
            FPX = weapon.getSpec().getTurretFireOffsets().get(0).getX();
            FPY = weapon.getSpec().getTurretFireOffsets().get(0).getY();
            controller = ship.getEngineController();
            switch (weapon.getSpec().getWeaponId()){
                case "FSD_CoWL1":
                    DIFFX = -4f;
                    DIFFY = 7.5f;
                    DIFFA = -22.5f;
                    break;
                case "FSD_CoWL2":
                    DIFFX = 9f;
                    DIFFY = 12f;
                    DIFFA = -40f;
                    break;
                case "FSD_CoWL3":
                    DIFFX = 25.5f;
                    DIFFY = 26.5f;
                    DIFFA = -53.5f;
                    break;
                case "FSD_CoWR1":
                    DIFFX = 4f;
                    DIFFY = 7.5f;
                    DIFFA = 22.5f;
                    break;
                case "FSD_CoWR2":
                    DIFFX = -9f;
                    DIFFY = 12f;
                    DIFFA = 40f;
                    break;
                case "FSD_CoWR3":
                    DIFFX = -25.5f;
                    DIFFY = 26.5f;
                    DIFFA = 53.5f;
                    break;
            }
            ShipVariantAPI v = Global.getSettings().createEmptyVariant("FSD_FX_DRONE",Global.getSettings().getHullSpec("FSD_EngineFXDrone"));
            engineDrone = engine.createFXDrone(v);
            engineDrone.getMutableStats().getHullDamageTakenMult().modifyMult("FSD_DX",0f);
            engineDrone.setDrone(true);
            engineDrone.setShipAI(null);
            engine.addEntity(engineDrone);
            droneEngineSlot = engineDrone.getEngineController().getShipEngines().get(0).getEngineSlot();
            CONTRAIL_DUR = droneEngineSlot.getContrailDuration();
            CONTRAIL_WIDTH = droneEngineSlot.getContrailWidth();
            engineDrone.setForceHideFFOverlay(true);
            engineDrone.setDoNotFlareEnginesWhenStrafingOrDecelerating(true);
        }
        float angle = weapon.getSlot().getAngle()+weapon.getShip().getFacing();
        if (ship != null) {
            weapon.getSprite().setCenter(X+DIFFX* ship.getSystem().getEffectLevel(), Y + DIFFY * ship.getSystem().getEffectLevel());
            weapon.setCurrAngle(angle+DIFFA*ship.getSystem().getEffectLevel());

            if(engineDrone!=null) {
                engineDrone.getEngineController().setFlameLevel(droneEngineSlot, (float)(Math.sin(Math.PI*0.5f*ship.getSystem().getEffectLevel())));
                droneEngineSlot.setContrailDuration(CONTRAIL_DUR*(float)(Math.sin(Math.PI*0.5f*ship.getSystem().getEffectLevel())));
                droneEngineSlot.setContrailWidth(CONTRAIL_WIDTH*(float)(Math.sin(Math.PI*0.5f*ship.getSystem().getEffectLevel())));
                droneEngineSlot.setColor(ship.getEngineController().getFlameColorShifter().getCurr());
                engineDrone.getEngineController().getExtendGlowFraction().setBase(ship.getEngineController().getExtendGlowFraction().getCurr());
                engineDrone.getEngineController().getExtendLengthFraction().setBase(ship.getEngineController().getExtendLengthFraction().getCurr());
                engineDrone.getEngineController().getExtendWidthFraction().setBase(ship.getEngineController().getExtendWidthFraction().getCurr());
                if (controller.isAccelerating()) {
                    engineDrone.giveCommand(ShipCommand.ACCELERATE, null, 0);
                }
                if (controller.isDecelerating()) {
                    engineDrone.giveCommand(ShipCommand.DECELERATE, null, 0);
                }
                if (controller.isStrafingLeft()) {
                    engineDrone.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                }
                if (controller.isStrafingRight()) {
                    engineDrone.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                }
                engineDrone.setFacing(weapon.getCurrAngle() + weapon.getSpec().getTurretAngleOffsets().get(0) - 180f);
                engineDrone.getLocation().set(Vector2f.add(weapon.getFirePoint(0),
                        VectorUtils.rotate(new Vector2f(- DIFFY * ship.getSystem().getEffectLevel(),DIFFX* ship.getSystem().getEffectLevel()),engineDrone.getFacing()),null));
            }
        }

    }
    private Vector2f getWorldLocation(ShipAPI ship, Vector2f localOffset) {
        Vector2f rotated = new Vector2f(localOffset);

        VectorUtils.rotate(rotated, ship.getFacing(), rotated);

        return Vector2f.add(ship.getLocation(), rotated, null);
    }

    private Vector2f getAdjustedVelocity(ShipAPI ship, float localAngle) {
        float radians = (float) Math.toRadians(localAngle);
        Vector2f direction = new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));
        VectorUtils.rotate(direction, ship.getFacing(), direction);
        direction.scale(65f);
        Vector2f.add(direction, ship.getVelocity(), direction);
        return direction;
    }
}
