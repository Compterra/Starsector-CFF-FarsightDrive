package data.everyframe;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class FSD_WeepingBloodanimation implements EveryFrameWeaponEffectPlugin {
    int current = 0;
    boolean notready = false;
    float ready = -1f;


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }
        if (weapon.getAnimation() == null) {
            return;
        }
        weapon.getAnimation().pause();
        weapon.getAnimation().setFrame(current);
        int total = weapon.getAnimation().getNumFrames() - 1;
        float chargeLevel = weapon.getChargeLevel();
        current = (int) (chargeLevel * total);
    }

}