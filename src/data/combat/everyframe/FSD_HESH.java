package data.combat.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.everyframe.FSD_CombatRender;
import org.lwjgl.util.vector.Vector2f;

import java.util.Objects;

public class FSD_HESH implements EveryFrameWeaponEffectPlugin {


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//        float range = weapon.getRange();
//        MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
//        if(weapon.getShip().getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER){
//            if(weapon.getChargeLevel() == 1)weapon.getShip().getFluxTracker().increaseFlux(weapon.getFluxCostToFire()*(1/3f), false);
//        }
//        if(weapon.getShip().getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP){
//            if(weapon.getChargeLevel() == 1)weapon.getShip().getFluxTracker().increaseFlux(weapon.getFluxCostToFire()*(1/3f), false);
//        }
        if(!weapon.getShip().hasListenerOfClass(HESH_RangeModifier.class)){
            weapon.getShip().addListener(new HESH_RangeModifier(weapon,weapon.getShip().getHullSize()));
        }


//        //FSD_CombatRender.getRenders(Global.getCombatEngine()).add(render);
////        render.setLocation(projectile.getLocation());
////        render.setFacing(projectile.getFacing());
////        render.setRed(135);
//        //render.ThrowShell(weapon.getLocation(), weapon.getCurrAngle(), weapon,amount);
//            }


    }
    public static class HESH_RangeModifier implements WeaponBaseRangeModifier{
        public WeaponAPI weapon;
        public ShipAPI.HullSize size;
        public HESH_RangeModifier(WeaponAPI weapon,ShipAPI.HullSize size){
            this.weapon = weapon;
            this.size = size;
        }
        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {return 0;}
        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {return 1f;}

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            //float bonus = 0;
            if(weapon.getId()==this.weapon.getId()){
                switch (size){
                    case CRUISER:
                        return 200;
                    case CAPITAL_SHIP:
                        return 300;
                    default:
                        return 0;
                }
            }
            return 0;
        }
    }
}
