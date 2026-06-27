package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.hullmods.FSD_ReflectLight;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class RuneRenderer {
  public static final Logger log = Global.getLogger(RuneRenderer.class);
  private static final float MIN_RUNE_SIZE = 10f;
  private static final float MAX_JITTER = 5f;
  private final List<String> reusableCharList = new ArrayList<>();
  private final Vector2f reusableJitter = new Vector2f();

  public void render(
      EntropyFieldState state,
      ShipAPI ship,
      Vector2f position,
      Color baseColor,
      ViewportAPI viewport) {
    if (state == null || state.getAnimState() == AnimationState.INACTIVE || baseColor == null) {
      return;
    }
    AnimationState animState = state.getAnimState();
    List<String> innerRingSentence = state.getCurrentSentence();
    List<String> outerRingSentence = state.getOuterRingSentence();
    if (innerRingSentence == null || innerRingSentence.isEmpty()) {
      return;
    }
    GL11.glPushMatrix();
    GL11.glTranslatef(position.x, position.y, 0);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    float innerRadius = state.getInnerRingRadius();
    float outerRadius = state.getOuterRingRadius();
    float innerRuneSize = state.getInnerRuneSize();
    float outerRuneSize = state.getOuterRuneSize() * 2f / 3f;
    float innerRotation = state.getInnerRingRotation();
    float outerRotation = state.getOuterRingRotation();
    float alpha = state.getCurrentAlpha();
    Vector2f jitter = state.getJitter();
    float karma = state.getDisplayKarma();
    boolean isPlayer = state.isPlayerShip();
    float displayMaxCharsInner = state.getDisplayMaxCharsInner();
    float displayMaxCharsOuter = state.getDisplayMaxCharsOuter() * 3f / 2f;
    float safeAlpha = Math.min(1.0f, Math.max(0f, alpha));
    Color innerColor = adjustColorForKarma(baseColor, karma, safeAlpha);
    Color outerColor = adjustColorForKarma(baseColor, karma, safeAlpha);
    float innerDisplayPercent = 1f;
    float outerDisplayPercent = 1f;
    if (isPlayer) {
      innerDisplayPercent = Math.min(1f, karma / 0.5f);
      outerDisplayPercent = Math.max(0f, (karma - 0.5f) / 0.5f);
    } else {
      innerDisplayPercent = karma;
      outerDisplayPercent = 0f;
    }
    switch (animState) {
      case FORMING:
        renderFormingAnimation(
            state,
            innerRingSentence,
            outerRingSentence,
            innerRadius,
            outerRadius,
            innerRuneSize,
            outerRuneSize,
            innerRotation,
            outerRotation,
            innerColor,
            outerColor,
            jitter,
            innerDisplayPercent,
            outerDisplayPercent,
            displayMaxCharsInner,
            displayMaxCharsOuter);
        break;
      case IDLE:
        renderSingleRing(
            innerRingSentence,
            innerRadius + innerRuneSize,
            innerRuneSize,
            innerRotation,
            innerColor,
            jitter,
            innerDisplayPercent,
            displayMaxCharsInner);
        break;
      case ENHANCED_IDLE:
        renderDualRings(
            innerRingSentence,
            outerRingSentence,
            innerRadius,
            outerRadius,
            innerRuneSize,
            outerRuneSize,
            innerRotation,
            outerRotation,
            innerColor,
            outerColor,
            jitter,
            innerDisplayPercent,
            outerDisplayPercent,
            displayMaxCharsInner,
            displayMaxCharsOuter);
        if (isPlayer) {
          float realFieldRadius = state.getEntropyFieldRadius();
          Color rangeColor = new Color(128, 0, 0, 64);
          GL11.glDisable(GL11.GL_TEXTURE_2D);
          renderRangeIndicator(realFieldRadius, rangeColor);
          GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        break;
      case ENHANCING:
        renderEnhancingAnimation(
            state,
            innerRingSentence,
            outerRingSentence,
            innerRadius,
            outerRadius,
            innerRuneSize,
            outerRuneSize,
            innerRotation,
            outerRotation,
            innerColor,
            outerColor,
            jitter,
            innerDisplayPercent,
            outerDisplayPercent,
            displayMaxCharsInner,
            displayMaxCharsOuter);
        if (isPlayer) {
          float realFieldRadius = state.getEntropyFieldRadius();
          float animProgress = state.getAnimProgress();
          float currentRadius = realFieldRadius * animProgress;
          Color rangeColor = new Color(128, 0, 0, 64);
          GL11.glDisable(GL11.GL_TEXTURE_2D);
          renderRangeIndicator(currentRadius, rangeColor);
          GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        break;
      case DEHANCING:
        renderDehancingAnimation(
            state,
            innerRingSentence,
            outerRingSentence,
            innerRadius,
            outerRadius,
            innerRuneSize,
            outerRuneSize,
            innerRotation,
            outerRotation,
            innerColor,
            outerColor,
            jitter,
            innerDisplayPercent,
            outerDisplayPercent,
            displayMaxCharsInner,
            displayMaxCharsOuter);
        if (isPlayer) {
          float realFieldRadius = state.getEntropyFieldRadius();
          float animProgress = state.getAnimProgress();
          float currentRadius = realFieldRadius * (1.0f - animProgress);
          Color rangeColor = new Color(128, 0, 0, 64);
          GL11.glDisable(GL11.GL_TEXTURE_2D);
          renderRangeIndicator(currentRadius, rangeColor);
          GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        break;
      case DISSOLVING:
        renderDissolvingAnimation(
            state,
            innerRingSentence,
            outerRingSentence,
            innerRadius,
            outerRadius,
            innerRuneSize,
            outerRuneSize,
            innerRotation,
            outerRotation,
            innerColor,
            outerColor,
            jitter,
            innerDisplayPercent,
            outerDisplayPercent,
            displayMaxCharsInner,
            displayMaxCharsOuter);
        break;
    }
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glPopMatrix();
  }

  private void renderFormingAnimation(
      EntropyFieldState state,
      List<String> innerSentence,
      List<String> outerSentence,
      float innerRadius,
      float outerRadius,
      float innerRuneSize,
      float outerRuneSize,
      float innerRotation,
      float outerRotation,
      Color innerColor,
      Color outerColor,
      Vector2f jitter,
      float innerDisplayPercent,
      float outerDisplayPercent,
      float displayMaxCharsInner,
      float displayMaxCharsOuter) {
    if (state.isPlayerShip()) {
      renderSingleRing(
          innerSentence,
          innerRadius,
          innerRuneSize,
          innerRotation,
          innerColor,
          jitter,
          innerDisplayPercent,
          displayMaxCharsInner);
      float outerAlpha = state.getAnimProgress();
      int alpha = Math.max(0, Math.min(255, (int) (outerColor.getAlpha() * outerAlpha)));
      Color adjustedOuterColor =
          new Color(outerColor.getRed(), outerColor.getGreen(), outerColor.getBlue(), alpha);
      renderSingleRing(
          outerSentence,
          outerRadius,
          outerRuneSize,
          -outerRotation,
          adjustedOuterColor,
          jitter,
          outerDisplayPercent,
          displayMaxCharsOuter);
      float realFieldRadius = state.getEntropyFieldRadius();
      float animProgress = state.getAnimProgress();
      float currentRadius = realFieldRadius * animProgress;
      Color rangeColor = new Color(128, 0, 0, 64);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      renderRangeIndicator(currentRadius, rangeColor);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
    } else {
      float animProgress = state.getAnimProgress();
      float currentInnerRadius = innerRadius + (innerRuneSize * animProgress);
      renderSingleRing(
          innerSentence,
          currentInnerRadius,
          innerRuneSize,
          innerRotation,
          innerColor,
          jitter,
          innerDisplayPercent,
          displayMaxCharsInner);
    }
  }

  private void renderEnhancingAnimation(
      EntropyFieldState state,
      List<String> innerSentence,
      List<String> outerSentence,
      float innerRadius,
      float outerRadius,
      float innerRuneSize,
      float outerRuneSize,
      float innerRotation,
      float outerRotation,
      Color innerColor,
      Color outerColor,
      Vector2f jitter,
      float innerDisplayPercent,
      float outerDisplayPercent,
      float displayMaxCharsInner,
      float displayMaxCharsOuter) {
    float animProgress = state.getAnimProgress();
    float currentInnerRadius = innerRadius + (innerRuneSize * (1.0f - animProgress));
    renderSingleRing(
        innerSentence,
        currentInnerRadius,
        innerRuneSize,
        innerRotation,
        innerColor,
        jitter,
        innerDisplayPercent,
        displayMaxCharsInner);
    float outerAlpha = state.getAnimProgress();
    int alpha = Math.max(0, Math.min(255, (int) (outerColor.getAlpha() * outerAlpha)));
    Color adjustedOuterColor =
        new Color(outerColor.getRed(), outerColor.getGreen(), outerColor.getBlue(), alpha);
    renderSingleRing(
        outerSentence,
        outerRadius,
        outerRuneSize,
        -outerRotation,
        adjustedOuterColor,
        jitter,
        outerDisplayPercent,
        displayMaxCharsOuter);
  }

  private void renderDehancingAnimation(
      EntropyFieldState state,
      List<String> innerSentence,
      List<String> outerSentence,
      float innerRadius,
      float outerRadius,
      float innerRuneSize,
      float outerRuneSize,
      float innerRotation,
      float outerRotation,
      Color innerColor,
      Color outerColor,
      Vector2f jitter,
      float innerDisplayPercent,
      float outerDisplayPercent,
      float displayMaxCharsInner,
      float displayMaxCharsOuter) {
    float animProgress = state.getAnimProgress();
    float currentInnerRadius = innerRadius + (innerRuneSize * animProgress);
    renderSingleRing(
        innerSentence,
        currentInnerRadius,
        innerRuneSize,
        innerRotation,
        innerColor,
        jitter,
        innerDisplayPercent,
        displayMaxCharsInner);
    float outerAlpha = 1.0f - state.getAnimProgress();
    int alpha = Math.max(0, Math.min(255, (int) (outerColor.getAlpha() * outerAlpha)));
    Color adjustedOuterColor =
        new Color(outerColor.getRed(), outerColor.getGreen(), outerColor.getBlue(), alpha);
    reusableJitter.set(
        jitter.x + (float) (Math.random() - 0.5) * state.getAnimProgress() * MAX_JITTER,
        jitter.y + (float) (Math.random() - 0.5) * state.getAnimProgress() * MAX_JITTER);
    renderSingleRing(
        outerSentence,
        outerRadius,
        outerRuneSize,
        -outerRotation,
        adjustedOuterColor,
        reusableJitter,
        outerDisplayPercent,
        displayMaxCharsOuter);
  }

  private void renderDissolvingAnimation(
      EntropyFieldState state,
      List<String> innerSentence,
      List<String> outerSentence,
      float innerRadius,
      float outerRadius,
      float innerRuneSize,
      float outerRuneSize,
      float innerRotation,
      float outerRotation,
      Color innerColor,
      Color outerColor,
      Vector2f jitter,
      float innerDisplayPercent,
      float outerDisplayPercent,
      float displayMaxCharsInner,
      float displayMaxCharsOuter) {
    if (state.isPlayerShip()) {
      renderSingleRing(
          innerSentence,
          innerRadius,
          innerRuneSize,
          innerRotation,
          innerColor,
          jitter,
          innerDisplayPercent,
          displayMaxCharsInner);
      renderSingleRing(
          outerSentence,
          outerRadius,
          outerRuneSize,
          -outerRotation,
          outerColor,
          jitter,
          outerDisplayPercent,
          displayMaxCharsOuter);
    } else {
      float animProgress = state.getAnimProgress();
      float currentInnerRadius = innerRadius + (innerRuneSize * (1.0f - animProgress));
      renderSingleRing(
          innerSentence,
          currentInnerRadius,
          innerRuneSize,
          innerRotation,
          innerColor,
          jitter,
          innerDisplayPercent,
          displayMaxCharsInner);
    }
  }

  private void renderDualRings(
      List<String> innerSentence,
      List<String> outerSentence,
      float innerRadius,
      float outerRadius,
      float innerRuneSize,
      float outerRuneSize,
      float innerRotation,
      float outerRotation,
      Color innerColor,
      Color outerColor,
      Vector2f jitter,
      float innerDisplayPercent,
      float outerDisplayPercent,
      float displayMaxCharsInner,
      float displayMaxCharsOuter) {
    renderSingleRing(
        innerSentence,
        innerRadius,
        innerRuneSize,
        innerRotation,
        innerColor,
        jitter,
        innerDisplayPercent,
        displayMaxCharsInner);
    renderSingleRing(
        outerSentence,
        outerRadius,
        outerRuneSize,
        -outerRotation,
        outerColor,
        jitter,
        outerDisplayPercent,
        displayMaxCharsOuter);
  }

  private void renderSingleRing(
      List<String> sentence,
      float radius,
      float runeSize,
      float rotation,
      Color color,
      Vector2f jitter,
      float displayPercent,
      float displayMaxChars) {
    if (sentence == null
        || sentence.isEmpty()
        || displayPercent <= 0.01f
        || displayMaxChars <= 0.01f) return;
    GL11.glPushMatrix();
    try {
      reusableCharList.clear();
      for (String s : sentence) {
        if (!" ".equals(s)) {
          reusableCharList.add(s);
        }
      }
      if (reusableCharList.isEmpty()) return;
      runeSize = Math.max(MIN_RUNE_SIZE, runeSize);
      int maxCharsInRing = (int) Math.ceil(displayMaxChars);
      if (maxCharsInRing <= 0) return;
      int charListSize = reusableCharList.size();
      float anglePerChar = 360f / displayMaxChars;
      float visibleCharsFloat = displayMaxChars * displayPercent;
      int totalVisibleChars = (int) visibleCharsFloat;
      float partialCharAlpha = visibleCharsFloat - totalVisibleChars;
      GL11.glRotatef(rotation, 0, 0, 1);
      for (int i = 0; i < totalVisibleChars + 1 && i < maxCharsInRing; i++) {
        String runeChar = reusableCharList.get(i % charListSize);
        SpriteAPI sprite = RuneConfigLoader.getRuneSprite(runeChar);
        if (sprite == null) {
          continue;
        }
        float angle = i * anglePerChar;
        float r = (float) Math.toRadians(angle);
        float x = radius * (float) Math.cos(r) + jitter.x;
        float y = radius * (float) Math.sin(r) + jitter.y;
        float alphaMod = 1.0f;
        if (i == totalVisibleChars) {
          alphaMod = partialCharAlpha;
        }
        if (alphaMod <= 0.01f) continue;
        sprite.setAngle(angle + 90);
        sprite.setSize(runeSize, runeSize);
        sprite.setColor(color);
        sprite.setAlphaMult(alphaMod * (color.getAlpha() / 255f));
        sprite.renderAtCenter(x, y);
      }
    } finally {
      GL11.glPopMatrix();
    }
  }

  private Color adjustColorForKarma(Color baseColor, float karma, float alpha) {
    int r = baseColor.getRed();
    int g = baseColor.getGreen();
    int b = baseColor.getBlue();
    int karmaEffect = (int) (karma * 0.3f * 255);
    r = Math.min(255, r + karmaEffect);
    g = Math.min(255, g + karmaEffect);
    b = Math.min(255, b + karmaEffect);
    int finalAlpha = Math.max(0, Math.min(255, (int) (255 * alpha)));
    return new Color(r, g, b, finalAlpha);
  }

  private void renderRangeIndicator(float radius, Color color) {
    if (radius <= 0f) return;
    GL11.glLineWidth(1.5f);
    GL11.glColor4ub(
        (byte) color.getRed(),
        (byte) color.getGreen(),
        (byte) color.getBlue(),
        (byte) color.getAlpha());
    GL11.glBegin(GL11.GL_LINE_LOOP);
    int segments = 120;
    for (int i = 0; i < segments; i++) {
      float angle = 2.0f * (float) Math.PI * i / segments;
      float x = radius * (float) Math.cos(angle);
      float y = radius * (float) Math.sin(angle);
      GL11.glVertex2f(x, y);
    }
    GL11.glEnd();
    GL11.glLineWidth(1f);
  }
}
