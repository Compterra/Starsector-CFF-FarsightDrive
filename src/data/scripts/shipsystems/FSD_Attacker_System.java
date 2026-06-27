package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.DistortionEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_Attacker_System extends BaseShipSystemScript {
    private Color COLOR = new Color(255, 145, 0, 255);
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        stats.getBallisticRoFMult().modifyMult(id, 3f);
        stats.getEnergyRoFMult().modifyMult(id, 3f);
        stats.getMissileRoFMult().modifyMult(id, 3f);
        stats.getDamageToCapital().modifyMult(id, 0.75f);
        stats.getDamageToCruisers().modifyMult(id, 0.75f);
        stats.getDamageToDestroyers().modifyMult(id, 0.75f);
        stats.getDamageToFrigates().modifyMult(id, 0.75f);
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(weapon.getAmmo()>=0){
                weapon.setAmmo(30);
            }
        }
        ship.addAfterimage(COLOR, (float) 0, 0, new Vector2f(0, 0).x, new Vector2f(0, 0).y, 4f, 0, 0.1f, 0.1f, true, false, false);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getBallisticRoFMult().unmodify();
        stats.getEnergyRoFMult().unmodify();
        stats.getMissileRoFMult().unmodify();
        stats.getDamageToCapital().unmodify();
        stats.getDamageToCruisers().unmodify();
        stats.getDamageToDestroyers().unmodify();
        stats.getDamageToFrigates().unmodify();
    }
}
