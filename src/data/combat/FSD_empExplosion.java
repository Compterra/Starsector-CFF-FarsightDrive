package data.combat;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.entities.Ship;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import static com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry.getSizeMult;
import static com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel.getLocation;

public class FSD_empExplosion implements BeamEffectPlugin {

    protected float spawned = 0;
    protected int numToSpawn = 0;
    protected Vector2f prevMineLoc = null;
    public static String RIFTCASCADE_MINELAYER = "riftcascade_minelayer";


    public float getSizeMult() {
        float sizeMult = 1f - spawned / (float) Math.max(1, numToSpawn - 1);
        sizeMult = 0.75f + (1f - sizeMult) * 0.5f;
        return sizeMult;
    }

    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                RIFTCASCADE_MINELAYER,
                mineLoc,
                (float) Math.random() * 360f, null);

        // "spawned" does not include this mine
        float sizeMult = getSizeMult();
        mine.setCustomData(RiftCascadeMineExplosion.SIZE_MULT_KEY, sizeMult);

        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
        }

        mine.getDamage().getModifier().modifyMult("mine_sizeMult", sizeMult);


        float fadeInTime = 0.05f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);


        float liveTime = 0f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        mine.addDamagedAlready(source);
        mine.setNoMineFFConcerns(true);

        prevMineLoc = mineLoc;
    }
        private boolean count = false;
        private boolean count1 = false;

        private IntervalUtil fireInterval = new IntervalUtil(0.25f, 1.75f);
        private boolean wasZero = true;


        public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
            CombatEntityAPI target = beam.getDamageTarget();
            if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
                float dur = beam.getDamage().getDpsDuration();
                if (!wasZero) dur = 0;
                wasZero = beam.getDamage().getDpsDuration() <= 0;
                fireInterval.advance(dur);

                if (fireInterval.intervalElapsed()) {
                    ShipAPI ship = (ShipAPI) target;
                    boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                    float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.1f;
                    pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                    boolean piercedShield = hitShield && (float) Math.random() < pierceChance;

                    if (!hitShield || piercedShield) {
                        Vector2f point = beam.getRayEndPrevFrame();
                        float emp = beam.getDamage().getFluxComponent() * 1f;
                        float dam = beam.getDamage().getDamage() * 1f;
                        engine.spawnEmpArcPierceShields(
                                beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                                DamageType.ENERGY,
                                dam,
                                emp,
                                100000f,
                                "tachyon_lance_emp_impact",
                                beam.getWidth() + 9f,
                                beam.getFringeColor(),
                                beam.getCoreColor()
                        );
                    }
                }
            }

            if (target instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) target;
                if(!ship.getCustomData().containsKey("FSD_EEcounter")){
                    ship.setCustomData("FSD_EEcounter", new IntervalUtil(0.5f, 0.5f));
                }
                if (ship.getCustomData().containsKey("FSD_EEcounter")) {
                    IntervalUtil timer = (IntervalUtil) ship.getCustomData().get("FSD_EEcounter");

                for (int i = 0; i < ship.getAllWeapons().size(); i++) {
                    if (ship.getAllWeapons().get(i).isDisabled()) {
                        ship.getFluxTracker().increaseFlux(200, false);
                        if (!ship.getCustomData().containsKey("FSD_EEcounter") && !count) {
                            ship.setCustomData("FSD_EEcounter", 0f);
                            count = true;
                        }
                        timer.advance(amount);
                        if (timer.intervalElapsed() && !count1) {
                            spawnMine(ship, ship.getAllWeapons().get(i).getLocation());
                            count1 = true;
                            count = false;
                            ship.getCustomData().remove("FSD_EEcounter");
                        }
                    }
                }
                    for (int j = 0; j < ship.getEngineController().getShipEngines().size(); j++) {
                        if (ship.getEngineController().getShipEngines().get(j).isDisabled()) {
                            ship.getFluxTracker().increaseFlux(200, false);
                            if (!ship.getCustomData().containsKey("FSD_EEcounter") && !count) {
                                ship.setCustomData("FSD_EEcounter", 0f);
                                count = true;
                            }
                            timer.advance(amount);
                            if (timer.intervalElapsed() && !count1) {
                                spawnMine(ship, ship.getEngineController().getShipEngines().get(j).getLocation());
                                count1 = true;
                                count = false;
                                ship.getCustomData().remove("FSD_EEcounter");
                            }
                        }
                    }
                }
            }
            }
        }

