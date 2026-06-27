package data.scripts;

import data.combat.AI.*;
import data.campaign.CrystalInfusionFleetListener;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;


import data.scripts.ai.FSD_CocxistenceAI;
import data.combat.AI.FSD_SurroundAI;

public class Farsight_Drive_ModPlugin extends BaseModPlugin {

    private final String Proliferation_ID = "FSD_Proliferation_warhead";
    private final String FSD_RepairMissile_ID = "FSD_RepairShell";
    private final String FSD_LSWM_Child_Shell_ID = "FSD_LSWM_Child_Shell";
    private final String FSD_LSWM_Shell_ID = "FSD_LongSongmissile";
    private final String FSD_InspireLight_ID = "FSD_InspireLight_Shell";

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case FSD_InspireLight_ID:
                return new PluginPick<MissileAIPlugin>(new FSD_SurroundArcAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case Proliferation_ID:
                return new PluginPick<MissileAIPlugin>(new FSD_SurroundAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
//            case FSD_LSWMissile_ID:
//                return new PluginPick<MissileAIPlugin>(new FSD_PD_MissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case FSD_RepairMissile_ID:
                return new PluginPick<MissileAIPlugin>(new FSD_Friendly_MissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case FSD_LSWM_Child_Shell_ID:
                return new PluginPick<MissileAIPlugin>(new FSD_LSWM_Child_MissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case FSD_LSWM_Shell_ID:
                return new PluginPick<MissileAIPlugin>(new FSD_LSWM_MissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship != null && "FSD_Cocxistence".equals(ship.getHullSpec().getHullId())) {
            return new PluginPick<ShipAIPlugin>(new FSD_CocxistenceAI(ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public void onApplicationLoad() throws Exception {

    }

    @Override
    public void onGameLoad(boolean newGame) {
        CrystalInfusionFleetListener.register();
    }

    @Override
    public void onNewGameAfterTimePass() {

    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {

    }

    
    @Override
    public void onNewGame() {
        Farsight_Drive_NormalGenerate generator = new Farsight_Drive_NormalGenerate();
        if (shouldGenerateGolgatha()) {
            if (!hasGolgatha()) {
                generator.generate(Global.getSector());
            } else {
                generator.relationAdj(Global.getSector());
            }
        } else {
            generator.relationAdj(Global.getSector());
        }
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("Farsight_Drive");
        
        CrystalInfusionFleetListener.register();
    }

    private static boolean hasGolgatha() {
        return Global.getSector() != null
                && (Global.getSector().getEntityById("Golgatha") != null
                || Global.getSector().getEntityById("Golgatha_I") != null
                || Global.getSector().getEntityById("FSD_Relay") != null);
    }

    private static boolean isNexerelinCorvusMode() {
        if (Global.getSector() != null) {
            Object result = Global.getSector().getMemoryWithoutUpdate().get("$nex_corvusMode");
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
        return false;
    }

    private static boolean shouldGenerateGolgatha() {
        return !NEX() || isNexerelinCorvusMode();
    }

    public static boolean NEX() {
        return Global.getSettings().getModManager().isModEnabled("nexerelin");
    }
}
