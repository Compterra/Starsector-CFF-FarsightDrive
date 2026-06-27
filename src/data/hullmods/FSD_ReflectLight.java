package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

public class FSD_ReflectLight extends BaseHullMod {
  public static final Logger log = Global.getLogger(FSD_ReflectLight.class);
  public static final boolean ENABLE_DETAIL_LOGGING = false;
  public static final boolean ENABLE_PRIORITY_LOGGING = false;
  public static final boolean ENABLE_ENTROPY_FIELD_INDICATOR = false;
  public static final boolean ENABLE_ENTROPY_FIELD_PROCESSOR = true;
  public static final Color FIELD_COLOR_BASE =
      new Color(200, 50, 50, Math.min(255, Math.max(0, 150)));
  public static final Color FIELD_RING_COLOR =
      new Color(100, 200, 255, Math.min(255, Math.max(0, 200)));
  public static final float CHAR_SPACING_FACTOR = 0.4f;
  private static final Map<ShipAPI, EntropyFieldState> fieldVisuals = new HashMap<>();
  private static final Random random = new Random();
  private static boolean combatInitialized = false;
  private static long lastCombatStartTime = 0;
  public static final Map<String, List<String>> shipFixedSentences = new HashMap<>();
  public static final Map<HullSize, Integer> maxSentenceLengthByHullSize = new HashMap<>();
  private static final EntropyFieldProcessor fieldProcessor = new EntropyFieldProcessor();

  static {
    maxSentenceLengthByHullSize.put(HullSize.FRIGATE, 20);
    maxSentenceLengthByHullSize.put(HullSize.DESTROYER, 30);
    maxSentenceLengthByHullSize.put(HullSize.CRUISER, 40);
    maxSentenceLengthByHullSize.put(HullSize.CAPITAL_SHIP, 50);
  }

  static {
    if (ENABLE_DETAIL_LOGGING) log.info("[FSD][FSD_ReflectLight] static initialization - start");
    TrigCache.initialize();
    ObjectPool.initialize();
    ShipDetectionManager.initialize();
    if (ENABLE_DETAIL_LOGGING) log.info("[FSD][FSD_ReflectLight] static initialization - end");
  }

  public static final float REPAIR_MULT = 0.5f;
  public static final float RELOAD_MULT = 1.66f;
  public static float FLUX_THRESHOLD_INCREASE_PERCENT = 75f;
  private static final Map<HullSize, Float> ENTROPY_FIELD_RANGE = new HashMap<>();

  static {
    ENTROPY_FIELD_RANGE.put(HullSize.FRIGATE, 600f);
    ENTROPY_FIELD_RANGE.put(HullSize.DESTROYER, 900f);
    ENTROPY_FIELD_RANGE.put(HullSize.CRUISER, 1200f);
    ENTROPY_FIELD_RANGE.put(HullSize.CAPITAL_SHIP, 1500f);
  }

  public static final Map<HullSize, Float> INITIAL_KARMA = new HashMap<>();

  static {
    INITIAL_KARMA.put(HullSize.FRIGATE, 1.0f);
    INITIAL_KARMA.put(HullSize.DESTROYER, 0.75f);
    INITIAL_KARMA.put(HullSize.CRUISER, 0.5f);
    INITIAL_KARMA.put(HullSize.CAPITAL_SHIP, 0.25f);
  }

  public static final Map<HullSize, Float> KARMA_GAIN_MULT = new HashMap<>();

  static {
    KARMA_GAIN_MULT.put(HullSize.FRIGATE, 0.1f);
    KARMA_GAIN_MULT.put(HullSize.DESTROYER, 0.2f);
    KARMA_GAIN_MULT.put(HullSize.CRUISER, 0.3f);
    KARMA_GAIN_MULT.put(HullSize.CAPITAL_SHIP, 0.4f);
  }

  private static final Map<HullSize, Float> REPAIR_SPEED_MULT = new HashMap<>();

  static {
    REPAIR_SPEED_MULT.put(HullSize.FIGHTER, 0.00f);
    REPAIR_SPEED_MULT.put(HullSize.FRIGATE, 0.008f);
    REPAIR_SPEED_MULT.put(HullSize.DESTROYER, 0.006f);
    REPAIR_SPEED_MULT.put(HullSize.CRUISER, 0.006f);
    REPAIR_SPEED_MULT.put(HullSize.CAPITAL_SHIP, 0.004f);
  }

  public static List<String> getFixedRuneSentence(ShipAPI ship) {
    if (ship == null) return getRandomRuneSentence();
    String shipId = ship.getId();
    if (shipFixedSentences.containsKey(shipId)) {
      return new ArrayList<String>(shipFixedSentences.get(shipId));
    }
    if (!RuneConfigLoader.isLoaded()) {
      RuneConfigLoader.loadRuneSentences();
    }
    if (RuneConfigLoader.runeSentences.isEmpty()) {
      List<String> defaultOnError = new ArrayList<String>();
      defaultOnError.add("D");
      defaultOnError.add("E");
      defaultOnError.add("F");
      return defaultOnError;
    }
    List<String> chosenSentence =
        new ArrayList<String>(
            RuneConfigLoader.runeSentences.get(
                random.nextInt(RuneConfigLoader.runeSentences.size())));
    HullSize hullSize = ship.getHullSize();
    int maxLength =
        maxSentenceLengthByHullSize.containsKey(hullSize)
            ? maxSentenceLengthByHullSize.get(hullSize)
            : 30;
    if (chosenSentence.size() > maxLength) {
      int lastSpace = -1;
      for (int i = 0; i < maxLength; i++) {
        if (chosenSentence.get(i).equals(" ")) {
          lastSpace = i;
        }
      }
      if (lastSpace >= 0 && maxLength - lastSpace < 10) {
        chosenSentence = new ArrayList<String>(chosenSentence.subList(0, lastSpace));
      } else {
        chosenSentence = new ArrayList<String>(chosenSentence.subList(0, maxLength));
      }
    }
    shipFixedSentences.put(shipId, new ArrayList<String>(chosenSentence));
    return chosenSentence;
  }

  public static List<String> getRandomRuneSentence() {
    if (!RuneConfigLoader.isLoaded()) {
      RuneConfigLoader.loadRuneSentences();
    }
    if (RuneConfigLoader.runeSentences.isEmpty()) {
      List<String> defaultOnError = new ArrayList<String>();
      defaultOnError.add("R");
      defaultOnError.add("N");
      defaultOnError.add("D");
      return defaultOnError;
    }
    return new ArrayList<String>(
        RuneConfigLoader.runeSentences.get(random.nextInt(RuneConfigLoader.runeSentences.size())));
  }

  public class EntropyFieldRenderer extends BaseCombatLayeredRenderingPlugin {
    private final EntropyFieldState fieldState;
    private final ShipAPI ship;
    private final RuneRenderer runeRenderer;
    private final FieldVisualEffectRenderer fieldEffectRenderer;
    private final Vector2f position;

    public EntropyFieldRenderer(EntropyFieldState initialState, ShipAPI ship) {
      this.fieldState = initialState;
      this.ship = ship;
      this.position = new Vector2f(ship.getLocation());
      this.runeRenderer = new RuneRenderer();
      this.fieldEffectRenderer = new FieldVisualEffectRenderer(ship);
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info(
            "[FSD][FSD_ReflectLight_Renderer] for ship "
                + (ship != null ? ship.getId() : "null")
                + " createinstance");
      }
    }

    public void updatePlayerStatus(boolean isPlayerShip) {
      this.fieldState.setPlayerShip(isPlayerShip);
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info(
            "[FSD][FSD_ReflectLight_Renderer] updateship "
                + (this.ship != null ? this.ship.getId() : "null")
                + "  player state for  "
                + isPlayerShip);
      }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
      if (ship != null && ship.isStationModule()) {
        return;
      }
      synchronizePlayerState("render()");
      handleTerminatedShip("render()", 0f);
      if (fieldState.getAnimState() == AnimationState.INACTIVE) {
        return;
      }
      float displayKarma = fieldState.getDisplayKarma();
      boolean isForming = fieldState.getAnimState() == AnimationState.FORMING;
      if (!isForming && displayKarma < 0.02f) {
        return;
      }
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING
          && FSD_ReflectLight.log != null
          && Global.getCombatEngine() != null
          && Global.getCombatEngine().getTotalElapsedTime(false) % 1.0f < 0.017f) {
        FSD_ReflectLight.log.info(
            "[FSD][FSD_ReflectLight_Renderer] render() called，ship "
                + (this.ship != null ? this.ship.getId() : "null")
                + "  in layer "
                + layer
                + ", animationstate: "
                + fieldState.getAnimState());
      }
      float alpha = viewport.getAlphaMult();
      if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
        this.fieldEffectRenderer.render(this.fieldState, alpha, displayKarma, layer);
        float currentDisplayKarma = this.fieldState.getDisplayKarma();
        float baseRed = FSD_ReflectLight.FIELD_COLOR_BASE.getRed();
        float baseGreen = FSD_ReflectLight.FIELD_COLOR_BASE.getGreen();
        float baseBlue = FSD_ReflectLight.FIELD_COLOR_BASE.getBlue();
        float karmaEffect = currentDisplayKarma * 0.25f;
        float r = (baseRed / 255f) * (1f - karmaEffect * 0.5f) + karmaEffect;
        float g = (baseGreen / 255f) * (1f - karmaEffect * 0.7f);
        float b = (baseBlue / 255f) * (1f - karmaEffect * 0.8f);
        r = Math.min(1.0f, Math.max(0f, r));
        g = Math.min(1.0f, Math.max(0f, g));
        b = Math.min(1.0f, Math.max(0f, b));
        Color baseRuneColor = new Color(r, g, b);
        this.runeRenderer.render(
            this.fieldState, this.ship, this.position, baseRuneColor, viewport);
      } else if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
        this.fieldEffectRenderer.render(this.fieldState, alpha, displayKarma, layer);
      }
    }

    @Override
    public float getRenderRadius() {
      return Float.MAX_VALUE;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
      if (this.fieldState.getAnimState() == AnimationState.ENHANCED_IDLE) {
        return EnumSet.of(
            CombatEngineLayers.BELOW_SHIPS_LAYER, CombatEngineLayers.ABOVE_SHIPS_LAYER);
      } else {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
      }
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine().isPaused()) return;
      synchronizePlayerState("advance()");
      if (handleTerminatedShip("advance()", amount * 1.5f)) {
        return;
      }
      this.position.set(ship.getLocation());
      this.fieldState.updateState(amount);
      float karma = fieldState.getDisplayKarma();
      boolean isIdle = fieldState.getAnimState() == AnimationState.IDLE;
      float prevKarma = 0f;
      if (ship.getCustomData().containsKey("FSD_ReflectLight_PrevKarma")) {
        prevKarma = (float) ship.getCustomData().get("FSD_ReflectLight_PrevKarma");
      }
      if (isIdle && prevKarma < 0.05f && karma >= 0.05f) {
        if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
          FSD_ReflectLight.log.info(
              "[FSD][FSD_ReflectLight_Renderer] advance() - ship "
                  + (this.ship != null ? this.ship.getId() : "null")
                  + " karma value from increased from low to high ("
                  + prevKarma
                  + " -> "
                  + karma
                  + ")，againset toFORMINGstate");
        }
        fieldState.setAnimationState(AnimationState.FORMING);
      }
      ship.setCustomData("FSD_ReflectLight_PrevKarma", karma);
      if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) {
        if (fieldState.getAnimState() != AnimationState.DISSOLVING
            && fieldState.getAnimState() != AnimationState.INACTIVE) {
          if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null)
            FSD_ReflectLight.log.info(
                "[FSD][FSD_ReflectLight_Renderer] advance() - ship "
                    + (this.ship != null ? this.ship.getId() : "null")
                    + " lost hullmod，state from  "
                    + fieldState.getAnimState()
                    + " set to DISSOLVING");
          fieldState.setAnimationState(AnimationState.DISSOLVING);
        }
      }
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING
          && FSD_ReflectLight.log != null
          && Global.getCombatEngine() != null
          && Global.getCombatEngine().getTotalElapsedTime(false) % 1.0f
              < (amount > 0 ? amount : 0.017f)) {
        FSD_ReflectLight.log.info(
            "[FSD][FSD_ReflectLight_Renderer] advance() called，ship "
                + (this.ship != null ? this.ship.getId() : "null")
                + ", animationstate: "
                + fieldState.getAnimState());
      }
    }

    @Override
    public boolean isExpired() {
      boolean expired =
          this.fieldState.getAnimState() == AnimationState.INACTIVE
              && this.fieldState.getAnimProgress() >= 0.99f;
      if (expired && FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info(
            "[FSD][FSD_ReflectLight_Renderer] instance already expired，ship "
                + (this.ship != null ? this.ship.getId() : "null"));
      }
      return expired;
    }

    private void synchronizePlayerState(String caller) {
      if (ship == null || Global.getCombatEngine() == null) {
        return;
      }
      ShipAPI currentPlayerShip = Global.getCombatEngine().getPlayerShip();
      boolean isCurrentlyPlayerShip = (ship == currentPlayerShip);
      if (fieldState.isPlayerShip() != isCurrentlyPlayerShip) {
        if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
          FSD_ReflectLight.log.info(
              "[FSD][FSD_ReflectLight_Renderer] "
                  + caller
                  + "detectedplayer statechanged，ship "
                  + (this.ship != null ? this.ship.getId() : "null")
                  + " player state from  "
                  + fieldState.isPlayerShip()
                  + " updated to "
                  + isCurrentlyPlayerShip);
        }
        updatePlayerStatus(isCurrentlyPlayerShip);
      }
    }

    private boolean handleTerminatedShip(String caller, float stateAdvanceAmount) {
      if (ship == null || !ship.isAlive() || ship.isHulk()) {
        if (fieldState.getAnimState() != AnimationState.DISSOLVING
            && fieldState.getAnimState() != AnimationState.INACTIVE) {
          if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
            FSD_ReflectLight.log.info(
                "[FSD][FSD_ReflectLight_Renderer] "
                    + caller
                    + "ship "
                    + (this.ship != null ? this.ship.getId() : "null")
                    + " destroyed/became wreck，state from  "
                    + fieldState.getAnimState()
                    + " set to DISSOLVING");
          }
          fieldState.setAnimationState(AnimationState.DISSOLVING);
        }
        if (stateAdvanceAmount > 0f) {
          this.fieldState.updateState(stateAdvanceAmount);
        }
        return true;
      }
      return false;
    }
  }

  public static boolean isInRefit(ShipAPI ship) {
    return ship.getOriginalOwner() == -1 && Global.getCurrentState() != GameState.COMBAT;
  }

  private static final KarmaSystemAPI karmaSystem = KarmaManager.getInstance();

  /**
   * getkarma systeminstance
   */
  @Deprecated
  public static KarmaSystemAPI getKarmaSystem() {
    return karmaSystem;
  }

  /**
   * getship current karmavalue
   * @deprecated use KarmaAPI.getKarma(ship) instead
   */
  @Deprecated
  public static float getKarma(ShipAPI ship) {
    return KarmaAPI.getKarma(ship);
  }

  /**
   * setship karma value
   * @deprecated use KarmaAPI.setKarma(ship, karma) instead
   */
  @Deprecated
  public static void setKarma(ShipAPI ship, float karma) {
    KarmaAPI.setKarma(ship, karma);
  }

  /**
   * @deprecated use KarmaAPI.addKarma(ship, amount) instead
   */
  @Deprecated
  public static float addKarma(ShipAPI ship, float amount) {
    return KarmaAPI.addKarma(ship, amount);
  }

  /**
   * @deprecated use KarmaAPI.reduceKarma(ship, amount) instead
   */
  @Deprecated
  public static float reduceKarma(ShipAPI ship, float amount) {
    return KarmaAPI.reduceKarma(ship, amount);
  }

  /**
   * @deprecated use KarmaAPI.getKarmaPercent(ship) instead
   */
  @Deprecated
  public static float getKarmaPercent(ShipAPI ship) {
    return KarmaAPI.getKarmaPercent(ship);
  }

  /**
   * @deprecated use KarmaAPI.hasKarma(ship, amount) instead
   */
  @Deprecated
  public static boolean hasKarma(ShipAPI ship, float amount) {
    return KarmaAPI.hasKarma(ship, amount);
  }

  /**
   * consumekarma
   * @deprecated use KarmaAPI.consumeKarma(ship, amount) instead
   */
  @Deprecated
  public static float consumeKarma(ShipAPI ship, float amount) {
    return KarmaAPI.consumeKarma(ship, amount);
  }

  /**
   * getkarmadata
   * @deprecated use KarmaAPI.getKarmaData(ship) instead
   */
  @Deprecated
  public static KarmaData getKarmaData(ShipAPI ship) {
    return KarmaAPI.getKarmaData(ship);
  }

  public static KarmaStatistics getKarmaStatistics(ShipAPI ship) {
    KarmaData data = karmaSystem.getKarmaData(ship);
    if (data == null) {
      return null;
    }
    return new KarmaStatistics(
        data.getInitialKarma(),
        data.getCombatGainedKarma(),
        data.getKillGainedKarma(),
        data.getPassiveLostKarma(),
        data.getActiveConsumedKarma(),
        data.getExternalModifiedKarma());
  }

  public static class KarmaStatistics {
    public final float initial;
    public final float combatGained;
    public final float killGained;
    public final float passiveLost;
    public final float activeConsumed;
    public final float externalModified;

    public KarmaStatistics(
        float initial,
        float combatGained,
        float killGained,
        float passiveLost,
        float activeConsumed,
        float externalModified) {
      this.initial = initial;
      this.combatGained = combatGained;
      this.killGained = killGained;
      this.passiveLost = passiveLost;
      this.activeConsumed = activeConsumed;
      this.externalModified = externalModified;
    }

    public float getTotalGained() {
      return initial + combatGained + killGained + externalModified;
    }

    public float getTotalLost() {
      return passiveLost + activeConsumed;
    }

    public float getNetChange() {
      return getTotalGained() - getTotalLost();
    }
  }

  @Override
  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
//    if (ship.getVariant().hasHullMod("FSD_ReflectLight")
//        && ship.getVariant().hasHullMod("adaptive_coils")) {
//      ship.getVariant().removeMod("adaptive_coils");
//    }
//    if (ship.getVariant().hasHullMod("FSD_ReflectLight")
//        && ship.getVariant().hasHullMod("phase_anchor")) {
//      ship.getVariant().removeMod("phase_anchor");
//    }
    
  }

  @Override
  public boolean shouldAddDescriptionToTooltip(
      HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    Color y = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    Color postive = Misc.getPositiveHighlightColor();
    Color gray = Misc.getGrayColor();
    tooltip.addPara("The core of Farsight Drive ships: living prism, reactor, spatial computer, and battlefield appetite in one sealed body.", pad);
    tooltip.addSectionHeading("Base Effects", Alignment.MID, pad);
    tooltip.addPara(
        "During peak operating time, the ship restores %s of maximum hull per second.\nBy ship size, this can restore up to %s of maximum hull.",
        pads, Misc.getTextColor(), y, "0.8%/0.6%/0.6%/0.4%", "100%/75%/50%/25%");
    tooltip.addPara(
        "Every %s karma provides an additional %s flux dissipation, up to %s additional dissipation.",
        pads, Misc.getTextColor(), y, "1%", "5/10/15/20", "50%");
//    tooltip.addSectionHeading("Phase Enhancement", Alignment.MID, pad);
//    tooltip.addPara("Weapon recovery speed increases by %s while phased;", pads, Misc.getTextColor(), y, "100%");
//    tooltip.addPara("Incompatible with %s and %s!", pads, Misc.getTextColor(), r, "Phase Anchor", "Adaptive Phase Coils");
    tooltip.addSectionHeading("Karma Mechanics", Alignment.MID, pad);
    tooltip.addPara(
        "For every %s of base flux capacity dealt as damage (shield damage counts half), the ship gains proportional karma.\nKarma gain improves within %s of enemies, reaching maximum efficiency at %s for %s increased gain.",
        pads, Misc.getTextColor(), y, "10%", "1000su", "500su", "20%");
    tooltip.addPara(
        "The warship maintains a %s entropy field.\nCasualties inside the field generate %s for the ship.\n %s ",
        pads, Misc.getTextColor(), y, "1000su", "karma", "Only Farsight Drive special hullmods can spend stored karma.");
//    tooltip.addSectionHeading("Ship Karma Panel", Alignment.MID, pad);
    float col1 = 90f;
    float col2 = 90f;
    float col3 = 90f;
    float col4 = 90f;
    float col5 = 180f;
    float col6 = 180f;
    Color r1 = gray;
    Color r2 = gray;
    Color r3 = gray;
    Color r4 = gray;
    Color b1 = gray;
    Color b2 = gray;
    Color b3 = gray;
    Color b4 = gray;
//    if (ship.getHullSize() != null) {
//      switch (ship.getHullSize()) {
//        case FRIGATE:
//          r1 = y;
//          b1 = r;
//          break;
//        case DESTROYER:
//          r2 = y;
//          b2 = r;
//          break;
//        case CRUISER:
//          r3 = y;
//          b3 = r;
//          break;
//        case CAPITAL_SHIP:
//          r4 = y;
//          b4 = r;
//          break;
//      }
//    }
//    tooltip.beginTable(
//        Misc.getBasePlayerColor(),
//        Misc.getDarkPlayerColor(),
//        Misc.getBrightPlayerColor(),
//        20f,
//        true,
//        true,
//        new Object[] {
//          "Ship Size", col1, "Initial Karma", col2, "Ceasefire Time", col3, "Dissipation Amount", col4,
//        });
//    tooltip.addRow(
//        Alignment.MID,
//        r1,
//        "Frigate",
//        Alignment.MID,
//        r1,
//        "100%",
//        Alignment.MID,
//        b1,
//        "2s",
//        Alignment.MID,
//        b1,
//        "5%");
//    tooltip.addRow(
//        Alignment.MID,
//        r2,
//        "Destroyer",
//        Alignment.MID,
//        r2,
//        "75%",
//        Alignment.MID,
//        b2,
//        "4s",
//        Alignment.MID,
//        b2,
//        "4%");
//    tooltip.addRow(
//        Alignment.MID,
//        r3,
//        "Cruiser",
//        Alignment.MID,
//        r3,
//        "50%",
//        Alignment.MID,
//        b3,
//        "6s",
//        Alignment.MID,
//        b3,
//        "3%");
//    tooltip.addRow(
//        Alignment.MID,
//        r4,
//        "Capital",
//        Alignment.MID,
//        r4,
//        "25%",
//        Alignment.MID,
//        b4,
//        "8s",
//        Alignment.MID,
//        b4,
//        "2%");
//    tooltip.addTable("", 0, pad);
    tooltip.addSectionHeading("Karma From Kills and Disables", Alignment.MID, pad);
    tooltip.beginTable(
        Misc.getBasePlayerColor(),
        Misc.getDarkPlayerColor(),
        Misc.getBrightPlayerColor(),
        20f,
        true,
        true,
        new Object[] {"Destroyed Target Type", col5, "Karma Gain", col6});
    tooltip.addRow(Alignment.MID, y, "Frigate", Alignment.MID, postive, "10%");
    tooltip.addRow(Alignment.MID, y, "Destroyer", Alignment.MID, postive, "20%");
    tooltip.addRow(Alignment.MID, y, "Cruiser", Alignment.MID, postive, "30%");
    tooltip.addRow(Alignment.MID, y, "Capital", Alignment.MID, postive, "40%");
    tooltip.addTable("", 0, pad);
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine == null || engine.isPaused()) {
      return;
    }
    
    if (isInRefit(ship)) {
      return;
    }
    
    initializeCombatSystems(engine);
    updateSpatialGrid(amount);
    processShipsWithReflectLight(ship, engine, amount);
//    handlePhaseEffects(ship);
    handleShipRepair(ship, amount);
    processKarmaGain(ship); // processkarmaconversion
    handleKarmaDissipation(ship, amount); // processkarmadissipation
    cleanupExpiredRenderers(engine);
    manageKarmaSystem(engine, amount);
    updatePerformanceMonitoring();
  }

  private void initializeCombatSystems(CombatEngineAPI engine) {
    KarmaDamageReportListener.ensureRegistered(engine);
    if (!combatInitialized
        || (engine.getTotalElapsedTime(false) < 0.1f && lastCombatStartTime != engine.hashCode())) {
      if (ENABLE_DETAIL_LOGGING) log.info("[FSD][FSD_ReflectLight] reinitializedcombat systemcomponents");
      TrigCache.initialize();
      ObjectPool.initialize();
      try {
        GLStateManager.initialize();
        OptimizedBatchRenderer.initialize();
        SpatialGrid.initialize();
      } catch (Exception e) {
        if (ENABLE_DETAIL_LOGGING) log.error("[FSD][FSD_ReflectLight] OpenGLcomponentsinitialization failed", e);
      }
      ShipDetectionManager.initialize();
      fieldVisuals.clear();
      boolean isNewCombat =
          !combatInitialized || lastCombatStartTime != Global.getCombatEngine().hashCode();
      if (isNewCombat) {
        if (ENABLE_DETAIL_LOGGING) log.info("[FSD][FSD_ReflectLight] new combatstart，cleared all caches。");
        shipFixedSentences.clear();
        KarmaManager.getInstance().clear();
      }
      combatInitialized = true;
      lastCombatStartTime = engine.hashCode();
      if (ENABLE_DETAIL_LOGGING) log.info("[FSD][FSD_ReflectLight] combat system in combatstart when initialization/reinitialized。");
    }
  }

  private void updateSpatialGrid(float amount) {
    SpatialGrid.update(amount);
  }

  private void processShipsWithReflectLight(ShipAPI ship, CombatEngineAPI engine, float amount) {
    ShipAPI playerShip = engine.getPlayerShip();
    if (ship.getVariant().hasHullMod("FSD_ReflectLight") && ship.isAlive() && !ship.isHulk()) {
      ShipDetectionManager.enqueueShip(ship);
    }
    List<ShipAPI> shipsToProcessThisFrame = ShipDetectionManager.processQueue(amount);
    for (ShipAPI shipToProcess : shipsToProcessThisFrame) {
      if (shipToProcess == null
          || !shipToProcess.isAlive()
          || shipToProcess.isHulk()
          || !shipToProcess.getVariant().hasHullMod("FSD_ReflectLight")) {
        continue;
      }
      String rendererKeyForProcessed = getRendererKey(shipToProcess);
      EntropyFieldState fieldStateForProcessed = fieldVisuals.get(shipToProcess);
      if (fieldStateForProcessed == null) {
        boolean isPlayer = shipToProcess == playerShip;
        float rangeMultiplier =
            KarmaManager.getInstance().getKarmaData(shipToProcess).getGainKarmaRange();
        float range = getEntropyFieldRange(shipToProcess, rangeMultiplier);
        EntropyFieldState newState = new EntropyFieldState(shipToProcess, range, isPlayer);
        newState.setAnimationState(AnimationState.FORMING);
        fieldVisuals.put(shipToProcess, newState);
        fieldStateForProcessed = newState;
        EntropyFieldRenderer renderer = new EntropyFieldRenderer(newState, shipToProcess);
        engine.addLayeredRenderingPlugin(renderer);
        shipToProcess.setCustomData(rendererKeyForProcessed, renderer);
        if (ENABLE_DETAIL_LOGGING)
          log.info(
              "[FSD][FSD_ReflectLight] for ship "
                  + shipToProcess.getId()
                  + " created new EntropyFieldState and renderer (from processQueue)。player: "
                  + (shipToProcess == playerShip)
                  + ", initial animation state: "
                  + newState.getAnimState());
      }
      if (fieldStateForProcessed != null) {
        if (ENABLE_ENTROPY_FIELD_PROCESSOR) {
          if (ENABLE_DETAIL_LOGGING
              && engine.getTotalElapsedTime(false) % 1.0f < (amount > 0f ? amount : 0.017f))
            log.info(
                "[FSD][FSD_ReflectLight] for ship "
                    + shipToProcess.getId()
                    + " called fieldProcessor。current Karma: "
                    + fieldStateForProcessed.getDisplayKarma());
          fieldProcessor.detectAndProcessNearbyHulks(shipToProcess, fieldStateForProcessed);
          // applykillreward speed buff
          fieldProcessor.applySpeedBoost(shipToProcess);
        }
      }
    }
    String currentShipRendererKey = getRendererKey(ship);
    EntropyFieldState currentShipFieldState = fieldVisuals.get(ship);
    if (!ship.isAlive() || ship.isHulk() || !ship.getVariant().hasHullMod("FSD_ReflectLight")) {
      PredictiveQueryCache.removeCache(ship);
      if (currentShipFieldState != null) {
        if (currentShipFieldState.getAnimState() != AnimationState.DISSOLVING
            && currentShipFieldState.getAnimState() != AnimationState.INACTIVE) {
          if (ENABLE_DETAIL_LOGGING)
            log.info(
                "[FSD][FSD_ReflectLight] current ship "
                    + ship.getId()
                    + " destroyed/became wreck/lost hullmod。field state from  "
                    + currentShipFieldState.getAnimState()
                    + " set to DISSOLVING。ship alive: "
                    + ship.isAlive()
                    + ", is wreck: "
                    + ship.isHulk()
                    + ", has hullmod: "
                    + ship.getVariant().hasHullMod("FSD_ReflectLight"));
          currentShipFieldState.setAnimationState(AnimationState.DISSOLVING);
        }
      }
    } else {
      boolean isPlayerShip = ship == playerShip;
      if (currentShipFieldState == null) {
        float rangeMultiplier = KarmaManager.getInstance().getKarmaData(ship).getGainKarmaRange();
        float range = getEntropyFieldRange(ship, rangeMultiplier);
        EntropyFieldState newState = new EntropyFieldState(ship, range, isPlayerShip);
        newState.setAnimationState(AnimationState.FORMING);
        fieldVisuals.put(ship, newState);
        EntropyFieldRenderer renderer = new EntropyFieldRenderer(newState, ship);
        engine.addLayeredRenderingPlugin(renderer);
        ship.setCustomData(currentShipRendererKey, renderer);
        if (ENABLE_DETAIL_LOGGING)
          log.info(
              "[FSD][FSD_ReflectLight] for current ship "
                  + ship.getId()
                  + " created new EntropyFieldState and renderer (fallback)。player: "
                  + isPlayerShip
                  + ", initial animation state: "
                  + newState.getAnimState());
      }
    }
    // displayplayership karma systemstate
    if (ship == playerShip && ship.getVariant().hasHullMod("FSD_ReflectLight")) {
      displayPlayerKarmaStatus(ship, engine);
    }
  }

  private void handlePhaseEffects(ShipAPI ship) {
    boolean phased = ship.isPhased();
    MutableShipStatsAPI stats = ship.getMutableStats();
    String id = "FSD_ReflectLight";
    if (phased) {
      stats.getBallisticRoFMult().modifyMult(id, RELOAD_MULT);
      stats.getEnergyRoFMult().modifyMult(id, RELOAD_MULT);
      stats.getMissileRoFMult().modifyMult(id, RELOAD_MULT);
      stats.getBallisticAmmoRegenMult().modifyMult(id, RELOAD_MULT);
      stats.getEnergyAmmoRegenMult().modifyMult(id, RELOAD_MULT);
      stats.getMissileAmmoRegenMult().modifyMult(id, RELOAD_MULT);
    } else {
      stats.getBallisticRoFMult().unmodifyMult(id);
      stats.getEnergyRoFMult().unmodifyMult(id);
      stats.getMissileRoFMult().unmodifyMult(id);
      stats.getBallisticAmmoRegenMult().unmodifyMult(id);
      stats.getEnergyAmmoRegenMult().unmodifyMult(id);
      stats.getMissileAmmoRegenMult().unmodifyMult(id);
    }
  }

  private void handleShipRepair(ShipAPI ship, float amount) {
    if (ship.getPeakTimeRemaining() >= 0f) {
      HullSize hullSize = ship.getHullSize();
      float maxHP = ship.getMaxHitpoints();
      boolean phased = ship.isPhased();
      float totalRepair = 0f;
      if (ship.getCustomData().containsKey("FSD_TotalRepair")) {
        totalRepair = (float) ship.getCustomData().get("FSD_TotalRepair");
      }
      float repairSpeedMult;
      if (ship.getCustomData().containsKey("FSD_REPAIR_SPEED_MULT")) {
        @SuppressWarnings("unchecked")
        Map<ShipAPI.HullSize, Float> customRepairMult =
            (Map<ShipAPI.HullSize, Float>) ship.getCustomData().get("FSD_REPAIR_SPEED_MULT");
        repairSpeedMult =
            customRepairMult.containsKey(hullSize)
                ? customRepairMult.get(hullSize)
                : REPAIR_SPEED_MULT.get(hullSize);
      } else {
        repairSpeedMult = REPAIR_SPEED_MULT.get(hullSize);
      }
      float repairCapMultiplier;
      switch (hullSize) {
        case FRIGATE:
          repairCapMultiplier = 1.0f;
          break;
        case DESTROYER:
          repairCapMultiplier = 0.75f;
          break;
        case CRUISER:
          repairCapMultiplier = 0.5f;
          break;
        case CAPITAL_SHIP:
          repairCapMultiplier = 0.25f;
          break;
        default:
          repairCapMultiplier = 1.0f;
          break;
      }
      float maxRepairAmount = maxHP * repairCapMultiplier;
      if (totalRepair < maxRepairAmount) {
        float residualHP = maxRepairAmount - totalRepair;
        float repairMultiplier = phased ? 1.5f : 1.0f;
        float realRepair = repairMultiplier * amount * repairSpeedMult * maxHP;
        realRepair = Math.min(realRepair, residualHP);
        totalRepair += realRepair;
        ship.setHitpoints(Math.min(ship.getHitpoints() + realRepair, maxHP));
        ship.setCustomData("FSD_TotalRepair", totalRepair);
      }
      if (!ship.isAlive()) {
        ship.setCustomData("FSD_TotalRepair", 0f);
      }
    }
  }

  private void cleanupExpiredRenderers(CombatEngineAPI engine) {
    Iterator<Map.Entry<ShipAPI, EntropyFieldState>> iter = fieldVisuals.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<ShipAPI, EntropyFieldState> entry = iter.next();
      ShipAPI mapShip = entry.getKey();
      EntropyFieldState mapState = entry.getValue();
      Object iterRendererObj = mapShip.getCustomData().get(getRendererKey(mapShip));
      boolean remove = false;
      if (!mapShip.isAlive()
          || mapShip.isHulk()
          || !mapShip.getVariant().hasHullMod("FSD_ReflectLight")) {
        if (mapState.getAnimState() != AnimationState.DISSOLVING
            && mapState.getAnimState() != AnimationState.INACTIVE) {
          if (ENABLE_DETAIL_LOGGING)
            log.info(
                "[FSD][FSD_ReflectLight] cleanup loop - ship "
                    + mapShip.getId()
                    + " destroyed/became wreck/lost hullmod ("
                    + !mapShip.isAlive()
                    + "/"
                    + mapShip.isHulk()
                    + "/"
                    + !mapShip.getVariant().hasHullMod("FSD_ReflectLight")
                    + ")。field state from  "
                    + mapState.getAnimState()
                    + " set to DISSOLVING");
          mapState.setAnimationState(AnimationState.DISSOLVING);
        }
      }
      if (mapState.getAnimState() == AnimationState.INACTIVE
          && mapState.getAnimProgress() >= 0.99f) {
        remove = true;
      }
      if (!remove && iterRendererObj instanceof EntropyFieldRenderer) {
        if (((EntropyFieldRenderer) iterRendererObj).isExpired()) {
          if (mapState.getAnimState() != AnimationState.DISSOLVING
              && mapState.getAnimState() != AnimationState.INACTIVE) {
            mapState.setAnimationState(AnimationState.DISSOLVING);
          }
        }
      } else if (iterRendererObj == null
          && mapState.getAnimState() == AnimationState.INACTIVE
          && mapState.getAnimProgress() >= 0.99f) {
        remove = true;
      }
      if (remove) {
        if (ENABLE_DETAIL_LOGGING)
          log.info(
              "[FSD][FSD_ReflectLight] cleanup loop - removing ship "
                  + mapShip.getId()
                  + "  field stateand renderer。final animation state: "
                  + mapState.getAnimState());
        iter.remove();
        if (iterRendererObj instanceof EveryFrameCombatPlugin) {
          engine.removePlugin((EveryFrameCombatPlugin) iterRendererObj);
        }
        mapShip.removeCustomData(getRendererKey(mapShip));
      }
    }
  }

  private void manageKarmaSystem(CombatEngineAPI engine, float amount) {
    KarmaManager karmaManager = KarmaManager.getInstance();
    karmaManager.autoManage(engine, amount);
  }

  private void updatePerformanceMonitoring() {
    if (ENABLE_DETAIL_LOGGING) {
      PerformanceMonitor.setEnabled(true);
      PerformanceMonitor.reportIfNeeded();
    } else {
      PerformanceMonitor.setEnabled(false);
    }
  }

  public static void SetUnapplicableHullMod(ShipAPI source, String thehullmod) {
    if (source.getVariant().getHullMods().contains(thehullmod)) {
      MagicIncompatibleHullmods.removeHullmodWithWarning(
          source.getVariant(), thehullmod, "FSD_ReflectLight");
    }
  }

  public static float getEntropyFieldRange(ShipAPI ship, float rangeMultiplier) {
    float baseRange =
        ENTROPY_FIELD_RANGE.containsKey(ship.getHullSize())
            ? ENTROPY_FIELD_RANGE.get(ship.getHullSize())
            : 900f;
    return baseRange * rangeMultiplier;
  }

  private static String getRendererKey(ShipAPI ship) {
    return "FSD_ReflectLight_Renderer_" + ship.getId();
  }

  // ==================== karmaconversionsystem ====================
  
  // karmaconversionconfig
  
  // distance bonusconfig
  private static final float DISTANCE_BONUS_RANGE = 1000f;
  private static final float DISTANCE_BONUS_MAX_RANGE = 500f;
  private static final float DISTANCE_BONUS_MAX = 0.2f;
  
  /**
   */
  private void processKarmaGain(ShipAPI ship) {
    KarmaManager manager = KarmaManager.getInstance();
    KarmaData karmaData = manager.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    // 1. getaccumulated damage
    float accumulatedDamage = karmaData.getKarmaDamageReport();
    if (accumulatedDamage <= 0) {
      return;
    }
    
    // 2. getbase fluxcapacity
    float baseFlux = ship.getHullSpec().getFluxCapacity();
    if (baseFlux <= 0) {
      return;
    }
    
    float distanceMultiplier = calculateDistanceBonus(ship);
    
    float gainKarmaMultiplier = karmaData.getGainKarma();
    
    float efficiencyMultiplier = karmaData.getEfficiencyMultiplier();
    
    float baseKarmaGain = accumulatedDamage / baseFlux;
    float karmaToAdd = baseKarmaGain * distanceMultiplier * gainKarmaMultiplier * efficiencyMultiplier;
    
    // 7. addkarma
    float oldKarma = karmaData.getKarma();
    karmaData.addKarma(karmaToAdd, KarmaType.COMBAT_GAIN);
    float newKarma = karmaData.getKarma();
    
    karmaData.setKarmaDamageReport(0f);
    
    if (ENABLE_DETAIL_LOGGING && log != null && karmaToAdd > 0.0001f) {
      String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
          ? ship.getName() 
          : ship.getHullSpec().getHullNameWithDashClass();
      log.info(String.format(
          "[Reflecting-Light Crystal-conversion] ship %s gained karma: %.2f%% → %.2f%% (+%.3f%%), damage=%.1f, base flux=%.0f, distance bonus=%.0f%%, Benefitsmultiplier=%.2f, efficiency=%.2f",
          shipName, 
          oldKarma * 100, 
          newKarma * 100, 
          karmaToAdd * 100,
          accumulatedDamage,
          baseFlux,
          (distanceMultiplier - 1.0f) * 100,
          gainKarmaMultiplier,
          efficiencyMultiplier));
    }
  }
  
  /**
   * calculationdistance bonus
   */
  private float calculateDistanceBonus(ShipAPI ship) {
    Object lastTargetObj = ship.getCustomData().get("FSD_ReflectLight_LastTarget");
    if (!(lastTargetObj instanceof ShipAPI)) {
      return 1.0f;
    }
    
    ShipAPI target = (ShipAPI) lastTargetObj;
    if (target == null || !target.isAlive() || target.isHulk()) {
      return 1.0f;
    }
    
    float dx = ship.getLocation().x - target.getLocation().x;
    float dy = ship.getLocation().y - target.getLocation().y;
    float distance = (float) Math.sqrt(dx * dx + dy * dy);
    
    if (distance >= DISTANCE_BONUS_RANGE) {
      return 1.0f;
    }
    
    if (distance <= DISTANCE_BONUS_MAX_RANGE) {
      return 1.0f + DISTANCE_BONUS_MAX;
    }
    
    float ratio = (DISTANCE_BONUS_RANGE - distance) / (DISTANCE_BONUS_RANGE - DISTANCE_BONUS_MAX_RANGE);
    return 1.0f + (DISTANCE_BONUS_MAX * ratio);
  }
  
  /**
   */
  private void applyFluxDissipationBonus(ShipAPI ship) {
    if (isInRefit(ship)) {
      return;
    }
    
    KarmaManager manager = KarmaManager.getInstance();
    KarmaData karmaData = manager.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    float karma = karmaData.getKarma();
    float karmaPercent = karma * 100f;
    HullSize hullSize = ship.getHullSize();
    MutableShipStatsAPI stats = ship.getMutableStats();
    
    String modId = "FSD_ReflectLight_FluxBonus";
    
    float baseDissipation = ship.getHullSpec().getFluxDissipation();
    float maxBonus = baseDissipation * 0.5f;
    
    float bonusPerPercent = 0f;
    switch (hullSize) {
      case FRIGATE:
        bonusPerPercent = 5f;
        break;
      case DESTROYER:
        bonusPerPercent = 10f;
        break;
      case CRUISER:
        bonusPerPercent = 15f;
        break;
      case CAPITAL_SHIP:
        bonusPerPercent = 20f;
        break;
      default:
        bonusPerPercent = 5f;
        break;
    }
    
    float bonusDissipation = Math.min(bonusPerPercent * karmaPercent, maxBonus);
    
    stats.getFluxDissipation().unmodify(modId);
    
    if (karma > 0 && bonusDissipation > 0) {
      stats.getFluxDissipation().modifyFlat(modId, bonusDissipation);
    }
  }
  
  // ==================== karmadissipationsystem ====================
  
  // karmadissipationconfig
  private static final java.util.Map<HullSize, Float> KARMA_CEASE_FIRE_TIME = new java.util.HashMap<>();
  private static final java.util.Map<HullSize, Float> KARMA_DISSIPATION_RATE = new java.util.HashMap<>();
  
  static {
    // Ceasefire Timeconfig（s）
    KARMA_CEASE_FIRE_TIME.put(HullSize.FRIGATE, 2.0f);
    KARMA_CEASE_FIRE_TIME.put(HullSize.DESTROYER, 4.0f);
    KARMA_CEASE_FIRE_TIME.put(HullSize.CRUISER, 6.0f);
    KARMA_CEASE_FIRE_TIME.put(HullSize.CAPITAL_SHIP, 8.0f);
    
    KARMA_DISSIPATION_RATE.put(HullSize.FRIGATE, 0.05f); // 5%
    KARMA_DISSIPATION_RATE.put(HullSize.DESTROYER, 0.04f); // 4%
    KARMA_DISSIPATION_RATE.put(HullSize.CRUISER, 0.03f); // 3%
    KARMA_DISSIPATION_RATE.put(HullSize.CAPITAL_SHIP, 0.02f); // 2%
  }
  
  /**
   * processkarmadissipation
   */
  private void handleKarmaDissipation(ShipAPI ship, float amount) {
    KarmaManager manager = KarmaManager.getInstance();
    KarmaData karmaData = manager.getKarmaData(ship);
    if (karmaData == null) {
      return;
    }

    float karma = karmaData.getKarma();
    if (karma <= 0||!ship.areSignificantEnemiesInRange()) {
      return;
    }
    
//    boolean isFiring = isWeaponFiring(ship);
    boolean isFiring = false;
    for (WeaponAPI weapon : ship.getAllWeapons()) {
        if(weapon.getChargeLevel()>=1){
            isFiring = true;
        }
    }


    String timerKey = "FSD_ReflectLight_CeaseFireTimer";
    Float ceaseFireTimer = 0f;
    if (ship.getCustomData().containsKey(timerKey)) {
      ceaseFireTimer = (Float) ship.getCustomData().get(timerKey);
    }
    
    HullSize hullSize = ship.getHullSize();
    Float ceaseFireThreshold = KARMA_CEASE_FIRE_TIME.get(hullSize);
    if (ceaseFireThreshold == null) {
      ceaseFireThreshold = 4.0f; // default4s
    }
    
    if (isFiring) {
      ceaseFireTimer = 0f;
      ship.setCustomData(timerKey, ceaseFireTimer);
      
      if (ENABLE_DETAIL_LOGGING && log != null && 
          Global.getCombatEngine().getTotalElapsedTime(false) % 5.0f < 0.1f) {
        String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
            ? ship.getName() 
            : ship.getHullSpec().getHullNameWithDashClass();
        log.info(String.format(
            "[Reflecting-Light Crystal-dissipation] ship %s is firing，timer reset",
            shipName));
      }
    } else {
      ceaseFireTimer += amount;
      ship.setCustomData(timerKey, ceaseFireTimer);
      
      if (ENABLE_DETAIL_LOGGING && log != null && 
          Global.getCombatEngine().getTotalElapsedTime(false) % 2.0f < 0.1f) {
        String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
            ? ship.getName() 
            : ship.getHullSpec().getHullNameWithDashClass();
        log.info(String.format(
            "[Reflecting-Light Crystal-dissipation] ship %s ceasefire active: %.1fs / %.1fs, karma: %.1f%%",
            shipName, ceaseFireTimer, ceaseFireThreshold, karma * 100));
      }
      
      if (ceaseFireTimer >= ceaseFireThreshold) {
        Float dissipationRate = KARMA_DISSIPATION_RATE.get(hullSize);
        if (dissipationRate == null) {
          dissipationRate = 0.04f; // default4%
        }
        
        float dissipationMult = karmaData.getDissipationKarmaMult(); // getdissipationmultiplier
        float karmaLoss = dissipationRate * amount * dissipationMult;
        
        if (ENABLE_DETAIL_LOGGING && log != null && 
            Global.getCombatEngine().getTotalElapsedTime(false) % 2.0f < 0.1f) {
          String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
              ? ship.getName() 
              : ship.getHullSpec().getHullNameWithDashClass();
          log.info(String.format(
              "[Reflecting-Light Crystal-dissipation-DEBUG] ship %s calculation：dissipationRate=%.4f, amount=%.4f, dissipationMult=%.2f, karmaLoss=%.6f",
              shipName, dissipationRate, amount, dissipationMult, karmaLoss));
        }
        
        float oldKarma = karma;
        karmaData.reduceKarma(karmaLoss, KarmaType.PASSIVE_LOSS);
        float newKarma = karmaData.getKarma();
        
        if (ENABLE_DETAIL_LOGGING && log != null) {
          String shipName = (ship.getName() != null && !ship.getName().isEmpty()) 
              ? ship.getName() 
              : ship.getHullSpec().getHullNameWithDashClass();
          
          if (karmaLoss > 0 && newKarma != oldKarma) {
            log.info(String.format(
                "[Reflecting-Light Crystal-dissipation] ship %s karma loss: %.1f%% → %.1f%% (this frame-%.3f%%), Ceasefire Time: %.1fs",
                shipName, 
                oldKarma * 100, 
                newKarma * 100, 
                karmaLoss * 100,
                ceaseFireTimer));
          } else if (Global.getCombatEngine().getTotalElapsedTime(false) % 2.0f < 0.1f) {
            log.warn(String.format(
                "[Reflecting-Light Crystal-dissipation-WARNING] ship %s should have dissipated but did not change！karmaLoss=%.6f, oldKarma=%.4f, newKarma=%.4f",
                shipName, karmaLoss, oldKarma, newKarma));
          }
        }
      }
    }
  }
  
  /**
   */
  private boolean isWeaponFiring(ShipAPI ship) {
    java.util.List<WeaponAPI> weapons = ship.getAllWeapons();
    if (weapons.isEmpty()) {
      return false;
    }
    
    final float CHARGE_THRESHOLD = 0.05f;
    for (WeaponAPI weapon : weapons) {
      if (weapon.isDecorative()) continue;
      if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL) continue;
      
      if (weapon.getChargeLevel() > CHARGE_THRESHOLD) {
        return true;
      }
    }
    
    return false;
  }
  
  // ==================== player statedisplay ====================
  
  /**
   * displayplayership karma systemstate
   */
  private void displayPlayerKarmaStatus(ShipAPI ship, CombatEngineAPI engine) {
    KarmaData karmaData = KarmaManager.getInstance().getKarmaData(ship);
    if (karmaData == null) {
      return;
    }
    
    HullSize hullSize = ship.getHullSize();
    
    // getdistance bonus
    float distanceBonus = calculateDistanceBonus(ship);
    
    String timerKey = "FSD_ReflectLight_CeaseFireTimer";
    float ceaseFireTimer = 0f;
    if (ship.getCustomData().containsKey(timerKey)) {
      ceaseFireTimer = (Float) ship.getCustomData().get(timerKey);
    }
    
    Float ceaseFireThreshold = KARMA_CEASE_FIRE_TIME.get(hullSize);
    if (ceaseFireThreshold == null) {
      ceaseFireThreshold = 4.0f;
    }
    
    Float dissipationRate = KARMA_DISSIPATION_RATE.get(hullSize);
    if (dissipationRate == null) {
      dissipationRate = 0.04f;
    }
    
    KarmaStatusDisplay.displayFullKarmaStatus(
        ship, engine, 
        distanceBonus,
        ceaseFireTimer, ceaseFireThreshold, dissipationRate,
        "ReflectLight");
  }
}
