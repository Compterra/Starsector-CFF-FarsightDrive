package data.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.combat.FSD_CSchargeGlow;
import org.boxutil.base.SimpleParticleControlData;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.attribute.Instance2Data;
import org.boxutil.units.standard.entity.DistortionEntity;
import org.boxutil.units.standard.entity.SpriteEntity;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import data.everyframe.FSD_CombatRender;

import java.awt.*;
import java.util.List;

public class FSD_CombatDataPlugin extends BaseEveryFrameCombatPlugin {
    private FSD_CSchargeGlow FSD_ChargeGlowPlugin;
    protected CombatEntityAPI FSD_ChargeGlowEntity;
    private float time5 = 0.0f;
    private float OriginalSIZE = 5f;
    private float OriginalUNDERSIZE = 40f;
    private SimpleParticleControlData particle = null;
    private Color OriginalUNDERCOLOR = new Color(94, 17, 17, 255);
    private Color OriginalRIFT_COLOR = new Color(241, 23, 23, 255);
    private IntervalUtil timer = new IntervalUtil(5f, 5f);
    private final String Proliferation_ID = "FSD_Proliferation_warhead";
    private final String RepairMissile_ID = "FSD_RepairShell";



       @Override
       public void advance(float amount, List<InputEventAPI> entities) {
           CombatEngineAPI engine = Global.getCombatEngine();
           if(engine == null || engine.isPaused()){
               return;
           }

           for(DamagingProjectileAPI proj : engine.getProjectiles()){
               FSD_CorruptSeaEffect(proj);

               FSD_FlowEffect(proj,amount);

               FSD_TuskEffect(proj,amount);

               FSD_RepairMissile(proj);


//               MissileAPI missile = (MissileAPI) proj;
//               FSD_ThrowShell(proj);
//               }
               //FSD_ThrowShell(proj);
           }
           for(ShipAPI ship : engine.getShips()){
               FSD_Formulate(ship);
           }

       }
       public void FSD_RepairMissile(DamagingProjectileAPI proj){
           if (!RepairMissile_ID.equals(proj.getProjectileSpecId())) return;
           CombatEngineAPI engine = Global.getCombatEngine();
           if (engine == null || !(proj instanceof MissileAPI)) return;

           MissileAPI missile = (MissileAPI) proj;
           if (!(missile.getAI() instanceof GuidedMissileAI)) return;

           GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
           CombatEntityAPI target = ai.getTarget();
           if (!(target instanceof ShipAPI)) return;

           float dist = target.getCollisionRadius() * 0.75f;
           if (MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation()) < dist * dist) {
               Vector2f loc = missile.getLocation();
               DistortionEntity newDistortion = new DistortionEntity();
               newDistortion.setGlobalTimer(0.25f, 0.25f, 0.1f);
               newDistortion.setInnerFull(0.7f, 0.7f);
               newDistortion.setInnerHardness(0.8f);
               newDistortion.setSizeIn(64, 64);
               newDistortion.setPowerIn(0);
               newDistortion.setPowerFull(1);
               newDistortion.setPowerOut(0);
               newDistortion.setSizeFull(32, 32);
               newDistortion.setSizeOut(16, 16);
               newDistortion.setLocation(loc);
               CombatRenderingManager.addEntity(BoxEnum.ENTITY_DISTORTION, newDistortion);
               RepairArmor(missile, (ShipAPI) target, missile.getLocation(), 50f);
               target.setHitpoints(Math.min(target.getHitpoints() + 300, target.getMaxHitpoints()));
               engine.addNebulaSmokeParticle(missile.getLocation(), new Vector2f(), 30f, 1.1f, 0.15f, 0.2f, 0.3f, new Color(3, 228, 32, 166));
               engine.addFloatingDamageText(loc, 300, new Color(135, 241, 150), missile, null);
               engine.removeEntity(missile);
           }
       }
    public static void RepairArmor(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float armorValue = grid.getMaxArmorInCell();

        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = armorDamage * damMult * damageTypeMult;
                damage = Math.min(armorValue, armorInCell+damage);
                if (damage >= armorValue) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
//                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }

       public void FSD_TuskEffect(DamagingProjectileAPI proj,float amount){
           if (!"FSD_Tusk_warhead".equals(proj.getProjectileSpecId())) return;
           if (!(proj instanceof MissileAPI)) return;

           WeaponAPI weapon = proj.getWeapon();
           if (weapon == null || weapon.getType() != WeaponAPI.WeaponType.MISSILE) return;

           MissileAPI missile = (MissileAPI) proj;
           String missileId = "FSD_TUSK_" + missile.hashCode();

           if (!missile.getCustomData().containsKey("FSD_BOOST_START_TIME")) {
               missile.setCustomData("FSD_BOOST_START_TIME", missile.getFlightTime());
           }

           float boostStartTime = (Float) missile.getCustomData().get("FSD_BOOST_START_TIME");
           float elapsedBoostTime = missile.getFlightTime() - boostStartTime;

           if (elapsedBoostTime < 0.25f) {
               float progress = elapsedBoostTime / 0.25f;
               missile.getEngineStats().getMaxSpeed().modifyMult(missileId, 2f);
               missile.getEngineStats().getProjectileSpeedMult().modifyMult(missileId, Math.min(progress * 3f, 2.5f));
           } else {
               missile.getEngineStats().getMaxSpeed().unmodify(missileId);
               missile.getEngineStats().getProjectileSpeedMult().unmodify(missileId);
           }
           if(proj.isFading() || proj.isExpired() || missile.isFizzling()){
               Global.getCombatEngine().applyDamage(proj, proj.getLocation(), 10000, DamageType.ENERGY, 0, false, false, null);
           }
       }

       public void FSD_CorruptSeaEffect(DamagingProjectileAPI proj){
//                   "wdnmd",
//                   20f,
//                   0.005f,
//                   0.001f
           WeaponAPI weapon = proj.getWeapon();
           if(weapon == null || !"FSD_CorruptSea".equals(weapon.getId())){
               return;
           }
           if(proj.getCustomData().containsKey("FSD_Particle_Glow")){
               return;
           }

           proj.setCustomData("FSD_Particle_Glow", true);
           if("FSD_CorruptSea_empty".equals(proj.getProjectileSpecId())) {
               return;
           }

           FSD_CSchargeGlow.SIZE = OriginalSIZE;
           FSD_CSchargeGlow.UNDERSIZE = OriginalUNDERSIZE;
           FSD_CSchargeGlow.UNDERCOLOR = OriginalUNDERCOLOR;
           FSD_CSchargeGlow.RIFT_COLOR = OriginalRIFT_COLOR;
           FSD_CSchargeGlow.DETECT = true;

           if ("FSD_CorruptSea_warhead".equals(proj.getProjectileSpecId())) {
               FSD_CSchargeGlow.SIZE = 15f;
               FSD_CSchargeGlow.UNDERSIZE = 30f;
               FSD_CSchargeGlow.UNDERCOLOR = new Color(204, 13, 13, 255);
               FSD_CSchargeGlow.RIFT_COLOR = new Color(255, 42, 42, 158);
               FSD_CSchargeGlow.DETECT = false;
           }

           FSD_ChargeGlowPlugin = new FSD_CSchargeGlow(weapon);
           FSD_ChargeGlowEntity = Global.getCombatEngine().addLayeredRenderingPlugin(FSD_ChargeGlowPlugin);
           FSD_ChargeGlowPlugin.attachToProjectile(proj);
       }

    public void FSD_FlowEffect(DamagingProjectileAPI proj,float amount){

        WeaponAPI weapon = proj.getWeapon();
//        if (BoxConfigs.isShaderEnable() && BoxConfigs.isGLParallelSupported()) {
//            if (this.particle == null) {
//                this.particle = new SimpleParticleControlData(2048, 2.0f, -5120.0f, false);
//
//                SpriteEntity particleEntity = new SpriteEntity("graphics/fx/nebula_colorless.png");
//                particleEntity.getMaterialData().setEmissive(Global.getSettings().getSprite("graphics/fx/fx_clouds01.png"));
//                particleEntity.getMaterialData().setColor(new Color(0x0d0101 , true));
//                particleEntity.getMaterialData().setEmissiveColor(new Color(0x510505 , true));
//                particleEntity.getMaterialData().setColorAlpha(1.0f);
//                particleEntity.getMaterialData().setEmissiveColorAlpha(0.8f);
//                particleEntity.getMaterialData().setColorToEmissive(0.2f);
//                particleEntity.getMaterialData().setAlphaToEmissive(0.0f);
//                particleEntity.getMaterialData().setGlowPower(0.25f);
//                particleEntity.setTileSize(4, 4);
//                particleEntity.setBaseSizePerTiles(16.0f, 16.0f);
//                particleEntity.setRandomTile(true);
//                particleEntity.setRandomTileEachInstance(true);
//                particleEntity.setAdditiveBlend();
//                particleEntity.setLayer(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
//                particleEntity.setControlData(this.particle);
//                CombatRenderingManager.addEntity(BoxEnum.ENTITY_SPRITE, particleEntity);
//            } else {
//                if (this.time5 > 0.01f) {
//                    this.time5 -= 0.01f;
//                    for (byte i = 0; i < 5; i++) {
//                        Instance2Data addedParticle = this.particle.addParticle();
//                        if (addedParticle != null) {
//                            addedParticle.setLocation(proj.getLocation());
//                            addedParticle.setScaleAll(0.5f + (float) Math.random() * 1.5f);
//                            addedParticle.setScaleRateAll(2.0f);
//                            addedParticle.setLowColor(Misc.getNegativeHighlightColor());
//                            addedParticle.setAlpha((float) Math.random() * 0.4f + 0.4f);
//                            addedParticle.setVelocity((float) Math.random() * 64.0f - 32.0f, (float) Math.random() * 64.0f - 32.0f);
//                            addedParticle.setTimer(0.1f, 0.8f, 1.1f);
//                        }
//                    }
//                } this.time5 += amount;
//            }
//        }
        if(!"FSD_Flow_Shell".equals(proj.getProjectileSpecId())) {
            return;
        }
        if(weapon == null || proj.getCustomData().containsKey("FSD_Particle_Glow")) {
            return;
        }
        proj.setCustomData("FSD_Particle_Glow", true);
        FSD_ChargeGlowPlugin = new FSD_CSchargeGlow(weapon);
        FSD_ChargeGlowEntity = Global.getCombatEngine().addLayeredRenderingPlugin(FSD_ChargeGlowPlugin);
        FSD_ChargeGlowPlugin.attachToProjectile(proj);
    }

    public void FSD_Formulate(ShipAPI ship){
        if(ship.hasListenerOfClass(FSD_CombatRender.class)&&ship.getOwner() == ship.getOriginalOwner()) {
            ship.removeListenerOfClass(FSD_CombatRender.class);
        }
    }
}

