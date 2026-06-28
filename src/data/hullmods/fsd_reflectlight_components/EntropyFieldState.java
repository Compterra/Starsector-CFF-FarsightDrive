package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.ShipAPI;
import data.hullmods.FSD_ReflectLight;
import java.util.List;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

public class EntropyFieldState {
  private static final float INNER_RING_RADIUS_FACTOR = 0.85f;
  private static final float KARMA_TRANSITION_SPEED = 0.8f;
  private static final float PULSE_SPEED = 1.0f;
  private static final float INNER_ROTATION_SPEED = 2.0f;
  private static final float OUTER_ROTATION_SPEED = INNER_ROTATION_SPEED;
  private static final float MAX_JITTER_AMOUNT = 3.0f;
  private static final float PHASE_JITTER_AMOUNT = 1.5f;
  private static final float PHASE_ALPHA_MULT = 0.3f;
  private static final float RUNE_SIZE_TO_RADIUS_RATIO = 0.08f;
  private static final float MIN_RUNE_SIZE_DYNAMIC = 12f;
  private static final float MAX_RUNE_SIZE_DYNAMIC_INNER = 40f;
  private static final float MAX_RUNE_SIZE_DYNAMIC_OUTER = 80f;
  private final ShipAPI ship;
  private boolean isPlayerShip;
  private final float entropyFieldRadius;
  private final float shipVisualRadius;
  private AnimationState animState;
  private float animProgress;
  private float animDuration;
  private float displayKarma;
  private List<String> currentSentence;
  private List<String> outerRingSentence;
  private float pulsePhase;
  private float innerRingRotation;
  private float outerRingRotation;
  private float currentAlpha;
  private float innerRingRadius;
  private float outerRingRadius;
  private float innerRuneSize;
  private float outerRuneSize;
  private Vector2f jitter;
  private float displayMaxCharsInner;
  private float displayMaxCharsOuter;
  private static final Random random = new Random();

  public EntropyFieldState(ShipAPI ship, float initialEntropyRadius, boolean isPlayerShip) {
    this.ship = ship;
    this.isPlayerShip = isPlayerShip;
    this.jitter = new Vector2f(0f, 0f);
    this.entropyFieldRadius = initialEntropyRadius;
    this.shipVisualRadius = calculateShipVisualRadius();
    this.displayKarma = getActualKarma();
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
      FSD_ReflectLight.log.info(
          "[EntropyFieldState] ship "
              + (ship != null ? ship.getId() : "null")
              + " constructed，initialdisplayKarma="
              + this.displayKarma
              + ", isPlayer="
              + isPlayerShip);
    }
    this.animState = AnimationState.FORMING;
    this.animProgress = 0f;
    this.animDuration = this.animState.getDefaultDuration();
    this.currentAlpha = 0f;
    this.pulsePhase = 0f;
    this.innerRingRotation = 0f;
    this.outerRingRotation = 0f;
    this.innerRingRadius = initialEntropyRadius * 0.1f;
    this.outerRingRadius = initialEntropyRadius * 2.0f;
    this.innerRuneSize = calculateRuneSize(false);
    this.outerRuneSize = calculateRuneSize(true);
    this.displayMaxCharsInner = 0f;
    this.displayMaxCharsOuter = 0f;
    if (ship != null) {
      this.currentSentence = FSD_ReflectLight.getFixedRuneSentence(ship);
      List<String> randomSentence;
      do {
        randomSentence = FSD_ReflectLight.getRandomRuneSentence();
      } while (randomSentence.equals(this.currentSentence)
          && RuneConfigLoader.runeSentences.size() > 1);
      this.outerRingSentence = randomSentence;
    } else {
      this.currentSentence = FSD_ReflectLight.getRandomRuneSentence();
      this.outerRingSentence = FSD_ReflectLight.getRandomRuneSentence();
    }
  }

  public void updateState(float amount) {
    this.jitter.set(0f, 0f);
    float actualKarma = getActualKarma();
    if (this.displayKarma != actualKarma) {
      this.displayKarma = lerp(this.displayKarma, actualKarma, amount * KARMA_TRANSITION_SPEED);
      if (Math.abs(this.displayKarma - actualKarma) < 0.01f) {
        this.displayKarma = actualKarma;
      }
    }
    this.pulsePhase = (this.pulsePhase + amount * PULSE_SPEED) % (float) (Math.PI * 2);
    this.innerRingRotation += amount * INNER_ROTATION_SPEED * (0.5f + this.displayKarma * 0.5f);
    this.outerRingRotation += amount * OUTER_ROTATION_SPEED * (0.5f + this.displayKarma * 0.5f);
    if (this.animState.isTransitionState()) {
      updateAnimation(amount);
    } else {
      if (this.animState == AnimationState.IDLE) {
        this.innerRingRadius = calculateIdleRadius();
        this.currentAlpha = 1.0f;
      } else if (this.animState == AnimationState.ENHANCED_IDLE) {
        this.outerRingRadius = this.entropyFieldRadius * 0.7f;
        this.innerRingRadius = this.outerRingRadius * INNER_RING_RADIUS_FACTOR;
        this.currentAlpha = 1.0f;
      }
    }
    this.innerRuneSize = calculateRuneSize(false);
    this.outerRuneSize = calculateRuneSize(true);
    updateDisplayMaxChars(amount);
    handlePhaseEffects();
  }

  private void updateAnimation(float amount) {
    this.animProgress += amount / this.animDuration;
    if (this.animProgress >= 1.0f) {
      this.animProgress = 1.0f;
      completeAnimation();
      return;
    }
    float easedProgress = easeInOutCubic(this.animProgress);
    switch (this.animState) {
      case FORMING:
        updateFormingAnimation(easedProgress);
        break;
      case ENHANCING:
        updateEnhancingAnimation(easedProgress);
        break;
      case DEHANCING:
        updateDehancingAnimation(easedProgress);
        break;
      case DISSOLVING:
        updateDissolvingAnimation(easedProgress);
        break;
    }
  }

  private void completeAnimation() {
    AnimationState nextState = this.animState.getNextState();
    if (this.animState == AnimationState.FORMING) {
      nextState = this.isPlayerShip ? AnimationState.ENHANCED_IDLE : AnimationState.IDLE;
    }
    setAnimationState(nextState);
  }

  private void updateFormingAnimation(float progress) {
    this.currentAlpha = Math.min(1.0f, Math.max(0f, progress));
    if (this.isPlayerShip) {
      float targetOuterRadius = this.entropyFieldRadius * 0.7f;
      float targetInnerRadius = targetOuterRadius * INNER_RING_RADIUS_FACTOR;
      this.innerRingRadius = lerp(targetInnerRadius * 0.1f, targetInnerRadius, progress);
      this.outerRingRadius = lerp(targetOuterRadius * 2.0f, targetOuterRadius, progress);
    } else {
      float targetIdleRadius = calculateIdleRadius();
      this.innerRingRadius = lerp(targetIdleRadius * 0.1f, targetIdleRadius, progress);
    }
  }

  private void updateEnhancingAnimation(float progress) {
    float targetOuterRadius = this.entropyFieldRadius * 0.7f;
    float targetInnerRadius = targetOuterRadius * INNER_RING_RADIUS_FACTOR;
    this.innerRingRadius = lerp(calculateIdleRadius(), targetInnerRadius, progress);
    this.outerRingRadius = lerp(targetOuterRadius * 2.0f, targetOuterRadius, progress);
  }

  private void updateDehancingAnimation(float progress) {
    float startOuterRadius = this.entropyFieldRadius * 0.7f;
    float startInnerRadius = startOuterRadius * INNER_RING_RADIUS_FACTOR;
    this.innerRingRadius = lerp(startInnerRadius, calculateIdleRadius(), progress);
    this.outerRingRadius = lerp(startOuterRadius, startOuterRadius * 2.0f, progress);
  }

  private void updateDissolvingAnimation(float progress) {
    this.currentAlpha = Math.min(1.0f, Math.max(0f, 1.0f - progress));
    float jitterAmount = MAX_JITTER_AMOUNT * progress;
    this.jitter.set(
        (random.nextFloat() - 0.5f) * jitterAmount, (random.nextFloat() - 0.5f) * jitterAmount);
  }

  public void setAnimationState(AnimationState newState) {
    if (this.animState == newState) return;
    this.animState = newState;
    this.animProgress = 0f;
    this.animDuration = newState.getDefaultDuration();
    if (newState == AnimationState.IDLE) {
      this.innerRingRadius = calculateIdleRadius();
      this.currentAlpha = 1.0f;
      this.jitter.set(0f, 0f);
    } else if (newState == AnimationState.ENHANCED_IDLE) {
      this.outerRingRadius = this.entropyFieldRadius * 0.7f;
      this.innerRingRadius = this.outerRingRadius * INNER_RING_RADIUS_FACTOR;
      this.currentAlpha = 1.0f;
      this.jitter.set(0f, 0f);
    }
  }

  public void setPlayerShip(boolean isPlayerShip) {
    if (this.isPlayerShip == isPlayerShip) return;
    this.isPlayerShip = isPlayerShip;
    if (isPlayerShip) {
      if (this.animState == AnimationState.IDLE) {
        setAnimationState(AnimationState.ENHANCING);
      }
    } else {
      if (this.animState == AnimationState.ENHANCED_IDLE) {
        setAnimationState(AnimationState.DEHANCING);
      }
    }
  }

  private float getActualKarma() {
    if (ship == null) {
      return 0f;
    }
    KarmaData data = KarmaManager.getInstance().getKarmaData(ship);
    return data != null ? data.getKarma() : 0f;
  }

  private float calculateRuneSize(boolean isOuterRing) {
    if (isOuterRing) {
      float innerSize = calculateRuneSize(false);
      float calculatedSize = innerSize * 2f;
      return Math.min(MAX_RUNE_SIZE_DYNAMIC_OUTER, calculatedSize);
    } else {
      float radius = this.innerRingRadius;
      if (radius < 1f) {
        return MIN_RUNE_SIZE_DYNAMIC;
      }
      float calculatedSize = radius * RUNE_SIZE_TO_RADIUS_RATIO;
      return Math.max(MIN_RUNE_SIZE_DYNAMIC, Math.min(MAX_RUNE_SIZE_DYNAMIC_INNER, calculatedSize));
    }
  }

  private float calculateShipVisualRadius() {
    if (ship == null) {
      return 100f;
    }
    if (ship.getShield() != null) {
      return ship.getShield().getRadius();
    } else {
      return Math.max(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()) / 2f;
    }
  }

  private float calculateIdleRadius() {
    return this.shipVisualRadius + getInnerRuneSize();
  }

  public float getPulseAlpha() {
    return 0.6f + 0.4f * (float) Math.sin(this.pulsePhase);
  }

  private static float lerp(float a, float b, float t) {
    return a + t * (b - a);
  }

  private static float easeInOutCubic(float t) {
    return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
  }

  public ShipAPI getShip() {
    return ship;
  }

  public boolean isPlayerShip() {
    return isPlayerShip;
  }

  public AnimationState getAnimState() {
    return animState;
  }

  public float getAnimProgress() {
    return animProgress;
  }

  public float getDisplayKarma() {
    return displayKarma;
  }

  public float getCurrentAlpha() {
    return Math.min(1.0f, Math.max(0f, currentAlpha));
  }

  public float getInnerRingRadius() {
    return innerRingRadius;
  }

  public float getOuterRingRadius() {
    return outerRingRadius;
  }

  public float getInnerRuneSize() {
    return innerRuneSize;
  }

  public float getOuterRuneSize() {
    return outerRuneSize;
  }

  public float getInnerRingRotation() {
    return innerRingRotation;
  }

  public float getOuterRingRotation() {
    return outerRingRotation;
  }

  public Vector2f getJitter() {
    return jitter;
  }

  public List<String> getCurrentSentence() {
    return currentSentence;
  }

  public List<String> getOuterRingSentence() {
    return outerRingSentence;
  }

  public float getDisplayMaxCharsInner() {
    return displayMaxCharsInner;
  }

  public float getDisplayMaxCharsOuter() {
    return displayMaxCharsOuter;
  }

  public float getEntropyFieldRadius() {
    return entropyFieldRadius;
  }

  private void handlePhaseEffects() {
    if (ship == null || animState == AnimationState.INACTIVE) {
      return;
    }
    if (ship.isPhased()) {
      this.currentAlpha *= PHASE_ALPHA_MULT;
      this.jitter.set(
          (random.nextFloat() - 0.5f) * PHASE_JITTER_AMOUNT,
          (random.nextFloat() - 0.5f) * PHASE_JITTER_AMOUNT);
    }
  }

  private void updateDisplayMaxChars(float amount) {
    if (this.innerRingRadius > 1f && this.innerRuneSize > 1f) {
      float circumferenceInner = 2f * (float) Math.PI * this.innerRingRadius;
      float widthPerCharInner =
          this.innerRuneSize + this.innerRuneSize * FSD_ReflectLight.CHAR_SPACING_FACTOR;
      float targetMaxCharsInner = circumferenceInner / widthPerCharInner;
      float t = Math.min(1.0f, amount * 5f);
      this.displayMaxCharsInner = lerp(this.displayMaxCharsInner, targetMaxCharsInner, t);
    }
    if (this.outerRingRadius > 1f && this.outerRuneSize > 1f) {
      float circumferenceOuter = 2f * (float) Math.PI * this.outerRingRadius;
      float widthPerCharOuter =
          this.outerRuneSize + this.outerRuneSize * FSD_ReflectLight.CHAR_SPACING_FACTOR;
      float targetMaxCharsOuter = circumferenceOuter / widthPerCharOuter;
      float t = Math.min(1.0f, amount * 5f);
      this.displayMaxCharsOuter = lerp(this.displayMaxCharsOuter, targetMaxCharsOuter, t);
    }
  }
}
