package data.hullmods.fsd_reflectlight_components;

/**
 */
public class KarmaPriority {
  /**
   */
  public static final int PRIORITY_OVERRIDE = 100;
  
  /**
   */
  public static final int PRIORITY_HIGH = 50;
  
  /**
   */
  public static final int PRIORITY_NORMAL = 0;
  
  /**
   */
  public static final int PRIORITY_NONE = -1;
  
  private KarmaPriority() {
  }
  
  /**
   */
  public static String getPriorityName(int priority) {
    if (priority >= PRIORITY_OVERRIDE) {
      return "OVERRIDE";
    } else if (priority >= PRIORITY_HIGH) {
      return "HIGH";
    } else if (priority >= PRIORITY_NORMAL) {
      return "NORMAL";
    } else {
      return "NONE";
    }
  }
}

