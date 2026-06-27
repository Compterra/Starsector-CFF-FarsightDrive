package data.hullmods;

import static data.hullmods.FSD_ReflectLight.KARMA_GAIN_MULT;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import data.hullmods.fsd_reflectlight_components.KarmaData;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FSD_UpgradeModule extends BaseHullMod {
    private static final Map<ShipAPI.HullSize, Float> RecoverSpeed = new HashMap<>();

    static {
        RecoverSpeed.put(ShipAPI.HullSize.FRIGATE, 0.02f);
        RecoverSpeed.put(ShipAPI.HullSize.DESTROYER, 0.03f);
        RecoverSpeed.put(ShipAPI.HullSize.CRUISER, 0.04f);
        RecoverSpeed.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.05f);
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("FSD_ReflectLight")) {
            KarmaData karmaData = KarmaAPI.getKarmaData(ship);
            if (karmaData != null) {
                KarmaAPI.modifyKarmaMaxMultiplier(ship, "FSD_UpgradeModule", 2.0f);

                KarmaAPI.addGainKarmaMultiplier(ship, "FSD_UpgradeModule", 1.5f);

                float rangeMultiplier = 1.5f;
                if (ship.getVariant().hasHullMod("FSD_RLIntranet")) {
                    rangeMultiplier = 2.0f;
                } else if (ship.getVariant().hasHullMod("FSD_DeepphasedInterface")) {
                    rangeMultiplier = 1.33f;
                }
                KarmaAPI.addRangeMultiplier(ship, "FSD_UpgradeModule", rangeMultiplier);
            }
        }
    }

    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1.5f);
        stats.getSuppliesPerMonth().modifyMult(id, 1.5f);
        stats.getSuppliesToRecover().modifyMult(id, 1.5f);
        stats.getSystemUsesBonus().modifyFlat(id, 1f);
        if (isSMod(stats)) {
            stats.getSuppliesPerMonth().modifyMult(id + "_smod", 0.9f);
            stats.getSuppliesToRecover().modifyMult(id + "_smod", 0.9f);
        }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        ensureModifiersApplied(ship);
        SystemRecover(ship);
    }

    private static final java.util.Set<String> appliedShips = new java.util.HashSet<>();

    /**
     */
    private void ensureModifiersApplied(ShipAPI ship) {
        if (ship == null) return;

        if (!hasKarmaSystem(ship)) return;

        String shipId = ship.getId();

        if (appliedShips.contains(shipId)) return;

        KarmaData karmaData = KarmaAPI.getKarmaData(ship);
        if (karmaData == null) return;

        KarmaAPI.modifyKarmaMaxMultiplier(ship, "FSD_UpgradeModule", 2.0f);

        KarmaAPI.addGainKarmaMultiplier(ship, "FSD_UpgradeModule", 1.5f);

        float rangeMultiplier = 1.5f;
        if (ship.getVariant().hasHullMod("FSD_RLIntranet")) {
            rangeMultiplier = 2.0f;
        } else if (ship.getVariant().hasHullMod("FSD_DeepphasedInterface")) {
            rangeMultiplier = 1.33f;
        }
        KarmaAPI.addRangeMultiplier(ship, "FSD_UpgradeModule", rangeMultiplier);

        appliedShips.add(shipId);
    }

    /**
     */
    public static void clearAppliedShips() {
        appliedShips.clear();
    }

    public void SystemRecover(ShipAPI ship) {
        float karma = KarmaAPI.getKarma(ship);
        Float recoverSpeedForSize = RecoverSpeed.get(ship.getHullSize());
        if (recoverSpeedForSize == null) {
            recoverSpeedForSize = 0.03f;
        }
        float karmaBonus = karma * recoverSpeedForSize;
        MutableShipStatsAPI stats = ship.getMutableStats();
        String modId = ship.getId() + "_FSD_UpgradeModule";
        stats.getSystemCooldownBonus().modifyMult(modId, 1 + karmaBonus);
        stats.getSystemRegenBonus().modifyMult(modId, 1 + karmaBonus);
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
        tooltip.addPara("Reprograms the crystal into an elite battlefield core. Powerful, invasive, and ruinously expensive.", pads);
        tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
        tooltip.addPara(
                "Based on ship size, every %s accumulated karma accelerates ship-system recharge and cooldown speed by %s.\nShip systems with charges gain %s extra charge.\n",
                pads, Misc.getTextColor(), y, "10%", "2%/3%/4%/5%", "1");
        tooltip.addPara(
                "Maximum karma capacity rises from %s to %s.\nKarma gained from kills increases from %s to %s.\nPhase entropy field radius increases by %s.",
                pads, Misc.getTextColor(), y, "100%", "200%", "10%/20%/30%/40%", "30%/40%/50%/60%", "50%");
        tooltip.addSectionHeading("Drawbacks", Alignment.MID, pad);
        tooltip.addPara("Deployment cost and maintenance cost increase by %s, and this module cannot be installed on %s.\n %s", pads, r, "+50%", "frigates", "Can only be installed on Farsight Drive ships!");
        tooltip.addPara("S-mod: Oracle integration seals the command lattice, reducing the deployment and maintenance penalties to %s and improving ship-system charge regeneration by %s.", pads, y, "+35%", "10%");
    }

    /**
     */
    private boolean hasKarmaSystem(ShipAPI ship) {
        return ship.getVariant().hasHullMod("FSD_ReflectLight")
                || ship.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion");
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) {
            return false;
        }
        return hasKarmaSystem(ship);
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) {
            return "Cannot be installed on frigates!";
        }
        if (!hasKarmaSystem(ship)) {
            return "Requires a ship with Reflecting-Light Crystal or Secondary Crystal Overgrowth!";
        }
        return null;
    }
}
