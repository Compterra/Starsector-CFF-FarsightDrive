package data.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class FSD_Longinus_shot_everyframe extends BaseCombatLayeredRenderingPlugin {
    private List<AttachedSpearData> attachedSpears = new ArrayList<AttachedSpearData>();
    private static final String SPEAR_SPRITE_PATH = "graphics/missiles/FSD_Longinus_shot.png";

    private static class AttachedSpearData {
        ShipAPI ship;
        Vector2f relativePosition;
        float relativeFacing;
        DamagingProjectileAPI spear;
        float spawnTime;
        ShipAPI sourceShip;
        SpriteAPI sprite; 
        AttachedSpearData(ShipAPI ship, Vector2f worldPosition, float worldFacing, DamagingProjectileAPI spear, ShipAPI sourceShip, float spawnTime) {
            this.ship = ship;
            this.spear = spear;
            this.sourceShip = sourceShip;
            this.spawnTime = spawnTime;
            Vector2f offset = Vector2f.sub(worldPosition, ship.getLocation(), new Vector2f());
            this.relativePosition = rotateVector(offset, -ship.getFacing());
            this.relativeFacing = worldFacing - ship.getFacing();
            this.sprite = Global.getSettings().getSprite(SPEAR_SPRITE_PATH);
        }

        Vector2f getWorldPosition() {
            Vector2f rotated = rotateVector(relativePosition, ship.getFacing());
            return Vector2f.add(ship.getLocation(), rotated, new Vector2f());
        }

        float getWorldFacing() {
            return ship.getFacing() + relativeFacing;
        }

        private static Vector2f rotateVector(Vector2f vector, float angle) {
            float rad = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float x = vector.x * cos - vector.y * sin;
            float y = vector.x * sin + vector.y * cos;
            return new Vector2f(x, y);
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            engine.getCustomData().put("FSD_Longinus_plugin_instance", this);
        }
    }
    
    public void addAttachedSpear(ShipAPI ship, Vector2f worldPosition, float worldFacing, DamagingProjectileAPI spear, ShipAPI sourceShip) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float currentTime = engine.getTotalElapsedTime(false);
        AttachedSpearData data = new AttachedSpearData(ship, worldPosition, worldFacing, spear, sourceShip, currentTime);
        attachedSpears.add(data);
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        float currentTime = engine.getTotalElapsedTime(false);
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            
            if (ship.getCustomData().containsKey("FSD_Longinus_slow_start_time")) {
                float startTime = (float) ship.getCustomData().get("FSD_Longinus_slow_start_time");
                if (currentTime - startTime >= 10f) {
                    ship.getMutableStats().getMaxSpeed().unmodifyMult("FSD_Longinus_slow");
                    ship.getMutableStats().getAcceleration().unmodifyMult("FSD_Longinus_slow");
                    ship.getMutableStats().getDeceleration().unmodifyMult("FSD_Longinus_slow");
                    ship.getMutableStats().getTurnAcceleration().unmodifyMult("FSD_Longinus_slow");
                    ship.getMutableStats().getMaxTurnRate().unmodifyMult("FSD_Longinus_slow");
                    ship.removeCustomData("FSD_Longinus_slow_start_time");
                }
            }
            
            if (ship.getCustomData().containsKey("FSD_Longinus_target") && 
                ship.getCustomData().containsKey("FSD_Longinus_target_time")) {
                
                ShipAPI target = (ShipAPI) ship.getCustomData().get("FSD_Longinus_target");
                float targetTime = (float) ship.getCustomData().get("FSD_Longinus_target_time");
                
                if (currentTime - targetTime < 10f && target != null && target.isAlive()) {
                    Vector2f toTarget = Vector2f.sub(target.getLocation(), ship.getLocation(), new Vector2f());
                    float angleToTarget = Misc.getAngleInDegrees(toTarget);
                    float shipFacing = ship.getFacing();
                    float angleDiff = Math.abs(Misc.getAngleDiff(shipFacing, angleToTarget));
                    
                    if (angleDiff < 60f) {
                        ship.getMutableStats().getMaxSpeed().modifyMult("FSD_Longinus_speed_boost", 1.75f);
                        ship.getMutableStats().getAcceleration().modifyMult("FSD_Longinus_speed_boost", 2.0f);
                    } else {
                        ship.getMutableStats().getMaxSpeed().unmodifyMult("FSD_Longinus_speed_boost");
                        ship.getMutableStats().getAcceleration().unmodifyMult("FSD_Longinus_speed_boost");
                    }
                } else {
                    ship.getMutableStats().getMaxSpeed().unmodifyMult("FSD_Longinus_speed_boost");
                    ship.getMutableStats().getAcceleration().unmodifyMult("FSD_Longinus_speed_boost");
                    ship.removeCustomData("FSD_Longinus_target");
                    ship.removeCustomData("FSD_Longinus_target_time");
                }
            }
        }
        
        Iterator<AttachedSpearData> iterator = attachedSpears.iterator();
        while (iterator.hasNext()) {
            AttachedSpearData data = iterator.next();
            
            if (currentTime - data.spawnTime >= 10f) {
                if (data.spear != null && engine.isEntityInPlay(data.spear)) {
                    engine.removeEntity(data.spear);
                }
                iterator.remove();
                continue;
            }
            
            if (!engine.isEntityInPlay(data.ship) || data.ship.isHulk()) {
                if (data.spear != null && engine.isEntityInPlay(data.spear)) {
                    engine.removeEntity(data.spear);
                }
                iterator.remove();
                continue;
            }
            
            if (data.spear == null || !engine.isEntityInPlay(data.spear)) {
                iterator.remove();
                continue;
            }
            
            Vector2f worldPos = data.getWorldPosition();
            float worldFacing = data.getWorldFacing();
            data.spear.getLocation().set(worldPos);
            data.spear.setFacing(worldFacing);

            if (data.sourceShip != null && data.sourceShip.isAlive() && 
                data.sourceShip.getSystem() != null && data.sourceShip.getSystem().isActive()) {
                String empKey = "FSD_Longinus_EMP_triggered_" + data.ship.getId() + "_" + (int)(data.spawnTime * 1000);
                if (!data.ship.getCustomData().containsKey(empKey)) {
                    
                    engine.spawnEmpArcVisual(
                        worldPos,
                        data.ship,
                        worldPos,
                        data.ship,
                        20f,
                        new Color(100, 165, 255, 255),
                        Color.WHITE
                    );
                    
                    for (ShipAPI nearbyShip : engine.getShips()) {
                        if (nearbyShip.getOwner() == data.sourceShip.getOwner()) continue;
                        if (!nearbyShip.isAlive() || nearbyShip.isHulk()) continue;
                        
                        float distance = Misc.getDistance(worldPos, nearbyShip.getLocation());
                        if (distance <= 400f) {
                            engine.applyDamage(
                                nearbyShip,
                                nearbyShip.getLocation(),
                                0f,
                                DamageType.ENERGY,
                                3000f,
                                true,
                                false,
                                data.sourceShip
                            );
                            
                            engine.spawnEmpArcVisual(
                                worldPos,
                                nearbyShip,
                                nearbyShip.getLocation(),
                                nearbyShip,
                                10f,
                                new Color(100, 165, 255, 255),
                                Color.WHITE
                            );
                        }
                    }
                    
                    for (int i = 0; i < 8; i++) {
                        Vector2f randomPoint = Misc.getPointAtRadius(worldPos, (float)(Math.random() * 400f));
                        engine.spawnEmpArcVisual(
                            worldPos,
                            null,
                            randomPoint,
                            null,
                            8f,
                            new Color(100, 165, 255, 200),
                            Color.WHITE
                        );
                    }
                    
                    Global.getSoundPlayer().playSound("system_emp_emitter_activate", 1.5f, 1.2f, worldPos, new Vector2f());
                    
                    data.ship.setCustomData(empKey, true);
                }
            }
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.BELOW_SHIPS_LAYER) {
            return;
        }
        
        if (attachedSpears.isEmpty()) {
            return;
        }
        
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        
        for (AttachedSpearData data : attachedSpears) {
            if (data.ship == null || !engine.isEntityInPlay(data.ship)) {
                continue;
            }
            
            if (data.sprite == null) {
                continue;
            }
            
            Vector2f worldPos = data.getWorldPosition();
            float worldFacing = data.getWorldFacing();
            
            float timeLeft = 10f - (engine.getTotalElapsedTime(false) - data.spawnTime);
            float alpha = 1f;
            if (timeLeft < 1f) {
                alpha = timeLeft;
            }
            
            data.sprite.setAngle(worldFacing - 90f);
            data.sprite.setAlphaMult(alpha);
            data.sprite.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
            
            data.sprite.renderAtCenter(worldPos.x, worldPos.y);
        }
    }
    
    @Override
    public float getRenderRadius() {
        return 999999f;
    }
    
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }
}
