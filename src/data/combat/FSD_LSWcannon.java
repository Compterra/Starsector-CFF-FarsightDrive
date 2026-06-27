package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_BloodEatEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import java.util.HashMap;
import java.util.Map;

public class FSD_LSWcannon implements OnHitEffectPlugin {

    private static final float DEBUFF_DURATION = 0.5f;
    private static final float MAX_DEBUFF_DURATION = 3f;
    private static final float SHEILD_REDUCTION = 0.3f;
    private static final float MOBILITY_REDUCTION = 0.5f;
    private static DamagingProjectileAPI proj = null;
    private int count = 1;
    public static Vector2f OnHitloc = new Vector2f();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        OnHitloc = point;
        proj = projectile;
        if (!(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;
        if (ship.getOwner() != projectile.getSource().getOwner()) {
            if (!ship.getCustomData().containsKey("LSWCannon_Debuff")) {
                ship.setCustomData("LSWCannon_Debuff", 1);
            } else {
                int count = (int) ship.getCustomData().get("LSWCannon_Debuff");
                count = Math.min(count + 1, 99);
                ship.setCustomData("LSWCannon_Debuff", count);
            }

            if (!ship.hasListenerOfClass(FSD_LSWCDebuff.class)) {
                ship.addListener(new FSD_LSWCDebuff(ship));
            }
        }
    }

    public static class FSD_LSWCDebuff implements AdvanceableListener {
        private ShipAPI ship;
        private Map<Integer, WeaponAPI> weaponEntities = new HashMap<>();
        private Map<Integer, ShipEngineControllerAPI.ShipEngineAPI> engineEntities = new HashMap<>();
        private IntervalUtil NoFire = new IntervalUtil(1f, 1f);
        private IntervalUtil Clock = new IntervalUtil(2f, 2f);
        private boolean once = true;
        private boolean Detect = false;
        private String ID = "FSD_LSWCDebuff";
        private CombatEngineAPI engine = Global.getCombatEngine();
        private int numWeaponsToDisable;
        private int count;

        public FSD_LSWCDebuff(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            numWeaponsToDisable = Math.min(5, ship.getAllWeapons().size());
            count = (int) ship.getCustomData().get("LSWCannon_Debuff");
            int WIndex = 0;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                weaponEntities.put(WIndex++, weapon);
            }

            int EIndex = 0;
            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                engineEntities.put(EIndex++, engine);
            }

            Clock.advance(amount);

            if (ship.getListenerManager().hasListenerOfClass(FSD_LSWbeam.FSD_LSWBDEBUFF.class) && !ship.getCustomData().containsKey("LSWCannon")) {
                NoFire.setInterval(1.5f, 1.5f);
                Detect = true;
            } else if (!ship.getListenerManager().hasListenerOfClass(FSD_LSWbeam.FSD_LSWBDEBUFF.class) || ship.getCustomData().containsKey("LSWCannon")) {
                NoFire.setInterval(1f, 1f);
                Detect = false;
            }
            NoFire.advance(amount);

            if (Detect && !ship.getCustomData().containsKey("LSWCannon") && count >= 5) {
                disableWeaponsAndEngines();
            }

            ship.getMutableStats().getBallisticRoFMult().modifyMult(ID, 0.75f);
            ship.getMutableStats().getMissileRoFMult().modifyMult(ID, 0.75f);
            ship.getMutableStats().getEnergyRoFMult().modifyMult(ID, 0.75f);
            ship.getMutableStats().getEngineDamageTakenMult().modifyMult(ID, 1.5f);

            if (Clock.intervalElapsed()) {
                ship.getMutableStats().getBallisticRoFMult().unmodify(ID);
                ship.getMutableStats().getMissileRoFMult().unmodify(ID);
                ship.getMutableStats().getEnergyRoFMult().unmodify(ID);
                ship.getMutableStats().getEngineDamageTakenMult().unmodify(ID);
                ship.getCustomData().remove("LSWCannon");
                ship.getListenerManager().removeListenerOfClass(FSD_LSWCDebuff.class);
            }
        }

        private void disableWeaponsAndEngines() {
            count -= 5;
            int weaponsToDisable = Math.min(numWeaponsToDisable, weaponEntities.size());
            for (int i = 0; i < weaponsToDisable; i++) {
                if (weaponEntities.isEmpty()) break;
                int weaponIndex = (int) (Math.random() * weaponEntities.size());
                WeaponAPI weapon = weaponEntities.get(weaponIndex);
                if (weapon != null) {
                    weapon.disable(false);
                    Vector2f weaponLoc = weapon.getLocation();
                    Vector2f targetLoc = ship.getLocation();
                    Vector2f offset = Vector2f.sub(weaponLoc, targetLoc, new Vector2f());

                    offset = Misc.rotateAroundOrigin(offset, -ship.getFacing());

                    FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(proj, ship, offset, 1f, 255, 0, 1.5f, false, false);
                    CombatEntityAPI e = engine.addLayeredRenderingPlugin(bloodEatEffect);
                    weaponEntities.remove(weaponIndex);
                }
            }

            int enginesToDisable = Math.min(numWeaponsToDisable - weaponsToDisable, engineEntities.size());
            for (int i = 0; i < enginesToDisable; i++) {
                if (engineEntities.isEmpty()) break;
                int engineIndex = (int) (Math.random() * engineEntities.size());
                ShipEngineControllerAPI.ShipEngineAPI engine = engineEntities.get(engineIndex);
                if (engine != null) {
                    engine.disable(false);
                    Vector2f offset = Vector2f.sub(engine.getLocation(), ship.getLocation(), new Vector2f());
                    FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(proj, ship, offset, 0.75f, 255, 0, 1.5f, false, false);
                    CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(bloodEatEffect);
                    engineEntities.remove(engineIndex);
                }
            }
            ship.setCustomData("LSWCannon_Debuff", count);
            ship.setCustomData("LSWCannon", true);
        }

    }

}

