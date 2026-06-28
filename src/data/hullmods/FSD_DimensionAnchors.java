package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

public class FSD_DimensionAnchors extends BaseHullMod {//Abyssal Dimensional Anchor
  private static final float KARMA_THRESHOLD = 0.25f;
  private static final float KARMA_COST = 0.05f;
  private static final float SMOD_KARMA_COST = 0.07f;
  private static final float INTERVAL = 10f;
  private static final float SMOD_INTERVAL = 8f;
  private static final float PHASE_EFFICIENCY = 0.2f;
  private static final float SHIELD_CAP_FLUX_PERCENT = 0.75f;
  private static final float SMOD_SHIELD_CAP_FLUX_PERCENT = 0.85f;
  private static final float KINETIC_RESISTANCE = 0.1f;
  private static final float HIGH_EXPLOSIVE_RESISTANCE = 0.3f;
  private static final float ENERGY_RESISTANCE = 0.2f;
  private static final float FRAGMENTATION_RESISTANCE = 0f;

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    if (!ship.hasListenerOfClass(FSD_DimensionAnchors_listener.class)) {
      ship.addListener(new FSD_DimensionAnchors_listener(ship, isSMod(ship)));
    }
  }

  public void applyEffectsBeforeShipCreation(
      HullSize hullSize, MutableShipStatsAPI stats, String id) {
    if (isSMod(stats)) {
      stats.getFluxCapacity().modifyPercent(id + "_smod", 5f);
    }
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) return false;
//    if (!ship.getHullSpec().isPhase()) return false;
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) return "Can only be installed on ships with the \"Reflecting-Light Crystal\" hullmod";
//    if (!ship.getHullSpec().isPhase()) {
//      return "Can only be installed on phase ships";
//    }
    return null;
  }

  @Override
  public boolean shouldAddDescriptionToTooltip(
      ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
    float pad = 10.0f;
    float pads = 3.0f;
    Color y = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    Color g = Misc.getPositiveHighlightColor();
    Color gray = Misc.getGrayColor();
    tooltip.addPara("Installs auxiliary crystal organs that let the entropy field convert incoming pressure into defensive plating.", pads);
    tooltip.addSectionHeading("Special Ability", Alignment.MID, pad);
    tooltip.addPara(
        "When ship karma is at least %s,\nevery %s enemy projectiles inside the entropy field are converted into equivalent plating based on damage.\nEach conversion consumes %s karma.\n",
        pads, Misc.getTextColor(), y, "25%", "10s", "5%");
    tooltip.addPara("When installed on a phase ship, phasing can trigger one additional conversion at %s efficiency.\n", pads, Misc.getTextColor(), r, "20%");
    tooltip.addPara("Plating is capped at %s of flux capacity.", pads, Misc.getTextColor(), y, "75%");
    tooltip.addPara("S-mod effect: sealed abyss geometry increases the plating cap to %s and shortens the conversion interval to %s. Each conversion consumes %s karma.", pad, Misc.getTextColor(), y, "85%", "8 seconds", "7%");
    tooltip.addSectionHeading("Plating Attributes", Alignment.MID, pad);
    float col1 = 180f;
    float col2 = 180f;
    tooltip.beginTable(
        Misc.getBasePlayerColor(),
        Misc.getDarkPlayerColor(),
        Misc.getBrightPlayerColor(),
        20f,
        true,
        true,
        new Object[] {
          "Damage Type", col1, "Resistance", col2,
        });
    tooltip.addRow(Alignment.MID, y, "Kinetic", Alignment.MID, g, "x0.9");
    tooltip.addRow(Alignment.MID, y, "High Explosive", Alignment.MID, g, "x0.7");
    tooltip.addRow(Alignment.MID, y, "Energy", Alignment.MID, g, "x0.8");
    tooltip.addRow(Alignment.MID, y, "Fragmentation", Alignment.MID, r, "x1");
    tooltip.addTable("", 0, pad);
  }

  @Override
  public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {}

  @Override
  public boolean isSModEffectAPenalty() {
    return true;
  }

  public static class FSD_DimensionAnchors_listener
      implements AdvanceableListener, DamageTakenModifier {
    private ShipAPI ship;
    public final String id = "FSD_DimensionAnchors_listener_effect";
    float RANGE_FR = 700f;
    float RANGE_DD = 700f;
    float RANGE_CA = 700f;
    float range = 0f;
    boolean canUsePhase = true;
    float timeSinceLastAuto = 0f;
    float extraHitpoints = 0f;
    float maxShieldValue = 0f;
    boolean init = false;
    private final float interval;
    private final float karmaCost;
    private final float shieldCapFluxPercent;

    private FSD_DimensionAnchors_listener(ShipAPI ship, boolean sMod) {
      this.ship = ship;
      this.interval = sMod ? SMOD_INTERVAL : INTERVAL;
      this.karmaCost = sMod ? SMOD_KARMA_COST : KARMA_COST;
      this.shieldCapFluxPercent = sMod ? SMOD_SHIELD_CAP_FLUX_PERCENT : SHIELD_CAP_FLUX_PERCENT;
    }

    @Override
    public void advance(float amount) {
      if (Global.getCombatEngine() == null) return;
      if (ship.isHulk() || !ship.isAlive()) {
        ship.removeListenerOfClass(this.getClass());
        return;
      }
      if (!ship.getVariant().hasHullMod("FSD_ReflectLight")) {
        return;
      }
      if (!init) {
        init = true;
        switch (ship.getHullSize()) {
          case FRIGATE:
            range = RANGE_FR;
            break;
          case DESTROYER:
            range = RANGE_DD;
            break;
          default:
            range = RANGE_CA;
            break;
        }
        maxShieldValue =
            ship.getMutableStats().getFluxCapacity().getBaseValue() * shieldCapFluxPercent;
      }
      if (canUsePhase && ship.isPhased()) {
        canUsePhase = false;
        activatePhaseConversion();
      }
      if (!canUsePhase && !ship.isPhased()) {
        canUsePhase = true;
      }
      timeSinceLastAuto += amount;
      if (timeSinceLastAuto >= interval) {
        if (hasEnoughKarma()) {
          boolean success = activateAutoConversion();
          if (success) {
            consumeKarma();
            timeSinceLastAuto = 0f;
          }
        }
      }
      if (ship == Global.getCombatEngine().getPlayerShip()) {
        String statusIcon =
            extraHitpoints > 0
                ? "graphics/icons/hullsys/fortress_shield.png"
                : "graphics/icons/hullsys/damper_field.png";
        String statusTitle = "Abyssal Dimensional Anchor";
        StringBuilder statusDesc = new StringBuilder();
        if (extraHitpoints > 0) {
          statusDesc
              .append("Plating strength: ")
              .append((int) extraHitpoints)
              .append("/")
              .append((int) maxShieldValue);
        } else {
          statusDesc.append("Plating status: ");
          statusDesc.append(
              timeSinceLastAuto >= INTERVAL
                  ? "Ready"
                  : "Charging: " + (int) (interval - timeSinceLastAuto) + "s");
        }
        Global.getCombatEngine()
            .maintainStatusForPlayerShip(
                "fsd_dimension_anchors", statusIcon, statusTitle, statusDesc.toString(), false);
      }
    }

    private boolean hasEnoughKarma() {
      return KarmaAPI.hasKarma(ship, KARMA_THRESHOLD);
    }

    private void consumeKarma() {
      float consumed = KarmaAPI.consumeKarma(ship, karmaCost);
      CombatEngineAPI engine = Global.getCombatEngine();
      if (engine != null && ship == engine.getPlayerShip()) {
//        engine.addFloatingText(
//            ship.getLocation(), "Karma cost: -5%", 15f, new Color(150, 100, 255), ship, 0.5f, 1.0f);1111
        for (int i = 0; i < 10; i++) {
          float angle = (float) (Math.random() * Math.PI * 2);
          float distance = (float) (Math.random() * 50 + 50);
          Vector2f offset =
              new Vector2f((float) Math.cos(angle) * distance, (float) Math.sin(angle) * distance);
          Vector2f particlePos = Vector2f.add(ship.getLocation(), offset, null);
          engine.addHitParticle(
              particlePos,
              new Vector2f(0, 0),
              5f + (float) (Math.random() * 10),
              0.5f,
              0.5f + (float) (Math.random() * 0.5f),
              new Color(120, 80, 200, 200));
        }
      }
    }

    private void activatePhaseConversion() {
      convertProjectiles(PHASE_EFFICIENCY);
    }

    private boolean activateAutoConversion() {
      return convertProjectiles(1.0f);
    }

    private boolean convertProjectiles(float efficiencyMult) {
      CombatEngineAPI engine = Global.getCombatEngine();
      if (engine == null) return false;
      boolean anyConverted = false;
      float maxAddition = maxShieldValue - extraHitpoints;
      if (maxAddition <= 0) return false;
      float totalDamage = 0f;
      List<DamagingProjectileAPI> projectilesToRemove = new ArrayList<>();
      for (DamagingProjectileAPI proj : engine.getProjectiles()) {
        if (proj.getOwner() != 1) continue;
        if (Misc.getDistanceSq(ship.getLocation(), proj.getLocation()) > range * range) continue;
        projectilesToRemove.add(proj);
        totalDamage += proj.getDamage().getDamage() * efficiencyMult;
      }
      if (!projectilesToRemove.isEmpty()) {
        anyConverted = true;
        for (DamagingProjectileAPI proj : projectilesToRemove) {
          engine.removeEntity(proj);
        }
        totalDamage = Math.min(totalDamage, maxAddition);
        extraHitpoints += totalDamage;
        addConversionVisuals();
//        if (ship == Global.getCombatEngine().getPlayerShip()) {1111
//          String conversionType;
//          if (efficiencyMult < 1.0f) {
//            conversionType = "Phase conversion (20% efficiency)";
//          } else {
//            conversionType = "Active conversion (100% efficiency)";
//          }
//          engine.addFloatingText(
//              ship.getLocation(),
//              conversionType + " +" + (int) totalDamage,
//              15f,
//              new Color(236, 9, 75),
//              ship,
//              0.5f,
//              1.0f);
//        }
      }
      return anyConverted;
    }

    private void addConversionVisuals() {
      CombatEngineAPI engine = Global.getCombatEngine();
      if (engine == null) return;
      Color coreColor = new Color(236, 9, 75, 255);
      Color fringeColor = new Color(162, 3, 3, 255);
      float radius = range * 0.8f;
      int numParticles = 24;
      float angle = 0f;
      float angleIncrement = 360f / numParticles;
      for (int i = 0; i < numParticles; i++) {
        float particleRadius = radius * (0.9f + 0.2f * (float) Math.random());
        float xOffset = (float) Math.cos(Math.toRadians(angle)) * particleRadius;
        float yOffset = (float) Math.sin(Math.toRadians(angle)) * particleRadius;
        Vector2f particlePos =
            new Vector2f(ship.getLocation().x + xOffset, ship.getLocation().y + yOffset);
        float size = 5f + 10f * (float) Math.random();
        float duration = 0.5f + 0.5f * (float) Math.random();
        engine.addHitParticle(particlePos, new Vector2f(0, 0), size, 1.0f, duration, coreColor);
        size *= 1.5f;
        engine.addHitParticle(
            particlePos, new Vector2f(0, 0), size, 0.8f, duration * 0.7f, fringeColor);
        angle += angleIncrement;
      }
      Global.getSoundPlayer()
          .playSound("shield_raise", 1f, 1f, ship.getLocation(), ship.getVelocity());
    }

    @Override
    public String modifyDamageTaken(
        Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
      if (extraHitpoints <= 0) return null;
      addDamageVisuals(point, damage);
      float damageAmount = damage.getDamage();
      float reduction = 0f;
      switch (damage.getType()) {
        case KINETIC:
          reduction = KINETIC_RESISTANCE;
          break;
        case HIGH_EXPLOSIVE:
          reduction = HIGH_EXPLOSIVE_RESISTANCE;
          break;
        case ENERGY:
          reduction = ENERGY_RESISTANCE;
          break;
        case FRAGMENTATION:
          reduction = FRAGMENTATION_RESISTANCE;
          break;
        default:
          break;
      }
      float actualDamage = damageAmount * (1f - reduction);
      if (actualDamage > extraHitpoints) {
        float remainingDamage = actualDamage - extraHitpoints;
        extraHitpoints = 0f;
        damage.getModifier().modifyMult(id, remainingDamage / damageAmount);
      } else {
        extraHitpoints -= actualDamage;
        damage.getModifier().modifyMult(id, 0f);
        if (param instanceof ArmorGridAPI) {
          return "NO_EFFECT";
        }
      }
      return null;
    }

    private void addDamageVisuals(Vector2f point, DamageAPI damage) {
      CombatEngineAPI engine = Global.getCombatEngine();
      if (engine == null) return;
      Color hitCore = new Color(236, 9, 75, 255);
      Color hitFringe = new Color(162, 3, 3, 255);
      engine.addFloatingDamageText(point, damage.getDamage(), new Color(255, 0, 166), ship, null);
      engine.addHitParticle(point, new Vector2f(0, 0), 12f, 15f, 0.25f, hitCore);
      engine.addHitParticle(point, new Vector2f(0, 0), 18f, 10f, 0.5f, hitFringe);
      engine.addHitParticle(
          point, new Vector2f(0, 0), 25f, 7.5f, 0.75f, new Color(253, 117, 117, 255));
    }
  }
}
