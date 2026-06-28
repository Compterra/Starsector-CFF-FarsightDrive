package data.combat.everyframe;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class FSD_Cruise implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.ENERGY || weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.SYNERGY){
            weapon.getAmmoTracker().setAmmoPerSecond(1/10f);
        }
    }
}
