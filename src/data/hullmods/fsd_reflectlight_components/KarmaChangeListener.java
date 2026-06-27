package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.combat.ShipAPI;

public interface KarmaChangeListener {
  void onKarmaChanged(ShipAPI ship, float oldKarma, float newKarma, float delta, KarmaType type);

  void onKarmaThresholdReached(ShipAPI ship, float threshold, ThresholdType type);
}
