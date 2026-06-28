package data.everyframe.FSD_LongSongpart;

import com.fs.starfarer.api.combat.*;


public class FSD_LSLpart implements EveryFrameWeaponEffectPlugin {
    private boolean FSD_LSdetect = false;
    private float X;
    private float Y;

    private WeaponAPI weapon1;
    private ShipAPI ship;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {


        if (weapon.getShip().getCustomData().containsKey("FSD_LSdetect") && !FSD_LSdetect) {
            ship = null;
            for (WeaponAPI weapon2 : weapon.getShip().getAllWeapons()) {
                if (weapon2.getSpec().hasTag("FSD_LSweapon")) {
                    ship = weapon2.getShip();
                }
            }
            FSD_LSdetect = true;
            X = weapon.getSprite().getCenterX();
            Y = weapon.getSprite().getCenterY();

        }
        float angle = weapon.getSlot().getAngle()+weapon.getShip().getFacing();
        if (ship != null) {
            if(weapon.getSpec().getWeaponId().equals("FSD_LSLpart1")){
                weapon.getSprite().setCenter(X + 3 * ship.getSystem().getEffectLevel(), Y - 7 * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle-7*ship.getSystem().getEffectLevel());
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_LSLpart2")){
                weapon.getSprite().setCenter(X + 13 * ship.getSystem().getEffectLevel(), Y - 19 * ship.getSystem().getEffectLevel());
                weapon.setCurrAngle(angle-10*ship.getSystem().getEffectLevel());
            }
            if(weapon.getSpec().getWeaponId().equals("FSD_LSLpart3")){
                weapon.getSprite().setCenter(X, Y + 5 * ship.getSystem().getEffectLevel());
            }

//
//                }
//
//                }
//
//                }

        }

    }
}