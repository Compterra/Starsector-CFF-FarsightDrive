package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_Avision implements BeamEffectPlugin {
    private IntervalUtil fireInterval = new IntervalUtil(0.5f, 0.5f);
    private IntervalUtil clockInterval = new IntervalUtil(0.75f, 0.75f);
    private boolean wasZero = true;
    private static final float SPEED_REDUCTION = 0.15f;
    private static final float MOBILITY_REDUCTION = 0.3f;
    private static final float WEAPON_REDUCTION = 0.1f;
    private float dam = 50;
    private float emp = 100;
    private final Color CoreColor = new Color(248, 228, 228,255);
    private final Color FringeColor_FRIGATE = new Color(12, 234, 253,255);
    private final Color FringeColor_DESTROYER = new Color(255, 106, 0,255);
    private final Color FringeColor_CRUISER = new Color(64, 0, 255,255);
    private final Color FringeColor_CAPITAL_SHIP = new Color(255, 0, 0,255);

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
                    fireInterval.setInterval(2f, 2f);
                    break;
                case DESTROYER:
                    fireInterval.setInterval(1.75f, 1.75f);
                    break;
                case CRUISER:
                    fireInterval.setInterval(1.5f, 1.5f);
                    break;
                case CAPITAL_SHIP:
                    fireInterval.setInterval(1f, 1f);
                    break;
                default:
                    break;
            }

            if(!((ShipAPI) target).hasListenerOfClass(FSD_AvisionDEBUFF.class)){
                ((ShipAPI) target).addListener(new FSD_AvisionDEBUFF((ShipAPI) target));
            }

            clockInterval.advance(dur);
            if(source.getFluxLevel()>=0.5f&&clockInterval.intervalElapsed()){
                CombatEntityAPI entity1 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_LightLauncher",beam.getFrom(),beam.getWeapon().getCurrAngle()+30,source.getVelocity());
                CombatEntityAPI entity2 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_LightLauncher",beam.getFrom(),beam.getWeapon().getCurrAngle()+60,source.getVelocity());
                CombatEntityAPI entity3 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_LightLauncher",beam.getFrom(),beam.getWeapon().getCurrAngle()-30,source.getVelocity());
                CombatEntityAPI entity4 = engine.spawnProjectile(source,beam.getWeapon(),"FSD_LightLauncher",beam.getFrom(),beam.getWeapon().getCurrAngle()-60,source.getVelocity());
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
                source.getFluxTracker().decreaseFlux(dam*8);
            }
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
            float pierceChance = ((ShipAPI)target).getHardFluxLevel() - 0.4f;
            pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
            Vector2f point = beam.getRayEndPrevFrame();
            fireInterval.advance(dur);
            if (!fireInterval.intervalElapsed()) {
                if(!hitShield&&Math.random()<pierceChance*0.25f&&fireInterval.getElapsed()<=0.1f) {
                    switch (ship.getHullSize()){
                        case FRIGATE:
                            engine.spawnEmpArc(
                                    beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                    DamageType.ENERGY,
                                    dam,
                                    emp*3,
                                    100000f,
                                    "tachyon_lance_emp_impact",
                                    beam.getWidth() - 5f,
                                    FringeColor_FRIGATE,
                                    CoreColor
                            );
                            break;
                        case DESTROYER:
                            engine.spawnEmpArc(
                                    beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                    DamageType.ENERGY,
                                    dam*2,
                                    emp*2,
                                    100000f,
                                    "tachyon_lance_emp_impact",
                                    beam.getWidth() - 2f,
                                    FringeColor_DESTROYER,
                                    CoreColor
                            );
                            break;
                        case CRUISER:
                            engine.spawnEmpArc(
                                    beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                    DamageType.ENERGY,
                                    dam*3,
                                    emp*2,
                                    100000f,
                                    "tachyon_lance_emp_impact",
                                    beam.getWidth() + 2f,
                                    FringeColor_CRUISER,
                                    CoreColor
                            );
                            break;
                        case CAPITAL_SHIP:
                            engine.spawnEmpArc(
                                    beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                    DamageType.ENERGY,
                                    dam*4,
                                    emp*3f,
                                    100000f,
                                    "tachyon_lance_emp_impact",
                                    beam.getWidth() + 9f,
                                    FringeColor_CAPITAL_SHIP,
                                    CoreColor
                            );
                            break;
                    }
                }
            }
            if (!fireInterval.intervalElapsed()) {

                boolean piercedShield = hitShield && (float) Math.random() < pierceChance;
                if(fireInterval.getElapsed()<=0.2f&&ship.getHullSize()== ShipAPI.HullSize.CAPITAL_SHIP){
                    if(Math.random()<0.015||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() - 5f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.2f&&ship.getHullSize()== ShipAPI.HullSize.CRUISER){
                    if(Math.random()<0.0225||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() - 2f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.35f&&ship.getHullSize()== ShipAPI.HullSize.DESTROYER){
                    if(Math.random()<0.03||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 2f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                }
                if(fireInterval.getElapsed()<=0.4f&&ship.getHullSize()== ShipAPI.HullSize.FRIGATE){
                    if(Math.random()<0.0375||piercedShield){
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam*0.75f,
                                emp*0.45f,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor().darker(),
                                beam.getCoreColor().brighter()
                        );
                    }
                }
            }
        }


    }
    public static class FSD_AvisionDEBUFF implements AdvanceableListener{
        private IntervalUtil timer = new IntervalUtil(15f,15f);
        private ShipAPI ship;
        public FSD_AvisionDEBUFF(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public void advance(float amount) {
            timer.advance(amount);
            if(!timer.intervalElapsed()) {
                applyDebuff(ship);
            }else {
                removeDebuff(ship);
                ship.getListenerManager().removeListenerOfClass(FSD_AvisionDEBUFF.class);
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

