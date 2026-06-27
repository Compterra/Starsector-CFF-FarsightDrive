package data.everyframe;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class FSD_BiomassRefiningMachineCheck implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, OnHitEffectPlugin {

    boolean check = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon){
        boolean recover=false;

        ShipAPI ship = weapon.getShip();

        if(ship.getVariant().getHullMods().contains("FSD_BiomassRefiningMachine")){
            recover = true;
        }

        if(recover){
        }else{
            weapon.getAmmoTracker().setAmmoPerSecond(0f);
        }


        if(ship.getVariant().getHullMods().contains("missleracks")&&!check){
            weapon.setMaxAmmo(weapon.getSpec().getMaxAmmo());
            check=true;
        }


    }


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

    }
}
