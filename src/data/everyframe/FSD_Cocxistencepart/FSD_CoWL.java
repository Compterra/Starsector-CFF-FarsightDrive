package data.everyframe.FSD_Cocxistencepart;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class FSD_CoWL implements EveryFrameWeaponEffectPlugin {
    private boolean FSD_LSdetect = false;
    private float X;
    private float Y;

    private WeaponAPI weapon1;

    private ShipEngineControllerAPI.ShipEngineAPI eng;
    private float Eng_Angle;
    private ShipAPI ship;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.getShip().getCustomData().containsKey("FSD_Codetect") && !FSD_LSdetect) {
            ship = null;
            ship = weapon.getShip();
            FSD_LSdetect = true;
            X = weapon.getSprite().getCenterX();
            Y = weapon.getSprite().getCenterY();
            for(ShipEngineControllerAPI.ShipEngineAPI e:ship.getEngineController().getShipEngines()){
                if(Misc.getDistanceSq(weapon.getFirePoint(0),e.getLocation())<=9f){
                    this.eng = e;
                    this.Eng_Angle = eng.getEngineSlot().getAngle();
                    break;
                }
            }

        }
        float angle = weapon.getSlot().getAngle()+weapon.getShip().getFacing();
        if (ship != null) {
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWL1")){
                weapon.getSprite().setCenter(X - 4f * ship.getSystem().getEffectLevel(), Y + 7.5f * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle-22.5f*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle-22.5f*ship.getSystem().getEffectLevel());
                }
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWL2")){
                weapon.getSprite().setCenter(X + 9f * ship.getSystem().getEffectLevel(), Y + 12.5f * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle-40*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle-40f*ship.getSystem().getEffectLevel());
                }
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWL3")){
                weapon.getSprite().setCenter(X+25.5f *ship.getSystem().getEffectLevel(), Y + 26.5f * ship.getSystem().getEffectLevel());

                weapon.setCurrAngle(angle-53.5f*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle-53.5f*ship.getSystem().getEffectLevel());
                }
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