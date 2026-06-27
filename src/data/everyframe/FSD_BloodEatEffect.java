package data.everyframe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;


public class FSD_BloodEatEffect extends BaseCombatLayeredRenderingPlugin {
    private int maxTicks = 0;
    private float damageRemaining = 0.0F;
    protected float sizeMult;
    private boolean degradeArmor;
    private boolean damagehull;
    private Color cloudColor;
    protected List<FSD_BEParticleData> particles = new ArrayList<FSD_BEParticleData>();
    protected DamagingProjectileAPI proj;
    protected ShipAPI target;
    protected Vector2f offset;
    protected int ticks = 0;
    protected IntervalUtil interval;
    protected FaderUtil fader = new FaderUtil(1.0F, 0.5F, 0.5F);
    protected EnumSet<CombatEngineLayers> layers;

    public FSD_BloodEatEffect() {
        this.layers = EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);
    }

    public FSD_BloodEatEffect(DamagingProjectileAPI proj, ShipAPI target, Vector2f offset, float sizeMult, int alpha, float damage, float duration, boolean degradeArmor, boolean damageHull) {
        this.layers = EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);
        this.proj = proj;
        this.target = target;
        this.offset = offset;
        this.maxTicks = (int)(duration * 2.0F);
        this.damageRemaining = damage;
        this.sizeMult = sizeMult;
        this.degradeArmor = degradeArmor;
        this.damagehull = damageHull;
        this.cloudColor = new Color(35,176,176, alpha);
        this.interval = new IntervalUtil(0.4F, 0.6F);
        this.interval.forceIntervalElapsed();
    }

    public float getRenderRadius() {
        return 500.0F;
    }

    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return this.layers;
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    public void advance(float amount) {
        if (!Global.getCombatEngine().isPaused()) {
            if (this.degradeArmor) {
                this.target.getMutableStats().getMinArmorFraction().modifyMult("FarsightDrive_BE_Armor", 0.5F);
            }

            Vector2f loc = new Vector2f(this.offset);
            loc = Misc.rotateAroundOrigin(loc, this.target.getFacing());
            Vector2f.add(this.target.getLocation(), loc, loc);
            this.entity.getLocation().set(loc);
            List<FSD_BEParticleData> remove = new ArrayList<FSD_BEParticleData>();

            for(FSD_BEParticleData p : this.particles) {
                p.advance(amount);
                if (p.elapsed >= p.maxDur) {
                    remove.add(p);
                }
            }

            this.particles.removeAll(remove);
            float volume = 1.0F;
            if (this.ticks >= this.maxTicks || !this.target.isAlive() || !Global.getCombatEngine().isEntityInPlay(this.target)) {
                this.fader.fadeOut();
                this.fader.advance(amount);
                volume = this.fader.getBrightness();
                if (this.degradeArmor) {
                    this.target.getMutableStats().getMinArmorFraction().unmodify("FarsightDrive_BE_Armor");
                }
            }

            Global.getSoundPlayer().playLoop("disintegrator_loop", this.target, 1.0F, volume, loc, this.target.getVelocity());
            this.interval.advance(amount);
            if (this.interval.intervalElapsed() && this.ticks < this.maxTicks) {
                this.dealDamage();
                ++this.ticks;
            }

        }
    }

    protected void dealDamage() {
        CombatEngineAPI engine = Global.getCombatEngine();
            int num = 3;

            for(int i = 0; i < num; ++i) {
                FSD_BEParticleData p = new FSD_BEParticleData(30.0F * this.sizeMult, 3.0F + (float)Math.random() * 2.0F, 2.0F);
                this.particles.add(p);
                p.offset = Misc.getPointWithinRadius(p.offset, 20.0F);
            }
//        }

        Vector2f point = new Vector2f(this.entity.getLocation());
        ArmorGridAPI grid = this.target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell != null) {
            int gridWidth = grid.getGrid().length;
            int gridHeight = grid.getGrid()[0].length;
            float damageTypeMult = DisintegratorEffect.getDamageTypeMult(this.proj.getSource(), this.target) * this.target.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
            float damagePerTick = this.damageRemaining / (float)(this.maxTicks - this.ticks);
            float damageDealt = 0.0F;
            float excessDamage = 0.0F;

            for(int i = -2; i <= 2; ++i) {
                for(int j = -2; j <= 2; ++j) {
                    if (i != 2 && i != -2 || j != 2 && j != -2) {
                        int cx = cell[0] + i;
                        int cy = cell[1] + j;
                        if (cx >= 0 && cx < gridWidth && cy >= 0 && cy < gridHeight) {
                            float damMult = 0.033333335F;
                            if (i == 0 && j == 0) {
                                damMult = 0.06666667F;
                            } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) {
                                damMult = 0.06666667F;
                            } else {
                                damMult = 0.033333335F;
                            }

                            float armorInCell = grid.getArmorValue(cx, cy);
                            float damage = damagePerTick * damMult * damageTypeMult;
                            if (damage > armorInCell) {
                                excessDamage += damage - armorInCell;
                            }

                            damage = Math.min(damage, armorInCell);
                            if (!(damage <= 0.0F)) {
                                this.target.getArmorGrid().setArmorValue(cx, cy, Math.max(0.0F, armorInCell - damage));
                                damageDealt += damage;
                            }
                        }
                    }
                }
            }

            if (this.damagehull) {
                this.target.setHitpoints(Math.max(this.target.getHitpoints() - excessDamage, 1.0F));
                if (this.target.getHitpoints() == 1.0F) {
                    engine.applyDamage(this.target, point, 50.0F, DamageType.FRAGMENTATION, 0.0F, true, false, this.proj.getSource(), false);
                }
            }

            if (damageDealt > 0.0F || excessDamage > 0.0F) {
                if (Misc.shouldShowDamageFloaty(this.proj.getSource(), this.target)) {
                    if (damageDealt > 0.0F) {
                        engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, this.target, this.proj.getSource());
                    }

                    if (excessDamage > 0.0F) {
                        engine.addFloatingDamageText(point, excessDamage, Misc.FLOATY_HULL_DAMAGE_COLOR, this.target, this.proj.getSource());
                    }
                }

                this.damageRemaining -= damageDealt;
                this.damageRemaining -= excessDamage;
                this.target.syncWithArmorGridState();
            }

        }
    }

    public boolean isExpired() {
        return this.particles.isEmpty() && (this.ticks >= this.maxTicks || !this.target.isAlive() || !Global.getCombatEngine().isEntityInPlay(this.target));
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = this.entity.getLocation().x;
        float y = this.entity.getLocation().y;
        float b = viewport.getAlphaMult();
        GL14.glBlendEquation(32779);

        for(FSD_BEParticleData p : this.particles) {
            float size = p.baseSize * p.scale;
            Vector2f loc = new Vector2f(x + p.offset.x, y + p.offset.y);
            float alphaMult = 1.0F;
            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
            p.sprite.setColor(this.cloudColor);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }

        GL14.glBlendEquation(32774);
    }
}
