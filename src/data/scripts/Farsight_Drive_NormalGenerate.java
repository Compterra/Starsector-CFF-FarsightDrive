package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.magiclib.util.MagicCampaign;


import java.util.ArrayList;
import java.util.List;

public class Farsight_Drive_NormalGenerate implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {
        new FSD_Golgatha().generate(sector);

        relationAdj(sector);
    }

    protected void relationAdj(SectorAPI sector) {
        FactionAPI faction = sector.getFaction("Farsight_Drive");


        faction.setRelationship("player", 0f);
        faction.setRelationship("pirates", RepLevel.HOSTILE);
        faction.setRelationship("hegemony", RepLevel.SUSPICIOUS);
        faction.setRelationship("tritachyon", RepLevel.FRIENDLY);

        faction.setRelationship("luddic_path", RepLevel.HOSTILE);
        faction.setRelationship("derelict", RepLevel.HOSTILE);
        faction.setRelationship("remnant", RepLevel.HOSTILE);
        faction.setRelationship("threat", RepLevel.HOSTILE);

        faction.setRelationship("cabal", RepLevel.VENGEFUL);

    }
    public static MarketAPI addMarketplace (String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
                                            int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries, float tarrif,
                                            boolean freePort, boolean withJunkAndChatter) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", tarrif);

        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        for (String industry : industries) {
            newMarket.addIndustry(industry);
        }

        newMarket.setFreePort(freePort);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, withJunkAndChatter);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        return newMarket;
    }
    private MarketAPI addMarket(SectorEntityToken entity, String faction, float tarrif, List<String> conditions, List<String> industries, List<String> submarkets) {
        int size = 0;
        for (String condition : conditions) {
            if (condition.startsWith("population_")) {
                String sub = condition.replace("population_", "");
                size = Integer.parseInt(sub);
            }
        }

        MarketAPI market = MagicCampaign.addSimpleMarket(entity, entity.getId(), entity.getName(),
                size, faction, false, false,
                conditions, industries, false, false, false, false, false, false);

        if (conditions.contains("free_market")) market.setFreePort(true);
        for (String submarket : submarkets) {
            market.addSubmarket(submarket);
        }

        market.getTariff().modifyFlat("generator", tarrif);
        Global.getSector().getEconomy().addMarket(market, true);

        entity.setMarket(market);
        entity.setFaction(faction);
        return market;
    }
}
