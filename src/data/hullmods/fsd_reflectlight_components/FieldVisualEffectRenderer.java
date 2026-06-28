package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.hullmods.FSD_ReflectLight;
import java.awt.Color;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class FieldVisualEffectRenderer {
  private static final float FIELD_PULSE_SPEED = 1.0f;
  private static final float FIELD_ROTATION_SPEED = 15.0f;
  private static final int MIST_PARTICLE_COUNT = 15;
  private static final int INDICATOR_RING_SEGMENTS = 120;
  private static final float INDICATOR_RING_WIDTH = 4f;
  private static final int SOFT_CIRCLE_SEGMENTS = 32;
  private final ShipAPI ship;
  private float pulsePhase = 0f;
  private float ringRotation = 0f;
  private final Random noiseGenerator;
  private float jitterTimer = 0f;
  private float currentJitterX = 0f;
  private float currentJitterY = 0f;
  private SpriteAPI ringSprite;
  private boolean hasCustomRing = false;

  public FieldVisualEffectRenderer(ShipAPI ship) {
    this.ship = ship;
    this.noiseGenerator = new Random(ship.getId().hashCode());
    loadShipRing(ship);
  }

  private void loadShipRing(ShipAPI ship) {
    hasCustomRing = false;
    if (ship == null || ship.getHullSpec() == null) {
      return;
    }
    String hullId = ship.getHullSpec().getHullId();
    try {
      String ringPath = "graphics/fx/FSD_ring/" + hullId + ".png";
      try {
        ringSprite = Global.getSettings().getSprite(ringPath);
        if (ringSprite != null) {
          hasCustomRing = true;
          if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
            FSD_ReflectLight.log.info("[FSD][FieldVisualEffectRenderer] loaded ship-specific halo: " + ringPath);
          }
        }
      } catch (Exception e) {
        if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
          FSD_ReflectLight.log.info(
              "[FSD][FieldVisualEffectRenderer] failed to load halo from primary path: "
                  + ringPath
                  + ", error: "
                  + e.getMessage());
        }
      }
      if (!hasCustomRing) {
        String altPath = "graphics/fx/FSD_ring/" + hullId.toLowerCase() + ".png";
        try {
          ringSprite = Global.getSettings().getSprite(altPath);
          if (ringSprite != null) {
            hasCustomRing = true;
            if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
              FSD_ReflectLight.log.info(
                  "[FSD][FieldVisualEffectRenderer] loaded ship-specific halo(lowercaseID): " + altPath);
            }
          }
        } catch (Exception e) {
          if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
            FSD_ReflectLight.log.info(
                "[FSD][FieldVisualEffectRenderer] failed to load halo from fallback path: "
                    + altPath
                    + ", error: "
                    + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      hasCustomRing = false;
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][FieldVisualEffectRenderer] error loading ship-specific halo", e);
      }
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
      FSD_ReflectLight.log.info(
          "[FSD][FieldVisualEffectRenderer] ship "
              + (ship != null ? ship.getHullSpec().getHullName() + " (ID: " + hullId + ")" : "unknown")
              + " custom halo load result: "
              + (hasCustomRing ? "success" : "failed"));
    }
  }

  public void render(
      EntropyFieldState state,
      float alpha,
      float karma,
      com.fs.starfarer.api.combat.CombatEngineLayers layer) {
    if (this.ship == null || state == null) return;
    if (state.getAnimState() == AnimationState.INACTIVE) {
      return;
    }
    if (layer == com.fs.starfarer.api.combat.CombatEngineLayers.BELOW_SHIPS_LAYER) {
      float amount = Global.getCombatEngine().getElapsedInLastFrame();
      this.pulsePhase += amount * FIELD_PULSE_SPEED;
      this.pulsePhase %= 1.0f;
      this.ringRotation += amount * FIELD_ROTATION_SPEED;
    }
    float pulseAlpha = 0.75f + (0.25f * (float) Math.sin(this.pulsePhase * 2.0f * Math.PI));
    if (layer == com.fs.starfarer.api.combat.CombatEngineLayers.BELOW_SHIPS_LAYER) {
      if (hasCustomRing && ringSprite != null) {
        renderShipSpecificRing(state, alpha, karma, Math.min(1.0f, pulseAlpha));
      }
      if (state.isPlayerShip()) {
        renderBackground(state, alpha, karma, Math.min(1.0f, pulseAlpha));
      }
    } else if (layer == com.fs.starfarer.api.combat.CombatEngineLayers.ABOVE_SHIPS_LAYER) {
      if (state.getAnimState() == AnimationState.ENHANCED_IDLE) {
        if (FSD_ReflectLight.ENABLE_ENTROPY_FIELD_INDICATOR) {
          float entropyRadius = FSD_ReflectLight.getEntropyFieldRange(ship, 1f);
          renderEntropyFieldIndicator(entropyRadius, alpha);
        }
      }
    }
  }

  private void renderBackground(
      EntropyFieldState state, float alpha, float karma, float pulseAlpha) {
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  private void renderProgrammaticMist(EntropyFieldState state, float alpha, float karma) {
    float entropyRadius = FSD_ReflectLight.getEntropyFieldRange(ship, 1f);
    if (entropyRadius <= 0) return;
    Vector2f shipLocation = ship.getLocation();
    int particleCount = MIST_PARTICLE_COUNT + (int) (karma * 10);
    float baseAlpha = Math.min(0.5f, 0.05f + karma * 0.45f) * alpha / particleCount;
    Color baseColor = FSD_ReflectLight.FIELD_COLOR_BASE;
    float r = baseColor.getRed() / 255f;
    float g = baseColor.getGreen() / 255f;
    float b = baseColor.getBlue() / 255f;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      long seed = (long) (Global.getCombatEngine().getTotalElapsedTime(false) * 1000);
      noiseGenerator.setSeed(seed);
      for (int i = 0; i < particleCount; i++) {
        float t = (Global.getCombatEngine().getTotalElapsedTime(false) % 10f) + i * 0.1f;
        float angle = noiseGenerator.nextFloat() * 360f;
        float distance = noiseGenerator.nextFloat() * entropyRadius * 0.8f;
        float x = (float) (Math.cos(Math.toRadians(angle)) * distance);
        float y = (float) (Math.sin(Math.toRadians(angle)) * distance);
        float sizeFactor = 0.8f + noiseGenerator.nextFloat() * 0.4f;
        float size = (entropyRadius / 3f) * sizeFactor;
        float particleAlpha = baseAlpha * (0.5f + 0.5f * (float) Math.sin(t * 0.7f + i));
        float colorPulse = (float) Math.sin(t * 0.3f + i);
        float rMod = r + (karma * 0.2f * colorPulse);
        float gMod = g + (karma * 0.05f * colorPulse);
        float bMod = b + (karma * 0.05f * colorPulse);
        Color mistColor = new Color(rMod, gMod, bMod, particleAlpha);
        GL11.glPushMatrix();
        GL11.glTranslatef(shipLocation.x + x, shipLocation.y + y, 0f);
        drawSoftCircle(size, mistColor);
        GL11.glPopMatrix();
      }
    } finally {
      GL11.glPopAttrib();
    }
  }

  private void drawSoftCircle(float radius, Color color) {
    float r = color.getRed() / 255f;
    float g = color.getGreen() / 255f;
    float b = color.getBlue() / 255f;
    float a = color.getAlpha() / 255f;
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    GL11.glColor4f(r, g, b, a);
    GL11.glVertex2f(0, 0);
    for (int i = 0; i <= SOFT_CIRCLE_SEGMENTS; i++) {
      double angle = 2 * Math.PI * i / SOFT_CIRCLE_SEGMENTS;
      float edgeAlpha = a * 0.3f;
      GL11.glColor4f(r, g, b, edgeAlpha);
      GL11.glVertex2f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius);
    }
    GL11.glEnd();
  }

  private void renderShipSpecificRing(
      EntropyFieldState state, float alpha, float karma, float pulseAlpha) {
    if (ringSprite == null
        || !hasCustomRing
        || ship == null
        || !ship.isAlive()
        || ship.isHulk()
        || state == null
        || state.getAnimState() == AnimationState.INACTIVE) {
      return;
    }
    if (ship.isStationModule()) {
      return;
    }
    float gameTime = Global.getCombatEngine().getTotalElapsedTime(false);
    Vector2f shipPos = ship.getLocation();
    float shipFacing = ship.getFacing();
    float ringAlpha = 0f;
    if (karma <= 0f) {
      ringAlpha = 0f;
      ringSprite.setAlphaMult(ringAlpha);
      return;
    } else if (karma <= 0.5f) {
      ringAlpha = karma * 2f;
      ringSprite.setColor(Color.WHITE);
      ringSprite.setAlphaMult(ringAlpha * alpha * pulseAlpha);
    } else {
      ringAlpha = 1.0f;
      float flickerIntensity = (karma - 0.5f) * 2.0f;
      float fastFlicker = (float) Math.sin(gameTime * 15f) * 0.5f + 0.5f;
      float slowFlicker = (float) Math.sin(gameTime * 4.93f) * 0.5f + 0.5f;
      float flickerEffect = fastFlicker * 0.7f + slowFlicker * 0.3f;
      float minAlpha = 0.6f;
      float flickerRange = 1.0f - minAlpha;
      ringAlpha = minAlpha + flickerRange * flickerEffect * flickerIntensity;
      ringSprite.setColor(Color.WHITE);
      ringSprite.setAlphaMult(ringAlpha * alpha * pulseAlpha);
    }
    float offsetX = 0f;
    float offsetY = 0f;
    if (ship != null && ship.getSpriteAPI() != null) {
      SpriteAPI sprite = ship.getSpriteAPI();
      float width = sprite.getWidth();
      float height = sprite.getHeight();
      float halfWidth = width / 2f;
      float halfHeight = height / 2f;
      float centerX = sprite.getCenterX();
      float centerY = sprite.getCenterY();
      float rawOffsetX = halfWidth - centerX;
      float rawOffsetY = halfHeight - centerY;
      float adjustedOffsetX = rawOffsetY;
      float adjustedOffsetY = -rawOffsetX;
      float posJitterX = 0f;
      float posJitterY = 0f;
      if (karma > 0.5f) {
        float minJitter = 0.5f;
        float maxJitter = 1.5f;
        float jitterStrength = minJitter + (maxJitter - minJitter) * ((karma - 0.5f) * 2.0f);
        float updateInterval = 0.033f;
        jitterTimer += Global.getCombatEngine().getElapsedInLastFrame();
        if (jitterTimer >= updateInterval) {
          jitterTimer = (float) (Math.random() * 0.02f);
          currentJitterX = (float) ((Math.random() * 2.0 - 1.0) * jitterStrength);
          currentJitterY = (float) ((Math.random() * 2.0 - 1.0) * jitterStrength);
        }
        posJitterX = currentJitterX;
        posJitterY = currentJitterY;
      }
      float facingRad = (float) Math.toRadians(shipFacing);
      float rotatedOffsetX =
          (float) (adjustedOffsetX * Math.cos(facingRad) - adjustedOffsetY * Math.sin(facingRad));
      float rotatedOffsetY =
          (float) (adjustedOffsetX * Math.sin(facingRad) + adjustedOffsetY * Math.cos(facingRad));
      offsetX = rotatedOffsetX + posJitterX;
      offsetY = rotatedOffsetY + posJitterY;
    }
    if (karma <= 0.0f) {
      return;
    }
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
      ringSprite.setAngle(shipFacing - 90f);
      ringSprite.renderAtCenter(shipPos.x + offsetX, shipPos.y + offsetY);
      if (karma > 0.5f) {
        float highLightIntensity = (karma - 0.5f) * 2.0f;
        float pulseEffect = (float) Math.sin(gameTime * 8.0f) * 0.5f + 0.5f;
        float glowAlpha = 0.45f + (0.35f * pulseEffect * highLightIntensity);
        ringSprite.setColor(new Color(1.0f, 0.6f, 0.3f, Math.min(1.0f, glowAlpha)));
        ringSprite.renderAtCenter(shipPos.x + offsetX, shipPos.y + offsetY);
        if (karma > 0.75f) {
          float secondIntensity = (karma - 0.75f) * 4.0f;
          ringSprite.setColor(
              new Color(1.0f, 0.4f, 0.2f, Math.min(1.0f, 0.3f * secondIntensity * pulseEffect)));
          ringSprite.renderAtCenter(shipPos.x + offsetX, shipPos.y + offsetY);
        }
      }
    } finally {
      GL11.glPopAttrib();
    }
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING
        && FSD_ReflectLight.log != null
        && Global.getCombatEngine() != null
        && Global.getCombatEngine().getTotalElapsedTime(false) % 5.0f < 0.017f) {
      String hullId = ship.getHullSpec().getHullId();
      String ringPath = "graphics/fx/FSD_ring/" + hullId + ".png";
      FSD_ReflectLight.log.info(
          "[FSD][FieldVisualEffectRenderer] ring texture render complete，texture: "
              + ringPath
              + ", location: world coordinates("
              + (shipPos.x + offsetX)
              + ", "
              + (shipPos.y + offsetY)
              + ")"
              + ", jitter: ("
              + currentJitterX
              + ", "
              + currentJitterY
              + ")"
              + ", original size: "
              + ringSprite.getWidth()
              + "x"
              + ringSprite.getHeight()
              + ", alpha: "
              + (ringAlpha * alpha * pulseAlpha)
              + ", angle: "
              + (shipFacing - 90f));
    }
  }

  private void renderEntropyFieldIndicator(float radius, float alpha) {
    if (radius <= 0) return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_LINE_SMOOTH);
    GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    float outerRadius = radius;
    float innerRadius = radius - INDICATOR_RING_WIDTH;
    Color color = new Color(255, 100, 80, 150);
    float r = color.getRed() / 255f;
    float g = color.getGreen() / 255f;
    float b = color.getBlue() / 255f;
    float a = (color.getAlpha() / 255f) * alpha;
    GL11.glBegin(GL11.GL_QUAD_STRIP);
    for (int i = 0; i <= INDICATOR_RING_SEGMENTS; i++) {
      double angle = Math.toRadians((float) i * 360f / (float) INDICATOR_RING_SEGMENTS);
      float cos = (float) Math.cos(angle);
      float sin = (float) Math.sin(angle);
      GL11.glColor4f(r, g, b, a);
      GL11.glVertex2f(cos * outerRadius, sin * outerRadius);
      GL11.glColor4f(r, g, b, a * 0.5f);
      GL11.glVertex2f(cos * innerRadius, sin * innerRadius);
    }
    GL11.glEnd();
    GL11.glPopAttrib();
  }
}
