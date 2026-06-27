package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;


import java.awt.*;
import java.util.List;

import static com.fs.starfarer.api.combat.CollisionClass.NONE;
import static com.fs.starfarer.api.combat.CollisionClass.PROJECTILE_FIGHTER;


public class FSD_TimeBrust extends BaseShipSystemScript {

    private static final Color ARC_COLOR = new Color(163, 86, 91, 189);
    private static final float ENERGY_DAMAGE = 500f;
    private static final float EMP_DAMAGE = 1000f;
    private static final float NORMAL_ARC_RADIUS=450f;
    private static final float REGEN_BONUS=250f;
    private static final DamagingExplosionSpec spec = new DamagingExplosionSpec(
            1f,
            15f,
            5f,
            1000f,
            1000f,
            PROJECTILE_FIGHTER,
            NONE,
            2f,
            5f,
            0.5f,
            3,
            ARC_COLOR,
            Color.WHITE);
    int i=1;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null) return;

        if(!ship.isPhased()){
            if(i!=0){
                i=0;
                for (ShipAPI otherShip : engine.getShips()) {
                    if (otherShip != ship && Misc.getDistance(ship.getLocation(), otherShip.getLocation()) < NORMAL_ARC_RADIUS && otherShip.getOwner()!=ship.getOwner() && otherShip.isAlive()) {
                        float angle = (float) Math.random() * 360f;
                        Vector2f arcStart = new Vector2f(
                                ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * NORMAL_ARC_RADIUS,
                                ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * NORMAL_ARC_RADIUS
                        );

                        for(int i = 0;i < 3;i++){
                            engine.spawnEmpArc(ship, arcStart, ship, otherShip, DamageType.ENERGY, 0f, 0f, NORMAL_ARC_RADIUS, null, 20f, ARC_COLOR, ARC_COLOR);
                        }
                        engine.spawnDamagingExplosion(spec,ship,otherShip.getLocation());
                    }
                }

                java.util.List<MissileAPI> missiles = engine.getMissiles();
                for (MissileAPI missile : missiles) {
                    if (missile.getOwner() == ship.getOwner()) continue;
                    float angle = (float) Math.random() * 360f;
                    Vector2f arcStart = new Vector2f(
                            ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * NORMAL_ARC_RADIUS,
                            ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * NORMAL_ARC_RADIUS
                    );
                    if(Misc.getDistance(ship.getLocation(), missile.getLocation())<=NORMAL_ARC_RADIUS){
                    engine.spawnEmpArc(ship, arcStart, ship, missile, DamageType.ENERGY, 0f, 0f, NORMAL_ARC_RADIUS, null, 10f, ARC_COLOR, ARC_COLOR);
                    engine.removeEntity(missile);
                    }
                }

                List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
                for (DamagingProjectileAPI projectile : projectiles) {
                    if (projectile.getOwner() == ship.getOwner()) continue;
                    float angle = (float) Math.random() * 360f;
                    Vector2f arcStart = new Vector2f(
                            ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * NORMAL_ARC_RADIUS,
                            ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * NORMAL_ARC_RADIUS
                    );
                    if(Misc.getDistance(ship.getLocation(), projectile.getLocation())<=NORMAL_ARC_RADIUS){
                    engine.spawnEmpArc(ship, arcStart, ship, projectile, DamageType.ENERGY, 0f, 0f, NORMAL_ARC_RADIUS, null, 25f, ARC_COLOR, ARC_COLOR);
                    engine.removeEntity(projectile);
                    }
                }


            }

            stats.getTimeMult().modifyMult(id, 5f);
            stats.getHullDamageTakenMult().modifyMult(id, 0f);
            stats.getArmorDamageTakenMult().modifyMult(id, 0f);
            stats.getShieldDamageTakenMult().modifyMult(id, 0f);
            stats.getEmpDamageTakenMult().modifyMult(id, 0f);
            ship.addAfterimage(new Color(163, 86, 91, 189), (float) 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 50, 0, 0, 0.7f, false, false, false);

            stats.getBallisticAmmoRegenMult().unmodifyPercent(id);
            stats.getEnergyAmmoRegenMult().unmodifyPercent(id);

        }else{
            stats.getBallisticAmmoRegenMult().modifyPercent(id, REGEN_BONUS);
            stats.getEnergyAmmoRegenMult().modifyPercent(id, REGEN_BONUS);
            stats.getMaxTurnRate().modifyMult(id,2f);
            stats.getTurnAcceleration().modifyMult(id,2f);
            stats.getMaxSpeed().modifyMult(id, 4f);
            stats.getAcceleration().modifyMult(id, 12f);
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {

        i=1;

        stats.getBallisticAmmoRegenMult().unmodifyPercent(id);
        stats.getEnergyAmmoRegenMult().unmodifyPercent(id);

        stats.getTimeMult().unmodifyMult(id);

        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);


        stats.getMaxTurnRate().unmodifyMult(id);
        stats.getTurnAcceleration().unmodifyMult(id);
        stats.getAcceleration().unmodifyMult(id);
        stats.getMaxSpeed().unmodifyMult(id);

    }
}
