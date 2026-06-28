package data.hullmods.fsd_reflectlight_components;

public enum AnimationState {
  INACTIVE,
  FORMING,
  IDLE,
  ENHANCED_IDLE,
  ENHANCING,
  DEHANCING,
  DISSOLVING;

  public AnimationState getNextState() {
    switch (this) {
      case FORMING:
        return IDLE;
      case ENHANCING:
        return ENHANCED_IDLE;
      case DEHANCING:
        return IDLE;
      case DISSOLVING:
        return INACTIVE;
      default:
        return this;
    }
  }

  public static AnimationState getPlayerFormingTargetState() {
    return ENHANCED_IDLE;
  }

  public static AnimationState getNonPlayerFormingTargetState() {
    return IDLE;
  }

  public boolean isTransitionState() {
    return this == ENHANCING || this == DEHANCING || this == FORMING || this == DISSOLVING;
  }

  public boolean isStableState() {
    return this == IDLE || this == ENHANCED_IDLE;
  }

  public boolean isEnhancedOrEnhancing() {
    return this == ENHANCED_IDLE || this == ENHANCING;
  }

  public boolean needsDualRingRendering() {
    return this == ENHANCED_IDLE || this == ENHANCING || this == DEHANCING;
  }

  public float getDefaultDuration() {
    switch (this) {
      case FORMING:
        return 1.5f;
      case ENHANCING:
        return 1.0f;
      case DEHANCING:
        return 1.0f;
      case DISSOLVING:
        return 1.2f;
      default:
        return 0f;
    }
  }
}
