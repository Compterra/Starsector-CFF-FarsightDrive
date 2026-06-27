package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_CombatRender;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static data.scripts.shipsystems.FSD_MineStrikeStats.*;


public class FSD_HESHOnFire implements OnFireEffectPlugin {


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
//        if(weapon.getShip().getHullSize() == ShipAPI.HullSize.CRUISER|| weapon.getShip().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP){
//            projectile.getVelocity().scale(2f);
//        }
       // FSD_ThrowShell(projectile);
    }
//
//        CombatEngineAPI engine = Global.getCombatEngine();
////               for(WeaponAPI weapon : ship.getAllWeapons()){
//            WeaponAPI weapon = proj.getWeapon();
//            Vector2f ejectVelocity = new Vector2f(
//                    (float)Math.cos(Math.toRadians(weapon.getCurrAngle() + 180)) * 150f,
//                    (float)Math.sin(Math.toRadians(weapon.getCurrAngle() + 180)) * 50f
//            FSD_CombatRender.shellObj shell = new FSD_CombatRender.shellObj(
//                    new Vector2f(weapon.getLocation()),
//
//
//        }
//    }

}
