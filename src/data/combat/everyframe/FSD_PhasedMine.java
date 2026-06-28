package data.combat.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import data.hullmods.FSD_IonBurst;
import org.lwjgl.util.vector.Vector2f;

public class FSD_PhasedMine implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if(ship!= null && ship.getShipAI() ==null){
            if(ship.getSelectedGroupAPI()==ship.getWeaponGroupFor(weapon)){
                weapon.setForceNoFireOneFrame(true);
            }
        }
        if(!ship.hasListenerOfClass(FSD_Mine_listener.class)){
            ship.addListener(new FSD_Mine_listener(ship));
        }
    }
    public static class FSD_Mine_listener implements DamageDealtModifier{
        private ShipAPI ship;
        private FSD_Mine_listener(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if(param instanceof DamagingProjectileAPI){
                if(((DamagingProjectileAPI) param).getSource() != null) {
                    if (target instanceof ShipAPI) {
                        if (Math.random() < 0.25f) {
                            engine.applyDamage(target, point, 0, DamageType.ENERGY, 500, false, false, this.ship);
                        }
                    }
                }
            }
            return null;
        }
    }
}
