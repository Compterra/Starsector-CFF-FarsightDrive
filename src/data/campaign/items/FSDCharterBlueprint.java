package data.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.IndustryBlueprintItemPlugin;
import data.campaign.industry.FSDCharterOffice;

public class FSDCharterBlueprint extends IndustryBlueprintItemPlugin {

    public static final int BASE_VALUE = 75000;

    @Override
    public void init(CargoStackAPI stack) {
        if (stack == null) return;
        this.stack = stack;
        if (stack.getSpecialDataIfSpecial() != null) {
            stack.getSpecialDataIfSpecial().setData(FSDCharterOffice.RELIEF_ID);
        }
        industry = Global.getSettings().getIndustrySpec(FSDCharterOffice.RELIEF_ID);
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        if (industry != null) return BASE_VALUE;
        return super.getPrice(market, submarket);
    }
}
