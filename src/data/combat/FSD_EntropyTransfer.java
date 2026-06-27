package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize.FIGHTER;
import static com.fs.starfarer.api.impl.campaign.DModManager.getMod;
import static com.fs.starfarer.api.impl.campaign.missions.academy.GAPZPostEncounters.id;

public class FSD_EntropyTransfer implements BeamEffectPlugin {
    private float Hull;
    private float newHull;
    private boolean wasZero = true;
    float damage = 0f;
    private IntervalUtil fireInterval=new IntervalUtil(1f, 1f);


    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = beam.getSource();
        if(target instanceof ShipAPI && beam.getBrightness() >= 1f){
            float dur = beam.getDamage().getDpsDuration();
            if(!wasZero) dur = 0f;
            wasZero = beam.getDamage().getDpsDuration() <= 0f;
            fireInterval.advance(dur);
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getRayEndPrevFrame());
            if(!hitShield){
                ship.setHitpoints(Math.min(ship.getMaxHitpoints(),ship.getHitpoints()+beam.getDamage().getDamage()*amount));
            }
////                 if(((ShipAPI) target).getHullSpec().getHullSize() == ShipAPI.HullSize.FIGHTER&&target.getHitpoints()>= 150f){
////                     Hull=target.getHitpoints();
////                     newHull = Hull-(30+getNumDMods(((ShipAPI) target).getVariant()));
////                     target.setHitpoints(newHull);
////                 }
////                 if(((ShipAPI) target).getFluxLevel()>0.2&&target.getHullLevel()>=0.15f){
////                     Hull=target.getHitpoints();
////                     newHull =30+getNumDMods(((ShipAPI) target).getVariant());
////                     target.setHitpoints(Hull-newHull);
////                 }
////                 if(((ShipAPI) target).getFluxLevel()>0.2f&&target.getHitpoints()>= 2000f&&((ShipAPI) target).getHullSpec().getHullSize()!= ShipAPI.HullSize.CRUISER&&((ShipAPI) target).getHullSpec().getHullSize()!= ShipAPI.HullSize.CAPITAL_SHIP){
////                     Hull=target.getHitpoints();
////                     newHull =30+getNumDMods(((ShipAPI) target).getVariant());
////                     target.setHitpoints(Hull-newHull);
////
//////                     engine.addSmoothParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(255, 169, 169, 255));
//////                     engine.addSmoothParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(239, 197, 210, 255));
//////                     engine.addSmoothParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(206, 3, 171, 255));
////                 }
//                         }
//                             }
//                         }
//                     case CAPITAL_SHIP:
//                             }
//                         }
//                 }
//                 ShipAPI ship = beam.getWeapon().getShip();
//                 this.Hull=beam.getWeapon().getShip().getHitpoints();
//             }
        }
    }

}

