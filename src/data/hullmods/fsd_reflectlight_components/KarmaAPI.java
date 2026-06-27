package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/**
 * <pre>
 *
 * float karma = KarmaAPI.getKarma(ship);
 * KarmaAPI.addKarma(ship, 0.2f);
 * 
 *
 * KarmaAPI.setInitialKarmaByHullId("onslaught", 0.15f);
 * KarmaAPI.setInitialKarmaByHullSize(HullSize.FRIGATE, 0.9f);
 * 
 *
 * KarmaAPI.addKarmaListener(ship, new KarmaChangeListener() {
 *     public void onKarmaChanged(...) { ... }
 * });
 * </pre>
 * 
 * @author FarsightDrive Team
 * @version 2.0
 */
public class KarmaAPI {
    
    private KarmaAPI() {
    }
    
    
    /**
     */
    public static float getKarma(ShipAPI ship) {
        return KarmaManager.getInstance().getKarma(ship);
    }
    
    /**
     */
    public static void setKarma(ShipAPI ship, float karma) {
        KarmaManager.getInstance().setKarma(ship, karma, KarmaType.EXTERNAL);
    }
    
    /**
     */
    public static void setKarma(ShipAPI ship, float karma, KarmaType type) {
        KarmaManager.getInstance().setKarma(ship, karma, type);
    }
    
    /**
     */
    public static float addKarma(ShipAPI ship, float amount) {
        return KarmaManager.getInstance().addKarma(ship, amount, KarmaType.EXTERNAL);
    }
    
    /**
     */
    public static float addKarma(ShipAPI ship, float amount, KarmaType type) {
        return KarmaManager.getInstance().addKarma(ship, amount, type);
    }
    
    /**
     */
    public static float reduceKarma(ShipAPI ship, float amount) {
        return KarmaManager.getInstance().reduceKarma(ship, amount, KarmaType.PASSIVE_LOSS);
    }
    
    /**
     */
    public static float reduceKarma(ShipAPI ship, float amount, KarmaType type) {
        return KarmaManager.getInstance().reduceKarma(ship, amount, type);
    }
    
    /**
     */
    public static float consumeKarma(ShipAPI ship, float amount) {
        return KarmaManager.getInstance().consumeKarma(ship, amount, KarmaType.ACTIVE_COST);
    }
    
    
    /**
     */
    public static float getKarmaPercent(ShipAPI ship) {
        return KarmaManager.getInstance().getKarmaPercent(ship);
    }
    
    /**
     */
    public static boolean hasKarma(ShipAPI ship, float amount) {
        return KarmaManager.getInstance().hasKarma(ship, amount);
    }
    
    /**
     */
    public static float getKarmaMax(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getKarmaMax() : 1.0f;
    }
    
    /**
     */
    public static boolean hasKarmaData(ShipAPI ship) {
        return KarmaManager.getInstance().hasKarmaData(ship);
    }
    
    
    /**
     * 
     */
    public static void setInitialKarmaByHullSize(HullSize hullSize, float initialKarma) {
        KarmaManager.getInstance().setInitialKarmaByHullSize(hullSize, initialKarma);
    }
    
    /**
     * 
     */
    public static void setInitialKarmaByHullId(String hullId, float initialKarma) {
        KarmaManager.getInstance().setInitialKarmaByHullId(hullId, initialKarma);
    }
    
    /**
     * 
     */
    public static void setInitialKarmaByShipId(String shipId, float initialKarma) {
        KarmaManager.getInstance().setInitialKarmaByShipId(shipId, initialKarma);
    }
    
    /**
     * 
     */
    public static float getInitialKarmaFor(ShipAPI ship) {
        return KarmaManager.getInstance().getInitialKarmaFor(ship);
    }
    
    /**
     * 
     */
    public static void resetInitialKarmaByHullSize(HullSize hullSize) {
        KarmaManager.getInstance().resetInitialKarmaByHullSize(hullSize);
    }
    
    /**
     * 
     */
    public static void resetInitialKarmaByHullId(String hullId) {
        KarmaManager.getInstance().resetInitialKarmaByHullId(hullId);
    }
    
    /**
     * 
     */
    public static void resetInitialKarmaByShipId(String shipId) {
        KarmaManager.getInstance().resetInitialKarmaByShipId(shipId);
    }
    
    /**
     */
    public static void clearAllInitialKarmaConfigs() {
        KarmaManager.getInstance().clearAllInitialKarmaConfigs();
    }
    
    
    /**
     * 
     * 
     */
    public static void modifyKarmaMaxMultiplier(ShipAPI ship, String source, float value) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.modifyKarmaMaxMultiplier(source, value);
        }
    }
    
    /**
     * 
     */
    public static void removeKarmaMaxMultiplier(ShipAPI ship, String source) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.removeKarmaMaxMultiplier(source);
        }
    }
    
    /**
     * 
     * 
     */
    public static void addGainKarmaMultiplier(ShipAPI ship, String source, float multiplier) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.addGainKarmaMultiplier(source, multiplier);
        }
    }
    
    /**
     * 
     */
    public static void removeGainKarmaMultiplier(ShipAPI ship, String source) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.removeGainKarmaMultiplier(source);
        }
    }
    
    /**
     * 
     */
    public static float getKarmaGainMult(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getGainKarma() : 0f;
    }
    
    @Deprecated
    public static void setKarmaDissipationMult(ShipAPI ship, float dissipationMult) {
        setKarmaDissipationMult(ship, dissipationMult, KarmaPriority.PRIORITY_NORMAL, "KarmaAPI");
    }
    
    public static boolean setKarmaDissipationMult(ShipAPI ship, float dissipationMult, int priority, String source) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            return data.setDissipationKarmaMult(dissipationMult, priority, source);
        }
        return false;
    }
    
    /**
     * 
     * 
     */
    public static void addEfficiencyMultiplier(ShipAPI ship, String source, float multiplier) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.addEfficiencyMultiplier(source, multiplier);
        }
    }
    
    /**
     * 
     */
    public static void removeEfficiencyMultiplier(ShipAPI ship, String source) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.removeEfficiencyMultiplier(source);
        }
    }
    
    /**
     * 
     */
    public static float getKarmaEfficiencyMult(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getEfficiencyMultiplier() : 1.0f;
    }
    
    /**
     * 
     * 
     */
    public static void addRangeMultiplier(ShipAPI ship, String source, float multiplier) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.addGainKarmaRangeMultiplier(source, multiplier);
        }
    }
    
    /**
     * 
     */
    public static void removeRangeMultiplier(ShipAPI ship, String source) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.removeGainKarmaRangeMultiplier(source);
        }
    }
    
    /**
     * 
     */
    public static float getEntropyFieldRangeMult(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getGainKarmaRange() : 1.0f;
    }
    
    
    /**
     * 
     */
    public static void addKarmaListener(ShipAPI ship, KarmaChangeListener listener) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.addListener(listener);
        }
    }
    
    /**
     * 
     */
    public static void removeKarmaListener(ShipAPI ship, KarmaChangeListener listener) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.removeListener(listener);
        }
    }
    
    /**
     * 
     */
    public static void clearKarmaListeners(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.clearListeners();
        }
    }
    
    
    /**
     * 
     */
    public static float getInitialKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getInitialKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float getCombatGainedKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getCombatGainedKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float getKillGainedKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getKillGainedKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float getPassiveLostKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getPassiveLostKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float getActiveConsumedKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getActiveConsumedKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float getExternalModifiedKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getExternalModifiedKarma() : 0f;
    }
    
    
    /**
     * 
     */
    public static String getCacheStats() {
        return KarmaManager.getInstance().getCacheStats();
    }
    
    /**
     * 
     */
    public static int getCacheSize() {
        return KarmaManager.getInstance().getCacheSize();
    }
    
    /**
     * 
     */
    public static KarmaData getKarmaData(ShipAPI ship) {
        return KarmaManager.getInstance().getKarmaData(ship);
    }
    
    
    /**
     * 
     */
    public static float getOverflowKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        return data != null ? data.getOverflowKarma() : 0f;
    }
    
    /**
     * 
     */
    public static float consumeOverflowKarma(ShipAPI ship, float amount) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            return data.consumeOverflowKarma(amount);
        }
        return 0f;
    }
    
    /**
     * 
     */
    public static void resetOverflowKarma(ShipAPI ship) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.resetOverflowKarma();
        }
    }
    
    /**
     * 
     */
    public static void setOverflowKarma(ShipAPI ship, float amount) {
        KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
        if (data != null) {
            data.setOverflowKarma(amount);
        }
    }
}

