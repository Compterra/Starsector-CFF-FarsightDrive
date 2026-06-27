package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.util.vector.Vector2f;

public class FSD_EntropyMatrix extends BaseHullMod {
  public static final float RANGE_MULTIPLIER = 2.0f;
  public static final float SMOD_RANGE_MULTIPLIER = 2.25f;
  public static final float KARMA_MULTIPLIER = 1.5f;
  public static final float SMOD_KARMA_MULTIPLIER = 1.65f;
  private static final float KARMA_PER_CIRCUIT = 0.10f;
  private static final float CIRCUIT_COOLDOWN = 5.0f;
  private static final int MAX_CIRCUITS = 10;
  private static final float WEAPON_DAMAGE_PENALTY = -15f;
  private static final float FLUX_PRODUCTION_PENALTY = -15f;
  private static final float DAMAGE_BUFF_PER_CIRCUIT = 7f;
  private static final float PROJECTILE_SPEED_BUFF_PER_CIRCUIT = 14f;
  private static final float KARMA_COST_VOID_BOMB = 0.33f;
  private static final float SMOD_KARMA_COST_VOID_BOMB = 0.40f;
  private static final int SMOD_MAX_CIRCUITS = 12;
  private static final float SMOD_CIRCUIT_COOLDOWN = 4.0f;
  private static final float SMOD_WEAPON_DAMAGE_PENALTY = -20f;
  private static final float SMOD_FLUX_PRODUCTION_PENALTY = -20f;
  private static final float VOID_BOMB_COOLDOWN = 15f;
  private static final float HULL_THRESHOLD_AUTO_BOMB = 0.15f;
  private static final float MANUAL_ENERGY_DAMAGE_BASE = 500f;
  private static final float MANUAL_ENERGY_DAMAGE_PER_CIRCUIT = 100f;
  private static final float MANUAL_EMP_DAMAGE_BASE = 500f;
  private static final float MANUAL_EMP_DAMAGE_PER_CIRCUIT = 50f;
  private static final float MANUAL_KNOCKBACK = 200f;
  private static final float MANUAL_RANGE = 1500f;
  private static final float MANUAL_RANGE_PER_CIRCUIT = 50f;
  private static final float MANUAL_OVERLOAD_BASE = 1f;
  private static final float MANUAL_OVERLOAD_PER_CIRCUIT = 0.2f;
  private static final float AUTO_ENERGY_DAMAGE_BASE = 0f;
  private static final float AUTO_ENERGY_DAMAGE_PER_CIRCUIT = 50f;
  private static final float AUTO_EMP_DAMAGE_BASE = 500f;
  private static final float AUTO_EMP_DAMAGE_PER_CIRCUIT = 500f;
  private static final float AUTO_KNOCKBACK = 1000f;
  private static final float AUTO_RANGE = 1000f;
  private static final float AUTO_OVERLOAD = 6f;
  private static final int VOID_BOMB_KEY_CODE = Keyboard.KEY_B;
  private static boolean hasInitializedGlobalListener = false;
  private static Map<Integer, Boolean> keyStates = new HashMap<>();
  private static KeyboardInputListener keyboardListener = null;

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    try {
      boolean sMod = isSMod(ship);
      ship.setCustomData("FSD_EntropyField_RangeMultiplier", sMod ? SMOD_RANGE_MULTIPLIER : RANGE_MULTIPLIER);
      KarmaAPI.addEfficiencyMultiplier(ship, "FSD_EntropyMatrix", sMod ? SMOD_KARMA_MULTIPLIER : KARMA_MULTIPLIER);
      if (!ship.hasListenerOfClass(FSD_EntropyMatrix_Listener.class)) {
        ship.addListener(new FSD_EntropyMatrix_Listener(ship, sMod));
        Global.getLogger(FSD_EntropyMatrix.class).info("[Entropy Matrix] added main listener toship: " + ship.getName());
      }
      if (!keyStates.containsKey(VOID_BOMB_KEY_CODE)) {
        keyStates.put(VOID_BOMB_KEY_CODE, false);
      }
      if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() != null) {
        boolean playerShipChanged =
            Global.getCombatEngine().getPlayerShip() != null
                && keyboardListener != null
                && !Global.getCombatEngine().getPlayerShip().hasListener(keyboardListener);
        if (ship == Global.getCombatEngine().getPlayerShip()
            || playerShipChanged
            || !hasInitializedGlobalListener) {
          ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
          if (keyboardListener != null && playerShip.hasListener(keyboardListener)) {
            playerShip.removeListener(keyboardListener);
            Global.getLogger(FSD_EntropyMatrix.class)
                .info("[Bombsystem]  from shipremoved old key listener from: " + playerShip.getName());
          }
          keyboardListener = new KeyboardInputListener();
          playerShip.addListener(keyboardListener);
          hasInitializedGlobalListener = true;
          Global.getLogger(FSD_EntropyMatrix.class)
              .info("[Bombsystem] initializationBombkey listeneraddtoship: " + playerShip.getName());
        }
      }
    } catch (Exception e) {
      Global.getLogger(FSD_EntropyMatrix.class).error("[Entropy Matrix] initializationerror: " + e.getMessage(), e);
    }
  }

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {}

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    return null;
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
    tooltip.addPara("Variant equipment from an unidentified high-output warship, adapted for use with standard crystals.", pads);
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara("1. Phase entropy field range %s; karma conversion efficiency %s.\n", pads, Misc.getTextColor(), y, "x2", "x1.5");
    tooltip.addSectionHeading("Special Ability", Alignment.MID, pad);
    tooltip.addPara(
        "1. Entropy Loop: at battle start, non-missile weapon output and flux generation are %s. Every %s accumulated karma grants one loop enhancement, increasing weapon output by %s and projectile speed by %s. Each enhancement requires a %s"
            + " cooldown, stacks up to %s times, and lasts for the battle.\n",
        pad, y, "-15%", "10%", "7%", "14%", "5s", "10");
    tooltip.addPara(
        "2. Singularity Bomb: press \"B\" to consume %s karma and manually detonate one V.O.M bomb (unavailable during forced vent). When the ship overloads or hull first drops below %s, it consumes %s"
            + " karma to automatically release one V.O.M bomb. Bomb cooldown is %s. V.O.M clears all projectiles in range and can inflict energy damage, EMP damage, impact force, and forced overload.\n",
        pad, y, "33%", "15%", "33%", "15s");
    tooltip.addSectionHeading("Bomb Effects", Alignment.MID, pad);
    tooltip.addSectionHeading("Manual Bomb", Alignment.MID, pads);
    tooltip.addPara("Energy damage: %s + loop stacks * %s", pads, y, "500", "100");
    tooltip.addPara("EMP damage: %s + loop stacks * %s", pads, y, "500", "50");
    tooltip.addPara("Knockback distance: %s", pads, y, "200");
    tooltip.addPara("Explosion radius: %s + loop stacks * %s", pads, y, "1500", "50");
    tooltip.addPara("Forced overload time: %s + loop stacks * %s", pads, y, "1s", "0.2s");
    tooltip.addSectionHeading("Automatic Bomb", Alignment.MID, pad);
    tooltip.addPara("Energy damage: %s + loop stacks * %s", pads, y, "0", "50");
    tooltip.addPara("EMP damage: %s + loop stacks * %s", pads, y, "500", "500");
    tooltip.addPara("Knockback distance: %s", pads, y, "1000");
    tooltip.addPara("Explosion radius: %s", pads, y, "1000");
    tooltip.addPara("Forced overload time: %s", pads, y, "6s");
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {}

  @Override
  public boolean isSModEffectAPenalty() {
    return true;
  }

  public static class KeyboardInputListener implements AdvanceableListener {
    private boolean wasPressed = false;

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      try {
        boolean isBPressed = Keyboard.isKeyDown(VOID_BOMB_KEY_CODE);
        if (isBPressed && !wasPressed) {
          Global.getLogger(FSD_EntropyMatrix.class).info("[Bombsystem] B key pressed，triggeredcheck");
          keyStates.put(VOID_BOMB_KEY_CODE, true);
        }
        keyStates.put(VOID_BOMB_KEY_CODE, isBPressed);
        wasPressed = isBPressed;
        if (isBPressed && Global.getCombatEngine().getPlayerShip() != null) {
          Global.getCombatEngine()
              .maintainStatusForPlayerShip(
                  "FSD_VoidBomb_KeyPress",
                  "graphics/icons/hullsys/temporal_shell.png",
                  "Bomb hotkey",
                  "B key pressed (state:" + keyStates.get(VOID_BOMB_KEY_CODE) + ")",
                  false);
        }
      } catch (Exception e) {
        Global.getLogger(FSD_EntropyMatrix.class).error("[Bombsystem] keyboard checkerror: " + e.getMessage(), e);
      }
    }
  }

  public static class FSD_EntropyMatrix_Listener
      implements AdvanceableListener, HullDamageAboutToBeTakenListener {
    private ShipAPI ship;
    private final String id = "FSD_EntropyMatrix_effect";
    private float lastKarma = 0.0f;
    private float accumulatedKarma = 0.0f;
    private int currentCircuits = 0;
    private float circuitProgress = 0f;
    private boolean circuitCooling = false;
    private float circuitCooldown = 0f;
    private boolean hasInitialKarma = false;
    private boolean autoVoidBombTriggered = false;
    private float voidBombCooldown = 0f;
    private boolean lastKeyBState = false;
    private Map<DamagingProjectileAPI, Float> projectileFadeInProgress = new HashMap<>();
    private Set<DamagingProjectileAPI> trackedProjectiles = new HashSet<>();
    private Vector2f voidBombLocation = null;
    private float voidBombCountdown = 0f;
    private float voidBombRange = 0f;
    private float voidBombEnergyDamage = 0f;
    private float voidBombEmpDamage = 0f;
    private float voidBombKnockback = 0f;
    private float voidBombOverloadTime = 0f;
    private boolean isManualBomb = false;
    private boolean hasAppliedEnergyDamage = false;
    private float empDamageDelay = 0f;
    private final IntervalUtil karmaCheckTimer = new IntervalUtil(0.2f, 0.2f);
    private VoidBombRenderer activeVoidBombRenderer = null;
    private final int maxCircuits;
    private final float circuitCooldownDuration;
    private final float weaponDamagePenalty;
    private final float fluxProductionPenalty;
    private final float voidBombKarmaCost;

    public FSD_EntropyMatrix_Listener(ShipAPI ship, boolean sMod) {
      this.ship = ship;
      this.maxCircuits = sMod ? SMOD_MAX_CIRCUITS : MAX_CIRCUITS;
      this.circuitCooldownDuration = sMod ? SMOD_CIRCUIT_COOLDOWN : CIRCUIT_COOLDOWN;
      this.weaponDamagePenalty = sMod ? SMOD_WEAPON_DAMAGE_PENALTY : WEAPON_DAMAGE_PENALTY;
      this.fluxProductionPenalty = sMod ? SMOD_FLUX_PRODUCTION_PENALTY : FLUX_PRODUCTION_PENALTY;
      this.voidBombKarmaCost = sMod ? SMOD_KARMA_COST_VOID_BOMB : KARMA_COST_VOID_BOMB;
      applyWeaponPenalties();
      this.accumulatedKarma = 0.0f;
      Global.getLogger(FSD_EntropyMatrix.class)
          .info(
              String.format(
                  "[Entropy Matrix] initialized listener - ship: %s, initial accumulated karma: %.2f", ship.getName(), accumulatedKarma));
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        removeWeaponEffects();
        ship.removeListener(this);
        return;
      }
      CombatEngineAPI engine = Global.getCombatEngine();
      if (voidBombLocation != null) {
        voidBombCountdown -= amount;
        if (ship == engine.getPlayerShip()) {
          engine.maintainStatusForPlayerShip(
              "FSD_VoidBomb_Countdown",
              null,
              "Singularity Bomb",
              String.format("Detonation countdown: %.1fs", voidBombCountdown),
              false);
        }
        if (voidBombCountdown <= 0f) {
          spawnVoidBombEffect(
              voidBombLocation,
              voidBombRange,
              voidBombEnergyDamage,
              voidBombEmpDamage,
              voidBombKnockback,
              voidBombOverloadTime,
              isManualBomb);
          voidBombLocation = null;
          hasAppliedEnergyDamage = false;
          empDamageDelay = 0f;
        }
      }
      if (hasAppliedEnergyDamage) {
        empDamageDelay += amount;
        if (empDamageDelay >= 0.5f) {
          applyEmpDamage();
          hasAppliedEnergyDamage = false;
        }
      }
      float karma = KarmaAPI.getKarma(ship);
      if (karma > 0f) {
        if (!hasInitialKarma) {
          hasInitialKarma = true;
          lastKarma = karma;
          Global.getLogger(FSD_EntropyMatrix.class)
              .info(String.format("[Entropy Matrix] ship %s initializationkarma value: %.2f", ship.getName(), karma));
          return;
        }
      }
      if (karma > lastKarma) {
        float gained = karma - lastKarma;
        accumulatedKarma += gained;
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(
                String.format(
                    "[Entropy Matrix] ship %s gained karma: +%.2f, current: %.2f, accumulated: %.2f",
                    ship.getName(), gained, karma, accumulatedKarma));
      }
      if (voidBombCooldown > 0f) {
        voidBombCooldown -= amount;
        if (voidBombCooldown <= 0f) {
          Global.getLogger(FSD_EntropyMatrix.class)
              .info(String.format("[Bombsystem] ship %s Bombcooldown complete", ship.getName()));
        }
      }
      handleEntropyCircuits(accumulatedKarma, amount);
      handleVoidBomb(karma, amount);
      if (voidBombCooldown <= 0f && karma >= voidBombKarmaCost) {
        if (!autoVoidBombTriggered && ship.getHullLevel() <= HULL_THRESHOLD_AUTO_BOMB) {
          Global.getLogger(FSD_EntropyMatrix.class)
              .info(
                  String.format(
                      "[Bombsystem] ship %s hull below threshold (%.1f%%), triggeredAutomatic Bomb",
                      ship.getName(), ship.getHullLevel() * 100f));
          triggerAutoVoidBomb(karma);
        }
        if (ship.getFluxTracker().isOverloaded()) {
          Global.getLogger(FSD_EntropyMatrix.class)
              .info(
                  String.format(
                      "[Bombsystem] ship %s overloaded，overload time: %.1fs, triggeredAutomatic Bomb",
                      ship.getName(), ship.getFluxTracker().getOverloadTimeRemaining()));
          triggerAutoVoidBomb(karma);
        }
      }
      lastKarma = karma;
    }

    private void handleEntropyCircuits(float totalKarma, float amount) {
      int targetCircuits = Math.min(maxCircuits, (int) (totalKarma / KARMA_PER_CIRCUIT));
      if (circuitCooling) {
        circuitCooldown -= amount;
        if (circuitCooldown <= 0f) {
          circuitCooling = false;
          Global.getLogger(FSD_EntropyMatrix.class)
              .info(String.format("[Entropy Loop] ship %s loop cooldown complete", ship.getName()));
        }
      }
      if (targetCircuits > currentCircuits && !circuitCooling) {
        currentCircuits++;
        applyCircuitEffects();
        circuitCooling = true;
        circuitCooldown = circuitCooldownDuration;
        Global.getCombatEngine()
            .addFloatingText(
                ship.getLocation(),
                "Entropy loop increase: " + currentCircuits + "/" + maxCircuits,
                15f,
                Color.MAGENTA,
                ship,
                1f,
                2f);
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(
                String.format(
                    "[Entropy Loop] ship %s loop increased: %d/%d (accumulated karma: %.2f)",
                    ship.getName(), currentCircuits, maxCircuits, totalKarma));
      }
      if (targetCircuits < currentCircuits) {
        currentCircuits = targetCircuits;
        applyCircuitEffects();
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(
                String.format(
                    "[Entropy Loop] ship %s loop decreased: %d/%d (accumulated karma: %.2f)",
                    ship.getName(), currentCircuits, maxCircuits, totalKarma));
      }
    }

    private void applyCircuitEffects() {
      removeWeaponEffects();
      applyWeaponPenalties();
      if (currentCircuits > 0) {
        float damageBuff = DAMAGE_BUFF_PER_CIRCUIT * currentCircuits;
        float projectileSpeedBuff = PROJECTILE_SPEED_BUFF_PER_CIRCUIT * currentCircuits;
        ship.getMutableStats()
            .getEnergyWeaponDamageMult()
            .modifyPercent(id + "_circuit", damageBuff);
        ship.getMutableStats()
            .getBallisticWeaponDamageMult()
            .modifyPercent(id + "_circuit", damageBuff);
        ship.getMutableStats()
            .getProjectileSpeedMult()
            .modifyPercent(id + "_circuit", projectileSpeedBuff);
      }
    }

    private void applyWeaponPenalties() {
      ship.getMutableStats()
          .getEnergyWeaponDamageMult()
          .modifyPercent(id + "_base", weaponDamagePenalty);
      ship.getMutableStats()
          .getBallisticWeaponDamageMult()
          .modifyPercent(id + "_base", weaponDamagePenalty);
      ship.getMutableStats()
          .getFluxDissipation()
          .modifyPercent(id + "_base", fluxProductionPenalty);
    }

    private void removeWeaponEffects() {
      ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(id + "_base");
      ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(id + "_base");
      ship.getMutableStats().getFluxDissipation().unmodify(id + "_base");
      ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(id + "_circuit");
      ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(id + "_circuit");
      ship.getMutableStats().getProjectileSpeedMult().unmodify(id + "_circuit");
    }

    private boolean checkVoidBombKeyPressed() {
      try {
        return Keyboard.isKeyDown(VOID_BOMB_KEY_CODE);
      } catch (Exception e) {
        Global.getLogger(FSD_EntropyMatrix.class).error("[Bombsystem] checkkey stateerror: " + e.getMessage());
        return false;
      }
    }

    private void handleVoidBomb(float karma, float amount) {
      if (ship == Global.getCombatEngine().getPlayerShip()) {
        try {
          boolean keyDownNow = checkVoidBombKeyPressed();
          boolean isKeyPressed =
              keyStates.containsKey(VOID_BOMB_KEY_CODE) ? keyStates.get(VOID_BOMB_KEY_CODE) : false;
          if (karma >= voidBombKarmaCost) {
            if (voidBombCooldown <= 0f) {
              if (ship.getFluxTracker().isVenting()) {
                Global.getCombatEngine()
                    .maintainStatusForPlayerShip("FSD_VoidBomb", null, "Singularity Bomb", "Cannot use while venting", true);
              } else {
                Global.getCombatEngine()
                    .maintainStatusForPlayerShip("FSD_VoidBomb", null, "Singularity Bomb", "Press B to trigger Singularity Bomb", false);
              }
            } else {
              Global.getCombatEngine()
                  .maintainStatusForPlayerShip(
                      "FSD_VoidBomb",
                      null,
                      "Singularity Bomb",
                      String.format("Cooling down %.1fs", voidBombCooldown),
                      true);
            }
          } else {
            Global.getCombatEngine()
                .maintainStatusForPlayerShip("FSD_VoidBomb", null, "Singularity Bomb", "Insufficient karma (requires 33%)", true);
          }
          boolean keyPressed = (keyDownNow && !lastKeyBState) || (isKeyPressed && !lastKeyBState);
          if (keyPressed
              && voidBombCooldown <= 0f
              && karma >= voidBombKarmaCost
              && !ship.getFluxTracker().isVenting()) {
            triggerManualVoidBomb(karma);
          }
          lastKeyBState = keyDownNow || isKeyPressed;
        } catch (Exception e) {
          Global.getLogger(FSD_EntropyMatrix.class)
              .error("[Bombsystem] processSingularity Bomblogicerror: " + e.getMessage(), e);
        }
      }
    }

    private void triggerManualVoidBomb(float karma) {
      try {
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(
                String.format(
                    "[Bombsystem] ship %s triggeredManual Bomb - karma: %.2f → %.2f, loop stacks: %d",
                    ship.getName(), karma, karma - voidBombKarmaCost, currentCircuits));
        KarmaAPI.consumeKarma(ship, voidBombKarmaCost);
        voidBombEnergyDamage =
            MANUAL_ENERGY_DAMAGE_BASE + currentCircuits * MANUAL_ENERGY_DAMAGE_PER_CIRCUIT;
        voidBombEmpDamage =
            MANUAL_EMP_DAMAGE_BASE + currentCircuits * MANUAL_EMP_DAMAGE_PER_CIRCUIT;
        voidBombRange = MANUAL_RANGE + currentCircuits * MANUAL_RANGE_PER_CIRCUIT;
        voidBombOverloadTime = MANUAL_OVERLOAD_BASE + currentCircuits * MANUAL_OVERLOAD_PER_CIRCUIT;
        voidBombKnockback = MANUAL_KNOCKBACK;
        voidBombLocation = new Vector2f(ship.getLocation());
        voidBombCountdown = 2f;
        isManualBomb = true;
        registerVoidBombRenderer(
            voidBombLocation,
            voidBombRange,
            voidBombEnergyDamage,
            voidBombEmpDamage,
            voidBombKnockback,
            voidBombOverloadTime,
            true);
        voidBombCooldown = VOID_BOMB_COOLDOWN;
        Global.getCombatEngine()
            .addFloatingText(ship.getLocation(), "V.O.M bomb activated", 20f, Color.RED, ship, 1f, 2f);
        Global.getSoundPlayer()
            .playSound(
                "system_emp_emitter_impact", 1.0f, 1.0f, ship.getLocation(), ship.getVelocity());
      } catch (Exception e) {
        Global.getLogger(FSD_EntropyMatrix.class).error("[Bombsystem] triggeredManual Bomberror: " + e.getMessage(), e);
      }
    }

    private void triggerAutoVoidBomb(float karma) {
      try {
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(
                String.format(
                    "[Bombsystem] ship %s triggeredAutomatic Bomb - karma: %.2f → %.2f, loop stacks: %d",
                    ship.getName(), karma, karma - voidBombKarmaCost, currentCircuits));
        KarmaAPI.consumeKarma(ship, voidBombKarmaCost);
        voidBombEnergyDamage =
            AUTO_ENERGY_DAMAGE_BASE + currentCircuits * AUTO_ENERGY_DAMAGE_PER_CIRCUIT;
        voidBombEmpDamage = AUTO_EMP_DAMAGE_BASE + currentCircuits * AUTO_EMP_DAMAGE_PER_CIRCUIT;
        voidBombRange = AUTO_RANGE;
        voidBombOverloadTime = AUTO_OVERLOAD;
        voidBombKnockback = AUTO_KNOCKBACK;
        voidBombLocation = new Vector2f(ship.getLocation());
        voidBombCountdown = 2f;
        isManualBomb = false;
        registerVoidBombRenderer(
            voidBombLocation,
            voidBombRange,
            voidBombEnergyDamage,
            voidBombEmpDamage,
            voidBombKnockback,
            voidBombOverloadTime,
            false);
        autoVoidBombTriggered = true;
        voidBombCooldown = VOID_BOMB_COOLDOWN;
        Global.getCombatEngine()
            .addFloatingText(ship.getLocation(), "Automatic V.O.M bomb triggered", 20f, Color.RED, ship, 1f, 2f);
        Global.getSoundPlayer()
            .playSound(
                "system_temporalshell_impact", 1.0f, 1.0f, ship.getLocation(), ship.getVelocity());
      } catch (Exception e) {
        Global.getLogger(FSD_EntropyMatrix.class).error("[Bombsystem] triggeredAutomatic Bomberror: " + e.getMessage(), e);
      }
    }

    private void registerVoidBombRenderer(
        Vector2f location,
        float range,
        float energyDamage,
        float empDamage,
        float knockback,
        float overloadTime,
        boolean isManual) {
      try {
        Global.getLogger(VoidBombRenderer.class)
            .info("[Bombrender] attempting to registerBombrenderer - location:" + location + ", range:" + range);
        if (activeVoidBombRenderer == null || activeVoidBombRenderer.isExpired()) {
          ShipAPI ship = FSD_EntropyMatrix_Listener.this.ship;
          activeVoidBombRenderer =
              new VoidBombRenderer(
                  location,
                  range,
                  energyDamage,
                  empDamage,
                  knockback,
                  overloadTime,
                  isManual,
                  ship,
                  Global.getCombatEngine());
          Global.getCombatEngine().addLayeredRenderingPlugin(activeVoidBombRenderer);
          Global.getLogger(VoidBombRenderer.class).info("[Bombrender] rendererregistered successfully!");
        } else {
          Global.getLogger(VoidBombRenderer.class).info("[Bombrender] active renderer already exists，skipping registration");
        }
      } catch (Exception e) {
        Global.getLogger(VoidBombRenderer.class).error("[Bombrender] renderer registration failed: " + e.getMessage(), e);
      }
    }

    private void applyBombEffects(
        Vector2f location,
        float range,
        float energyDamage,
        float empDamage,
        float knockback,
        float overloadTime,
        boolean isManual) {
      CombatEngineAPI engine = Global.getCombatEngine();
      List<ShipAPI> affectedShips = new ArrayList<>();
      for (ShipAPI target : engine.getShips()) {
        if (target.isPhased() || !target.isAlive()) continue;
        float distance = MathUtils.getDistance(target.getLocation(), location);
        if (distance <= range) {
          affectedShips.add(target);
          float distanceFactor = 1f - (distance / range);
          float scaledEnergy = energyDamage * distanceFactor;
          float scaledEMP = empDamage * distanceFactor;
          if (target.getOwner() != ship.getOwner()) {
            engine.applyDamage(
                target,
                target.getLocation(),
                scaledEnergy,
                DamageType.ENERGY,
                scaledEMP,
                true,
                false,
                ship);
            if (overloadTime > 0
                && !target.getFluxTracker().isOverloaded()
                && target.getFluxTracker().getCurrFlux()
                    > target.getFluxTracker().getMaxFlux() * 0.1f) {
              target.getFluxTracker().forceOverload(overloadTime);
              engine.addFloatingText(
                  target.getLocation(),
                  String.format("Overload: %.1fs", overloadTime),
                  15f,
                  Color.RED,
                  target,
                  1f,
                  2f);
            }
          }
          Vector2f knockbackVector =
              MathUtils.getPointOnCircumference(
                  new Vector2f(0, 0),
                  knockback * distanceFactor,
                  VectorUtils.getAngle(location, target.getLocation()));
          target.getVelocity().x += knockbackVector.x * (1f / target.getMass()) * 20f;
          target.getVelocity().y += knockbackVector.y * (1f / target.getMass()) * 20f;
        }
      }
      List<DamagingProjectileAPI> projectilesToRemove = new ArrayList<>();
      for (DamagingProjectileAPI proj : engine.getProjectiles()) {
        if (MathUtils.getDistance(proj.getLocation(), location) <= range * 1.5f) {
          projectilesToRemove.add(proj);
        }
      }
      for (DamagingProjectileAPI proj : projectilesToRemove) {
        engine.removeEntity(proj);
      }
      engine.addHitParticle(
          location, new Vector2f(0f, 0f), range * 0.6f, 0.8f, 2.0f, new Color(255, 100, 100, 150));
      engine.addHitParticle(
          location, new Vector2f(0f, 0f), range * 0.4f, 0.5f, 0.1f, new Color(255, 200, 200, 200));
      for (int i = 0; i < 10; i++) {
        float angle = (float) (Math.random() * 360f);
        float dist = (float) (Math.random() * range * 0.5f);
        Vector2f particleLoc = MathUtils.getPointOnCircumference(location, dist, angle);
        engine.addHitParticle(
            particleLoc,
            new Vector2f(0f, 0f),
            20f + (float) (Math.random() * 30f),
            0.7f,
            0.5f + (float) (Math.random() * 0.5f),
            new Color(255, 100 + (int) (Math.random() * 100), 50, 200));
      }
      Global.getLogger(FSD_EntropyMatrix.class)
          .info(
              String.format(
                  "[Bombsystem] Bomb Effectsapplied - affected ships: %d, cleared projectiles: %d",
                  affectedShips.size(), projectilesToRemove.size()));
      hasAppliedEnergyDamage = true;
      empDamageDelay = 0f;
    }

    private void spawnVoidBombEffect(
        Vector2f location,
        float range,
        float energyDamage,
        float empDamage,
        float knockback,
        float overloadTime,
        boolean isManual) {
      try {
        Global.getLogger(FSD_EntropyMatrix.class)
            .info(String.format("[Bombsystem] Bomb Effectswill be applied in renderer - location: [%.1f, %.1f]", location.x, location.y));
      } catch (Exception e) {
        Global.getLogger(FSD_EntropyMatrix.class).error("[Bombsystem] Bombtriggerederror: " + e.getMessage(), e);
      }
    }

    private void applyEmpDamage() {
      if (voidBombLocation == null) return;
      CombatEngineAPI engine = Global.getCombatEngine();
      List<ShipAPI> affectedShips = new ArrayList<>();
      for (ShipAPI target : engine.getShips()) {
        if (target == ship || target.isPhased() || !target.isAlive()) continue;
        float distance = MathUtils.getDistance(target.getLocation(), voidBombLocation);
        if (distance <= voidBombRange) {
          affectedShips.add(target);
          float distanceFactor = 1f - (distance / voidBombRange);
          if (target.getOwner() != ship.getOwner()) {
            engine.applyDamage(
                target,
                target.getLocation(),
                0f,
                DamageType.ENERGY,
                voidBombEmpDamage * distanceFactor,
                true,
                false,
                ship);
          }
        }
      }
      Global.getLogger(FSD_EntropyMatrix.class)
          .info(String.format("BombEMPdamage affected %d  ship(s) ship", affectedShips.size()));
    }

    private class VoidBombRenderer extends BaseCombatLayeredRenderingPlugin {
      private Vector2f location;
      private float range;
      private float countdown;
      private float totalDuration = 2.0f;
      private boolean isManualBomb;
      private float energyDamage, empDamage, knockback, overloadTime;
      private boolean hasExploded = false;
      private float postExplosionTime = 0f;
      private final float POST_EXPLOSION_DURATION = 1.8f;
      private ShipAPI sourceShip;
      private transient int shaderProgramID = 0;
      private transient int u_baseColor_location = -1;
      private transient int u_time_location = -1;
      private transient CombatEngineAPI engine;
      private transient int vaoID = 0;
      private transient int vboQuadID = 0;
      private transient int vboInstanceID = 0;
      private transient int a_position_location = -1;
      private transient int a_texCoord_location = -1;
      private transient int a_particlePos_location = -1;
      private transient int a_particleSize_location = -1;
      private transient int a_particleColor_location = -1;
      private transient int a_particleTime_location = -1;
      private transient int a_particleType_location = -1;
      private transient int u_useInstanceTime_location = -1;
      private transient java.nio.FloatBuffer instanceDataBuffer = null;
      private transient int maxInstances = 1000;
      private transient int activeInstanceCount = 0;
      private static final int INSTANCE_DATA_SIZE = 10;
      private final float[] QUAD_VERTICES = {
        -0.5f, -0.5f, 0.0f, 0.0f, 0.5f, -0.5f, 1.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f, 0.5f, 0.5f, 1.0f,
        1.0f, -0.5f, 0.5f, 0.0f, 1.0f, -0.5f, -0.5f, 0.0f, 0.0f
      };
      private transient List<ExplosionParticle> explosionParticles = new ArrayList<>();
      private transient java.util.Random explosionRng;
      private float particleEmissionTimer = 0f;
      private final float PARTICLE_EMISSION_DURATION = 0.7f;
      private final float PARTICLES_PER_SECOND_PER_EMITTER = 180;
      private final int NUM_EMITTER_ARMS = 6;
      private float timeSinceLastEmission = 0f;

      private class ExplosionParticle {
        Vector2f position;
        Vector2f velocity;
        float initialSize;
        float currentSize;
        float age;
        float maxAge;
        Color color;
        float initialAlpha;
        float currentAlpha;
        List<Vector2f> trailPositions;
        int maxTrailSegments = 12;
        float timeSinceLastTrailUpdate = 0f;
        float trailUpdateInterval = 0.015f;
        float decelerationFactor;
        float maxSizeMultiplier;
        float wobbleFrequency = 1.5f;
        float wobbleAmplitude = 2.0f;
        float colorVariation = 0.05f;
        int particleType = 0;

        public ExplosionParticle(
            Vector2f position,
            Vector2f velocity,
            float size,
            float maxAge,
            Color color,
            float alpha,
            float decelerationFactor,
            float maxSizeMultiplier) {
          this.position = new Vector2f(position);
          this.velocity = new Vector2f(velocity);
          this.initialSize = size;
          this.currentSize = size;
          this.maxAge = maxAge;
          this.age = 0f;
          this.color = color;
          this.initialAlpha = alpha;
          this.currentAlpha = alpha;
          this.trailPositions = new ArrayList<>();
          this.trailPositions.add(new Vector2f(position));
          this.decelerationFactor = decelerationFactor;
          this.maxSizeMultiplier = maxSizeMultiplier;
        }

        public void advance(float amount) {
          age += amount;
          float lifeProgress = age / maxAge;
          float turbulenceStrength = 10.0f * (1.0f - lifeProgress);
          float wobble =
              (float) Math.sin(age * wobbleFrequency) * wobbleAmplitude * (1.0f - lifeProgress);
          float wobbleX = (float) Math.sin(age * 1.5f) * turbulenceStrength + wobble;
          float wobbleY = (float) Math.cos(age * 1.7f) * turbulenceStrength + wobble;
          velocity.x += wobbleX * amount * 0.2f;
          velocity.y += wobbleY * amount * 0.2f;
          velocity.x *= (1f - amount * decelerationFactor);
          velocity.y *= (1f - amount * decelerationFactor);
          position.x += velocity.x * amount;
          position.y += velocity.y * amount;
          timeSinceLastTrailUpdate += amount;
          if (timeSinceLastTrailUpdate >= trailUpdateInterval) {
            timeSinceLastTrailUpdate = 0f;
            trailPositions.add(0, new Vector2f(position));
            if (trailPositions.size() > maxTrailSegments) {
              trailPositions.remove(trailPositions.size() - 1);
            }
          }
          float growthPhase = Math.min(1f, age / (maxAge * 0.2f));
          float shrinkPhase = Math.max(0f, (age - maxAge * 0.2f) / (maxAge * 0.8f));
          float pulseFactor = 1.0f + (float) Math.sin(age * 2.0f) * 0.05f;
          this.currentSize =
              initialSize
                  * (1f + growthPhase * (maxSizeMultiplier - 1f))
                  * (1f - shrinkPhase * 0.7f)
                  * pulseFactor;
          float alphaPulseFactor = 1.0f + (float) Math.sin(age * 3.0f) * 0.05f;
          this.currentAlpha =
              initialAlpha * (1f - (float) Math.pow(lifeProgress, 0.8f)) * alphaPulseFactor;
          if (this.currentSize < initialSize * 0.1f) this.currentSize = initialSize * 0.1f;
          if (this.currentSize < 1f) this.currentSize = 1f;
          if (this.currentSize > initialSize * maxSizeMultiplier)
            this.currentSize = initialSize * maxSizeMultiplier;
        }

        public boolean isExpired() {
          return age >= maxAge || currentAlpha <= 0.01f || currentSize < 0.5f;
        }
      }

      private String loadShaderFile(String path) throws IOException {
        try {
          Global.getLogger(VoidBombRenderer.class).info("[Bombinitialization] starting shader file load: " + path);
          java.io.InputStream stream = Global.getSettings().openStream(path);
          if (stream == null) {
            Global.getLogger(VoidBombRenderer.class).error("[Bombinitialization] unable to open shader stream: " + path);
            throw new IOException("unable to open shader stream: " + path);
          }
          StringBuilder source = new StringBuilder();
          byte[] buffer = new byte[1024];
          int read;
          while ((read = stream.read(buffer)) != -1) {
            source.append(new String(buffer, 0, read, "UTF-8"));
          }
          stream.close();
          String content = source.toString();
          Global.getLogger(VoidBombRenderer.class)
              .info("[Bombinitialization] shader file loaded: " + path + ", length: " + content.length() + "bytes");
          return content;
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombinitialization] shader file load failed: " + e.getMessage(), e);
          throw new IOException("shader file load failed: " + path, e);
        }
      }

      private void initBuffers() {
        try {
          Global.getLogger(VoidBombRenderer.class).info("[Bombinitialization] starting buffer initialization");
          if (instanceDataBuffer == null) {
            instanceDataBuffer = BufferUtils.createFloatBuffer(maxInstances * INSTANCE_DATA_SIZE);
            Global.getLogger(VoidBombRenderer.class)
                .info("[Bombinitialization] created instance data buffer: capacity=" + instanceDataBuffer.capacity());
          }
          vaoID = GL30.glGenVertexArrays();
          vboQuadID = GL15.glGenBuffers();
          vboInstanceID = GL15.glGenBuffers();
          Global.getLogger(VoidBombRenderer.class)
              .info(
                  "[Bombinitialization] created OpenGL objects: VAO="
                      + vaoID
                      + ", VBOquad="
                      + vboQuadID
                      + ", VBOinstance="
                      + vboInstanceID);
          GL30.glBindVertexArray(vaoID);
          GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboQuadID);
          java.nio.FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
          quadBuffer.put(QUAD_VERTICES).flip();
          GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadBuffer, GL15.GL_STATIC_DRAW);
          GL20.glVertexAttribPointer(a_position_location, 2, GL11.GL_FLOAT, false, 4 * 4, 0);
          GL20.glEnableVertexAttribArray(a_position_location);
          GL20.glVertexAttribPointer(a_texCoord_location, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4);
          GL20.glEnableVertexAttribArray(a_texCoord_location);
          GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboInstanceID);
          GL15.glBufferData(
              GL15.GL_ARRAY_BUFFER, instanceDataBuffer.capacity() * 4, GL15.GL_STREAM_DRAW);
          GL20.glVertexAttribPointer(
              a_particlePos_location, 2, GL11.GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 0);
          GL20.glEnableVertexAttribArray(a_particlePos_location);
          GL33.glVertexAttribDivisor(a_particlePos_location, 1);
          GL20.glVertexAttribPointer(
              a_particleSize_location, 1, GL11.GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 2 * 4);
          GL20.glEnableVertexAttribArray(a_particleSize_location);
          GL33.glVertexAttribDivisor(a_particleSize_location, 1);
          GL20.glVertexAttribPointer(
              a_particleColor_location, 4, GL11.GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 3 * 4);
          GL20.glEnableVertexAttribArray(a_particleColor_location);
          GL33.glVertexAttribDivisor(a_particleColor_location, 1);
          GL20.glVertexAttribPointer(
              a_particleTime_location, 1, GL11.GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 7 * 4);
          GL20.glEnableVertexAttribArray(a_particleTime_location);
          GL33.glVertexAttribDivisor(a_particleTime_location, 1);
          GL20.glVertexAttribPointer(
              a_particleType_location, 1, GL11.GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 8 * 4);
          GL20.glEnableVertexAttribArray(a_particleType_location);
          GL33.glVertexAttribDivisor(a_particleType_location, 1);
          GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
          GL30.glBindVertexArray(0);
          Global.getLogger(VoidBombRenderer.class).info("[Bombinitialization] OpenGL buffer initialization succeeded：VAO=" + vaoID);
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class)
              .error("[Bombinitialization] OpenGL buffer initialization failed: " + e.getMessage(), e);
        }
      }

      private void initShaders() {
        if (this.engine == null) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombrender] shaderinitialization failed: engine is null");
          return;
        }
        try {
          String vertexShaderSource = loadShaderFile("data/shaders/fsd_plasma_ball.vert");
          String fragmentShaderSource = loadShaderFile("data/shaders/fsd_plasma_ball.frag");
          Global.getLogger(VoidBombRenderer.class)
              .info(
                  "[Bombrender] loaded shader source: vertex shader="
                      + vertexShaderSource.length()
                      + "bytes, fragment shader="
                      + fragmentShaderSource.length()
                      + "bytes");
          int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
          GL20.glShaderSource(vertexShader, vertexShaderSource);
          GL20.glCompileShader(vertexShader);
          if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombrender] vertex shader compile error: " + GL20.glGetShaderInfoLog(vertexShader, 512));
            GL20.glDeleteShader(vertexShader);
            return;
          }
          int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
          GL20.glShaderSource(fragmentShader, fragmentShaderSource);
          GL20.glCompileShader(fragmentShader);
          if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombrender] fragment shader compile error: " + GL20.glGetShaderInfoLog(fragmentShader, 512));
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            return;
          }
          shaderProgramID = GL20.glCreateProgram();
          GL20.glAttachShader(shaderProgramID, vertexShader);
          GL20.glAttachShader(shaderProgramID, fragmentShader);
          GL20.glLinkProgram(shaderProgramID);
          if (GL20.glGetProgrami(shaderProgramID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombrender] shader program link error: " + GL20.glGetProgramInfoLog(shaderProgramID, 512));
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            GL20.glDeleteProgram(shaderProgramID);
            shaderProgramID = 0;
            return;
          }
          GL20.glDeleteShader(vertexShader);
          GL20.glDeleteShader(fragmentShader);
          u_baseColor_location = GL20.glGetUniformLocation(shaderProgramID, "u_baseColor");
          u_time_location = GL20.glGetUniformLocation(shaderProgramID, "u_time");
          u_useInstanceTime_location =
              GL20.glGetUniformLocation(shaderProgramID, "u_useInstanceTime");
          a_position_location = GL20.glGetAttribLocation(shaderProgramID, "a_position");
          a_texCoord_location = GL20.glGetAttribLocation(shaderProgramID, "a_texCoord");
          a_particlePos_location = GL20.glGetAttribLocation(shaderProgramID, "a_particlePos");
          a_particleSize_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleSize");
          a_particleColor_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleColor");
          a_particleTime_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleTime");
          a_particleType_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleType");
          Global.getLogger(VoidBombRenderer.class)
              .info(
                  "[Bombrender] shader uniform locations: u_baseColor="
                      + u_baseColor_location
                      + ", u_time="
                      + u_time_location
                      + ", u_useInstanceTime="
                      + u_useInstanceTime_location);
          Global.getLogger(VoidBombRenderer.class)
              .info(
                  "[Bombrender] shader attribute locations: a_position="
                      + a_position_location
                      + ", a_texCoord="
                      + a_texCoord_location
                      + ", a_particlePos="
                      + a_particlePos_location
                      + ", a_particleSize="
                      + a_particleSize_location
                      + ", a_particleColor="
                      + a_particleColor_location
                      + ", a_particleTime="
                      + a_particleTime_location
                      + ", a_particleType="
                      + a_particleType_location);
          if (u_baseColor_location == -1
              || u_time_location == -1
              || u_useInstanceTime_location == -1) {
            Global.getLogger(VoidBombRenderer.class).warn("[Bombrender] some uniforms not found");
          }
          if (a_position_location == -1
              || a_texCoord_location == -1
              || a_particlePos_location == -1
              || a_particleSize_location == -1
              || a_particleColor_location == -1
              || a_particleTime_location == -1
              || a_particleType_location == -1) {
            Global.getLogger(VoidBombRenderer.class).warn("[Bombrender] some attributes not found");
          }
          Global.getLogger(VoidBombRenderer.class).info("[Bombrender] shaderinitializationsuccess: program ID=" + shaderProgramID);
          initBuffers();
        } catch (IOException e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombrender] loadshaderfileerror: " + e.getMessage(), e);
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombrender] shaderinitializationunexpected error: " + e.getMessage(), e);
        }
      }

      public VoidBombRenderer(
          Vector2f location,
          float range,
          float energyDamage,
          float empDamage,
          float knockback,
          float overloadTime,
          boolean isManual,
          ShipAPI sourceShip,
          CombatEngineAPI engine) {
        this.location = new Vector2f(location);
        this.range = range;
        this.countdown = totalDuration;
        this.isManualBomb = isManual;
        this.energyDamage = energyDamage;
        this.empDamage = empDamage;
        this.knockback = knockback;
        this.overloadTime = overloadTime;
        this.sourceShip = sourceShip;
        this.engine = engine;
        this.explosionRng =
            new java.util.Random(
                sourceShip.getId().hashCode()
                    + (long) (Global.getCombatEngine().getTotalElapsedTime(false) * 1000L));
        this.instanceDataBuffer = BufferUtils.createFloatBuffer(maxInstances * INSTANCE_DATA_SIZE);
        Global.getLogger(VoidBombRenderer.class).info("[Bombinitialization] starting shader and buffer initialization");
        initShaders();
        Global.getLogger(VoidBombRenderer.class)
            .info(
                "[Bombinitialization] initialization complete: shader ID="
                    + shaderProgramID
                    + ", VAO="
                    + vaoID
                    + ", buffer is null="
                    + (instanceDataBuffer == null));
        if (shaderProgramID == 0 || vaoID == 0) {
          Global.getLogger(VoidBombRenderer.class).warn("[Bombinitialization] shaderinitialization failed，trysimpleinitialization");
          initSimpleRendering();
        }
      }

      private void initSimpleRendering() {
        try {
          String vertexSource =
              "attribute vec2 a_position;\n"
                  + "attribute vec2 a_texCoord;\n"
                  + "attribute vec2 a_particlePos;\n"
                  + "attribute float a_particleSize;\n"
                  + "attribute vec4 a_particleColor;\n"
                  + "attribute float a_particleTime;\n"
                  + "attribute float a_particleType;\n"
                  + "varying vec2 v_texCoord;\n"
                  + "varying vec4 v_color;\n"
                  + "varying float v_particleTime;\n"
                  + "varying float v_particleType;\n"
                  + "void main() {\n"
                  + "    vec2 position = a_position * a_particleSize + a_particlePos;\n"
                  + "    gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 0.0, 1.0);\n"
                  + "    v_texCoord = a_texCoord;\n"
                  + "    v_color = a_particleColor;\n"
                  + "    v_particleTime = a_particleTime;\n"
                  + "    v_particleType = a_particleType;\n"
                  + "}";
          String fragmentSource =
              "varying vec2 v_texCoord;\n"
                  + "varying vec4 v_color;\n"
                  + "varying float v_particleTime;\n"
                  + "varying float v_particleType;\n"
                  + "void main() {\n"
                  + "    vec2 uv = v_texCoord - 0.5;\n"
                  + "    float dist = length(uv);\n"
                  + "    float alpha = v_color.a * (1.0 - smoothstep(0.4, 0.5, dist));\n"
                  + "    gl_FragColor = vec4(v_color.rgb, alpha);\n"
                  + "}";
          int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
          GL20.glShaderSource(vertexShader, vertexSource);
          GL20.glCompileShader(vertexShader);
          if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombinitialization] simple vertex shader compile failed: " + GL20.glGetShaderInfoLog(vertexShader, 512));
            return;
          }
          int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
          GL20.glShaderSource(fragmentShader, fragmentSource);
          GL20.glCompileShader(fragmentShader);
          if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombinitialization] simple fragment shader compile failed: " + GL20.glGetShaderInfoLog(fragmentShader, 512));
            GL20.glDeleteShader(vertexShader);
            return;
          }
          shaderProgramID = GL20.glCreateProgram();
          GL20.glAttachShader(shaderProgramID, vertexShader);
          GL20.glAttachShader(shaderProgramID, fragmentShader);
          GL20.glLinkProgram(shaderProgramID);
          if (GL20.glGetProgrami(shaderProgramID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(VoidBombRenderer.class)
                .error("[Bombinitialization] simple shader program link failed: " + GL20.glGetProgramInfoLog(shaderProgramID, 512));
            shaderProgramID = 0;
            return;
          }
          a_position_location = GL20.glGetAttribLocation(shaderProgramID, "a_position");
          a_texCoord_location = GL20.glGetAttribLocation(shaderProgramID, "a_texCoord");
          a_particlePos_location = GL20.glGetAttribLocation(shaderProgramID, "a_particlePos");
          a_particleSize_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleSize");
          a_particleColor_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleColor");
          a_particleTime_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleTime");
          a_particleType_location = GL20.glGetAttribLocation(shaderProgramID, "a_particleType");
          initBuffers();
          Global.getLogger(VoidBombRenderer.class)
              .info("[Bombinitialization] simple render initialization succeeded: shader ID=" + shaderProgramID + ", VAO=" + vaoID);
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombinitialization] simple render initialization failed: " + e.getMessage(), e);
        }
      }

      @Override
      public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (!viewport.isNearViewport(location, range * 2f)) {
          if (Global.getCombatEngine().getTotalElapsedTime(false) % 3f < 0.1f) {
            Global.getLogger(VoidBombRenderer.class)
                .info("[Bombrender] location not  in viewportrange within : " + location + ", viewport in center: " + viewport.getCenter());
          }
          return;
        }
        try {
          if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER && !hasExploded) {
            Global.getLogger(VoidBombRenderer.class).info("[Bombrender] render spiral effect - countdown when : " + countdown);
            renderSpiralEffect();
          } else if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER && hasExploded) {
            Global.getLogger(VoidBombRenderer.class)
                .info(
                    "[Bombrender] renderexplosionEffect - particle count: "
                        + explosionParticles.size()
                        + ", instance count: "
                        + activeInstanceCount);
            renderExplosionEffect();
          }
        } catch (Exception e) {
          Global.getLogger(FSD_EntropyMatrix.class).error("[Bombrender] rendererror: " + e.getMessage(), e);
        }
      }

      private void renderSpiralEffect() {
        float t = 1f - (countdown / totalDuration);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float overallIntensity = 0.8f + 0.2f * (float) Math.sin(t * Math.PI * 1.5f);
        float brightnessFactor = 1.0f + 0.4f * t * t;
        int numArms = 7;
        float baseRotationSpeed = 25f;
        float spiralTightness = 1.2f;
        int segmentsPerArm = 30;
        float maxArmLength = range * 0.9f;
        float minArmLengthFactor = 0.02f;
        float currentMaxScreenRadius =
            maxArmLength
                * (minArmLengthFactor + (1.0f - minArmLengthFactor) * (1.0f - t) * (1.0f - t));
        if (currentMaxScreenRadius < maxArmLength * minArmLengthFactor)
          currentMaxScreenRadius = maxArmLength * minArmLengthFactor;
        float currentOverallRotationDeg = baseRotationSpeed * (totalDuration - countdown);
        GL11.glLineWidth(3.0f * overallIntensity * brightnessFactor * (1f - t * 0.5f));
        for (int i = 0; i < numArms; i++) {
          float armBaseAngleDeg = (float) i * (360f / numArms) + currentOverallRotationDeg;
          GL11.glBegin(GL11.GL_LINE_STRIP);
          for (int j = 0; j <= segmentsPerArm; j++) {
            float segmentProgress = (float) j / segmentsPerArm;
            float distFromCenter = segmentProgress * currentMaxScreenRadius;
            float spiralAngleOffsetDeg = distFromCenter * spiralTightness * (1f - t * 0.3f);
            float pointActualAngleRad =
                (float) Math.toRadians(armBaseAngleDeg - spiralAngleOffsetDeg);
            float x = location.x + (float) Math.cos(pointActualAngleRad) * distFromCenter;
            float y = location.y + (float) Math.sin(pointActualAngleRad) * distFromCenter;
            float alphaFactor = 0.5f + 0.5f * segmentProgress;
            float finalAlpha =
                (0.8f * alphaFactor * overallIntensity * brightnessFactor) * (1f - t * 0.3f);
            if (t > 0.9f) finalAlpha *= Math.max(0, (1f - (t - 0.9f) / 0.1f));
            if (finalAlpha < 0) finalAlpha = 0;
            if (finalAlpha > 1) finalAlpha = 1;
            if (distFromCenter < range * 0.01f && t > 0.5f) finalAlpha = 0;
            float r_val = Math.min(1f, 1.0f * brightnessFactor);
            float g_val = Math.min(1f, 0.2f * brightnessFactor * (1f - segmentProgress * 0.5f));
            float b_val = Math.min(1f, 0.1f * brightnessFactor * (1f - segmentProgress * 0.7f));
            GL11.glColor4f(r_val, g_val, b_val, finalAlpha);
            GL11.glVertex2f(x, y);
          }
          GL11.glEnd();
        }
        if (t > 0.5f) {
          float coreGlowSize = range * 0.05f * (1f + t) * overallIntensity;
          float coreGlowAlpha = 0.8f * overallIntensity * brightnessFactor * (t - 0.5f) * 2f;
          if (coreGlowAlpha > 1f) coreGlowAlpha = 1f;
          if (coreGlowAlpha < 0f) coreGlowAlpha = 0f;
          drawGLCircle(location.x, location.y, coreGlowSize, 1f, 0.3f, 0.2f, coreGlowAlpha);
          drawGLCircle(
              location.x, location.y, coreGlowSize * 0.6f, 1f, 0.5f, 0.4f, coreGlowAlpha * 0.7f);
        }
        GL11.glLineWidth(1f);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
      }

      private void renderExplosionEffect() {
        float t_explosion_progress = postExplosionTime / POST_EXPLOSION_DURATION;
        if (t_explosion_progress > 1f) t_explosion_progress = 1f;
        float overallFadeOut = 1f - t_explosion_progress;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
          if (explosionParticles.isEmpty() || shaderProgramID == 0 || vaoID == 0) {
            Global.getLogger(VoidBombRenderer.class)
                .warn(
                    "[Bombrender] unable to render explosion: particles for null="
                        + explosionParticles.isEmpty()
                        + ", shader program="
                        + shaderProgramID
                        + ", VAO="
                        + vaoID);
            return;
          }
          Global.getLogger(VoidBombRenderer.class)
              .info(
                  "[Bombrender] startrenderexplosionEffect: particle count="
                      + explosionParticles.size()
                      + ", progress="
                      + t_explosion_progress);
          GL11.glEnable(GL11.GL_BLEND);
          GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
          GL11.glDisable(GL11.GL_DEPTH_TEST);
          GL11.glDisable(GL11.GL_CULL_FACE);
          GL20.glUseProgram(shaderProgramID);
          if (u_time_location != -1) {
            float globalTime = Global.getCombatEngine().getTotalElapsedTime(false);
            GL20.glUniform1f(u_time_location, globalTime);
          }
          if (u_useInstanceTime_location != -1) {
            GL20.glUniform1f(u_useInstanceTime_location, 1.0f);
          }
          if (u_baseColor_location != -1) {
            GL20.glUniform4f(u_baseColor_location, 1.0f, 0.1f, 0.1f, overallFadeOut);
          }
          GL30.glBindVertexArray(vaoID);
          GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 6, activeInstanceCount);
          GL30.glBindVertexArray(0);
          GL20.glUseProgram(0);
          Global.getLogger(VoidBombRenderer.class)
              .info("[Bombrender] explosion effect render complete: instance count=" + activeInstanceCount);
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombrender] explosion effect render error: " + e.getMessage(), e);
        } finally {
          GL11.glPopAttrib();
        }
      }

      private float lifeProgress(ExplosionParticle p) {
        return p.age / p.maxAge;
      }

      private void drawGLCircle(
          float cx, float cy, float radius, float r, float g, float b, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(r, g, b, alpha);
        GL11.glVertex2f(cx, cy);
        int segments = 20;
        for (int i = 0; i <= segments; i++) {
          float angle = (float) (2 * Math.PI * i / segments);
          float x = cx + (float) Math.cos(angle) * radius;
          float y = cy + (float) Math.sin(angle) * radius;
          GL11.glColor4f(r, g, b, 0.0f);
          GL11.glVertex2f(x, y);
        }
        GL11.glEnd();
      }

      @Override
      public float getRenderRadius() {
        return range * 2f;
      }

      @Override
      public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(
            CombatEngineLayers.BELOW_SHIPS_LAYER, CombatEngineLayers.ABOVE_SHIPS_LAYER);
      }

      @Override
      public boolean isExpired() {
        boolean expired =
            countdown <= 0
                && postExplosionTime >= POST_EXPLOSION_DURATION
                && explosionParticles.isEmpty();
        if (expired && shaderProgramID > 0) {
          GL20.glDeleteProgram(shaderProgramID);
          shaderProgramID = 0;
          Global.getLogger(VoidBombRenderer.class).info("VoidBombRenderer shader program deleted.");
        }
        return expired;
      }

      @Override
      public void advance(float amount) {
        if (!hasExploded) {
          countdown -= amount;
          if (countdown <= 0f) {
            FSD_EntropyMatrix_Listener.this.applyBombEffects(
                location, range, energyDamage, empDamage, knockback, overloadTime, isManualBomb);
            hasExploded = true;
            particleEmissionTimer = 0f;
            timeSinceLastEmission = 0f;
            explosionParticles.clear();
            Global.getSoundPlayer()
                .playSound("explosion_from_damage", 1.0f, 1.0f, location, new Vector2f(0f, 0f));
            Global.getLogger(FSD_EntropyMatrix.class).info("[Bombrender] bomb exploded! location: " + location);
          }
        } else {
          postExplosionTime += amount;
          particleEmissionTimer += amount;
          if (particleEmissionTimer < PARTICLE_EMISSION_DURATION) {
            timeSinceLastEmission += amount;
            float effective_particles_per_second_per_emitter = 80f;
            float particlesToEmitPerArmThisStep =
                effective_particles_per_second_per_emitter * timeSinceLastEmission;
            if (particlesToEmitPerArmThisStep >= 1.0f) {
              int numToEmitPerArmActual = (int) particlesToEmitPerArmThisStep;
              timeSinceLastEmission = 0;
              float armStructureRotationSpeed = -120f;
              float currentGlobalArmStructureAngleDeg =
                  armStructureRotationSpeed * postExplosionTime;
              float angleBetweenArms = 360f / NUM_EMITTER_ARMS;
              for (int armIdx = 0; armIdx < NUM_EMITTER_ARMS; armIdx++) {
                float armCenterAngleDeg =
                    currentGlobalArmStructureAngleDeg + armIdx * angleBetweenArms;
                for (int i = 0; i < numToEmitPerArmActual; i++) {
                  float randomAngleOffset = (explosionRng.nextFloat() - 0.5f) * 10f;
                  float particleLaunchAngleDeg = armCenterAngleDeg + randomAngleOffset;
                  float particleLaunchAngleRad = (float) Math.toRadians(particleLaunchAngleDeg);
                  float offsetDistance = explosionRng.nextFloat() * 10f;
                  float offsetAngleRad = (float) Math.toRadians(explosionRng.nextFloat() * 360f);
                  Vector2f emissionCenter =
                      new Vector2f(
                          this.location.x + offsetDistance * (float) Math.cos(offsetAngleRad),
                          this.location.y + offsetDistance * (float) Math.sin(offsetAngleRad));
                  float particleSpeed = range * (0.4f + explosionRng.nextFloat() * 0.2f);
                  Vector2f particleVelocity =
                      new Vector2f(
                          (float) Math.cos(particleLaunchAngleRad) * particleSpeed,
                          (float) Math.sin(particleLaunchAngleRad) * particleSpeed);
                  float particleSize = 60f + explosionRng.nextFloat() * 20f;
                  float particleMaxAge = 2.0f + explosionRng.nextFloat() * 0.5f;
                  Color particleColor =
                      new Color(
                          220 + explosionRng.nextInt(30),
                          5 + explosionRng.nextInt(10),
                          5 + explosionRng.nextInt(10),
                          200 + explosionRng.nextInt(55));
                  float particleInitialAlpha = 0.9f + explosionRng.nextFloat() * 0.1f;
                  float pDeceleration = 0.1f + explosionRng.nextFloat() * 0.05f;
                  float pMaxSizeMult = 1.2f + explosionRng.nextFloat() * 0.2f;
                  explosionParticles.add(
                      new ExplosionParticle(
                          emissionCenter,
                          particleVelocity,
                          particleSize,
                          particleMaxAge,
                          particleColor,
                          particleInitialAlpha,
                          pDeceleration,
                          pMaxSizeMult));
                }
              }
            }
          }
          List<ExplosionParticle> toRemove = new ArrayList<>();
          for (ExplosionParticle p : explosionParticles) {
            p.advance(amount);
            if (p.isExpired()) {
              toRemove.add(p);
            }
          }
          explosionParticles.removeAll(toRemove);
          if (!explosionParticles.isEmpty()) {
            updateInstanceData();
          } else if (particleEmissionTimer >= PARTICLE_EMISSION_DURATION) {
            Global.getLogger(VoidBombRenderer.class)
                .info("[Bombrender] all particles expired: explosion time=" + postExplosionTime);
          }
        }
      }

      private void updateInstanceData() {
        if (explosionParticles.isEmpty() || instanceDataBuffer == null) {
          return;
        }
        try {
          instanceDataBuffer.clear();
          activeInstanceCount = 0;
          for (ExplosionParticle p : explosionParticles) {
            if (p.currentAlpha <= 0.005f || p.currentSize < 0.5f) continue;
            instanceDataBuffer.put(p.position.x);
            instanceDataBuffer.put(p.position.y);
            instanceDataBuffer.put(p.currentSize);
            float r = Math.min(1.0f, p.color.getRed() / 255f);
            float g = p.color.getGreen() / 255f * 0.2f;
            float b = p.color.getBlue() / 255f * 0.2f;
            instanceDataBuffer.put(r);
            instanceDataBuffer.put(g);
            instanceDataBuffer.put(b);
            instanceDataBuffer.put(p.currentAlpha);
            instanceDataBuffer.put(p.age / p.maxAge);
            instanceDataBuffer.put(0.0f);
            instanceDataBuffer.put(0.0f);
            activeInstanceCount++;
            if (p.trailPositions.size() >= 2 && activeInstanceCount < maxInstances - 20) {
              int trailSegments = Math.min(p.trailPositions.size(), 10);
              for (int i = 1; i < trailSegments; i++) {
                if (activeInstanceCount >= maxInstances) break;
                Vector2f trailPos = p.trailPositions.get(i);
                instanceDataBuffer.put(trailPos.x);
                instanceDataBuffer.put(trailPos.y);
                float trailSizeFactor = 1.0f - (float) i / trailSegments * 0.8f;
                instanceDataBuffer.put(p.currentSize * trailSizeFactor);
                float trailAlpha = p.currentAlpha * (1.0f - (float) i / trailSegments * 0.9f);
                instanceDataBuffer.put(r);
                instanceDataBuffer.put(g);
                instanceDataBuffer.put(b);
                instanceDataBuffer.put(trailAlpha);
                instanceDataBuffer.put(p.age / p.maxAge);
                instanceDataBuffer.put(1.0f);
                instanceDataBuffer.put(0.0f);
                activeInstanceCount++;
              }
            }
            if (activeInstanceCount >= maxInstances) break;
          }
          instanceDataBuffer.flip();
          GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboInstanceID);
          GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceDataBuffer);
          GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        } catch (Exception e) {
          Global.getLogger(VoidBombRenderer.class).error("[Bombrender] failed to update instance data: " + e.getMessage(), e);
        }
      }
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(
        Object param, ShipAPI ship, Vector2f point, float damageAmount) {
      return false;
    }
  }
}
