package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.ShipAPI;
import java.util.List;

public interface KarmaSystemAPI {
  float getKarma(ShipAPI ship);

  float getKarmaMax(ShipAPI ship);

  float getKarmaPercent(ShipAPI ship);

  boolean hasKarma(ShipAPI ship, float amount);

  void setKarma(ShipAPI ship, float karma, KarmaType type);

  float addKarma(ShipAPI ship, float amount, KarmaType type);

  float consumeKarma(ShipAPI ship, float amount, KarmaType type);

  float reduceKarma(ShipAPI ship, float amount, KarmaType type);

  void multiplyKarma(ShipAPI ship, float multiplier, KarmaType type);

  void setKarmaMax(ShipAPI ship, float max);

  void modifyKarmaMax(ShipAPI ship, float multiplier);

  void setKarmaGainEfficiency(ShipAPI ship, float efficiency);

  void setKarmaDissipationRate(ShipAPI ship, float rate);
  
  void setKarmaDissipationRate(ShipAPI ship, float rate, int priority, String source);

  void setKarmaGainRange(ShipAPI ship, float rangeMultiplier);

  KarmaData getKarmaData(ShipAPI ship);

  void addKarmaChangeListener(ShipAPI ship, KarmaChangeListener listener);

  void removeKarmaChangeListener(ShipAPI ship, KarmaChangeListener listener);

  List<KarmaHistoryEntry> getKarmaHistory(ShipAPI ship, int count);
}
