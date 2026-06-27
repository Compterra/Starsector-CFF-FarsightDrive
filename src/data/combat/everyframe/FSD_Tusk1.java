package data.combat.everyframe;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.Global;

public class FSD_Tusk1 implements EveryFrameWeaponEffectPlugin {

    private boolean isEnabled = true;
    boolean check = false;
    private float time = 10f;
    public void recover(ShipAPI ship, WeaponAPI weapon, float amount){

            if(weapon.getAmmo() > 0 || weapon.isDisabled()){
                this.isEnabled = false;
            }
            if(weapon.getAmmo() <= 0&&!weapon.isDisabled()){
                this.isEnabled = true;
            }
            if(this.isEnabled){
                this.time =this.time - amount;
            }
            if(this.time <= 0){
                weapon.setAmmo(20);
                this.time = 10f;
            }

    }
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        for(MissileAPI MISSILE : engine.getMissiles()){
            if(MISSILE.getWeapon() == weapon){
                if(MISSILE.getFlightTime() >= MISSILE.getMaxFlightTime() - 0.1f && !MISSILE.getCustomData().containsKey("FSD_LOGGED_BURNOUT")) {
                    MISSILE.setCustomData("FSD_LOGGED_BURNOUT", true);
                }
                
                if(!MISSILE.getCustomData().containsKey("FSD_CALCULATED")) {
                    MISSILE.setCustomData("FSD_CALCULATED", true);
                    
                    float targetRange = weapon.getRange();
                    float maxSpeed = MISSILE.getMaxSpeed();
                    float acceleration = MISSILE.getAcceleration();
                    float deceleration = 350f;
                    
                    float launchBoostTime = 0.25f;
                    float boostMaxSpeedMult = 2f;
                    float boostSpeedMult = 2.5f;
                    
                    float boostMaxSpeed = maxSpeed * boostMaxSpeedMult;
                    float boostAcceleration = acceleration * boostSpeedMult;
                    float launchBoostDistance = 0.5f * boostAcceleration * launchBoostTime * launchBoostTime;
                    
                    float remainingRange = targetRange - launchBoostDistance;
                    
                    if (remainingRange <= 0) {
                        float flightTime = (float) Math.sqrt(2 * targetRange / boostAcceleration);
                        flightTime = Math.max(0.1f, flightTime - 0.15f);
                        MISSILE.setMaxFlightTime(flightTime);
                    } else {
                        float timeToMaxSpeed = maxSpeed / acceleration;
                        float distanceToMaxSpeed = 0.5f * acceleration * timeToMaxSpeed * timeToMaxSpeed;
                        
                        if (remainingRange <= distanceToMaxSpeed) {
                            float normalFlightTime = (float) Math.sqrt(2 * remainingRange / acceleration);
                            float totalFlightTime = launchBoostTime + normalFlightTime;
                            totalFlightTime = Math.max(0.1f, totalFlightTime - 0.15f);
                            MISSILE.setMaxFlightTime(totalFlightTime);
                        } else {
                            float constantSpeedDistance = remainingRange - distanceToMaxSpeed;
                            float constantSpeedTime = constantSpeedDistance / maxSpeed;
                            float totalFlightTime = launchBoostTime + timeToMaxSpeed + constantSpeedTime;
                            totalFlightTime = Math.max(0.1f, totalFlightTime - 0.15f);
                            MISSILE.setMaxFlightTime(totalFlightTime);
                        }
                    }
                }
            }
        }

        if(weapon.getShip().getVariant().getHullMods().contains("missleracks")&&!check){
            weapon.setMaxAmmo((int)weapon.getMaxAmmo()/2);
            check=true;
        }
        recover(weapon.getShip(), weapon, amount);
    }
}