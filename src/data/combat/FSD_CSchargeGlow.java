package data.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;
import java.util.List;


public class FSD_CSchargeGlow extends CombatEntityPluginWithParticles {


	public static int MAX_ARC_RANGE = 600;
	public static float SIZE = 50f;
	public static float UNDERSIZE = 75f;
	public static float IN = 0.25f;
	public static float OUT = 0.75f;

	public static boolean DETECT = true;
	public static float REPAIR_RATE_DEBUFF_DUR = 10f;
	
	public static Color UNDERCOLOR = new Color(94, 17, 17, 255);
	public static Color RIFT_COLOR = new Color(241, 23, 23, 255);
	

	protected WeaponAPI weapon;
	protected DamagingProjectileAPI proj;
	protected IntervalUtil interval = new IntervalUtil(0.2f, 0.4f);
	protected IntervalUtil arcInterval = new IntervalUtil(0.17f, 0.23f);
	protected float delay = 1f;
	
	public FSD_CSchargeGlow(WeaponAPI weapon) {
		super();
		this.weapon = weapon;
		arcInterval = new IntervalUtil(0.17f, 0.23f);
		delay = 0.05f;
		setSpriteSheetKey("fx_particles2");
	}
	
	public void attachToProjectile(DamagingProjectileAPI proj) {
		this.proj = proj;
	}
	
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) return;
		if (proj != null) {
			entity.getLocation().set(proj.getLocation());
		} else {
			entity.getLocation().set(weapon.getFirePoint(0));
		}
		super.advance(amount);
		
		boolean keepSpawningParticles = isWeaponCharging(weapon) ||
					(proj != null && !isProjectileExpired(proj) && !proj.isFading());
		if (keepSpawningParticles) {
			interval.advance(amount);
			if (interval.intervalElapsed()) {
				addChargingParticles(weapon);
			}
		}
		if (proj != null) {
			Global.getSoundPlayer().playLoop("realitydisruptor_loop", proj, 1f, 1f * proj.getBrightness(),
											 proj.getLocation(), proj.getVelocity());
		}
		
//		}
	}
	
	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		super.render(layer, viewport, null);
	}

	public boolean isExpired() {
		boolean keepSpawningParticles = isWeaponCharging(weapon) || 
					(proj != null && !isProjectileExpired(proj) && !proj.isFading());
		return super.isExpired() && (!keepSpawningParticles || (!weapon.getShip().isAlive() && proj == null));
	}

	
	public float getRenderRadius() {
		return 500f;
	}
	
	
	@Override
	protected float getGlobalAlphaMult() {
		if (proj != null && proj.isFading()) {
			return proj.getBrightness();
		}
		return super.getGlobalAlphaMult();
	}

	public void addChargingParticles(WeaponAPI weapon) {
		//CombatEngineAPI engine = Global.getCombatEngine();
		Color color = RiftLanceEffect.getColorForDarkening(RIFT_COLOR);

		
		float size = SIZE;
		float underSize = UNDERSIZE;

		float in = IN;
		float out = OUT;

		out *= 3f;

		float velMult = 0.2f;
		
		if (isWeaponCharging(weapon)) {
			size *= 0.25f + weapon.getChargeLevel() * 0.75f;
		}else if(!isWeaponCharging(weapon)){size *= 1f;}
        if(DETECT){
		addDarkParticle(size, in, out, 1f, size * 0.5f * velMult, 0f, color);
		}
		else{
			addParticle(size, in, out, 1f, size * 0.5f * velMult, 0f, color);
		}
		randomizePrevParticleLocation(size * 0.33f);

		if (proj != null) {
			Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
			if (proj.getElapsed() > 0.2f) {
				if(DETECT){
				    addDarkParticle(size, in, out, 1.5f, size * 0.5f * velMult, 0f, color);
				}else {
					addParticle(size, in, out, 1.5f, size * 0.5f * velMult, 0f, color);
				}
				Vector2f offset = new Vector2f(dir);
				offset.scale(size * 0.6f + (float) Math.random() * 0.2f);
				Vector2f.add(prev.offset, offset, prev.offset);
			}
			if (proj.getElapsed() > 0.4f) {
				if(DETECT){
				    addDarkParticle(size * 1f, in, out, 1.3f, size * 0.5f * velMult, 0f, color);
				}else {
					addParticle(size * 1f, in, out, 1.3f, size * 0.5f * velMult, 0f, color);
				}
				Vector2f offset = new Vector2f(dir);
				offset.scale(size * 1.2f + (float) Math.random() * 0.2f);
				Vector2f.add(prev.offset, offset, prev.offset);
			}
			if (proj.getElapsed() > 0.6f) {
				if(DETECT){
				    addDarkParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
				}else {
					addParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
				}
				Vector2f offset = new Vector2f(dir);
				offset.scale(size * 1.6f + (float) Math.random() * 0.2f);
				Vector2f.add(prev.offset, offset, prev.offset);
			}
			
			if (proj.getElapsed() > 0.8f) {
				if(DETECT){
				    addDarkParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
				} else {
					addParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
				}
				Vector2f offset = new Vector2f(dir);
				offset.scale(size * 2.0f + (float) Math.random() * 0.2f);
				Vector2f.add(prev.offset, offset, prev.offset);
			}

		}
		

		addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, UNDERCOLOR);
		randomizePrevParticleLocation(underSize * 0.67f);
		addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, UNDERCOLOR);
		randomizePrevParticleLocation(underSize * 0.67f);
		

	}

	
	public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
		return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
	}

	
	public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
	}
}

