package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class FSD_ManeuveringJet extends BaseShipSystemScript {
    private static final float EnergyWeaponDamageBonus = 0.33f;
    private static final float SPEED_BONUS = 100f;
    private static final float MOBILITY_BONUS = 1.0f;
    private final String ShipGlowTexture = "graphics/hulls/FSD_baiju/FSD_baiju_glow1.png";
    private Color COLOR = new Color(255,241,67, 100);
    public static final Color JITTER_COLOR = new Color(255, 90, 109,55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 90, 90, 128);
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        float jitterLevel = effectLevel;
        float jitterRangeBonus = 0;
        float maxRangeBonus = 10f;
        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        }
        jitterLevel = (float) Math.sqrt(jitterLevel);
        effectLevel *= effectLevel;

//                0,0,
//                7f,
//                0.1f,0.25f,0.1f,
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 10, 0f, 1f + jitterRangeBonus);

        ship.setExtraAlphaMult(1-(0.25f*effectLevel));
        drawGlow(ship, ship.getLocation());
//                1f,
//                2,
//                10f,
//                20f,
//                1);
        stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
        stats.getAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getDeceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getTurnAcceleration().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getMaxTurnRate().modifyPercent(id, MOBILITY_BONUS * 100f);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, EnergyWeaponDamageBonus * 100f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

    }
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData(String.format("Energy weapon damage +%.0f%%", EnergyWeaponDamageBonus * 100), false);
        }
        int SpeedBonus = (int) (SPEED_BONUS);
        if (index == 1) {
            return new StatusData(String.format("Speed +%d Maneuverability +%.0f%%",
                    SpeedBonus, MOBILITY_BONUS * 100), false);
        }
        return null;
    }

    public void drawGlow(ShipAPI ship, Vector2f loc){
        SpriteAPI sprite = Global.getSettings().getSprite(ShipGlowTexture);
        float alpha = ship.getSystem().getEffectLevel();
        //Color color = new Color(Math.min(255-(14*alpha/9),255), Math.min(255-(232*alpha/9),255), Math.min(255-(232*alpha/9),255), COLOR.getAlpha());
        Vector2f size = new Vector2f(sprite.getWidth(), sprite.getHeight());
        float clampedAlpha = Math.min(1.0f, Math.max(0.0f, alpha / 9f));
//        float clampedAlpha = 1.0f;
        float visualAlpha = (float) (1.0 - Math.pow(1.0 - clampedAlpha, 2));
        for (int i = 0; i < 2; i = i + 1) {
            sprite.setAlphaMult(visualAlpha);
            MagicRender.objectspace(
                    sprite,
                    ship,
                    new Vector2f(),
                    new Vector2f(),
                    size,
                    ship.getRenderOffset(),
                    -180f,
                    0f,
                    true,
                    Misc.scaleAlpha(COLOR,visualAlpha),
                    2f,
                    0.5f,
                    1f,
                    1f,
                    0f,
                    0.05f,
                    0.05f,
                    0.2f,
                    true,
                    CombatEngineLayers.ABOVE_SHIPS_LAYER,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE
            );
        }
//                1f,
//                2,
//                10f,
//                20f,
//                1);
    }
}
