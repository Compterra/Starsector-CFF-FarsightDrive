package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FSD_LimitationOfTissueHyperplasia extends BaseHullMod {
    private static final Map<ShipAPI.HullSize, Float> REPAIR_SPEED_MULT = new HashMap<>();

    static {
        REPAIR_SPEED_MULT.put(ShipAPI.HullSize.FIGHTER, 0.00f);
        REPAIR_SPEED_MULT.put(ShipAPI.HullSize.FRIGATE, 0.016f);
        REPAIR_SPEED_MULT.put(ShipAPI.HullSize.DESTROYER, 0.012f);
        REPAIR_SPEED_MULT.put(ShipAPI.HullSize.CRUISER, 0.012f);
        REPAIR_SPEED_MULT.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.008f);
    }
    private static final Set<String> INCOMPATIBLE_HULLMODS = new HashSet<>();

    static {
//        INCOMPATIBLE_HULLMODS.add("FSD_OverrunPosition");
        INCOMPATIBLE_HULLMODS.add("FSD_IonBurst");
    }

    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        for (String hullmodId : INCOMPATIBLE_HULLMODS) {
            if (stats.getVariant().getHullMods().contains(hullmodId)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(
                        stats.getVariant(), hullmodId, "FSD_LimitationOfTissueHyperplasia");
            }
        }

        stats.getDynamic().getStat(Stats.NON_COMBAT_CREW_LOSS_MULT).modifyMult(id, 2f);
        stats.getArmorBonus().modifyMult(id, 0.5f);
        stats.getMinArmorFraction().modifyMult(id, 0.5f);
        stats.getHullBonus().modifyMult(id, 3f);
        if (isSMod(stats)) {
            stats.getArmorBonus().modifyMult(id + "_smod", 1.1f);
            stats.getMinArmorFraction().modifyMult(id + "_smod", 1.1f);
            stats.getDynamic().getStat(Stats.NON_COMBAT_CREW_LOSS_MULT).modifyMult(id + "_smod", 0.75f);
        }
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        ShipVariantAPI variant = ship.getVariant();
//        for (String hullmodId : INCOMPATIBLE_HULLMODS) {
////            if (ship.getVariant().getHullMods().contains(hullmodId)) {
////                MagicIncompatibleHullmods.removeHullmodWithWarning(
////                        ship.getVariant(), hullmodId, "FSD_LimitationOfTissueHyperplasia");
////            }
//
//        }
        if(variant.hasHullMod("FSD_LimitationOfTissueHyperplasia")){
            variant.removeMod("FSD_IonBurst");
        }
        if(variant.hasHullMod("FSD_IonBurst")){
            variant.removeMod("FSD_LimitationOfTissueHyperplasia");
        }
        ship.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);
    }

    /**
     */
    private boolean hasKarmaSystem(ShipAPI ship) {
        return ship.getVariant().hasHullMod("FSD_ReflectLight") 
            || ship.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (!hasKarmaSystem(ship)) return false;
        for (String hullmodId : INCOMPATIBLE_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(hullmodId)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!hasKarmaSystem(ship)) return "Requires a ship with Reflecting-Light Crystal or Secondary Crystal Overgrowth!";
        for (String hullmodId : INCOMPATIBLE_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(hullmodId)) {
                String hullmodName = Global.getSettings().getHullModSpec(hullmodId).getDisplayName();
                return "The crystal has entered a high-activity state,"+"cannot install \"" + hullmodName + "\".";
            }
        }
        return null;
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        float karma = KarmaAPI.getKarma(ship);
        String ID = ship.getId() + "_FSD_LimitationOfTissueHyperplasia";
        handleShipRepair(ship, amount);
//        Global.getCombatEngine().addFloatingText(ship.getLocation(),
//                ""+ship.getHullSpec().getHitpoints(),
//                20f,
//                Color.WHITE,
//                ship,
//                1f,
//                1f);
        if(ship.getHitpoints()<=ship.getHullSpec().getHitpoints()){
            stats.getEngineDamageTakenMult().unmodify();
            stats.getArmorDamageTakenMult().unmodify();
            stats.getShieldDamageTakenMult().unmodify();
            stats.getHullDamageTakenMult().unmodify();
        }
        if(ship.getHitpoints()>ship.getHullSpec().getHitpoints()&&
                ship.getHitpoints()<=ship.getHullSpec().getHitpoints()*2f){
            stats.getEngineDamageTakenMult().modifyMult(ID, Math.min(1.5f, 1.5f-0.25f*karma));
            stats.getArmorDamageTakenMult().modifyMult(ID, Math.min(1.5f, 1.5f-0.25f*karma));
            stats.getHullDamageTakenMult().modifyMult(ID, Math.min(1.5f, 1.5f-0.25f*karma));
            stats.getShieldDamageTakenMult().modifyMult(ID, Math.min(1.5f, 1.5f-0.25f*karma));
        }
        if(ship.getHitpoints()>ship.getHullSpec().getHitpoints()*2f&&
            ship.getHitpoints()<=ship.getHullSpec().getHitpoints()*3f){
            stats.getEngineDamageTakenMult().modifyMult(ID, Math.min(1.75f, 1.75f-0.25f*karma));
            stats.getArmorDamageTakenMult().modifyMult(ID, Math.min(1.75f, 1.75f-0.25f*karma));
            stats.getHullDamageTakenMult().modifyMult(ID, Math.min(1.75f, 1.75f-0.25f*karma));
            stats.getShieldDamageTakenMult().modifyMult(ID, Math.min(1.75f, 1.75f-0.25f*karma));
        }
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(
            ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            boolean isForModSpec) {
        float pad = 10.0f;
        float pads = 3.0f;
        Color y = Misc.getHighlightColor();
        Color r = Misc.getNegativeHighlightColor();
        tooltip.addPara("Removes crystal growth suppression and lets the core rebuild the hull on instinct. Commanders should consider the structural consequences carefully.", pads);
        tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
        tooltip.addPara(
                "Maximum hull becomes %s of its initial value.\nThe hull regeneration cap becomes %s of base hull, and regeneration speed increases by %s.",
                pads, Misc.getTextColor(), y, "300%", "400%", "100%");
        tooltip.addPara("Based on karma level, hull regeneration speed can increase by up to %s.",pads,y,"50%");
//        tooltip.addPara(
//                "Adds minimum armor up to %s of initial armor based on ship size. Added minimum armor scales with karma and starts decreasing from %s karma based on ship size.",
//                pads, Misc.getTextColor(), y, "60%/40%/20%/10%", "25%/50%/75%/100%");
        tooltip.addSectionHeading("Drawbacks", Alignment.MID, pad);
        tooltip.addPara(
                "%s. Armor is reduced by %s and minimum armor by %s.\nWhen hull exceeds the frame's original limit, damage taken increases by %s.\nWhen hull exceeds the suppression limit (2x initial hull), damage taken increases by %s.",
                pads, r,
                "Disables shields", "50%","50%", "50%","75%","200%");
        tooltip.addPara("Based on karma level, grants up to %s damage reduction when overgrown structure exceeds the frame.",pads,y,"25%");
        tooltip.addPara("Lifted suppression makes the ship environment extremely dangerous; combat crew losses increase by %s.",pads,r,"100%");
    }

    private void handleShipRepair(ShipAPI ship, float amount) {
        float karma = KarmaAPI.getKarma(ship);
        if (ship.getPeakTimeRemaining() >= 0f) {
            ShipAPI.HullSize hullSize = ship.getHullSize();
            float maxHP = ship.getMaxHitpoints();
            boolean phased = ship.isPhased();
            float totalRepair = 0f;
            if (ship.getCustomData().containsKey("FSD_TotalRepair")) {
                totalRepair = (float) ship.getCustomData().get("FSD_TotalRepair");
            }
            float repairSpeedMult;
            if (ship.getCustomData().containsKey("FSD_REPAIR_SPEED_MULT")) {
                @SuppressWarnings("unchecked")
                Map<ShipAPI.HullSize, Float> customRepairMult =
                        (Map<ShipAPI.HullSize, Float>) ship.getCustomData().get("FSD_REPAIR_SPEED_MULT");
                repairSpeedMult =
                        customRepairMult.containsKey(hullSize)
                                ? customRepairMult.get(hullSize)
                                : REPAIR_SPEED_MULT.get(hullSize);
            } else {
                repairSpeedMult = REPAIR_SPEED_MULT.get(hullSize);
            }
            float repairCapMultiplier;
            switch (hullSize) {
                case FRIGATE:
                    repairCapMultiplier = 4f;
                    break;
                case DESTROYER:
                    repairCapMultiplier = 4f;
                    break;
                case CRUISER:
                    repairCapMultiplier = 4f;
                    break;
                case CAPITAL_SHIP:
                    repairCapMultiplier = 4f;
                    break;
                default:
                    repairCapMultiplier = 1.0f;
                    break;
            }
            float maxRepairAmount = maxHP * repairCapMultiplier;
            if (totalRepair < maxRepairAmount) {
                float residualHP = maxRepairAmount - totalRepair;
                float repairMultiplier = phased ? 1.5f : 1.0f;
                float realRepair = repairMultiplier * amount * repairSpeedMult * maxHP*Math.max(1, 1.5f*karma);
                realRepair = Math.min(realRepair, residualHP);
                totalRepair += realRepair;
                ship.setHitpoints(Math.min(ship.getHitpoints() + realRepair, maxHP));
                ship.setCustomData("FSD_TotalRepair", totalRepair);
            }
            if (!ship.isAlive()) {
                ship.setCustomData("FSD_TotalRepair", 0f);
            }
        }
    }
}
