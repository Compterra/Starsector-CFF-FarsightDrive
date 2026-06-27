package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;

public class FSD_RLMini extends BaseHullMod {
    private IntervalUtil timer = new IntervalUtil(0.25f, 0.25f);
    private boolean start = false;
    private float cooldown = 0.5f;
    private Color COLOR = new Color(154, 4, 49, 255);
    private float TIMEBUFF = 1.1f;
    private float RepairMult = 2f;
    private ShipAPI ship;
    private String id;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getTimeMult().modifyMult(id, TIMEBUFF);
        stats.getCombatEngineRepairTimeMult().modifyMult(id, RepairMult);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, RepairMult);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        this.ship = ship;
        float maxHP = ship.getMaxHitpoints();
        float perflameHP = maxHP * 0.03f * amount;
        ship.setHitpoints(Math.min(maxHP, ship.getHitpoints() + perflameHP));
//        if (source != null && source.isAlive()) {
//            if (source.getVariant().hasHullMod("FSD_ReflectLight")
//                    && source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//                ship.setHitpoints(Math.min(maxHP, ship.getHitpoints() + perflameHP));
//                stats.getBallisticRoFMult().modifyMult(id, 1.15f);
//                stats.getEnergyRoFMult().modifyMult(id, 1.15f);
//                stats.getMissileRoFMult().modifyMult(id, 1.15f);
//                if (ship.getSystem().getEffectLevel() >= 1) {
//                    start = true;
//                }
//                if (start) {
//                    timer.advance(amount);
//                    if (!timer.intervalElapsed()) {
//                        stats.getTimeMult().modifyMult(id, 2f);
//                        ship.addAfterimage(
//                                COLOR,
//                                (float) 0,
//                                0,
//                                -ship.getVelocity().x,
//                                -ship.getVelocity().y,
//                                2,
//                                0,
//                                0.075f,
//                                0.125f,
//                                true,
//                                false,
//                                false);
//                    } else {
//                        stats.getTimeMult().unmodify(id);
//                        start = false;
//                    }
//                }
//            }
//            if (source.getParentStation() != null) {
//                ShipAPI parent = source.getParentStation();
//                if (parent.getVariant().hasHullMod("FSD_RLIntranet")
//                        && parent.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//                    ship.setHitpoints(Math.min(maxHP, ship.getHitpoints() + perflameHP));
//                    stats.getBallisticRoFMult().modifyMult(id, 1.15f);
//                    stats.getEnergyRoFMult().modifyMult(id, 1.15f);
//                    stats.getMissileRoFMult().modifyMult(id, 1.15f);
//                    stats.getTimeMult().modifyMult(id, 1.15f);
//                    if (ship.getSystem().getEffectLevel() >= 1) {
//                        start = true;
//                    }
//                    if (start) {
//                        timer.advance(amount);
//                        if (!timer.intervalElapsed()) {
//                            stats.getTimeMult().modifyMult(id, 2f);
//                            ship.addAfterimage(
//                                    COLOR,
//                                    (float) 0,
//                                    0,
//                                    -ship.getVelocity().x,
//                                    -ship.getVelocity().y,
//                                    2,
//                                    0,
//                                    0.075f,
//                                    0.125f,
//                                    true,
//                                    false,
//                                    false);
//                        } else {
//                            stats.getTimeMult().unmodify(id);
//                            start = false;
//                        }
//                    }
//                }
//            }
//            if (source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//                stats.getBallisticRoFMult().modifyMult(id, 1.15f);
//                stats.getEnergyRoFMult().modifyMult(id, 1.15f);
//                stats.getMissileRoFMult().modifyMult(id, 1.15f);
//            }
//            ;
//            if (!source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//                stats.getFluxDissipation().modifyPercent(id, -DEBUFF * 100f);
//                stats.getEnergyWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f);
//                stats.getMissileWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f);
//                stats.getWeaponDamageTakenMult().modifyPercent(id, DEBUFF * 100f);
//                stats.getBeamWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f);
//                stats.getBallisticWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f);
//                stats.getMaxSpeed().modifyPercent(id, -DEBUFF * 100f);
//                stats.getAcceleration().modifyPercent(id, -DEBUFF * 100f);
//                stats.getDeceleration().modifyPercent(id, -DEBUFF * 100f);
//                stats.getTurnAcceleration().modifyPercent(id, -DEBUFF * 100f);
//                stats.getMaxTurnRate().modifyPercent(id, -DEBUFF * 100f);
//            }
//            if (source.getVariant().hasHullMod("converted_hangar")
//                    && !source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//                stats.getFluxDissipation().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getEnergyWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getMissileWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getWeaponDamageTakenMult().modifyPercent(id, DEBUFF * 100f * 2);
//                stats.getBeamWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getBallisticWeaponDamageMult().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getMaxSpeed().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getAcceleration().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getDeceleration().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getTurnAcceleration().modifyPercent(id, -DEBUFF * 100f * 2);
//                stats.getMaxTurnRate().modifyPercent(id, -DEBUFF * 100f * 2);
//            }
//        }
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        ship = this.ship;
        float pad = 10.0f;
        float pads = 3.0f;
        Color c = Misc.getHighlightColor();
        Color r = Misc.getNegativeHighlightColor();
        tooltip.addPara("A stabilized miniature Reflecting-Light Crystal carried by advanced Farsight Drive fighters.", pads, Misc.getTextColor(), c, "");
        tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
        tooltip.addPara("Fighter timeflow increased by %s and hull restored by %s per second in combat.", pads, Misc.getTextColor(), c, "10%", "3%");
        tooltip.addPara("Weapon and engine repair rate increased by %s.", pads, Misc.getTextColor(), c, "100%");
    }
}
