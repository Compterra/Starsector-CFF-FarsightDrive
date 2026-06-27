package data.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;

import java.awt.*;

public class FSD_Longinus_AmmoReloadListener implements AdvanceableListener {
    
    private ShipAPI ship;
    private float karmaAccumulated = 0f;
    private static final float KARMA_PER_RELOAD = 0.33f;
    
    public FSD_Longinus_AmmoReloadListener(ShipAPI ship) {
        this.ship = ship;
    }
    
    @Override
    public void advance(float amount) {
        if (ship == null || !ship.isAlive() || Global.getCombatEngine().isPaused()) {
            return;
        }
        
        if (ship.getCustomData().containsKey("FSD_Longinus_IsInitialized")) {
            Boolean isInitialized = (Boolean) ship.getCustomData().get("FSD_Longinus_IsInitialized");
            if (isInitialized != null && !isInitialized) {
                float currentKarma = KarmaAPI.getKarma(ship);
                ship.setCustomData("FSD_Longinus_LastKarma", currentKarma);
                ship.setCustomData("FSD_Longinus_IsInitialized", true);
                return;
            }
        } else {
            float currentKarma = KarmaAPI.getKarma(ship);
            ship.setCustomData("FSD_Longinus_LastKarma", currentKarma);
            ship.setCustomData("FSD_Longinus_IsInitialized", true);
            return;
        }
        
        WeaponAPI longinusWeapon = null;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if ("FSD_Longinus".equals(weapon.getId())) {
                longinusWeapon = weapon;
                break;
            }
        }
        
        if (longinusWeapon == null) {
            return;
        }
        
        float currentKarma = KarmaAPI.getKarma(ship);
        
        float lastKarma = 0f;
        if (ship.getCustomData().containsKey("FSD_Longinus_LastKarma")) {
            lastKarma = (float) ship.getCustomData().get("FSD_Longinus_LastKarma");
        }
        
        float karmaGain = currentKarma - lastKarma;
        
        float overflowKarma = KarmaAPI.getOverflowKarma(ship);
        
        if (karmaGain > 0) {
            karmaAccumulated += karmaGain;
            
            ship.setCustomData("FSD_Longinus_LastKarma", currentKarma);
        }
        
        if (overflowKarma > 0) {
            float overflowEfficiency = 2.0f;
            float overflowContribution = overflowKarma * overflowEfficiency;
            karmaAccumulated += overflowContribution;
            
            float actualConsumed = KarmaAPI.consumeOverflowKarma(ship, overflowKarma);
            
//            if (ship == Global.getCombatEngine().getPlayerShip() && actualConsumed > 0) {
//                Global.getCombatEngine().addFloatingText(
//                    ship.getLocation(),
//                    String.format("Overflow karma x2.0 -> +%.2f%% ammo progress", overflowContribution * 100f),
//                    15f,
//                    new Color(255, 215, 0, 200),
//                    ship,
//                    0.5f,
//                    1.5f
//                );
//            }
            
            if (karmaAccumulated >= KARMA_PER_RELOAD) {
                int reloadsAvailable = (int) (karmaAccumulated / KARMA_PER_RELOAD);
                
                int currentAmmo = longinusWeapon.getAmmo();
                int maxAmmo = longinusWeapon.getMaxAmmo();
                
                int actualReloads = Math.min(reloadsAvailable, maxAmmo - currentAmmo);
                
                if (actualReloads > 0) {
                    longinusWeapon.setAmmo(currentAmmo + actualReloads);
                    
                    karmaAccumulated -= actualReloads * KARMA_PER_RELOAD;
                    
//                    if (ship == Global.getCombatEngine().getPlayerShip()) {
//                        Global.getCombatEngine().addFloatingText(
//                            ship.getLocation(),
//                            "Spear of Longinus +" + actualReloads,
//                            20f,
//                            new Color(247, 166, 187, 255),
//                            ship,
//                            1f,
//                            2f
//                        );
//                    }
                }
            }
        }
        
        if (ship == Global.getCombatEngine().getPlayerShip() && longinusWeapon != null) {
            int currentAmmo = longinusWeapon.getAmmo();
            int maxAmmo = longinusWeapon.getMaxAmmo();
            
            float progressPercent = (karmaAccumulated / KARMA_PER_RELOAD) * 100f;
            int progressInt = (int) progressPercent;
            
            String statusText = String.format("Ammo: %d/%d", currentAmmo, maxAmmo);
            String detailText;
            boolean negative = false;
            
            float currentOverflow = KarmaAPI.getOverflowKarma(ship);
            boolean hasOverflow = currentOverflow > 0.01f;
            
            if (currentAmmo >= maxAmmo) {
                if (hasOverflow) {
                    detailText = String.format("Ammo full - overflow karma: %.2f%%", currentOverflow * 100f);
                    negative = false;
                } else {
                    detailText = "Ammo full";
                    negative = false;
                }
            } else if (currentAmmo == 0) {
                if (hasOverflow) {
                    detailText = String.format("Ammo depleted - overflow-accelerated recovery: %d%%", progressInt);
                    negative = false;
                } else {
                    detailText = String.format("Ammo depleted - recovery progress: %d%%", progressInt);
                    negative = true;
                }
            } else if (progressInt >= 75) {
                if (hasOverflow) {
                    detailText = String.format("Reload imminent (%d%%) [Overflow x2.0]", progressInt);
                } else {
                    detailText = String.format("Reload imminent (%d%%)", progressInt);
                }
                negative = false;
            } else if (progressInt >= 50) {
                if (hasOverflow) {
                    detailText = String.format("Recovering (%d%%) [Overflow x2.0]", progressInt);
                } else {
                    detailText = String.format("Recovering (%d%%)", progressInt);
                }
                negative = false;
            } else if (progressInt >= 25) {
                if (hasOverflow) {
                    detailText = String.format("Slow recovery (%d%%) [Overflow x2.0]", progressInt);
                    negative = false;
                } else {
                    detailText = String.format("Slow recovery (%d%%)", progressInt);
                    negative = true;
                }
            } else {
                if (hasOverflow) {
                    detailText = String.format("Waiting for karma (%d%%) [Overflow x2.0]", progressInt);
                    negative = false;
                } else {
                    detailText = String.format("Waiting for karma (%d%%)", progressInt);
                    negative = true;
                }
            }
            
            Global.getCombatEngine().maintainStatusForPlayerShip(
                "FSD_Longinus_Reload",
                "graphics/weapons/FSD_Longinus.png",
                "Spear of Longinus",
                statusText + " - " + detailText,
                negative
            );
        }
        
    }
}

