package data.combat;

import com.fs.graphics.TextureLoader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_BloodEatEffect;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;

public class FSD_WeepingBloodEffect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(0.25f, 1.75f);
    private boolean wasZero = true;
    public IntervalUtil timer = new IntervalUtil(0.8f, 0.8f);
    private boolean init = false;

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            ShipAPI ship = (ShipAPI) target;
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getRayEndPrevFrame());
            if(hitShield){
                applyToDamage(engine, beam, amount, ship);
            }else {
                applyToDamage(engine, beam, amount, target);
            }
        }
        Vector2f specloc = new Vector2f(beam.getWeapon().getLocation().x, beam.getWeapon().getLocation().y);
        timer.advance(amount);
        if (timer.intervalElapsed()) {
            Global.getCombatEngine().addFloatingText(beam.getWeapon().getLocation(),
                    "wqrwrqwetr",
                    1f,
                    Color.RED,
                    engine.getPlayerShip(),
                    1f, 4f);
            disappear(beam.getWeapon());
            engine.addSmoothParticle(specloc, new Vector2f(0, 0), 60f, 15f, 1f, new Color(255, 169, 169, 255));
            engine.addSmoothParticle(specloc, new Vector2f(0, 0), 40, 20f, 1.1f, new Color(239, 197, 210, 255));
            engine.addSmoothParticle(specloc, new Vector2f(0, 0), 60, 20f, 1.3f, new Color(206, 3, 171, 255));
        }
    }
     public void applyToDamage(CombatEngineAPI engine, BeamAPI beam, float amount,CombatEntityAPI target){
         Vector2f point = beam.getRayEndPrevFrame();
         float HEperframeDamage = 0;
         float TOTAL_DAMAGE = 2000;
         engine.applyDamage(target,point,HEperframeDamage,DamageType.HIGH_EXPLOSIVE,0,false,false,beam.getWeapon());
         DamagingProjectileAPI projectile = (DamagingProjectileAPI) engine.spawnProjectile(beam.getSource(), beam.getWeapon(), "FSD_PhasedMine",point,0,new Vector2f());
         Vector2f offset = new Vector2f();
         Vector2f.sub(point, target.getLocation(), offset);
         offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
         if(!init) {
             FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(projectile, (ShipAPI) target, offset, 1f, 100, TOTAL_DAMAGE, 4f, false, true);
             CombatEntityAPI e = engine.addLayeredRenderingPlugin(bloodEatEffect);
             init = true;
         }
//         engine.applyDamage(target,point,FRAGperframeDamage,DamageType.FRAGMENTATION,0,false,false,beam.getWeapon());
     }
     public void disappear(WeaponAPI weapon){
         Vector2f specloc = new Vector2f(weapon.getLocation().x, weapon.getLocation().y);
         Vector2f RIPVEL = new Vector2f(0f, 0f);
         RippleDistortion ripple = new RippleDistortion(specloc, RIPVEL);
         ripple.setAutoFadeSizeTime(1.2f);
         ripple.setMaxSize(30f);
         ripple.setSize(30f);
         ripple.setAutoFadeSizeTime(0.75f);
         ripple.setAutoFadeIntensityTime(0.5f);
         ripple.setIntensity(60f);
         ripple.setFrameRate(75f);
         ripple.fadeOutIntensity(100f);
         DistortionShader.addDistortion(ripple);
         Global.getSoundPlayer().playSound("devastator_explosion",1.5f,2f,specloc,new Vector2f(0, 0));
     }
//            this.ship = ship;
//        }
//
//                                        Vector2f point, boolean shieldHit) {
//
//            }
//        }
//    }

}
