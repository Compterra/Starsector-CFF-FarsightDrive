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

public class FSD_MissileRack extends BaseShipSystemScript {

	public static float CHANCE = 0.33f;
	public boolean init = false;
	public boolean init2 = false;
	public boolean init3 = false;
	public 	List<WeaponAPI> l1= new ArrayList<>();
    private Boolean once = true;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effeLctevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		if(state.equals(State.ACTIVE)){
            while (once) {
                if (!init) {
                    init = true;
                    for (WeaponAPI wea : ship.getAllWeapons()) {
                        if (wea.getSpec().getType().equals(WeaponAPI.WeaponType.MISSILE)) {
                            l1.add(wea);
                            //&& wea.getSpec().getTags().contains("FSD_Missile")
                        }
                    }
                }
                if (!init2 && !init3) {
                    for (WeaponAPI w : l1) {
                        float damage = w.getDamage().getDamage();
                        Vector2f firepoint = w.getLocation();
                        float facing = w.getCurrAngle();
                        CombatEntityAPI entity = Global.getCombatEngine().spawnProjectile(ship, w, "FSD_BlueLight", firepoint, facing, new Vector2f());
                        Global.getCombatEngine().spawnProjectile(ship, w, "FSD_InspireLight", firepoint, facing+30, new Vector2f());
                        Global.getCombatEngine().spawnProjectile(ship, w, "FSD_InspireLight", firepoint, facing-30, new Vector2f());
                        Global.getCombatEngine().spawnProjectile(ship, w, "FSD_InspireLight", firepoint, facing+60, new Vector2f());
                        Global.getCombatEngine().spawnProjectile(ship, w, "FSD_InspireLight", firepoint, facing-60, new Vector2f());
                        if (entity instanceof DamagingProjectileAPI) {
                            DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
                            float damagemult = 0.25f;
                            if (ship.getVariant().hasHullMod("FSD_LimitationOfTissueHyperplasia")) {
                                float ram = (float) Math.random();
                                if (ram <= CHANCE) {
                                    damagemult = 0.5f;
                                }
                            }
                            proj.getDamage().setDamage(damage * damagemult);
                        }
                    }
                    init3 = true;
                }
                once = false;
            }
        }
        stats.getMissileRoFMult().modifyMult(id,4f);
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		if(init2){init2 = false;}
		if(init3){init3 = false;}
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
        once = true;
        stats.getMissileRoFMult().unmodify(id);
	}
}
