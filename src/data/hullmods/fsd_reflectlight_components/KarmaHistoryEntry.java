package data.hullmods.fsd_reflectlight_components;

public class KarmaHistoryEntry {
  public final float timestamp;
  public final float oldValue;
  public final float newValue;
  public final float delta;
  public final KarmaType type;
  public final String source;

  public KarmaHistoryEntry(
      float timestamp, float oldValue, float newValue, float delta, KarmaType type, String source) {
    this.timestamp = timestamp;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.delta = delta;
    this.type = type;
    this.source = source;
  }

  @Override
  public String toString() {
    return String.format(
        "[%.2fs] %.2f -> %.2f (%.2f) [%s]%s",
        timestamp, oldValue, newValue, delta, type, source != null ? " - " + source : "");
  }
}
