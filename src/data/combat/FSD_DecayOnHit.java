package data.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.everyframe.FSD_BloodEatEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FSD_DecayOnHit implements OnHitEffectPlugin {
    private Color color = new Color(182, 127, 64, 255);
    private float TOTAL_DAMAGE = 200f;
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Vector2f offset = new Vector2f();
        Vector2f.sub(point, target.getLocation(), offset);
        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
        if(target instanceof ShipAPI) {
            FSD_BloodEatEffect bloodEatEffect = new FSD_BloodEatEffect(projectile, (ShipAPI) target, offset, 0.5f, 100, TOTAL_DAMAGE, 1f, false, true);
            CombatEntityAPI e = engine.addLayeredRenderingPlugin(bloodEatEffect);
        }
    }
}
