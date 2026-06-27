package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class OptimizedBatchRenderer {
  private static FloatBuffer sharedVertexBuffer;
  private static final int MAX_VERTICES = 2048;
  private static boolean batchActive = false;
  private static int currentVertexCount = 0;
  private static int currentRenderMode = GL11.GL_TRIANGLES;
  private static final Map<Integer, float[]> circleVertexCache = new HashMap<>();
  private static boolean initialized = false;

  public static void initialize() {
    if (initialized) return;
    try {
      if (GLContext.getCapabilities() == null) {
        Global.getLogger(OptimizedBatchRenderer.class)
            .info("OpenGL context unavailable，OptimizedBatchRenderer will  in defer initialization until first use");
        return;
      }
      if (sharedVertexBuffer == null) {
        sharedVertexBuffer = BufferUtils.createFloatBuffer(MAX_VERTICES * 2);
      }
      initialized = true;
    } catch (Exception e) {
      Global.getLogger(OptimizedBatchRenderer.class).error("OptimizedBatchRendererinitialization failed：", e);
    }
  }

  public static void beginBatch() {
    beginBatch(GL11.GL_TRIANGLES);
  }

  public static void beginBatch(int renderMode) {
    if (!initialized) initialize();
    if (!initialized) return;
    if (sharedVertexBuffer == null) initialize();
    if (sharedVertexBuffer == null) return;
    if (batchActive) {
      endBatch();
    }
    sharedVertexBuffer.clear();
    currentVertexCount = 0;
    currentRenderMode = renderMode;
    batchActive = true;
  }

  public static void addVertex(float x, float y) {
    if (!initialized) initialize();
    if (!initialized || sharedVertexBuffer == null) return;
    if (!batchActive) {
      beginBatch();
    }
    if (currentVertexCount >= MAX_VERTICES) {
      int savedRenderMode = currentRenderMode;
      endBatch();
      beginBatch(savedRenderMode);
    }
    sharedVertexBuffer.put(x).put(y);
    currentVertexCount++;
  }

  public static void endBatch() {
    endBatch(currentRenderMode);
  }

  public static void endBatch(int renderMode) {
    if (!initialized || !batchActive || sharedVertexBuffer == null) {
      return;
    }
    try {
      if (currentVertexCount > 0) {
        sharedVertexBuffer.flip();
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(2, 0, sharedVertexBuffer);
        GL11.glDrawArrays(renderMode, 0, currentVertexCount);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
      }
    } catch (Exception e) {
    } finally {
      batchActive = false;
      currentVertexCount = 0;
    }
  }

  public static void drawCircle(
      float centerX, float centerY, float radius, int segments, Color color, boolean filled) {
    if (!initialized) initialize();
    if (!initialized) return;
    if (segments <= 0) segments = 36;
    float[] circleVertices = getCircleVertices(segments);
    GL11.glColor4f(
        color.getRed() / 255f,
        color.getGreen() / 255f,
        color.getBlue() / 255f,
        color.getAlpha() / 255f);
    if (filled) {
      beginBatch(GL11.GL_TRIANGLE_FAN);
      addVertex(centerX, centerY);
      for (int i = 0; i <= segments; i++) {
        int idx = (i % segments) * 2;
        addVertex(
            centerX + radius * circleVertices[idx], centerY + radius * circleVertices[idx + 1]);
      }
      endBatch();
    } else {
      beginBatch(GL11.GL_LINE_LOOP);
      for (int i = 0; i < segments; i++) {
        int idx = i * 2;
        addVertex(
            centerX + radius * circleVertices[idx], centerY + radius * circleVertices[idx + 1]);
      }
      endBatch();
    }
  }

  private static float[] getCircleVertices(int segments) {
    if (!circleVertexCache.containsKey(segments)) {
      float[] vertices = new float[segments * 2];
      float angleStep = 360f / segments;
      for (int i = 0; i < segments; i++) {
        float angle = i * angleStep;
        int idx = i * 2;
        vertices[idx] = TrigCache.cos(angle);
        vertices[idx + 1] = TrigCache.sin(angle);
      }
      circleVertexCache.put(segments, vertices);
    }
    return circleVertexCache.get(segments);
  }

  public static void setupForFieldRendering() {
    if (!initialized) initialize();
    if (!initialized) return;
    GLStateManager.setTextureEnabled(false);
    GLStateManager.setBlendEnabled(true);
    GLStateManager.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }
}
