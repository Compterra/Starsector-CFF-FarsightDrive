package data.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 
 * 
 * @author FarsightDrive Team
 */
public class CrystalInfusionFleetListener implements EveryFrameScript, java.io.Serializable {
  private static final Logger log = Global.getLogger(CrystalInfusionFleetListener.class);
  private static final boolean ENABLE_LOGGING = false;
  private static final long serialVersionUID = 1L;
  
  private static final float BASE_INFUSION_RATE_PER_DAY = 0.005f;
  private static final float INFUSION_THRESHOLD_FOR_HULLMOD = 0.001f;
  private static final float MAX_INFUSION_PROGRESS = 1.0f;
  private static final float DECAY_RATE_PER_DAY = 0.01f;
  private static final float REMOVAL_THRESHOLD = 0.0005f;
  
  private static final java.util.Map<com.fs.starfarer.api.combat.ShipAPI.HullSize, Float> INFUSION_CONTRIBUTION = 
      new java.util.HashMap<com.fs.starfarer.api.combat.ShipAPI.HullSize, Float>();
  
  static {
    INFUSION_CONTRIBUTION.put(com.fs.starfarer.api.combat.ShipAPI.HullSize.FRIGATE, 1.0f);
    INFUSION_CONTRIBUTION.put(com.fs.starfarer.api.combat.ShipAPI.HullSize.DESTROYER, 2.0f);
    INFUSION_CONTRIBUTION.put(com.fs.starfarer.api.combat.ShipAPI.HullSize.CRUISER, 4.0f);
    INFUSION_CONTRIBUTION.put(com.fs.starfarer.api.combat.ShipAPI.HullSize.CAPITAL_SHIP, 8.0f);
  }
  
  private static final String INFUSION_PROGRESS_KEY_PREFIX = "$fsd_infusion_progress_";
  
  private long lastUpdateTimestamp;
  
  private boolean isDone = false;
  
  public CrystalInfusionFleetListener() {
    this.lastUpdateTimestamp = Global.getSector().getClock().getTimestamp();
    
    if (ENABLE_LOGGING && log != null) {
      log.info("[Crystal Infusion] Fleet listener initialized, current timestamp: " + lastUpdateTimestamp);
    }
  }
  
  /**
   */
  private float getInfusionProgress(FleetMemberAPI member) {
    String key = INFUSION_PROGRESS_KEY_PREFIX + member.getId();
    if (Global.getSector().getMemoryWithoutUpdate().contains(key)) {
      Object progressObj = Global.getSector().getMemoryWithoutUpdate().get(key);
      if (progressObj instanceof Float) {
        return (Float) progressObj;
      }
    }
    return 0f;
  }
  
  /**
   */
  private void setInfusionProgress(FleetMemberAPI member, float progress) {
    String key = INFUSION_PROGRESS_KEY_PREFIX + member.getId();
    Global.getSector().getMemoryWithoutUpdate().set(key, progress);
  }
  
  /**
   */
  public static float getInfusionProgressById(String fleetMemberId) {
    if (fleetMemberId == null || fleetMemberId.isEmpty()) {
      return 0f;
    }
    String key = INFUSION_PROGRESS_KEY_PREFIX + fleetMemberId;
    if (Global.getSector().getMemoryWithoutUpdate().contains(key)) {
      Object progressObj = Global.getSector().getMemoryWithoutUpdate().get(key);
      if (progressObj instanceof Float) {
        return (Float) progressObj;
      }
    }
    return 0f;
  }
  
  @Override
  public void advance(float amount) {
    CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
    if (playerFleet == null) {
      return;
    }
    
    long currentTimestamp = Global.getSector().getClock().getTimestamp();
    float daysPassed = Global.getSector().getClock().getElapsedDaysSince(lastUpdateTimestamp);
    
    if (daysPassed < 0.01f) {
      return;
    }
    
    lastUpdateTimestamp = currentTimestamp;
    
    float infusionMultiplier = calculateInfusionMultiplier(playerFleet);
    
    if (infusionMultiplier > 0f) {
      Set<FleetMemberAPI> infusionTargets = getInfusionTargets(playerFleet);
      
      updateInfusionProgress(infusionTargets, infusionMultiplier, daysPassed);
      
      if (ENABLE_LOGGING && log != null && !infusionTargets.isEmpty()) {
        log.info(String.format(
            "[Crystal Infusion] Updating progress: %d target ships, infusion multiplier %.2fx, elapsed %.3f days",
            infusionTargets.size(), infusionMultiplier, daysPassed));
      }
    } else {
      processInfusionDecay(playerFleet, daysPassed);
    }
    
    cleanupRemovedShips(playerFleet);
  }
  
  /**
   * 
   */
  private float calculateInfusionMultiplier(CampaignFleetAPI fleet) {
    float totalContribution = 0f;
    int sourceCrystalCount = 0;
    
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (!member.getVariant().hasHullMod("FSD_ReflectLight")) {
        continue;
      }
      
      if (!member.getVariant().hasHullMod("FSD_CrystalUnbinding")) {
        continue;
      }
      
      sourceCrystalCount++;
      
      com.fs.starfarer.api.combat.ShipAPI.HullSize hullSize = member.getHullSpec().getHullSize();
      Float contribution = INFUSION_CONTRIBUTION.get(hullSize);
      if (contribution != null) {
        totalContribution += contribution;
      } else {
        totalContribution += 1.0f;
      }
    }
    
    if (sourceCrystalCount == 0) {
      return 0f;
    }
    
    float multiplier = totalContribution;
    
    if (ENABLE_LOGGING && log != null) {
      log.info(String.format(
          "[Crystal Infusion] Fleet infusion strength: %d source-crystal ships, total contribution %.1f, infusion multiplier %.1fx",
          sourceCrystalCount, totalContribution, multiplier));
    }
    
    return multiplier;
  }
  
  /**
   */
  private Set<FleetMemberAPI> getInfusionTargets(CampaignFleetAPI fleet) {
    Set<FleetMemberAPI> targets = new HashSet<>();
    
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (member.getVariant().hasHullMod("FSD_ReflectLight")) {
        continue;
      }
      
      if (isFarsightDriveShip(member)) {
        continue;
      }
      
      float progress = getInfusionProgress(member);
      if (progress >= MAX_INFUSION_PROGRESS) {
        continue;
      }
      
      targets.add(member);
    }
    
    return targets;
  }
  
  /**
   * 
   */
  private void updateInfusionProgress(Set<FleetMemberAPI> targets, float infusionMultiplier, float daysPassed) {
    if (infusionMultiplier <= 0f || targets.isEmpty()) {
      return;
    }
    
    float progressIncrease = BASE_INFUSION_RATE_PER_DAY * infusionMultiplier * daysPassed;
    
    for (FleetMemberAPI member : targets) {
      float currentProgress = getInfusionProgress(member);
      float newProgress = Math.min(MAX_INFUSION_PROGRESS, currentProgress + progressIncrease);
      
      setInfusionProgress(member, newProgress);
      
      if (member.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
        ensureFleetMemberIdTag(member);
      }
      
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Crystal Infusion] Ship %s: progress %.3f%% -> %.3f%% (+%.4f%%, delta=%.6f)",
            getShipName(member), 
            currentProgress * 100, 
            newProgress * 100,
            (newProgress - currentProgress) * 100,
            progressIncrease));
      }
      
      if (currentProgress < INFUSION_THRESHOLD_FOR_HULLMOD && newProgress >= INFUSION_THRESHOLD_FOR_HULLMOD) {
        installSecondaryKarmaHullmod(member);
      }
    }
  }
  
  /**
   */
  private void ensureFleetMemberIdTag(FleetMemberAPI member) {
    String memberIdTag = "fsd_fleet_member_id_" + member.getId();
    if (!member.getVariant().hasTag(memberIdTag)) {
      member.getVariant().addTag(memberIdTag);
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Crystal Infusion] Added FleetMember ID tag to old-save ship %s",
            getShipName(member)));
      }
    }
  }
  
  /**
   */
  private void installSecondaryKarmaHullmod(FleetMemberAPI member) {
    if (member.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
      return;
    }
    
    member.getVariant().addPermaMod("FSD_SecondaryKarmaInfusion", false);
    
    String memberIdTag = "fsd_fleet_member_id_" + member.getId();
    if (!member.getVariant().hasTag(memberIdTag)) {
      member.getVariant().addTag(memberIdTag);
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Crystal Infusion] Added FleetMember ID tag to ship %s variant: %s",
            getShipName(member), member.getId()));
      }
    }
    
    member.updateStats();
    
    if (ENABLE_LOGGING && log != null) {
      float progress = getInfusionProgress(member);
      log.info(String.format(
          "[Crystal Infusion] Installing Secondary Crystal Overgrowth on ship %s (infusion progress: %.1f%%)",
          getShipName(member), progress * 100));
    }
    
      Global.getSector().getCampaignUI().addMessage(
              String.format("Ship %s has been crystal-infused and gained a secondary karma system", getShipName(member)),
              com.fs.starfarer.api.util.Misc.getPositiveHighlightColor());
  }
  
  /**
   */
  private String getShipName(FleetMemberAPI member) {
    String shipName = member.getShipName();
    if (shipName == null || shipName.isEmpty()) {
      shipName = member.getHullSpec().getHullName();
    }
    return shipName;
  }
  
  /**
   */
  private void processInfusionDecay(CampaignFleetAPI fleet, float daysPassed) {
    float decayAmount = DECAY_RATE_PER_DAY * daysPassed;
    
    List<FleetMemberAPI> toRemoveHullmod = new ArrayList<FleetMemberAPI>();
    
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      if (!member.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
        continue;
      }
      
      float currentProgress = getInfusionProgress(member);
      
      float newProgress = Math.max(0f, currentProgress - decayAmount);
      setInfusionProgress(member, newProgress);
      
      if (newProgress <= REMOVAL_THRESHOLD) {
        toRemoveHullmod.add(member);
      }
      
      if (ENABLE_LOGGING && log != null) {
        log.info(String.format(
            "[Crystal Infusion] Ship %s crystal depletion: %.1f%% -> %.1f%%",
            getShipName(member), currentProgress * 100, newProgress * 100));
      }
    }
    
    for (FleetMemberAPI member : toRemoveHullmod) {
      removeSecondaryKarmaHullmod(member);
    }
    
    if (ENABLE_LOGGING && log != null && !toRemoveHullmod.isEmpty()) {
      log.info(String.format(
          "[Crystal Infusion] No source crystals; secondary crystals fully depleted and removed from %d ships",
          toRemoveHullmod.size()));
    }
  }
  
  /**
   */
  private void removeSecondaryKarmaHullmod(FleetMemberAPI member) {
    if (!member.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion")) {
      return;
    }
    
    member.getVariant().removePermaMod("FSD_SecondaryKarmaInfusion");
    member.updateStats();
    
    setInfusionProgress(member, 0f);
    
    if (ENABLE_LOGGING && log != null) {
      log.info(String.format(
          "[Crystal Infusion] Removing Secondary Crystal Overgrowth from ship %s (fully depleted)",
          getShipName(member)));
    }
    
    Global.getSector().getCampaignUI().addMessage(
        String.format("Ship %s's secondary crystal has fully depleted and dispersed", getShipName(member)),
        new Color(180, 150, 100));
  }
  
  /**
   */
  private void cleanupRemovedShips(CampaignFleetAPI fleet) {
    java.util.Set<String> currentShipIds = new java.util.HashSet<>();
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
      currentShipIds.add(member.getId());
    }
    
    java.util.List<String> keysToRemove = new java.util.ArrayList<>();
    for (String key : Global.getSector().getMemoryWithoutUpdate().getKeys()) {
      if (key.startsWith(INFUSION_PROGRESS_KEY_PREFIX)) {
        String memberId = key.substring(INFUSION_PROGRESS_KEY_PREFIX.length());
        if (!currentShipIds.contains(memberId)) {
          keysToRemove.add(key);
        }
      }
    }
    
    for (String key : keysToRemove) {
      Global.getSector().getMemoryWithoutUpdate().unset(key);
      if (ENABLE_LOGGING && log != null) {
        log.info("[Crystal Infusion] Cleaning infusion data for removed ship: " + key);
      }
    }
  }
  
  /**
   */
  private boolean isFarsightDriveShip(FleetMemberAPI member) {
    if (member == null || member.getHullSpec() == null) {
      return false;
    }
    
    String manufacturer = member.getHullSpec().getManufacturer();
    if (manufacturer != null && manufacturer.contains("Farsight Drive")) {
      return true;
    }
    
    String hullId = member.getHullId();
    return hullId != null && hullId.startsWith("FSD_");
  }
  
  
  @Override
  public boolean isDone() {
    return isDone;
  }
  
  @Override
  public boolean runWhilePaused() {
    return false;
  }
  
  /**
   */
  public static CrystalInfusionFleetListener getInstance() {
    for (EveryFrameScript script : Global.getSector().getScripts()) {
      if (script instanceof CrystalInfusionFleetListener) {
        return (CrystalInfusionFleetListener) script;
      }
    }
    return null;
  }
  
  /**
   */
  public static void register() {
    // Keep this load-time listener transient so the mod can be removed from an existing save.
    Global.getSector().removeScriptsOfClass(CrystalInfusionFleetListener.class);
    CrystalInfusionFleetListener listener = new CrystalInfusionFleetListener();
    Global.getSector().addTransientScript(listener);
    
    if (ENABLE_LOGGING && log != null) {
      log.info("[Crystal Infusion] Listener registered in game");
    }
  }
}

