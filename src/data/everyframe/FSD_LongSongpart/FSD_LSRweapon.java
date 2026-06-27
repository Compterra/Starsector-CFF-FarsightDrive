package data.everyframe.FSD_LongSongpart;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class FSD_LSRweapon implements EveryFrameWeaponEffectPlugin {
    private boolean FSD_LSdetect = false;
    private float X;
    private float Y;
    private WeaponAPI weapon1;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.getShip().getCustomData().containsKey("FSD_LSdetect") && !FSD_LSdetect) {
            weapon1 = null;
            for (WeaponAPI weapon2 : weapon.getShip().getAllWeapons()) {
                if (weapon2.getSpec().hasTag("FSD_LSweapon")) {
                    weapon1 = weapon2;
                }
            }
            FSD_LSdetect = true;
            X = weapon.getSprite().getCenterX();
            Y = weapon.getSprite().getCenterY();
        }
        if (weapon1 != null) {
            weapon.getSprite().setCenter(X - 12 * weapon1.getChargeLevel(), Y + 12 * weapon1.getChargeLevel());
        }

    }
}
