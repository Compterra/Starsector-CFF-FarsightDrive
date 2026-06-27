package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FSD_system1 extends BaseShipSystemScript {

	public static float SPEED = 50f;
	public static float DAMAGE = 10f;
	public boolean init = false;
	public 	List<WeaponAPI> l1= new ArrayList<>();
	public void apply(MutableShipStatsAPI stats, String id, State state, float effeLctevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		float effect = 1f;


		if(state.equals(State.ACTIVE)){
			if(!init){
				init = true;

				for(WeaponAPI wea : ship.getAllWeapons()){
					if(wea.getSpec().getType().equals(WeaponAPI.WeaponType.MISSILE) && wea.getAmmoTracker().usesAmmo()){
						wea.getAmmoTracker().setAmmo(wea.getAmmo() + Math.round(wea.getAmmoTracker().getReloadSize()));
					}
				}
				for(WeaponAPI wea : ship.getAllWeapons()){
					if(wea.getSpec().getType().equals(WeaponAPI.WeaponType.MISSILE) && wea.getSpec().getTags().contains("")){
						l1.add(wea);
					}
				}
			}


			if(!ship.getVariant().hasHullMod("FSD_LimitationOfTissueHyperplasia")){
				for(MissileAPI m : Global.getCombatEngine().getMissiles()){
					if(m.getOwner() != 0)continue;
					if(m.getWeapon() == null)continue;
					WeaponAPI wea = m.getWeapon();
					if(!wea.getSpec().getTags().contains(""))continue;
					if(m.getSource() == null)continue;
					if(m.getSource() != ship)continue;
					if(Misc.getDistanceSq(m.getLocation(),ship.getLocation()) <= 250000){
						m.getEngineStats().getMaxSpeed().modifyPercent(id,SPEED);
						m.getDamage().getModifier().modifyPercent(id,DAMAGE);
					}
				}
				for(WeaponAPI w : l1){
					if(w.getCooldownRemaining() >=0){
						if(w.getCooldownRemaining() - Global.getCombatEngine().getElapsedInLastFrame() > 0){
							w.setRemainingCooldownTo(w.getCooldownRemaining() - Global.getCombatEngine().getElapsedInLastFrame());
						}else w.setRemainingCooldownTo(0);
					}
				}
			}
			if(ship.getVariant().hasHullMod("FSD_BiomassRefiningMachine")){
				// fire the designated weapon
			}
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		if(init){init = false;}
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Biomass Refining Machine linked missiles: launch speed and flight speed +50%%, damage dealt +10%%", false);
		}

		return null;
	}
}
