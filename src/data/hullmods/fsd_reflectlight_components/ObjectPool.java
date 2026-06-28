package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import data.hullmods.FSD_ReflectLight;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.util.vector.Vector2f;

public class ObjectPool {
  private static boolean initialized = false;
  private static final Queue<Vector2f> vector2fPool = new ConcurrentLinkedQueue<>();
  private static final Queue<Color> colorPool = new ConcurrentLinkedQueue<>();
  private static final Queue<List<String>> stringListPool = new ConcurrentLinkedQueue<>();
  private static final Queue<List<Integer>> intListPool = new ConcurrentLinkedQueue<>();
  private static final int INITIAL_POOL_SIZE = 100;
  private static final int MAX_POOL_SIZE = 1000;

  public static void initialize() {
    if (initialized) return;
    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      vector2fPool.offer(new Vector2f());
    }
    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      colorPool.offer(new Color(1f, 1f, 1f, 1f));
    }
    for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
      stringListPool.offer(new ArrayList<String>());
      intListPool.offer(new ArrayList<Integer>());
    }
    initialized = true;
    if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING) {
      Global.getLogger(ObjectPool.class).info("【Performance optimization】object pool system initialized");
    }
  }

  public static Vector2f getVector2f() {
    if (!initialized) initialize();
    Vector2f vec = vector2fPool.poll();
    if (vec == null) {
      vec = new Vector2f();
    }
    return vec;
  }

  public static Vector2f getVector2f(float x, float y) {
    Vector2f vec = getVector2f();
    vec.set(x, y);
    return vec;
  }

  public static void recycle(Vector2f vec) {
    if (vec == null || vector2fPool.size() >= MAX_POOL_SIZE) return;
    vec.set(0f, 0f);
    vector2fPool.offer(vec);
  }

  public static Color getColor() {
    if (!initialized) initialize();
    Color color = colorPool.poll();
    if (color == null) {
      color = new Color(1f, 1f, 1f, 1f);
    }
    return color;
  }

  public static Color getColor(float r, float g, float b, float a) {
    return new Color(r, g, b, a);
  }

  public static void recycle(Color color) {
    if (color == null || colorPool.size() >= MAX_POOL_SIZE) return;
    colorPool.offer(color);
  }

  public static List<String> getStringList() {
    if (!initialized) initialize();
    List<String> list = stringListPool.poll();
    if (list == null) {
      list = new ArrayList<>();
    }
    return list;
  }

  public static void recycleStringList(List<String> list) {
    if (list == null || stringListPool.size() >= MAX_POOL_SIZE) return;
    list.clear();
    stringListPool.offer(list);
  }

  public static List<Integer> getIntList() {
    if (!initialized) initialize();
    List<Integer> list = intListPool.poll();
    if (list == null) {
      list = new ArrayList<>();
    }
    return list;
  }

  public static void recycleIntList(List<Integer> list) {
    if (list == null || intListPool.size() >= MAX_POOL_SIZE) return;
    list.clear();
    intListPool.offer(list);
  }

  public static String getPoolStats() {
    return String.format(
        "Vector2f pool: %d, Color pool: %d, StringList pool: %d, IntList pool: %d",
        vector2fPool.size(), colorPool.size(), stringListPool.size(), intListPool.size());
  }
}
