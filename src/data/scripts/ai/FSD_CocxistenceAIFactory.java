package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 */
public class FSD_CocxistenceAIFactory {
    
    /**
     */
    public static ShipAIPlugin createOrGetAI(ShipAPI ship) {
        if (ship == null) return null;
        return new FSD_CocxistenceAI(ship);
    }
} 