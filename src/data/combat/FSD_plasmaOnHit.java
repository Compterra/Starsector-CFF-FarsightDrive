package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_BloodEatEffect;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.DistortionEntity;
import org.boxutil.units.standard.entity.FlareEntity;
import org.lazywizard.lazylib.LazyLib;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class FSD_plasmaOnHit implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Vector2f loc = new Vector2f();
        String WeaponName = "FSD_PhantomWeapon1";
        if(target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
//            Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
//            FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(projectile, ship, offset, 1.5f,255,500,10f,true,true);
//            CombatEntityAPI e = engine.addLayeredRenderingPlugin(bloodEatEffect);
            for(int i = 0; i < 10; i++){
                loc = getLocation(ship.getLocation(), projectile.getWeapon(), Misc.getDistance(projectile.getWeapon().getLocation(), ship.getLocation())/2.5f);
//                DistortionEntity newDistortion = new DistortionEntity();
//                newDistortion.setGlobalTimer(0.15f, 0.1f, 0.25f);
//                newDistortion.setInnerFull(0.7f, 0.7f);
//                newDistortion.setInnerHardness(0.8f);
//                newDistortion.setSizeIn(50, 50);
//                newDistortion.setPowerIn(0f);
//                newDistortion.setPowerFull(1);
//                newDistortion.setPowerOut(0f);
//                newDistortion.setSizeFull(30, 30);
//                newDistortion.setSizeOut(15, 15);
//                newDistortion.setLocation(loc);
//                CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, newDistortion);
//                FlareEntity flareEntity = new FlareEntity();
//                flareEntity.setLocation(loc);
//                flareEntity.setSize(30, 25);
//                flareEntity.setFlick(true);
//                flareEntity.setFlickWhenPaused(false);
//                flareEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
//                flareEntity.setSmoothDisc();
//                flareEntity.setFringeColor(Color.CYAN);
//                flareEntity.setCoreColor(Color.WHITE);
//                flareEntity.setCoreAlpha(1.0f);
//                flareEntity.setFringeAlpha(1.0f);
//                flareEntity.setAdditiveBlend();
//                flareEntity.setNoisePower(0.33f);
//                flareEntity.autoAspect();
//                flareEntity.setGlobalTimer(0.15f, 0.1f, 0.25f);
//                CombatRenderingManager.addEntity(BoxEnum.ENTITY_FLARE, flareEntity);
                NegativeExplosionVisual.NEParams neEffect = new NegativeExplosionVisual.NEParams();
                neEffect.fadeOut = 0.1f;
                neEffect.radius = 3.5f;
                neEffect.thickness = 5f;
                neEffect.color = new Color(14, 190, 213, 255);
                neEffect.underglow = new Color(81, 3, 28, 255);
                CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(neEffect));
                visual.getLocation().set(loc);
                CombatEntityAPI entity = engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(),WeaponName, loc, Misc.getAngleInDegrees(loc,target.getLocation()),new Vector2f());
                    DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
//                    entity.setFacing(angletoTarget);

//                }
            }
        }
    }
    public Vector2f getLocation(Vector2f point, WeaponAPI source,float distance) {
        if(source == null) return point;
        Vector2f sourceLoc = source.getLocation();
        float ratio = Math.min(0.5f, distance/Misc.getDistance(sourceLoc, point));
        Vector2f loc = new Vector2f();
        loc.setX(sourceLoc.x+ratio*(point.x-sourceLoc.x));
        loc.setY(sourceLoc.y+ratio*(point.y-sourceLoc.y));
        Vector2f offset = new Vector2f();
        if(Math.random() < 0.5f) {
            offset.set(loc.x + (float) (Math.random() * 100), loc.y + (float) (Math.random() * 100));
        }else{
            offset.set(loc.x - (float) (Math.random() * 100), loc.y - (float) (Math.random() * 100));
        }
        if(Misc.getDistance(loc, offset) > 400f){offset.set(loc.x, loc.y);}
        return offset;
    }
}
