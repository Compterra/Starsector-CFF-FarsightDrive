package data.hullmods.fsd_reflectlight_components;

public class TrigCache {
  private static final float ANGLE_PRECISION = 0.1f;
  private static final float[] SIN_CACHE = new float[(int) (361 / ANGLE_PRECISION)];
  private static final float[] COS_CACHE = new float[(int) (361 / ANGLE_PRECISION)];
  private static boolean initialized = false;

  public static void initialize() {
    if (initialized) return;
    for (float i = 0; i <= 360; i += ANGLE_PRECISION) {
      int index = (int) (i / ANGLE_PRECISION);
      if (index < SIN_CACHE.length) {
        double radians = Math.toRadians(i);
        SIN_CACHE[index] = (float) Math.sin(radians);
        COS_CACHE[index] = (float) Math.cos(radians);
      }
    }
    initialized = true;
  }

  public static float sin(float angleDegrees) {
    if (!initialized) initialize();
    while (angleDegrees < 0) angleDegrees += 360f;
    while (angleDegrees >= 360) angleDegrees -= 360f;
    int index = (int) Math.round(angleDegrees / ANGLE_PRECISION) % SIN_CACHE.length;
    return SIN_CACHE[index];
  }

  public static float cos(float angleDegrees) {
    if (!initialized) initialize();
    while (angleDegrees < 0) angleDegrees += 360f;
    while (angleDegrees >= 360) angleDegrees -= 360f;
    int index = (int) Math.round(angleDegrees / ANGLE_PRECISION) % COS_CACHE.length;
    return COS_CACHE[index];
  }

  public static float sinRad(float radians) {
    float degrees = (float) Math.toDegrees(radians);
    return sin(degrees);
  }

  public static float cosRad(float radians) {
    float degrees = (float) Math.toDegrees(radians);
    return cos(degrees);
  }
}
