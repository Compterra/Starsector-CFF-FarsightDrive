package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_MEGAGuider implements BeamEffectPlugin {
    private float ran = (float) Math.random();
    private float chance;
    private float duration;
    private boolean wasZero = true;
    private IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if(target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            ShipAPI ship = (ShipAPI) target;
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                if(((ShipAPI) target).getHullSize() == ShipAPI.HullSize.FIGHTER) {
                    if(!target.getCustomData().containsKey("OriginalOwner")){
                        target.setCustomData("OriginalOwner",target.getOwner());
                    }
                    if(!ship.hasListenerOfClass(FSD_Formulate.class)){
                        ship.addListener(new FSD_Formulate(ship));
                    }
                    if (beam.getWeapon().getShip().getFluxLevel() <= 0.4f) {
                        this.chance = 0.20f;
                        if (ran <= chance) {
                            ship.setOwner(beam.getSource().getOwner());
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(169, 189, 255, 255));
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(197, 238, 239, 255));
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(3, 44, 206, 255));
                        }
                    }else {
                        this.chance = 0.1f;
                        if (ran <= chance) {
                            ship.setOwner(beam.getSource().getOwner());
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(17, 9, 236, 255));
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(125, 63, 232, 255));
                            engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(25, 137, 148, 255));
                        }
                    }
                }
            }
        }
        if(target instanceof MissileAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                if (beam.getWeapon().getShip().getFluxLevel() <= 0.4f) {
                    this.chance = 0.40f;
                    if (ran <= chance) {
                        target.setFacing(target.getOwner());
                        target.setOwner(beam.getWeapon().getShip().getOwner());
                        duration = ((MissileAPI) target).getMaxFlightTime();
                        ((MissileAPI) target).setMaxFlightTime(duration * 2f);
                        ((MissileAPI) target).setSource(beam.getWeapon().getShip());
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(255, 169, 169, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(239, 197, 210, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(206, 3, 171, 255));

                    }
                } else {
                    this.chance = 0.20f;
                    if (ran <= chance) {
                        target.setFacing(target.getOwner());
                        target.setOwner(beam.getWeapon().getShip().getOwner());
                        duration = ((MissileAPI) target).getFlightTime();
                        ((MissileAPI) target).setMaxFlightTime(duration * 2f);
                        ((MissileAPI) target).setSource(beam.getWeapon().getShip());
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(236, 9, 75, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(241, 23, 23, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(103, 3, 3, 255));
                    }
                }
            }
        }
    }

    public static class FSD_Formulate implements AdvanceableListener {
        private IntervalUtil timer = new IntervalUtil(15f,15f);
        private ShipAPI ship;
        public FSD_Formulate(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public void advance(float amount) {
            FSD_Formulate(ship, amount);
        }
        public void FSD_Formulate(ShipAPI ship, float amount){
            if(ship.getCustomData().containsKey("OriginalOwner")) {
//                                    " Charges:"+10 ,
//                                    20f,
//                                    1f, 4f);
                timer.advance(amount);
                if (timer.intervalElapsed()) {
//                                    " Charges:"+10 ,
//                                    20f,
//                                    1f, 4f);
                    ship.setOwner(ship.getOriginalOwner());
                    if(ship.getCustomData().containsKey("OriginalOwner")){
                        ship.removeCustomData("OriginalOwner");
                    }
                }

            }
    }
    }
}
