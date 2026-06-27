package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_LSWbeam implements BeamEffectPlugin {
    private IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private IntervalUtil clockInterval = new IntervalUtil(0.2f, 0.2f);
    private boolean wasZero = true;
    private static final float SPEED_REDUCTION = 0.3f;
    private static final float MOBILITY_REDUCTION = 0.5f;
    private static final float WEAPON_REDUCTION = 0.25f;
    private float dam = 30;
    private float emp = 90;
    private Color coreColor = new Color(217, 217, 250, 255);
    private Color fringeColor = new Color(177, 0, 0, 255);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI source =beam.getSource();

        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            ShipAPI ship = (ShipAPI) target;
            MutableShipStatsAPI stats = ship.getMutableStats();
            switch (ship.getHullSize()){
                case FRIGATE:
                    fireInterval.setInterval(4f, 4f);
                    break;
                case DESTROYER:
                    fireInterval.setInterval(3.5f, 3.5f);
                    break;
                case CRUISER:
                    fireInterval.setInterval(3f, 3f);
                    break;
                case CAPITAL_SHIP:
                    fireInterval.setInterval(2f, 2f);
                    break;
                default:
                    break;
            }

            if(!((ShipAPI) target).hasListenerOfClass(FSD_LSWBDEBUFF.class)){
                ((ShipAPI) target).addListener(new FSD_LSWBDEBUFF((ShipAPI) target));
            }

            clockInterval.advance(dur);
            if(clockInterval.intervalElapsed()){
                CombatEntityAPI entity1 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_PDLightLauncher",beam.getWeapon().getLocation(),beam.getWeapon().getCurrAngle()+30,source.getVelocity());
                CombatEntityAPI entity2 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_PDLightLauncher",beam.getWeapon().getLocation(),beam.getWeapon().getCurrAngle()+60,source.getVelocity());
                CombatEntityAPI entity3 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_PDLightLauncher",beam.getWeapon().getLocation(),beam.getWeapon().getCurrAngle()-30,source.getVelocity());
                CombatEntityAPI entity4 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_PDLightLauncher",beam.getWeapon().getLocation(),beam.getWeapon().getCurrAngle()-60,source.getVelocity());
                if(entity1 instanceof DamagingProjectileAPI){
                    DamagingProjectileAPI proj1 = (DamagingProjectileAPI) entity1;
                    proj1.getDamage().setDamage(dam*4);
                }
                if(entity2 instanceof DamagingProjectileAPI){
                    DamagingProjectileAPI proj2 = (DamagingProjectileAPI) entity2;
                    proj2.getDamage().setDamage(dam*4);
                }
                if(entity3 instanceof DamagingProjectileAPI){
                    DamagingProjectileAPI proj3 = (DamagingProjectileAPI) entity3;
                    proj3.getDamage().setDamage(dam*4);
                }
                if(entity4 instanceof DamagingProjectileAPI){
                    DamagingProjectileAPI proj4 = (DamagingProjectileAPI) entity4;
                    proj4.getDamage().setDamage(dam*4);
                }
            }
            if(ship.getFluxLevel()>=0.5f){
                source.getFluxTracker().decreaseFlux(dam*8*amount);
            }

            fireInterval.advance(dur);
            if (!fireInterval.intervalElapsed()) {
                boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                float pierceChance = ((ShipAPI)target).getHardFluxLevel() - 0.3f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShield && (float) Math.random() < pierceChance;
                Vector2f point = beam.getRayEndPrevFrame();

                if(fireInterval.getElapsed()<=0.2f&&ship.getHullSize()== ShipAPI.HullSize.CAPITAL_SHIP){
                    if(Math.random()<0.1||piercedShield){

                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                    if(Math.random()<0.2&&!hitShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*5,
                                emp*10,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                fringeColor,
                                coreColor
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.2f&&ship.getHullSize()== ShipAPI.HullSize.CRUISER){
                    if(Math.random()<0.2||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                    if(Math.random()<0.4&&!hitShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*5,
                                emp*10,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                fringeColor,
                                coreColor
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.35f&&ship.getHullSize()== ShipAPI.HullSize.DESTROYER){
                    if(Math.random()<0.3||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp*2,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                    if(Math.random()<0.6&&!hitShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*5,
                                emp*10,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                fringeColor,
                                coreColor
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.4f&&ship.getHullSize()== ShipAPI.HullSize.FRIGATE){
                    if(Math.random()<0.125||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*0.75f,
                                emp*3,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                    if(Math.random()<0.25&&!hitShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*3,
                                emp*3,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                fringeColor,
                                coreColor
                        );
                    }
                }
            }
        }


    }

    public static class FSD_LSWBDEBUFF implements AdvanceableListener {
        private IntervalUtil timer = new IntervalUtil(15f,15f);
        private ShipAPI ship;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public FSD_LSWBDEBUFF(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public void advance(float amount) {

            timer.advance(amount);
            if(!timer.intervalElapsed()) {
                applyDebuff(ship);
            }else {
                removeDebuff(ship);
                ship.getListenerManager().removeListenerOfClass(FSD_LSWBDEBUFF.class);
            }
        }
        private void applyDebuff(ShipAPI ship) {
            MutableShipStatsAPI stats = ship.getMutableStats();

            stats.getMaxSpeed().modifyMult(ship.getId(), 1 - SPEED_REDUCTION);

            stats.getAcceleration().modifyMult(ship.getId(), 1 - MOBILITY_REDUCTION);
            stats.getDeceleration().modifyMult(ship.getId(), 1 - MOBILITY_REDUCTION);
            stats.getTurnAcceleration().modifyMult(ship.getId(), 1 - MOBILITY_REDUCTION);
            stats.getMaxTurnRate().modifyMult(ship.getId(), 1 - MOBILITY_REDUCTION);
            stats.getBallisticWeaponDamageMult().modifyMult(ship.getId(), 1-WEAPON_REDUCTION);
            stats.getBeamWeaponDamageMult().modifyMult(ship.getId(), 1-WEAPON_REDUCTION);
            stats.getMissileWeaponDamageMult().modifyMult(ship.getId(), 1-WEAPON_REDUCTION);
            stats.getEnergyWeaponDamageMult().modifyMult(ship.getId(), 1-WEAPON_REDUCTION);
        }

        private void removeDebuff(ShipAPI ship) {
            MutableShipStatsAPI stats = ship.getMutableStats();

            stats.getMaxSpeed().unmodify(ship.getId());

            stats.getAcceleration().unmodify(ship.getId());
            stats.getDeceleration().unmodify(ship.getId());
            stats.getTurnAcceleration().unmodify(ship.getId());
            stats.getMaxTurnRate().unmodify(ship.getId());
        }
    }
}
