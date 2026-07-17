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

import java.util.List;

public class FSD_PIMarket extends BaseSubmarketPlugin {

    @Override
    public void init(SubmarketAPI submarket) {
        this.submarket = submarket;
        this.market = submarket.getMarket();
    }
    public static String txt(String id) {
        return Global.getSettings().getString("campaign", id);
    }

    @Override
    public float getTariff() {
        return 0.35f;
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;
        //all
        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;
            pruneWeapons(0f);

            int weapons = 4 + Math.max(0, market.getSize()) * 5;
            int fighterNum = 1 + market.getSize();
            int hullmods = 1 + market.getSize();

            FactionAPI TDB_Market = null;
            List<FactionAPI> Factions = Global.getSector().getAllFactions();
            for (FactionAPI F : Factions) {
                if (F.getId().equals("PastItem")) {
                    TDB_Market = F;
                }
            }


            addFighters(fighterNum, fighterNum, 3, "PastItem"); //min number, max number, max tier, faction id
            addWeapons(weapons, weapons + 2, 3, "PastItem");
            addHullMods(hullmods, hullmods);

            getCargo().getMothballedShips().clear();
            FactionDoctrineAPI doctrineOverrided = submarket.getFaction().getDoctrine().clone();
            doctrineOverrided.setCombatFreighterProbability(0.25f);
            doctrineOverrided.setShipSize(3);

            addShips("PastItem",
                    100f, // combat
                    30f, // freighter
                    0f, // tanker
                    0f, // transport
                    0f, // liner
                    0f, // utilityPts
                    null, // qualityOverride
                    0f, // qualityMod
                    ShipPickMode.PRIORITY_THEN_ALL,//FactionAPI.ShipPickMode modeOverride, at what priority to pick ship in all availables
                    doctrineOverrided);// FactionDoctrineAPI doctrineOverride, at what fraction to pick ship among all availables


        }

        getCargo().sort();
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {

        FactionAPI player = Global.getSector().getPlayerFaction();
        RepLevel FSD_Level = Global.getSector().getFaction("Farsight_Drive").getRelationshipLevel(player);

        if (action == TransferAction.PLAYER_SELL) return true;
        if (action == TransferAction.PLAYER_BUY && !FSD_Level.isAtWorst(RepLevel.WELCOMING)) return true;
        //if(action == TransferAction.PLAYER_BUY && !hegeLevel.isAtWorst(RepLevel.SUSPICIOUS)) return true;
        return action == TransferAction.PLAYER_BUY && !FSD_Level.isAtWorst(RepLevel.WELCOMING);
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {

        FactionAPI player = Global.getSector().getPlayerFaction();
        RepLevel FSD_Level = Global.getSector().getFaction("Farsight_Drive").getRelationshipLevel(player);

        if (action == TransferAction.PLAYER_SELL) return true;
        if (action == TransferAction.PLAYER_BUY && !FSD_Level.isAtWorst(RepLevel.WELCOMING)) return true;
        return action == TransferAction.PLAYER_BUY && !FSD_Level.isAtWorst(RepLevel.WELCOMING);
    }


    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {

        FactionAPI player = Global.getSector().getPlayerFaction();
        RepLevel FSD_Level = Global.getSector().getFaction("Farsight_Drive").getRelationshipLevel(player);

//        if (action == TransferAction.PLAYER_SELL) return txt("MARKET_1");
//        if (!FSD_Level.isAtWorst(RepLevel.WELCOMING)) return txt("MARKET_2");
//        return txt("MARKET_3");
        if (action == TransferAction.PLAYER_SELL) return "The Old Remembrance Assembly does not buy from visiting captains.";
        if (!FSD_Level.isAtWorst(RepLevel.WELCOMING)) return "Access requires Welcoming relations with Farsight Drive.";
        return "Armory access denied.";

    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        FactionAPI player = Global.getSector().getPlayerFaction();
        RepLevel FSD_Level = Global.getSector().getFaction("Farsight_Drive").getRelationshipLevel(player);

        if (action == TransferAction.PLAYER_SELL) return "The Old Remembrance Assembly does not buy from visiting captains.";
        if (!FSD_Level.isAtWorst(RepLevel.WELCOMING)) return "Access requires Welcoming relations with Farsight Drive.";
        return "Armory access denied.";

    }

    @Override
    public boolean isHidden() {
        return !submarket.getFaction().getId().equals("Farsight_Drive");
    }

}
