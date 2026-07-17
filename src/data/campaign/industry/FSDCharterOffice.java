package data.campaign.industry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public class FSDCharterOffice extends BaseIndustry implements MarketImmigrationModifier {

    public static final String RELIEF_ID = "FSD_relief_charter";
    public static final String SPIRE_ID = "FSD_liaison_spire";
    public static final String SUBMARKET_ID = "FSD_liaison_market";
    public static final String FACTION_ID = "Farsight_Drive";

    public static final String MEM_STOCK_TIER = "$FSD_charterStockTier";
    public static final String MEM_IMPROVED = "$FSD_charterImproved";
    public static final String MEM_SPIRE = "$FSD_liaisonSpire";

    public static final float GAMMA_UPKEEP_REDUCTION = 5f;
    public static final float BETA_UPKEEP_REDUCTION = 10f;
    public static final float ALPHA_UPKEEP_REDUCTION = 20f;

    public static final float RELIEF_ACCESS = 0.05f;
    public static final float RELIEF_BETA_ACCESS = 0.075f;
    public static final float RELIEF_ALPHA_ACCESS = 0.10f;
    public static final float SPIRE_ACCESS = 0.10f;
    public static final float SPIRE_ALPHA_ACCESS = 0.15f;
    public static final float SPIRE_IMPROVE_ACCESS = 0.05f;

    public static final float SPIRE_DEFENSE = 100f;
    public static final float SPIRE_BETA_DEFENSE = 150f;
    public static final float SPIRE_ALPHA_DEFENSE = 200f;
    public static final float SPIRE_IMPROVE_DEFENSE = 50f;

    @Override
    public void apply() {
        super.apply(true);

        if (!isFunctional()) {
            unapply();
            return;
        }

        if (shouldMaintainStorefront()) {
            ensureStorefront();
        }

        applyCoreUpkeep();
        applyBaseModifiers();
        applyImproveModifiers();
        updateStorefrontMemory();
        market.addTransientImmigrationModifier(this);
    }

    @Override
    public void unapply() {
        super.unapply();

        if (market != null && market.getSubmarket(SUBMARKET_ID) != null) {
            market.removeSubmarket(SUBMARKET_ID);
        }

        getUpkeep().unmodifyMult("ind_core");
        market.getStability().unmodifyFlat(getModId(0));
        market.getStability().unmodifyFlat(getModId(1));
        market.getAccessibilityMod().unmodifyFlat(getModId(0));
        market.getAccessibilityMod().unmodifyFlat(getModId(2));
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(0));
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(2));
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(3));
        market.getStability().unmodifyFlat(getModId(3));
        market.getMemoryWithoutUpdate().unset(MEM_STOCK_TIER);
        market.getMemoryWithoutUpdate().unset(MEM_IMPROVED);
        market.getMemoryWithoutUpdate().unset(MEM_SPIRE);
    }

    protected boolean shouldMaintainStorefront() {
        return market != null && (market.isPlayerOwned()
                || market.getFaction() != null && FACTION_ID.equals(market.getFaction().getId()));
    }

    protected void ensureStorefront() {
        SubmarketAPI open = market.getSubmarket(SUBMARKET_ID);
        if (open == null) {
            market.addSubmarket(SUBMARKET_ID);
            open = market.getSubmarket(SUBMARKET_ID);
            if (open != null && Global.getSector() != null) {
                open.setFaction(Global.getSector().getFaction(FACTION_ID));
            }
            Global.getSector().getEconomy().forceStockpileUpdate(market);
        }
    }

    protected void applyCoreUpkeep() {
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - ALPHA_UPKEEP_REDUCTION / 100f, "Alpha Core assigned");
        } else if (Commodities.BETA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - BETA_UPKEEP_REDUCTION / 100f, "Beta Core assigned");
        } else if (Commodities.GAMMA_CORE.equals(aiCoreId)) {
            getUpkeep().modifyMult("ind_core", 1f - GAMMA_UPKEEP_REDUCTION / 100f, "Gamma Core assigned");
        } else {
            getUpkeep().unmodifyMult("ind_core");
        }
    }

    protected void applyBaseModifiers() {
        market.getStability().modifyFlat(getModId(0), 1f, getNameForModifier());
        if (!isSpire() && Commodities.ALPHA_CORE.equals(aiCoreId) && !isImproved()) {
            market.getStability().modifyFlat(getModId(1), 1f, "Alpha Core (" + getNameForModifier() + ")");
        } else {
            market.getStability().unmodifyFlat(getModId(1));
        }

        market.getAccessibilityMod().modifyFlat(getModId(0), getAccessibilityBonus(), getNameForModifier());

        if (isSpire()) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
                    .modifyFlat(getModId(0), getGroundDefenseBonus(), getNameForModifier());
        } else {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(0));
        }
    }

    protected float getAccessibilityBonus() {
        if (isSpire()) {
            return Commodities.ALPHA_CORE.equals(aiCoreId) ? SPIRE_ALPHA_ACCESS : SPIRE_ACCESS;
        }
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) return RELIEF_ALPHA_ACCESS;
        if (Commodities.BETA_CORE.equals(aiCoreId)) return RELIEF_BETA_ACCESS;
        return RELIEF_ACCESS;
    }

    protected float getGroundDefenseBonus() {
        if (!isSpire()) return 0f;
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) return SPIRE_ALPHA_DEFENSE;
        if (Commodities.BETA_CORE.equals(aiCoreId)) return SPIRE_BETA_DEFENSE;
        return SPIRE_DEFENSE;
    }

    protected void updateStorefrontMemory() {
        int tier = isSpire() ? 3 : 1;
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) tier += 2;
        else if (Commodities.BETA_CORE.equals(aiCoreId)) tier += 1;
        if (isImproved()) tier += 1;

        market.getMemoryWithoutUpdate().set(MEM_STOCK_TIER, Math.min(5, tier));
        market.getMemoryWithoutUpdate().set(MEM_IMPROVED, isImproved());
        market.getMemoryWithoutUpdate().set(MEM_SPIRE, isSpire());
    }

    protected boolean isSpire() {
        return SPIRE_ID.equals(getId());
    }

    @Override
    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.add(FACTION_ID, isSpire() ? 8f : 5f);
    }

    @Override
    public float getPatherInterest() {
        float interest = isSpire() ? 2f : 0f;
        if (Commodities.ALPHA_CORE.equals(aiCoreId)) interest += isSpire() ? 2f : 1f;
        return interest + super.getPatherInterest();
    }

    @Override
    public boolean isAvailableToBuild() {
        if (!market.hasSpaceport()) return false;
        if (Global.getSector() == null) return true;
        FactionAPI player = Global.getSector().getPlayerFaction();
        FactionAPI fsd = Global.getSector().getFaction(FACTION_ID);
        if (player != null && (player.knowsIndustry(RELIEF_ID) || player.knowsIndustry(SPIRE_ID))) return true;
        return fsd != null && fsd.getRelationshipLevel(player).isAtWorst(isSpire() ? RepLevel.FRIENDLY : RepLevel.WELCOMING);
    }

    @Override
    public boolean showWhenUnavailable() {
        return true;
    }

    @Override
    public String getUnavailableReason() {
        if (!market.hasSpaceport()) return "Requires an operational spaceport";
        return "Requires a Farsight charter specification, or strong relations with Farsight Drive";
    }

    @Override
    public String getCurrentImage() {
        return getSpec().getImageName();
    }

    @Override
    protected void applyAICoreToIncomeAndUpkeep() {
        applyCoreUpkeep();
    }

    @Override
    protected void applyAlphaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyBetaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyGammaCoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void applyNoAICoreModifiers() {
        applyBaseModifiers();
        updateStorefrontMemory();
    }

    @Override
    protected void addRightAfterDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        float opad = 10f;
        if (isSpire()) {
            tooltip.addPara("Maintains a sealed Farsight liaison compound with crystal-assisted dispatch, screened relief channels, and a controlled storefront for FSD hulls, LPCs, and rare support weapons.", opad);
        } else {
            tooltip.addPara("Opens a public Farsight relief office and limited storefront. Most stock is civilian, logistical, or common Prosody conversion equipment; military access remains tightly screened.", opad);
        }
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
            Color h = Misc.getHighlightColor();
            float opad = 10f;
            tooltip.addPara("Stability: %s", opad, h, "+1");
            tooltip.addPara("Accessibility: %s", opad, h, "+" + Math.round(getAccessibilityBonus() * 100f) + "%");
            if (isSpire()) {
                tooltip.addPara("Ground defenses: %s", opad, h, "+" + Math.round(getGroundDefenseBonus()));
            }
        }
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Alpha", ALPHA_UPKEEP_REDUCTION,
                isSpire()
                        ? "the Spire becomes a local crystal-intelligence node, too useful to remove and too opaque to trust"
                        : "the office no longer feels like an office; sealed routes answer before requests are made");
    }

    @Override
    protected void addBetaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Beta", BETA_UPKEEP_REDUCTION,
                isSpire()
                        ? "predictive convoy routing and screened procurement improve defensive coordination and stock quality"
                        : "predictive convoy routing and screened procurement improve accessibility and common military support access");
    }

    @Override
    protected void addGammaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        addCoreDescription(tooltip, mode, "Gamma", GAMMA_UPKEEP_REDUCTION,
                isSpire()
                        ? "public ledgers stay clean while sealed crystal channels hum beneath them"
                        : "a triage scheduler and relief-accounting assistant keeps queues short without opening rare military stock");
    }

    protected void addCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode, String coreName,
                                      float upkeepReduction, String detail) {
        float opad = 10f;
        Color highlight = Misc.getHighlightColor();
        String pre = coreName + "-level AI core. ";
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST) {
            pre = "Currently allocated " + coreName + "-level AI cores. ";
        }

        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
            TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
            text.addPara(pre + "Reduces upkeep by %s; " + detail + ".", 0f, highlight,
                    "" + (int) upkeepReduction + "%");
            tooltip.addImageWithText(opad);
            return;
        }

        tooltip.addPara(pre + "Reduces upkeep by %s; " + detail + ".", opad, highlight,
                "" + (int) upkeepReduction + "%");
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    protected void applyImproveModifiers() {
        if (isImproved()) {
            if (isSpire()) {
                market.getAccessibilityMod().modifyFlat(getModId(2), SPIRE_IMPROVE_ACCESS,
                        getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
                        .modifyFlat(getModId(3), SPIRE_IMPROVE_DEFENSE,
                                getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
                market.getStability().unmodifyFlat(getModId(3));
            } else {
                market.getStability().modifyFlat(getModId(3), 1f,
                        getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
                market.getStability().unmodifyFlat(getModId(1));
                market.getAccessibilityMod().unmodifyFlat(getModId(2));
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(3));
            }
        } else {
            market.getAccessibilityMod().unmodifyFlat(getModId(2));
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId(3));
            market.getStability().unmodifyFlat(getModId(3));
            applyBaseModifiers();
        }
        updateStorefrontMemory();
    }

    @Override
    public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
        Color highlight = Misc.getHighlightColor();
        if (isSpire()) {
            info.addPara("Expands the sealed sublevels into a permanent crystal-comms and security lattice, increasing accessibility by %s, adding %s ground defense, and improving storefront rarity.",
                    0f, highlight, "+5%", "+50");
        } else {
            info.addPara("Makes the relief warehouses, triage routes, and screening staff permanent, increasing stability by %s and improving storefront quantity.",
                    0f, highlight, "+1");
        }
        info.addSpacer(10f);
        super.addImproveDesc(info, mode);
    }
}
