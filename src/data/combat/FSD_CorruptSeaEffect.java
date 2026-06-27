package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.combat.CollisionClass.HITS_SHIPS_AND_ASTEROIDS;


public class FSD_CorruptSeaEffect implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {

	private Color color = new Color(52, 3, 3, 255);
	private Color Ccolor = new Color(189, 54, 24, 255);
	private Color Rcolor = new Color(52, 3, 3, 255);
	private Color RCcolor = new Color(255, 0, 0, 211);
	private Color EXcolor = new Color(215, 122, 19, 145);
	private ShipAPI ship;
	protected CombatEntityAPI chargeGlowEntity;
	protected FSD_CSchargeGlow chargeGlowPlugin;
	private IntervalUtil EXPLOSION_timer = new IntervalUtil(0.5f, 0.5f);
	boolean check = false;
	public FSD_CorruptSeaEffect() {
	}
	
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
//		ShipAPI ship =  weapon.getShip();
//			    	"Corrupt Sea",
//                    20f,
//                    0.1f,
//					1f


		ship = weapon.getShip();


		boolean charging = weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
		if (charging && chargeGlowEntity == null) {
			chargeGlowPlugin = new FSD_CSchargeGlow(weapon);
			chargeGlowEntity = Global.getCombatEngine().addLayeredRenderingPlugin(chargeGlowPlugin);	
		} else if (!charging && chargeGlowEntity != null) {
			chargeGlowEntity = null;
			chargeGlowPlugin = null;
		}

	}
	
	
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		engine.spawnExplosion(point,new Vector2f(),color,40f,2f);
		engine.addNebulaParticle(point,new Vector2f(),30f,30f,0.01f,1f,2.5f,RCcolor);
		engine.addNebulaParticle(point,new Vector2f(),30f,30f,0.01f,2f,1.5f,Rcolor);
		DamagingExplosionSpec spec = new DamagingExplosionSpec(
				0.1f,
				7.5f,
				5f,
				300f,
				150f,
				HITS_SHIPS_AND_ASTEROIDS,
				HITS_SHIPS_AND_ASTEROIDS,
				3f,
				3f,
				0.2f,
				100,
				RCcolor.darker(),
				EXcolor.darker()
		);
		for(CombatEntityAPI entity : engine.getShips()){
			if(Misc.getDistance(entity.getLocation(),point)<=200f){
			engine.applyDamage(entity,entity.getLocation(),150f,DamageType.HIGH_EXPLOSIVE,0,false,false,ship);
			engine.spawnDamagingExplosion(spec,projectile.getSource(),entity.getLocation(),true);
			}
		}
	}
	
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		FSD_CSchargeGlow.SIZE = 8f;
		FSD_CSchargeGlow.UNDERSIZE = 45f;
		FSD_CSchargeGlow.OUT = 0.55f;
		if (chargeGlowPlugin != null) {
			chargeGlowPlugin.attachToProjectile(projectile);
			chargeGlowPlugin = null;
			chargeGlowEntity = null;
			
			MissileAPI missile = (MissileAPI) projectile;
			missile.setMine(true);
			missile.setNoMineFFConcerns(true);
			missile.setMineExplosionRange(FSD_CSchargeGlow.MAX_ARC_RANGE + 50f);
			missile.setMinePrimed(true);
			missile.setUntilMineExplosion(0f);
		}
		
//		RiftTrailEffect trail = new RiftTrailEffect((MissileAPI) projectile);
//		((MissileAPI) projectile).setEmpResistance(1000);
		
		
//		RealityDisruptorEffect effect = new RealityDisruptorEffect(projectile);
//		CombatEntityAPI e = engine.addLayeredRenderingPlugin(effect);
	}
}

