package data.hullmods.fsd_reflectlight_components;

import data.hullmods.FSD_ReflectLight;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class GLStateManager {
  private static boolean initialized = false;
  private static boolean textureEnabled = false;
  private static boolean blendEnabled = false;
  private static int currentBlendSrc = GL11.GL_SRC_ALPHA;
  private static int currentBlendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
  private static boolean lineStippleEnabled = false;
  private static boolean matrixPushed = false;
  private static boolean depthTestEnabled = true;
  private static boolean savedTextureEnabled = false;
  private static boolean savedBlendEnabled = false;
  private static int savedBlendSrc = GL11.GL_SRC_ALPHA;
  private static int savedBlendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
  private static FloatBuffer savedColor = BufferUtils.createFloatBuffer(4);
  private static boolean savedDepthTestEnabled = true;

  public static void initialize() {
    if (initialized) return;
    try {
      if (GLContext.getCapabilities() == null) {
        if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
          FSD_ReflectLight.log.info(
              "[FSD][GLStateManager] OpenGL context unavailable，GLStateManager will  in defer initialization until first use");
        }
        return;
      }
      textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
      lineStippleEnabled = GL11.glIsEnabled(GL11.GL_LINE_STIPPLE);
      depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      initialized = true;
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info("[FSD][GLStateManager] initialization complete");
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] initialization failed", e);
      }
    }
  }

  public static void setDepthTestEnabled(boolean enable) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      if (depthTestEnabled != enable) {
        if (enable) {
          GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
          GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        depthTestEnabled = enable;
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set depth test state", e);
      }
    }
  }

  public static void setTextureEnabled(boolean enable) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      if (textureEnabled != enable) {
        if (enable) {
          GL11.glEnable(GL11.GL_TEXTURE_2D);
        } else {
          GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        textureEnabled = enable;
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set texture state", e);
      }
    }
  }

  public static void setBlendEnabled(boolean enable) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      if (blendEnabled != enable) {
        if (enable) {
          GL11.glEnable(GL11.GL_BLEND);
        } else {
          GL11.glDisable(GL11.GL_BLEND);
        }
        blendEnabled = enable;
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set blend state", e);
      }
    }
  }

  public static void setBlendFunc(int src, int dst) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      if (currentBlendSrc != src || currentBlendDst != dst) {
        GL11.glBlendFunc(src, dst);
        currentBlendSrc = src;
        currentBlendDst = dst;
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set blend function", e);
      }
    }
  }

  public static void setLineStippleEnabled(boolean enable) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      if (lineStippleEnabled != enable) {
        if (enable) {
          GL11.glEnable(GL11.GL_LINE_STIPPLE);
        } else {
          GL11.glDisable(GL11.GL_LINE_STIPPLE);
        }
        lineStippleEnabled = enable;
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set line stipple state", e);
      }
    }
  }

  public static void setLineStipple(int factor, short pattern) {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      GL11.glLineStipple(factor, pattern);
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to set line stipple mode", e);
      }
    }
  }

  public static void resetToDefault() {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      setTextureEnabled(true);
      setBlendEnabled(true);
      setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      setLineStippleEnabled(false);
      setDepthTestEnabled(true);
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] failed to reset to default state", e);
      }
    }
  }

  public static void applyRenderPreset() {
    setTextureEnabled(false);
    setBlendEnabled(true);
    setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  public static void applyRuneRenderPreset() {
    setTextureEnabled(true);
    setBlendEnabled(true);
    setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  public static void applyGlowPreset() {
    setTextureEnabled(true);
    setBlendEnabled(true);
    setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
  }

  public static void applyUnlimitedDistanceRenderPreset() {
    setTextureEnabled(true);
    setBlendEnabled(true);
    setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    setDepthTestEnabled(false);
  }

  public static void saveGLState() {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      savedTextureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      savedBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
      savedBlendSrc = currentBlendSrc;
      savedBlendDst = currentBlendDst;
      savedDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      savedColor.clear();
      GL11.glGetFloat(GL11.GL_CURRENT_COLOR, savedColor);
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info("[FSD][GLStateManager] saved OpenGL state");
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] saved OpenGL statefailed", e);
      }
    }
  }

  public static void restoreGLState() {
    if (!initialized) initialize();
    if (!initialized) return;
    try {
      setTextureEnabled(savedTextureEnabled);
      setBlendEnabled(savedBlendEnabled);
      setBlendFunc(savedBlendSrc, savedBlendDst);
      setDepthTestEnabled(savedDepthTestEnabled);
      savedColor.rewind();
      GL11.glColor4f(savedColor.get(0), savedColor.get(1), savedColor.get(2), savedColor.get(3));
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.info("[FSD][GLStateManager] restored OpenGL state");
      }
    } catch (Exception e) {
      if (FSD_ReflectLight.ENABLE_DETAIL_LOGGING && FSD_ReflectLight.log != null) {
        FSD_ReflectLight.log.error("[FSD][GLStateManager] restored OpenGL statefailed", e);
      }
    }
  }
}
