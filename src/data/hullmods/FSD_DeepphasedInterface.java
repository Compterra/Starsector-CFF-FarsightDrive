package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.*;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import data.hullmods.fsd_reflectlight_components.KarmaManager;
import data.hullmods.fsd_reflectlight_components.KarmaType;
import java.awt.*;
import java.util.*;
import java.util.List;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.*;
import org.boxutil.util.RenderingUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.magiclib.util.MagicRender;

public class FSD_DeepphasedInterface extends BaseHullMod {
  private static int Charge;
  private boolean Release;
  private FlareEntity flareEntity = null;
  private FlareEntity flareEntity2 = null;
  private final String RING_PATH = "graphics/fx/FSD_ring/FSD_FTRing.png";
  private static final String DELETE_PATH = "graphics/fx/FSD_ring/FSD_delete.png";
  private SpriteEntity spriteEntity = null;
  private Color UNDERCOLOR = new Color(94, 17, 17, 255);
  private Color COLOR = new Color(241, 23, 23, 100);

  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    ship.setCustomData("FSD_DeepphasedInterface", false);
    ship.setCustomData("FSD_DeepphasedInterface_Loc", new Vector2f());
    KarmaManager.getInstance().setKarmaGainRange(ship, 2f);
    if (!ship.hasListenerOfClass(FSD_DPI_listener.class)) {
      ship.addListener(new FSD_DPI_listener(ship));
    }
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    CombatEngineAPI engine = Global.getCombatEngine();
    if (engine.isPaused() || engine == null) {
      return;
    }
    if (ship.isAlive()) {
      FTRing(ship, ship.getLocation());
    }
  }

  public void FTRing(ShipAPI ship, Vector2f loc) {
    SpriteAPI sprite = Global.getSettings().getSprite(RING_PATH);
    float alpha = KarmaManager.getInstance().getKarma(ship);
    Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
    float clampedAlpha = Math.min(1.0f, Math.max(0.0f, alpha / 9f));
    float visualAlpha = (float) (1.0 - Math.pow(1.0 - clampedAlpha, 2));
    for (int i = 0; i < 1; i = i + 1) {
      sprite.setAlphaMult(visualAlpha);
      MagicRender.objectspace(
          sprite,
          ship,
          new Vector2f(),
          new Vector2f(),
          size,
          ship.getRenderOffset(),
          -180f,
          0f,
          true,
          Misc.scaleAlpha(COLOR, visualAlpha),
          2f,
          0.5f,
          1f,
          1f,
          0f,
          0.05f,
          0.05f,
          0.2f,
          true,
          CombatEngineLayers.ABOVE_SHIPS_LAYER,
          GL11.GL_SRC_ALPHA,
          GL11.GL_ONE);
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
    float pad = 10.0f;
    float pads = 3.0f;
    Color y = Misc.getHighlightColor();
    tooltip.addPara("A device that exports destructive dataflow from deep phase space, weaponized by the Autumn Thought-class.", pads);
    tooltip.addSectionHeading("Special Ability", Alignment.MID, pad);
    tooltip.addPara(
        "When the ship system is used, the warship leaves a phase rift that delivers hostile dataflow.\nThe rift lasts %s and fires slower tracking dataflow with %s range.\n",
        pads, Misc.getTextColor(), y, "3s", "1400su");
    tooltip.addPara(
        "Initial dataflow volley count is %s.\nEach volley deals fragmentation damage equal to %s of target hull plus %s EMP damage, and always creates an arc on hit.",
        pads, Misc.getTextColor(), y, "1", "8%", "30%");
    tooltip.addSectionHeading("Benefits", Alignment.MID, pad);
    tooltip.addPara(
        "When karma is above %s, consumes an additional %s karma to release %s extra dataflow volley, up to %s extra volleys.\n",
        pads, Misc.getTextColor(), y, "20%", "10%", "1", "5");
    tooltip.addPara(
        "When karma is above %s, dataflow damage changes from %s to %s.\n", pads, Misc.getTextColor(), y, "75%", "Fragmentation", "Energy");
  }

  public static class FSD_DPI_listener implements AdvanceableListener {
    private List<ActiveRift> activeRifts = new ArrayList<>();
    private IntervalUtil Timer = new IntervalUtil(3.1f, 3.1f);
    private IntervalUtil SpawnTimer = new IntervalUtil(1f, 1f);
    private Vector2f LastLoc;
    private boolean Once;
    private boolean FlareOnce;
    private boolean Detect = true;
    private Color color = new Color(255, 0, 0, 255);
    ShipAPI ship;
    static CombatEngineAPI engine = Global.getCombatEngine();
    private FlareEntity flareEntity = null;
    private FlareEntity flareEntity2 = null;
    private CurveEntity curveEntity = null;
    private DistortionEntity distortion = null;
    private Color ParticleColor = new Color(203, 12, 12, 16);
    private int Count = 0;
    private int Charge = 0;
    float CurrentKarma;

    public FSD_DPI_listener(ShipAPI ship) {
      this.ship = ship;
    }

    @Override
    public void advance(float amount) {
      CurrentKarma = KarmaManager.getInstance().getKarma(ship);
      int KarmaLevel = (int) (CurrentKarma * 10);
      ShipAPI Ship = this.ship;
      WeaponAPI Weapon = null;
      switch (KarmaLevel) {
        case 0:
          break;
        case 2:
        case 3:
          Charge = 1;
          break;
        case 4:
        case 5:
          Charge = 2;
          break;
        case 6:
        case 7:
          Charge = 3;
          break;
        case 8:
        case 9:
          Charge = 4;
          break;
        case 10:
          Charge = 5;
          break;
        default:
          break;
      }
      IntervalUtil SpawnTimer = new IntervalUtil(1f, 1f);
      IntervalUtil RiftTimer = new IntervalUtil(0.15f, 0.15f);
      HashMap<Vector2f, IntervalUtil> SpawnMap = new HashMap<>();
      for (WeaponAPI weapon : Ship.getAllWeapons()) {
        if (weapon.getId().equals("FSD_Flow")) Weapon = weapon;
      }
      if (!(boolean) ship.getCustomData().get("FSD_DeepphasedInterface")
          && ship.getSystem().getEffectLevel() > 0) {
        Once = true;
        FlareOnce = true;
        ship.setCustomData("FSD_DeepphasedInterface", true);
        int chargeToConsume = Math.min(Charge, 5);
        Charge -= chargeToConsume;
        KarmaManager.getInstance().reduceKarma(ship, chargeToConsume / 10f, KarmaType.ACTIVE_COST);
        activeRifts.add(new ActiveRift(ship.getLocation(), chargeToConsume));
        ship.setCustomData("FSD_DeepphasedInterface_Loc", new Vector2f(Ship.getLocation()));
      }
      Iterator<ActiveRift> iterator = activeRifts.iterator();
      while (iterator.hasNext()) {
        ActiveRift rift = iterator.next();
        rift.timer.advance(amount);
        if (rift.timer.intervalElapsed() && rift.shotsFired < rift.maxShots) {
          if (Weapon != null) {
            if (CurrentKarma >= 0.75f) {
              engine.spawnProjectile(
                  ship, Weapon, "FSD_Flow2", rift.location, ship.getFacing(), ship.getVelocity());
            } else {
              engine.spawnProjectile(
                  ship, Weapon, "FSD_Flow", rift.location, ship.getFacing(), ship.getVelocity());
            }
            if (SpawnTimer.intervalElapsed()) {
              if (CurrentKarma >= 0.75f) {
                engine.spawnProjectile(
                    ship, Weapon, "FSD_Flow2", rift.location, ship.getFacing(), ship.getVelocity());
              } else {
                engine.spawnProjectile(
                    ship, Weapon, "FSD_Flow", rift.location, ship.getFacing(), ship.getVelocity());
              }
            }
            rift.shotsFired++;
            Global.getSoundPlayer()
                .playSound("riftbeam_rift", 1f, 25f, rift.location, new Vector2f());
          }
        }
        if (rift.shotsFired >= rift.maxShots) {
          iterator.remove();
        }
      }
      if (Once) {
        LastLoc = (Vector2f) ship.getCustomData().get("FSD_DeepphasedInterface_Loc");
        Timer.advance(amount);
        if (!Timer.intervalElapsed()) {
          if (FlareOnce) {
            FSD_DPI_Render(LastLoc);
          }
          RiftTimer.advance(amount);
          if (RiftTimer.intervalElapsed()) {
            spawnRiftEffectAtLocation(LastLoc, color, 30f);
          }
          FlareOnce = false;
        }
        if (Timer.intervalElapsed()) {
          FlareOnce = true;
          Once = false;
        }
      }
      if (ship.getSystem().getEffectLevel() <= 0) {
        ship.setCustomData("FSD_DeepphasedInterface", false);
      }
    }

    public void FSD_DPI_Render(Vector2f loc) {
      if (ship == null) return;
      FlareEntity flareEntity = new FlareEntity();
      flareEntity.setLocation(loc);
      flareEntity.setSize(100, 25);
      flareEntity.setFlick(true);
      flareEntity.setFlickWhenPaused(false);
      flareEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
      flareEntity.setSmoothDisc();
      flareEntity.setFringeColor(Color.CYAN);
      flareEntity.setCoreColor(Color.WHITE);
      flareEntity.setCoreAlpha(1.0f);
      flareEntity.setFringeAlpha(1.0f);
      flareEntity.setAdditiveBlend();
      flareEntity.setNoisePower(0.33f);
      flareEntity.autoAspect();
      flareEntity.setGlobalTimer(0.0f, 6.5f, 1f);
      CombatRenderingManager.addEntity(BoxEnum.ENTITY_FLARE, flareEntity);
      Pair<FlareEntity, Byte> p =
          RenderingUtil.addCombatFlareField(
              loc,
              30,
              ship.getFacing(),
              360,
              128.0f,
              new Vector4f(96, 5, 128, 12),
              ParticleColor,
              Color.WHITE,
              6.5f,
              1.5f,
              CombatEngineLayers.ABOVE_PARTICLES);
      FlareEntity flareEntity2 = p.one;
      flareEntity2.setStateVanilla(loc, 0.0f);
      flareEntity2.setAdditiveBlend();
      flareEntity2.setSmoothDisc();
      flareEntity2.setFringeAlpha(1.0f);
      flareEntity2.setCoreAlpha(1.0f);
      flareEntity2.setGlowPower(1.0f);
      DistortionEntity newDistortion = new DistortionEntity();
      newDistortion.setGlobalTimer(0.25f, 2.75f, 0.5f);
      newDistortion.setInnerFull(0.7f, 0.7f);
      newDistortion.setInnerHardness(0.8f);
      newDistortion.setSizeIn(256, 256);
      newDistortion.setPowerIn(0);
      newDistortion.setPowerFull(1);
      newDistortion.setPowerOut(0);
      newDistortion.setSizeFull(128, 128);
      newDistortion.setSizeOut(96, 96);
      newDistortion.setLocation(loc);
      CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, newDistortion);
    }

    public static void spawnRiftEffectAtLocation(
        Vector2f loc, Color effectColor, float baseRadius) {
      CombatEngineAPI engine = Global.getCombatEngine();
      NEParams params = new NEParams();
      params.radius = baseRadius;
      params.color = effectColor;
      params.underglow = new Color(75, 75, 150, 255);
      params.withHitGlow = true;
      params.fadeOut = 1f;
      CombatEntityAPI prev = null;
      for (int i = 0; i < 2; i++) {
        NEParams p = params.clone();
        p.radius *= 0.75f + 0.5f * (float) Math.random();
        p.withHitGlow = (i == 0);
        Vector2f effectLoc = Misc.getPointAtRadius(loc, p.radius * 0.4f);
        CombatEntityAPI effectEntity =
            engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
        effectEntity.getLocation().set(effectLoc);
        if (prev != null) {
          float dist = Misc.getDistance(prev.getLocation(), effectLoc);
          Vector2f vel =
              Misc.getUnitVectorAtDegreeAngle(
                  Misc.getAngleInDegrees(effectLoc, prev.getLocation()));
          vel.scale(dist / (p.fadeIn + p.fadeOut) * 0.1f);
          effectEntity.getVelocity().set(vel);
        }
        prev = effectEntity;
      }
    }

    private class ActiveRift {
      public final Vector2f location;
      public final IntervalUtil timer;
      public int shotsFired;
      public final int maxShots;

      public ActiveRift(Vector2f loc, int chargeConsumed) {
        this.location = new Vector2f(loc);
        switch (chargeConsumed) {
          case 0:
            this.timer = new IntervalUtil(1.0f, 1.0f);
            this.maxShots = 1;
            break;
          case 1:
            this.timer = new IntervalUtil(1.0f, 1.0f);
            this.maxShots = 2;
            break;
          case 2:
            this.timer = new IntervalUtil(1.0f, 1.0f);
            this.maxShots = 3;
            break;
          case 3:
            this.timer = new IntervalUtil(1.0f, 1.0f);
            this.maxShots = 4;
            break;
          case 4:
            this.timer = new IntervalUtil(0.75f, 0.75f);
            this.maxShots = 5;
            break;
          case 5:
            this.timer = new IntervalUtil(0.6f, 0.6f);
            this.maxShots = 6;
            break;
          default:
            this.timer = new IntervalUtil(1.0f, 1.0f);
            this.maxShots = 1;
            break;
        }
        this.shotsFired = 0;
      }
    }
  }

  public static class NEParams extends NegativeExplosionVisual.NEParams implements Cloneable {
    public float radius;
    public Color color;
    public Color underglow;
    public float fadeIn = 1f;
    public float fadeOut = 2f;
    public float hitGlowSizeMult = 0.75f;
    public float noiseMag = 1f;
    public boolean withHitGlow;

    @Override
    public NEParams clone() {
      return (NEParams) super.clone();
    }
  }
}
