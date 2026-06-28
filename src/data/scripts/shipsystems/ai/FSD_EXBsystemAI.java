package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;

public class FSD_EXBsystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;
    private ShipSystemAPI system;
    private ArrayList<DamagingProjectileAPI> proj;
    private float totalDamage;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if(target == null)return;
        for(DamagingProjectileAPI projectile : engine.getProjectiles()){
            if(projectile.getDamageAmount()>=200 && Misc.getDistance(projectile.getLocation(),ship.getLocation())<=1000 && projectile.getOwner()!=ship.getOwner()){
                totalDamage+=projectile.getDamageAmount();
            }
        }
        if(totalDamage>=ship.getHitpoints()*2&& AIUtils.canUseSystemThisFrame(ship)){
            ship.useSystem();
            totalDamage=0;
        }
        if(!AIUtils.canUseSystemThisFrame(ship)){
            totalDamage=0;
        }
    }
}
