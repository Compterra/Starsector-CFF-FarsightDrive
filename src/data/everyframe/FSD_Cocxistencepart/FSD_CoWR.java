package data.everyframe.FSD_Cocxistencepart;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

public class FSD_CoWR implements EveryFrameWeaponEffectPlugin {
    private boolean FSD_LSdetect = false;
    private float X;
    private float Y;
   private ShipEngineControllerAPI.ShipEngineAPI eng;
   private float Eng_Angle;
    private WeaponAPI weapon1;
    private ShipAPI ship;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {


        if (weapon.getShip().getCustomData().containsKey("FSD_Codetect") && !FSD_LSdetect) {
            weapon1 = null;
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
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWR1")){
                weapon.getSprite().setCenter(X + 4 * ship.getSystem().getEffectLevel(), Y + 7.5f * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle+22.5f*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle+22.5f*ship.getSystem().getEffectLevel());
                }
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWR2")){
                weapon.getSprite().setCenter(X - 9 * ship.getSystem().getEffectLevel(), Y + 12.5f * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle+40*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle+40f*ship.getSystem().getEffectLevel());
                }
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_CoWR3")){
                weapon.getSprite().setCenter(X-25.5f*ship.getSystem().getEffectLevel() , Y + 26.5f * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle+53.5f*ship.getSystem().getEffectLevel());
                if(eng!=null){
                    eng.getLocation().set(weapon.getFirePoint(0));
                    eng.getEngineSlot().setAngle(Eng_Angle+53.5f*ship.getSystem().getEffectLevel());
                }
            }

//
//                }
//
//                }
//
//                }

        }

    }
//        float angle = weapon.getSlot().getAngle()+weapon.getShip().getFacing();
//    }
}