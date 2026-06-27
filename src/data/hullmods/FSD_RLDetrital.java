package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import org.lwjgl.util.vector.Vector2f;

public class FSD_RLDetrital extends BaseHullMod {
    private Color color = new Color(255, 0, 144,255);
    private Color Ccolor = new Color(97, 124, 118, 211);
    private float SpeedBonus = 1.15f;
    private float DEBUFF = 0.1f;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().modifyMult(id, SpeedBonus);
        stats.getAcceleration().modifyMult(id, SpeedBonus);
        stats.getDeceleration().modifyMult(id, SpeedBonus);
        stats.getTurnAcceleration().modifyMult(id, SpeedBonus);
        stats.getMaxTurnRate().modifyMult(id, SpeedBonus);

    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
//      if (!ship.hasListenerOfClass(FSD_RLDetrital_listener.class)) {
//       ship.addListener(new FSD_RLDetrital_listener(ship));
//     }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
      ship.getEngineController().fadeToOtherColor(this, color, Ccolor, 1f, 0.4f);
//    FighterWingAPI wing = ship.getWing();
//    ShipAPI source = wing.getSource().getShip();
//    MutableShipStatsAPI stats = ship.getMutableStats();
//    float maxHP = ship.getMaxHitpoints();
//    float perflameHP = maxHP * 0.05f * amount;
//    ship.setHitpoints(Math.min(maxHP, (ship.getHullLevel() * maxHP) + perflameHP));
//    if (source != null && source.isAlive()) {
//      if (source.getVariant().hasHullMod("FSD_ReflectLight")
//          && time >= 0
//          && source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//
//        time -= amount;
//      } else cooldown = 0.5f;
//      if (cooldown >= 0) {
//        cooldown -= amount;
//      } else time = 1f;
//      if (source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) return;
//      if (!source.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//        stats.getFluxDissipation().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getEnergyWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getMissileWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getWeaponDamageTakenMult().modifyPercent(ship.getId(), DEBUFF * 100f);
//        stats.getBeamWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getBallisticWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getMaxSpeed().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getAcceleration().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getDeceleration().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getTurnAcceleration().modifyPercent(ship.getId(), -DEBUFF * 100f);
//        stats.getMaxTurnRate().modifyPercent(ship.getId(), -DEBUFF * 100f);
//      }
//      if (source.getVariant().hasHullMod("converted_hangar")) {
//        stats.getFluxDissipation().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getEnergyWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getMissileWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getWeaponDamageTakenMult().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getBeamWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getBallisticWeaponDamageMult().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getMaxSpeed().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getAcceleration().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getDeceleration().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getTurnAcceleration().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//        stats.getMaxTurnRate().modifyPercent(ship.getId(), -DEBUFF * 100f * 2.5f);
//      }
//    }
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
    float pad = 10.0f;
    float pads = 3.0f;
    Color c = Misc.getHighlightColor();
    Color r = Misc.getNegativeHighlightColor();
    tooltip.addPara("A low-activity crystalline derivative carried by Farsight Drive fighters.\nShed from mature crystals, it cannot form a stable pilot link, but it serves as an excellent compact power source.", pads, Misc.getTextColor(), c, "");
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara(
        "Fighter maneuverability increased by %s.", pads, Misc.getTextColor(), c, "15%");
//    float col1 = 180f;
//    float col2 = 180f;
//    Color r1 = Misc.getHighlightColor();
//    Color r2 = Misc.getPositiveHighlightColor();
//    tooltip.beginTable(
//        Misc.getBasePlayerColor(),
//        Misc.getDarkPlayerColor(),
//        Misc.getBrightPlayerColor(),
//        20f,
//        true,
//        true,
//        new Object[] {
//        });
//    tooltip.addTable("", 0, pad);
//    tooltip.addPara(
  }

//  public static class FSD_RLDetrital_listener implements DamageDealtModifier, AdvanceableListener {
//    private ShipAPI ship;
//    private WeaponAPI weapon;
//    private Color color;
//    private Color Pcolor;
//    private float DamageBonus = 0.15f;
//    private boolean wasZero = true;
//    private IntervalUtil fireInterval = new IntervalUtil(0.05f, 0.075f);
//
//    private FSD_RLDetrital_listener(ShipAPI ship) {
//      this.ship = ship;
//    }
//
//    public String modifyDamageDealt(
//        Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
//      CombatEngineAPI engine = Global.getCombatEngine();
//      if (param instanceof BeamAPI) {
//        BeamAPI beam = (BeamAPI) param;
//        if (beam.getSource() != null) {
//          if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
//            float dur = beam.getDamage().getDpsDuration();
//            if (!wasZero) dur = 0;
//            wasZero = beam.getDamage().getDpsDuration() <= 0;
//            fireInterval.advance(dur);
//            if (fireInterval.intervalElapsed()) {
//              ShipAPI ship = (ShipAPI) target;
//              boolean hitShield =
//                  target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
//              float pierceChance = ((ShipAPI) target).getHardFluxLevel() + 0.1f;
//              pierceChance *=
//                  ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
//              boolean piercedShield = hitShield && (float) Math.random() < pierceChance;
//              if (!hitShield || piercedShield || Math.random() < 0.1f) {
//                float emp = beam.getDamage().getDamage() * DamageBonus;
//                float dam = beam.getDamage().getDamage() * DamageBonus;
//                engine.spawnEmpArc(
//                    beam.getSource(),
//                    point,
//                    beam.getDamageTarget(),
//                    beam.getDamageTarget(),
//                    DamageType.ENERGY,
//                    dam,
//                    emp,
//                    100000f,
//                    "tachyon_lance_emp_impact",
//                    beam.getWidth() + 9f,
//                    beam.getFringeColor(),
//                    beam.getCoreColor());
//              }
//            }
//          }
//        }
//      }
//      if (param instanceof DamagingProjectileAPI) {
//        if (((DamagingProjectileAPI) param).getSource() != null) {
//          if (target instanceof ShipAPI) {
//            DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
//            if (projectile.getDamageType() == DamageType.KINETIC) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.FRAGMENTATION,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(0, 217, 255, 255);
//              color = new Color(0, 81, 255, 255);
//              engine.addHitParticle(point, new Vector2f(), 25f, 3f, 0.5f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 20f, 6f, 0.5f, color);
//            }
//            if (projectile.getDamageType() == DamageType.ENERGY) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.HIGH_EXPLOSIVE,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(145, 70, 27, 255);
//              color = new Color(187, 30, 24, 255);
//              engine.addHitParticle(point, new Vector2f(), 25f, 1f, 0.5f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 20f, 2.5f, 0.5f, color);
//            }
//            if (projectile.getDamageType() == DamageType.HIGH_EXPLOSIVE) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.KINETIC,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(255, 174, 0, 255);
//              color = new Color(255, 255, 0, 255);
//              engine.addHitParticle(point, new Vector2f(), 25f, 1f, 0.5f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 20f, 2.5f, 0.5f, color);
//            }
//            if (projectile.getDamageType() == DamageType.FRAGMENTATION) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.ENERGY,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(191, 0, 255, 255);
//              color = new Color(255, 0, 98, 255);
//              engine.addHitParticle(point, new Vector2f(), 25f, 1f, 0.5f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 20f, 2.5f, 0.5f, color);
//            }
//          }
//        }
//      }
//      if (param instanceof MissileAPI) {
//        MissileAPI missile = (MissileAPI) param;
//        if (target instanceof ShipAPI) {
//          if (missile.getWeapon() != null) {
//            if (missile.getDamageType() == DamageType.KINETIC) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.FRAGMENTATION,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(0, 217, 255, 255);
//              color = new Color(0, 81, 255, 255);
//              engine.addHitParticle(point, new Vector2f(), 35f, 6f, 0.75f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 25f, 10f, 0.75f, color);
//            }
//            if (missile.getDamageType() == DamageType.ENERGY) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.HIGH_EXPLOSIVE,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(145, 70, 27, 255);
//              color = new Color(187, 30, 24, 255);
//              engine.addHitParticle(point, new Vector2f(), 35f, 6f, 0.75f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 25f, 10f, 0.75f, color);
//            }
//            if (missile.getDamageType() == DamageType.HIGH_EXPLOSIVE) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.KINETIC,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(255, 174, 0, 255);
//              color = new Color(255, 255, 0, 255);
//              engine.addHitParticle(point, new Vector2f(), 35f, 6f, 0.75f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 25f, 10f, 0.75f, color);
//            }
//            if (missile.getDamageType() == DamageType.FRAGMENTATION) {
//              engine.applyDamage(
//                  target,
//                  point,
//                  damage.getDamage() * DamageBonus,
//                  DamageType.KINETIC,
//                  0,
//                  false,
//                  false,
//                  ship);
//              Pcolor = new Color(191, 0, 255, 255);
//              color = new Color(255, 0, 98, 255);
//              engine.addHitParticle(point, new Vector2f(), 35f, 6f, 0.75f, Pcolor);
//              engine.addHitParticle(point, new Vector2f(), 25f, 10f, 0.75f, color);
//            }
//          }
//        }
//      }
//      return null;
//    }
//
//    @Override
//    public void advance(float amount) {
//      if (!ship.getVariant().hasHullMod("FSD_BiomassRefiningMachine")) {
//        DamageBonus = 0.05f;
//      }
//    }
//  }
}
