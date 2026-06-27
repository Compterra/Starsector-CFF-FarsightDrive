package data.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import data.campaign.industry.FSDCharterOffice;

public class FSDLiaisonMarket extends BaseSubmarketPlugin {

    @Override
    public void init(SubmarketAPI submarket) {
        this.submarket = submarket;
        this.market = submarket.getMarket();
    }

    @Override
    public float getTariff() {
        return 0.35f;
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;
            pruneWeapons(0f);

            int tier = getStockTier();
            int size = Math.max(3, market.getSize());
            boolean spire = isSpire();
            boolean improved = isImproved();

            int weaponMaxTier = Math.min(5, 1 + tier);
            int fighterMaxTier = Math.min(5, 1 + tier);
            int weapons = 3 + size * 2 + tier * 2;
            int fighters = (spire ? 2 : 1) + tier / 2;
            int hullmods = 1 + Math.max(1, tier / 2);
            if (improved) {
                weapons += 2;
                fighters += 1;
                hullmods += 1;
            }

            addWeapons(weapons, weapons + 2 + tier, weaponMaxTier, FSDCharterOffice.FACTION_ID);
            addFighters(fighters, fighters + (spire ? 2 : 1), fighterMaxTier, FSDCharterOffice.FACTION_ID);
            addHullMods(hullmods, hullmods + 1);

            getCargo().getMothballedShips().clear();
            FactionDoctrineAPI doctrine = submarket.getFaction().getDoctrine().clone();
            doctrine.setCombatFreighterProbability(spire ? 0.25f : 0.45f);
            doctrine.setShipSize(Math.min(4, spire ? 3 + tier / 3 : 2 + tier / 4));

            float combat = spire ? 45f + size * 12f + tier * 10f : 15f + size * 6f + tier * 3f;
            float logistics = spire ? 25f + size * 5f : 45f + size * 8f;
            addShips(FSDCharterOffice.FACTION_ID,
                    combat,
                    logistics,
                    15f + tier * 3f,
                    20f + tier * 2f,
                    spire ? 8f : 15f,
                    25f + tier * 5f,
                    null,
                    spire ? 0.15f : -0.05f,
                    ShipPickMode.PRIORITY_THEN_ALL,
                    doctrine);
        }

        getCargo().sort();
    }

    protected int getStockTier() {
        Object tier = market.getMemoryWithoutUpdate().get(FSDCharterOffice.MEM_STOCK_TIER);
        if (tier instanceof Number) {
            return Math.max(1, Math.min(5, ((Number) tier).intValue()));
        }
        return 1;
    }

    protected boolean isSpire() {
        return Boolean.TRUE.equals(market.getMemoryWithoutUpdate().get(FSDCharterOffice.MEM_SPIRE));
    }

    protected boolean isImproved() {
        return Boolean.TRUE.equals(market.getMemoryWithoutUpdate().get(FSDCharterOffice.MEM_IMPROVED));
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return action == TransferAction.PLAYER_BUY && !hasAccess();
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return true;
        return action == TransferAction.PLAYER_BUY && !hasAccess();
    }

    protected boolean hasAccess() {
        FactionAPI player = Global.getSector().getPlayerFaction();
        FactionAPI fsd = Global.getSector().getFaction(FSDCharterOffice.FACTION_ID);
        return fsd != null && fsd.getRelationshipLevel(player).isAtWorst(RepLevel.WELCOMING);
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return "The liaison storefront only releases screened Farsight stock.";
        if (!hasAccess()) return "Access requires Welcoming relations with Farsight Drive.";
        return "Liaison storefront access denied.";
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) return "The liaison storefront only releases screened Farsight stock.";
        if (!hasAccess()) return "Access requires Welcoming relations with Farsight Drive.";
        return "Liaison storefront access denied.";
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
