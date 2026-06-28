package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.fsd_reflectlight_components.KarmaAPI;
import data.hullmods.fsd_reflectlight_components.KarmaManager;
import data.hullmods.fsd_reflectlight_components.KarmaType;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FSD_IonBurst extends BaseHullMod {
  private static Map<ShipAPI.HullSize, Float> speed = new HashMap<ShipAPI.HullSize, Float>();
  private static Map<ShipAPI.HullSize, Float> range = new HashMap<ShipAPI.HullSize, Float>();

  static {
    speed.put(ShipAPI.HullSize.FIGHTER, 0f);
    speed.put(ShipAPI.HullSize.FRIGATE, 25f);
    speed.put(ShipAPI.HullSize.DESTROYER, 20f);
    speed.put(ShipAPI.HullSize.CRUISER, 15f);
    speed.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
    range.put(ShipAPI.HullSize.FIGHTER, 9999f);
    range.put(ShipAPI.HullSize.FRIGATE, 400f);
    range.put(ShipAPI.HullSize.DESTROYER, 500f);
    range.put(ShipAPI.HullSize.CRUISER, 600f);
    range.put(ShipAPI.HullSize.CAPITAL_SHIP, 700f);
  }

  private static final float WEAPON_FLUX_COST_MULT = 0.5f;
  private static final float EXTRA_DAMAGE_MIN = 0.25f;
  private static final float EXTRA_DAMAGE_MAX = 0.5f;
  private static final float RANGE_MULT = 0f;
  private static final float FSD_MISSILE_ROF_PENALTY = 0.30f;
  private static final float FLUX_PENALTY_REDUCTION_PER_KARMA = 0.025f;
  private static final float ROF_PENALTY_REDUCTION_PER_KARMA = 0.02f;
  private static final float MISSILE_ROF_PENALTY_REDUCTION_PER_KARMA = 0.03f;
  private static final float SMOD_KARMA_DRAIN_PER_SECOND = 0.02f;
  private static final float BASE_KARMA_DRAIN_PER_SECOND = 0.015f;
  private static final float BASE_DAMAGE_TAKEN_MULT = 1.15f;
  private static final float SMOD_DAMAGE_TAKEN_MULT = 1.20f;
  private final IntervalUtil clock = new IntervalUtil(1f,1f);

  @Override
  public void applyEffectsBeforeShipCreation(
      ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
//    stats.getMaxSpeed().modifyFlat(id, (Float) speed.get(hullSize));
//    stats.getAcceleration().modifyFlat(id, (Float) speed.get(hullSize) * 2f);
//    stats.getDeceleration().modifyFlat(id, (Float) speed.get(hullSize) * 2f);
    stats.getWeaponRangeThreshold().modifyFlat(id, (Float) range.get(hullSize));
    stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);
//    stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
    stats.getVentRateMult().modifyMult(id, 0f);
    if (isSMod(stats)) {
      stats.getFluxDissipation().modifyPercent(id + "_smod", 10f);
    }
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
      ShipVariantAPI variant = ship.getVariant();
      if(variant.hasHullMod("safetyoverrides")){
          variant.removeMod("FSD_IonBurst");
      }
      if(variant.hasHullMod("FSD_IonBurst")){
          variant.removeMod("safetyoverrides");
      }
      if(variant.hasHullMod("FSD_LimitationOfTissueHyperplasia")){
          variant.removeMod("FSD_IonBurst");
      }
      if(variant.hasHullMod("FSD_IonBurst")){
          variant.removeMod("FSD_LimitationOfTissueHyperplasia");
      }
//    if (!ship.hasListenerOfClass(FSD_IonBurst_listener.class)) {
//      ship.addListener(new FSD_IonBurst_listener(ship, id));
//    }
  }
  private void KarmaDecrease(ShipAPI ship, float percent) {
      KarmaManager manager = KarmaManager.getInstance();
      float currentKarma = manager.getKarma(ship);
      float karmaMax = manager.getKarmaMax(ship);
      float decreaseAmount = percent * karmaMax;
      manager.reduceKarma(ship, decreaseAmount, KarmaType.PASSIVE_LOSS);
  }
  public void advanceInCombat(ShipAPI ship, float amount) {
      float karma = KarmaAPI.getKarma(ship);
      MutableShipStatsAPI stats = ship.getMutableStats();
      String id = ship.getId() + "_FSD_IonBurst";
      clock.advance(amount);
      if (clock.intervalElapsed()) {
          KarmaDecrease(ship, isSMod(ship) ? SMOD_KARMA_DRAIN_PER_SECOND : BASE_KARMA_DRAIN_PER_SECOND);
      }
      float damageTakenMult = isSMod(ship) ? SMOD_DAMAGE_TAKEN_MULT : BASE_DAMAGE_TAKEN_MULT;
      stats.getHullDamageTakenMult().modifyMult(id, damageTakenMult);
      stats.getArmorDamageTakenMult().modifyMult(id, damageTakenMult);
      stats.getShieldDamageTakenMult().modifyMult(id, damageTakenMult);
      stats.getEngineDamageTakenMult().modifyMult(id, damageTakenMult);
      if(karma <= 0f) {
          stats.getZeroFluxMinimumFluxLevel().unmodify(id);
          stats.getMaxSpeed().unmodify(id);
          stats.getAcceleration().unmodify(id);
          stats.getDeceleration().unmodify(id);
          stats.getTurnAcceleration().unmodify(id);
          stats.getMissileRoFMult().unmodify(id);
          stats.getEnergyRoFMult().unmodify(id);
          stats.getBallisticRoFMult().unmodify(id);
          stats.getHullDamageTakenMult().unmodify(id);
          stats.getArmorDamageTakenMult().unmodify(id);
          stats.getEngineDamageTakenMult().unmodify(id);
          stats.getDamageToCapital().unmodify(id);
          stats.getDamageToCruisers().unmodify(id);
          stats.getDamageToDestroyers().unmodify(id);
          stats.getDamageToFrigates().unmodify(id);
      }
      if(karma >= 0f) {
          stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
          stats.getMaxSpeed().modifyMult(id, 1.25f);
          stats.getAcceleration().modifyMult(id, 1.25f);
          stats.getDeceleration().modifyMult(id, 1.25f);
          stats.getTurnAcceleration().modifyMult(id, 1.25f);
          stats.getBallisticRoFMult().unmodify(id);
          stats.getMissileRoFMult().unmodify(id);
          stats.getEnergyRoFMult().unmodify(id);
          stats.getFluxDissipation().unmodify(id);
          stats.getDamageToCapital().unmodify(id);
          stats.getDamageToCruisers().unmodify(id);
          stats.getDamageToDestroyers().unmodify(id);
          stats.getDamageToFrigates().unmodify(id);
      }
      if(karma >= 0.25f) {
          float rofMult = isSMod(ship) ? 1.20f : 1.15f;
          stats.getMissileRoFMult().modifyMult(id, rofMult);
          stats.getEnergyRoFMult().modifyMult(id, rofMult);
          stats.getBallisticRoFMult().modifyMult(id, rofMult);
          stats.getFluxDissipation().unmodify(id);
          stats.getDamageToCapital().unmodify(id);
          stats.getDamageToCruisers().unmodify(id);
          stats.getDamageToDestroyers().unmodify(id);
          stats.getDamageToFrigates().unmodify(id);
      }
      if(karma >= 0.5f) {
          stats.getFluxDissipation().modifyMult(id, isSMod(ship) ? 1.6f : 1.5f);
//          stats.getHullDamageTakenMult().modifyMult(id, 0.9f);
//          stats.getArmorDamageTakenMult().modifyMult(id, 0.9f);
//          stats.getShieldDamageTakenMult().modifyMult(id, 0.9f);
//          stats.getEngineDamageTakenMult().modifyMult(id, 0.9f);
          stats.getDamageToCapital().unmodify(id);
          stats.getDamageToCruisers().unmodify(id);
          stats.getDamageToDestroyers().unmodify(id);
          stats.getDamageToFrigates().unmodify(id);
      }
      if(karma >= 0.75f) {
          float damageMult = isSMod(ship) ? 1.15f : 1.1f;
          stats.getDamageToCapital().modifyMult(id, damageMult);
          stats.getDamageToCruisers().modifyMult(id, damageMult);
          stats.getDamageToDestroyers().modifyMult(id, damageMult);
          stats.getDamageToFrigates().modifyMult(id, damageMult);
      }

  }

  @Override
  public boolean shouldAddDescriptionToTooltip(
      ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return false;
  }

  @Override
  public void addPostDescriptionSection(
      TooltipMakerAPI tooltip,
      ShipAPI.HullSize hullSize,
      ShipAPI ship,
      float width,
      boolean isForModSpec) {
    float pad = 10f;
    Color h = Misc.getHighlightColor();
    Color bad = Misc.getNegativeHighlightColor();
    Color g = Misc.getPositiveHighlightColor();
    tooltip.addPara("Pressurizes the crystal to raise short-term battlefield output, a dangerous procedure even by FSD standards.", pad);
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara(
        "Provides the following benefits based on ship karma level.", pad, h, "");
    float col1 = 90f;
    float col2 = 270f;
    tooltip.beginTable(
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Misc.getBrightPlayerColor(),
            20f,
            true,
            true,
            new Object[] {
                    "Karma Level", col1, "Benefit Effect", col2,
            });
    tooltip.addRow(
            Alignment.MID,
            h,
            "0-25%",
            Alignment.MID,
            g,
            "Enables zero-flux acceleration and increases maneuverability by 25%.");
    tooltip.addRow(
            Alignment.MID,
            h,
            "25-50%",
            Alignment.MID,
            g,
            "Weapon rate of fire increased by 15%.");
    tooltip.addRow(
            Alignment.MID,
            h,
            "50-75%",
            Alignment.MID,
            g,
            "Flux dissipation increased by 50%.");
    tooltip.addRow(
            Alignment.MID,
            h,
            "75-100%",
            Alignment.MID,
            g,
            "Damage dealt increased by 10%.");
    tooltip.addTable("", 0, pad);
    tooltip.addSectionHeading("Drawbacks", Alignment.MID, pad);
    tooltip.addPara("Limits weapon range to %s based on ship size.\nKarma drains by %s per second, forced venting is disabled, and damage taken increases by %s.", pad, bad, "400/500/600/700","1.5%","15%");
    tooltip.addPara("S-mod: Bloodline channels bite harder: upper-tier RoF, dissipation, and damage bonuses improve, but karma drain rises to %s per second and incoming damage rises to %s.", pad, bad, "2%", "20%");
  }
  @Override
  public boolean isSModEffectAPenalty() {
    return true;
  }


  /**
   */
  private boolean hasKarmaSystem(ShipAPI ship) {
    return ship.getVariant().hasHullMod("FSD_ReflectLight") 
        || ship.getVariant().hasHullMod("FSD_SecondaryKarmaInfusion");
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    return hasKarmaSystem(ship)
        && !ship.getVariant().hasHullMod("FSD_LimitationOfTissueHyperplasia")
//        && !ship.getVariant().hasHullMod("FSD_OverrunPosition");
        && !ship.getVariant().hasHullMod("safetyoverrides");
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    if (ship.getVariant().hasHullMod("FSD_LimitationOfTissueHyperplasia")) {
      return "Incompatible with the Sacrifice hull restructuring plan; current crystal stability cannot support it.";
    }
    if (ship.getVariant().hasHullMod("safetyoverrides")) {
        return "Incompatible with Safety Overrides; current crystal stability cannot support it.";
    }
//    if (ship.getVariant().hasHullMod("FSD_OverrunPosition")) {
//      return "Incompatible with the Sacrifice hull restructuring plan; crystal stability cannot support it.";
//    }
    else return "Must be installed on a ship with Reflecting-Light Crystal.";
  }

//  public static class FSD_IonBurst_listener implements DamageDealtModifier, AdvanceableListener {
//    private ShipAPI ship;
//    private String id;
//    private String stats;
//    private float active_timer = 0f;
//    private float cooling_timer = 0f;
//    private Object obj1 = new Object();
//
//    private FSD_IonBurst_listener(ShipAPI ship, String id) {
//      this.ship = ship;
//      this.id = id;
//      this.ship.setCustomData("FSD_Rampage_stats", "NOT_READY");
//    }
//
//    @Override
//    public void advance(float amount) {
//      CombatEngineAPI engine = Global.getCombatEngine();
//      if (engine == null || engine.isPaused()) return;
//      if (!ship.isAlive() || ship.isHulk()) return;
//      float karma = 0f;
//      switch ((String) this.ship.getCustomData().get("FSD_Rampage_stats")) {
//        case "NOT_READY":
//          {
//            stats = "Insufficient karma";
//            break;
//          }
//        case "READY":
//          {
//            stats = "Berserk ready: press B to activate";
//            break;
//          }
//        case "ACTIVE":
//          {
//            stats = "Berserk active:";
//            break;
//          }
//        case "COOLING":
//          {
//            stats = "Berserk cooling down:";
//            break;
//          }
//        case "PAUSE":
//          {
//            stats = "Phase state, Berserk paused";
//            break;
//          }
//      }
//      if (this.ship.equals(engine.getPlayerShip())) {
//        if (stats.equals("Berserk active:")) {
//          engine.maintainStatusForPlayerShip(
//              obj1,
//              "icon_tactical_engine_boost",
//              "Bloodline Berserk",
//              stats + Math.round(10f - this.active_timer) + "s",
//              false);
//        } else if (stats.equals("Berserk cooling down:")) {
//          engine.maintainStatusForPlayerShip(
//              obj1,
//              "icon_tactical_engine_boost",
//              "Bloodline Berserk",
//              stats + Math.round(20f - this.cooling_timer) + "s",
//              true);
//        } else {
//          engine.maintainStatusForPlayerShip(
//              obj1, "icon_tactical_engine_boost", "Bloodline Berserk", stats, false);
//        }
//      }
//      karma = KarmaAPI.getKarma(ship);
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("NOT_READY")) {
//        if (karma > 0.5f) {
//          this.ship.setCustomData("FSD_Rampage_stats", "READY");
//        }
//      }
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("READY")) {
//        if (karma < 0.5f) {
//          this.ship.setCustomData("FSD_Rampage_stats", "NOT_READY");
//        }
//      }
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("READY")
//          && Keyboard.isKeyDown(Keyboard.getKeyIndex("B"))) {
//        this.ship.setCustomData("FSD_Rampage_stats", "ACTIVE");
//        this.ship
//            .getMutableStats()
//            .getEnergyWeaponFluxCostMod()
//            .modifyMult(this.id, WEAPON_FLUX_COST_MULT);
//        this.ship
//            .getMutableStats()
//            .getBallisticWeaponFluxCostMod()
//            .modifyMult(this.id, WEAPON_FLUX_COST_MULT);
//      }
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("ACTIVE")) {
//        if (this.ship.isPhased()) {
//          this.ship.setCustomData("FSD_Rampage_stats", "PAUSE");
//        }
//        this.active_timer += amount;
//        if (this.active_timer >= 10f) {
//          this.ship.setCustomData("FSD_Rampage_stats", "COOLING");
//          KarmaAPI.setKarma(this.ship, 0f);
//          this.ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyMult(this.id);
//          this.ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyMult(this.id);
//          this.active_timer = 0f;
//        }
//      }
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("PAUSE")
//          && !this.ship.isPhased()) {
//        this.ship.setCustomData("FSD_Rampage_stats", "ACTIVE");
//      }
//      if (this.ship.getCustomData().get("FSD_Rampage_stats").equals("COOLING")) {
//        this.cooling_timer += amount;
//        if (this.cooling_timer >= 20f) {
//          if (karma > 0.5f) {
//            this.ship.setCustomData("FSD_Rampage_stats", "READY");
//          } else {
//            this.ship.setCustomData("FSD_Rampage_stats", "NOT_READY");
//          }
//          this.cooling_timer = 0f;
//        }
//      }
//    }
//
//    @Override
//    public String modifyDamageDealt(
//        Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
//      if (!(target instanceof ShipAPI)) return null;
//      if (!this.ship.getCustomData().get("FSD_Rampage_stats").equals("ACTIVE")) return null;
//      CombatEngineAPI engine = Global.getCombatEngine();
//      if (engine == null) return null;
//      float karma = FSD_ReflectLight.getKarma(ship);
//      float extra_damage_mult = 0.25f + (0.025f * karma);
//      if (param instanceof DamagingProjectileAPI) {
//        DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
//        engine.applyDamage(
//            target,
//            point,
//            damage.getDamage() * extra_damage_mult,
//            DamageType.ENERGY,
//            0f,
//            false,
//            true,
//            projectile.getSource(),
//            true);
//      }
//      if (param instanceof BeamAPI) {
//        BeamAPI beam = (BeamAPI) param;
//        engine.applyDamage(
//            target,
//            point,
//            damage.getDamage() * 0.1f * extra_damage_mult,
//            DamageType.ENERGY,
//            0f,
//            false,
//            true,
//            beam.getSource(),
//            true);
//      }
//      return null;
//    }
//  }
}
