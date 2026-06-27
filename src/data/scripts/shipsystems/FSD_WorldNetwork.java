package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize.FIGHTER;

public class FSD_WorldNetwork extends BaseShipSystemScript {

    private static final float BAD_MULT = 0.5f;
    private static final float HEAL_AMOUNT = 100f;
    private static final float RANGE = 4000f;
    private static final float FLUX_MULT = 0.1f;
    private static final float DAMAGE_MULT = 0.2f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null) return;

        stats.getMaxSpeed().modifyMult(id, BAD_MULT);
        stats.getBallisticRoFMult().modifyMult(id, BAD_MULT);
        stats.getEnergyRoFMult().modifyMult(id, BAD_MULT);

        Vector2f shipLocation = ship.getLocation();
        float shipRadius = ship.getCollisionRadius();
        List<ShipAPI> allShips = engine.getShips();

        for (ShipAPI otherShip : allShips) {

            Vector2f otherLocation = otherShip.getLocation();
            float distance = Misc.getDistance(shipLocation, otherLocation);

            if (ship.getOwner() == otherShip.getOwner()) {
                if (distance <= RANGE) {

                    otherShip.setJitter(otherShip,new Color(163, 86, 91, 189),1,1,2);


                    otherShip.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f);
                    otherShip.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f);
                    otherShip.setHitpoints(Math.min(otherShip.getHitpoints()+HEAL_AMOUNT*engine.getElapsedInLastFrame()*otherShip.getMutableStats().getTimeMult().getModifiedValue(),otherShip.getMaxHitpoints()));

                    float fluxmult=0f;
                    float damagemult=0f;

                    ShipAPI.HullSize hullsize=otherShip.getHullSize();

                    if(hullsize == FIGHTER){
                        fluxmult=1f+2f*FLUX_MULT;
                        damagemult=1f+2f*DAMAGE_MULT;
                    }else{
                        fluxmult=1f+FLUX_MULT;
                        damagemult=1f+DAMAGE_MULT;
                    }

                    otherShip.getMutableStats().getFluxDissipation().modifyMult(id,fluxmult);
                    otherShip.getMutableStats().getBallisticWeaponDamageMult().modifyMult(id,damagemult);
                    otherShip.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id,damagemult);
                    otherShip.getMutableStats().getMissileWeaponDamageMult().modifyMult(id,damagemult);

                }else{
                    otherShip.getMutableStats().getFluxDissipation().unmodify(id);
                    otherShip.getMutableStats().getBallisticWeaponDamageMult().unmodify(id);
                    otherShip.getMutableStats().getEnergyWeaponDamageMult().unmodify(id);
                    otherShip.getMutableStats().getMissileWeaponDamageMult().unmodify(id);
                }
            }
        }


    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);

        stats.getHullBonus().unmodify(id);
        stats.getArmorBonus().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        stats.getPhaseCloakActivationCostBonus().unmodify();
    }


}