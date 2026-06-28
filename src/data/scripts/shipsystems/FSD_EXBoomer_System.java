package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.DistortionEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_EXBoomer_System extends BaseShipSystemScript {
    private Color COLOR = new Color(0, 102, 255, 218);
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        for(DamagingProjectileAPI projectile : engine.getProjectiles()){
            if(Misc.getDistance(projectile.getLocation(), ship.getLocation())<=300&&projectile.getOwner() != ship.getOwner()){
                if(projectile.getDamageAmount()>=200&&projectile.getOwner() != ship.getOwner()&&projectile.getDamageAmount()<=600){
                    Vector2f loc = projectile.getLocation();
                    DistortionEntity newDistortion = new DistortionEntity();
                    newDistortion.setGlobalTimer(0.2f, 0.3f, 0.2f);
                    newDistortion.setInnerFull(0.7f, 0.7f);
                    newDistortion.setInnerHardness(0.8f);
                    newDistortion.setSizeIn(50, 50);
                    newDistortion.setPowerIn(0f);
                    newDistortion.setPowerFull(1);
                    newDistortion.setPowerOut(0f);
                    newDistortion.setSizeFull(30, 30);
                    newDistortion.setSizeOut(15, 15);
                    newDistortion.setLocation(loc);
                    CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, newDistortion);
                    engine.spawnProjectile(ship,
                            null,
                            "FSD_flarelauncher2",
                            loc,
                            projectile.getFacing(),
                            projectile.getVelocity());
                    engine.addHitParticle(loc,
                            new Vector2f(),
                            40,
                            10f,
                            0.5f,
                            COLOR);
                    engine.removeEntity(projectile);
                }
            }
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify();
        stats.getFluxDissipation().unmodify();
    }
}
