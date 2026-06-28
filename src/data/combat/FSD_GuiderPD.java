package data.combat;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.combat.CollisionClass.MISSILE_FF;
import static com.fs.starfarer.api.combat.CollisionClass.MISSILE_NO_FF;
import static com.fs.starfarer.api.impl.campaign.DModManager.getMod;
import static com.fs.starfarer.api.impl.campaign.missions.academy.GAPZPostEncounters.id;

public class FSD_GuiderPD implements BeamEffectPlugin {
    private float ran = (float) Math.random();
    private float chance;
    private float duration;
    private boolean wasZero = true;
    private IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if(target instanceof MissileAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                if (beam.getWeapon().getShip().getFluxLevel() <= 0.3f) {
                    this.chance = 0.20f;
                    if (ran <= chance) {
                        target.setFacing(target.getOwner());
                        target.setOwner(beam.getWeapon().getShip().getOwner());
                        duration = ((MissileAPI) target).getMaxFlightTime();
                        ((MissileAPI) target).setMaxFlightTime(duration * 2f);
                        ((MissileAPI) target).setSource(beam.getWeapon().getShip());
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(255, 169, 169, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(239, 197, 210, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(206, 3, 171, 255));

                    }
                } else {
                    this.chance = 0.10f;
                    if (ran <= chance) {
                        target.setFacing(target.getOwner());
                        target.setOwner(beam.getWeapon().getShip().getOwner());
                        duration = ((MissileAPI) target).getFlightTime();
                        ((MissileAPI) target).setMaxFlightTime(duration * 2f);
                        ((MissileAPI) target).setSource(beam.getWeapon().getShip());
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60f, 15f, 1f, new Color(236, 9, 75, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 40, 20f, 1.1f, new Color(241, 23, 23, 255));
                        engine.addHitParticle(target.getLocation(), new Vector2f(0, 0), 60, 20f, 1.3f, new Color(103, 3, 3, 255));
                    }
                }


            }
        }

    }
}
