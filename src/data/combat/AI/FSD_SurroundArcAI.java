package data.combat.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicTargeting;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.Color;
import java.util.EnumSet;

public class FSD_SurroundArcAI implements MissileAIPlugin, GuidedMissileAI {
    private final float MAX_SPEED;
    private float PRECISION_RANGE_SQ;

    private static String WEAPON_ID = "FSD_PhantomWeapon2";

    private final Color CORE_COLOR = new Color(0, 35, 255);
    private final Color FRINGE_COLOR = new Color(188, 126, 225, 128);

    private ShipAPI demDrone;
    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    private boolean launch = true, arcing = false;
    private float eccm = 2,
            timer = 0,
            check = 0f,
            arcTimer = 0,
            arcRandomness = 0.5f,
            circlingRadius = 1000f,
            empCharge = 10,
            direction = 1;

    public FSD_SurroundArcAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed();

        if (missile.getSource().getVariant().getHullMods().contains("eccm")) {
            eccm = 1;
        }

        float precisionRange = 750;
        PRECISION_RANGE_SQ = (float) Math.pow(2 * precisionRange, 2);

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
            return;
        }

        if (target == null || targetInvalid()) {
            selectNewTarget();
            missile.giveCommand(ShipCommand.ACCELERATE);
            if (Math.random() > 0.5) direction = -direction;
            return;
        }

        if (empCharge <= 0) {
//            detonateMissile();
            return;
        }

        timer += amount;
        if (launch || timer >= check) {
            launch = false;
            timer -= check;
            updateTargeting();
        }

        controlMissile();

        if (arcing) {
            empArcing(amount);
        }
    }

    private boolean targetInvalid() {
        return (target instanceof ShipAPI && ((ShipAPI) target).isHulk()) ||
                !engine.isEntityInPlay(target) ||
                target.getCollisionClass() == CollisionClass.NONE;
    }

    private void selectNewTarget() {
        setTarget(MagicTargeting.pickMissileTarget(
                missile,
                MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(),
                360, 0, 1, 1, 1, 1));
    }

    private void updateTargeting() {
        float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());
        check = Math.min(0.5f, Math.max(0.1f, 2 * dist / PRECISION_RANGE_SQ));

        if (!MathUtils.isWithinRange(missile, target, 500)) {
            lead = AIUtils.getBestInterceptPoint(
                    missile.getLocation(),
                    MAX_SPEED * eccm,
                    target.getLocation(),
                    target.getVelocity());
        } else {
            float angleToMissile = VectorUtils.getFacing(
                    VectorUtils.getDirectionalVector(target.getLocation(), missile.getLocation()));

            circlingRadius = Math.max(
                    target.getCollisionRadius() + 100,
                    target.getCollisionRadius() / 3 + 300);

            float targetAngle = angleToMissile + 25 * direction;
            float randomOffset = (float) Math.random() * 50;
            lead = MathUtils.getPoint(target.getLocation(), circlingRadius - randomOffset, targetAngle);
        }

        if (lead == null) lead = target.getLocation();

        arcing = MathUtils.isWithinRange(missile, target, 250) && empCharge > 0;

        if (!arcing && engine != null) {
            engine.addHitParticle(
                    missile.getLocation(),
                    missile.getVelocity(),
                    50 + 25 * (float) Math.random(),
                    0.5f,
                    0.1f,
                    FRINGE_COLOR);
        }
    }

    private void controlMissile() {
        float aimAngle = MathUtils.getShortestRotation(
                missile.getFacing(),
                VectorUtils.getAngle(missile.getLocation(), lead));

        if (Math.abs(aimAngle) < 60) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }

        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * 0.1f) {
            missile.setAngularVelocity(aimAngle / 0.1f);
        }
    }

    private void detonateMissile() {
        if (engine == null) return;

        engine.addHitParticle(
                missile.getLocation(),
                new Vector2f(),
                150,
                1,
                0.5f + 0.5f * (float) Math.random(),
                CORE_COLOR);

        for (int i = 0; i <= 10; i++) {
            engine.addHitParticle(
                    missile.getLocation(),
                    MathUtils.getRandomPointInCircle(new Vector2f(), 300),
                    5 + 5 * (float) Math.random(),
                    1,
                    0.5f + (float) Math.random(),
                    FRINGE_COLOR);
        }

        engine.applyDamage(
                missile,
                missile.getLocation(),
                missile.getHitpoints() * 2,
                DamageType.FRAGMENTATION,
                0,
                true,
                false,
                missile.getSource());
    }

    void empArcing(float time) {
        if (!(target instanceof ShipAPI)) return;

        arcTimer += time;
        if (arcTimer < arcRandomness) return;

        arcTimer -= arcRandomness;
        arcRandomness = 0.3f - 0.2f + ((float) Math.random() * 0.4f);
        empCharge--;
        engine.addHitParticle(
                missile.getLocation(),
                missile.getVelocity(),
                50 + 25 * (float) Math.random(),
                0.5f,
                0.1f,
                FRINGE_COLOR);
        if (MagicRender.screenCheck(0.25f, missile.getLocation())) {
            MagicLensFlare.createSharpFlare(
                    engine,
                    missile.getSource(),
                    missile.getLocation(),
                    3,
                    200,
                    0,
                    FRINGE_COLOR,
                    CORE_COLOR);
        }
        NegativeExplosionVisual.NEParams neEffect = new NegativeExplosionVisual.NEParams();
        neEffect.fadeOut = 0.1f;
        neEffect.radius = 2f;
        neEffect.thickness = 7.5f;
        neEffect.color = new Color(27, 46, 97, 255);
        neEffect.underglow = new Color(186, 45, 255, 255);
        CombatEntityAPI visual = engine.addLayeredRenderingPlugin(new NegativeExplosionVisual(neEffect));
        visual.getLocation().set(missile.getLocation());
        engine.addPlugin(
                new BeamDronePlugin(
                        missile.getSource(), (ShipAPI) target, 0f, 0f, missile.getLocation(), Misc.getAngleInDegrees(missile.getLocation(),target.getLocation())));
//        createDrone(engine, missile.getSource(), missile.getLocation());
//        if (pierceShields) {
//            engine.spawnEmpArcPierceShields(
//                    missile.getSource(),
//                    missile.getLocation(),
//                    null,
//                    target,
//                    DamageType.FRAGMENTATION,
//                    0,
//                    100,
//                    1000,
//                    "tachyon_lance_emp_impact",
//                    5 + 3 * (float) Math.random(),
//                    FRINGE_COLOR,
//                    CORE_COLOR);
//        } else {
//            engine.spawnEmpArc(
//                    missile.getSource(),
//                    missile.getLocation(),
//                    null,
//                    target,
//                    DamageType.FRAGMENTATION,
//                    0,
//                    200,
//                    1000,
//                    "tachyon_lance_emp_impact",
//                    5 + 3 * (float) Math.random(),
//                    FRINGE_COLOR,
//                    CORE_COLOR);
//        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public static class BeamDronePlugin extends BaseEveryFrameCombatPlugin
            implements CombatLayeredRenderingPlugin {

        private static final String MISSILE_SPRITE = "graphics/fx/empty.png";
        private static final float SPRITE_SCALE = 1f;
        private static final float FADE_IN_TIME = 0.15f;
        private static final float FIRING_TIME = 0.05f;
        private static final float FADE_OUT_TIME = 0.15f;

        private static final float SLIDE_IN_DISTANCE = 30f;
        private static final float SLIDE_OUT_DISTANCE = 100f;

        private static final float WEAPON_OFFSET_X = 0f;
        private static final float WEAPON_OFFSET_Y = -10f;

        private ShipAPI source;
        private ShipAPI target;
        private float relativeDistance;
        private float relativeAngle;
        private ShipAPI demDrone;
        private Vector2f currentPos = new Vector2f();
        private float currentFacing = 0f;
        private SpriteAPI missileSprite;
        private float spriteAlpha = 0f;
        private Vector2f spawnLocation;

        private enum State {
            FADE_IN,
            FIRING,
            FADE_OUT,
            DONE
        }

        private State state = State.FADE_IN;
        private float stateTime = 0f;

        public BeamDronePlugin(
                ShipAPI source,
                ShipAPI target,
                float relativeDistance,
                float relativeAngle,
                Vector2f initialPos,
                float initialFacing) {
            this.source = source;
            this.target = target;
            this.relativeDistance = relativeDistance;
            this.relativeAngle = relativeAngle;
            this.spawnLocation = initialPos;
            this.currentPos.set(initialPos);
            this.currentFacing = initialFacing;
            try {
                this.missileSprite = Global.getSettings().getSprite(MISSILE_SPRITE);
                if (this.missileSprite != null) {
                    float w = this.missileSprite.getWidth() * SPRITE_SCALE;
                    float h = this.missileSprite.getHeight() * SPRITE_SCALE;
                    this.missileSprite.setSize(w, h);
                }
            } catch (Exception e) {
//                Global.getLogger(FloatingCannonHullmod.class)
//                        .error("Failed to load sprite: " + MISSILE_SPRITE, e);
            }
        }

        @Override
        public void advance(
                float amount, java.util.List<com.fs.starfarer.api.input.InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused()) {
                return;
            }
            if (target == null || !target.isAlive()) {
                cleanup(engine);
                return;
            }
            stateTime += amount;
            updatePosition();
            switch (state) {
                case FADE_IN:
                    spriteAlpha = Math.min(1f, stateTime / FADE_IN_TIME);
                    if (stateTime >= FADE_IN_TIME) {
                        createDrone(engine);
                        state = State.FIRING;
                        stateTime = 0f;
                    }
                    break;
                case FIRING:
                    if (stateTime >= FIRING_TIME) {
                        state = State.FADE_OUT;
                        stateTime = 0f;
                    }
                    break;
                case FADE_OUT:
                    spriteAlpha = Math.max(0f, 1f - (stateTime / FADE_OUT_TIME));
                    if (stateTime >= FADE_OUT_TIME) {
                        state = State.DONE;
                        cleanup(engine);
                        return;
                    }
                    break;
                case DONE:
                    cleanup(engine);
                    return;
            }
            updateDroneState();
            controlWeapons();
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {
            if (missileSprite != null && spriteAlpha > 0f) {
                missileSprite.setColor(Color.WHITE);
                missileSprite.setAngle(currentFacing - 90f);
                missileSprite.setAlphaMult(spriteAlpha);
                missileSprite.renderAtCenter(currentPos.x, currentPos.y);
            }
        }

        private void createDrone(CombatEngineAPI engine) {
            try {
                ShipVariantAPI variant =
                        Global.getSettings()
                                .createEmptyVariant(
                                        "floating_cannon_drone", Global.getSettings().getHullSpec("dem_drone"));
                variant.addWeapon("WS 000", WEAPON_ID);
                WeaponGroupSpec g1 = new WeaponGroupSpec(WeaponGroupType.LINKED);
                g1.addSlot("WS 000");
                variant.addWeaponGroup(g1);
                demDrone = engine.createFXDrone(variant);
                demDrone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
                demDrone.setOwner(source.getOwner());
                demDrone.setCollisionClass(CollisionClass.NONE);
                demDrone.getMutableStats().getHullDamageTakenMult().modifyMult("floating_cannon", 0f);
                demDrone.setDrone(true);
                demDrone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP, 100000f, source);
                demDrone
                        .getMutableStats()
                        .getEnergyWeaponDamageMult()
                        .applyMods(source.getMutableStats().getMissileWeaponDamageMult());
                demDrone.getMutableStats().getBeamWeaponRangeBonus().modifyFlat("floating_cannon", 900f);
                engine.addEntity(demDrone);
                try {
                    WeaponAPI weapon =
                            (WeaponAPI) demDrone.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
                    Color transparent = new Color(0, 0, 0, 0);
                    if (weapon.getSprite() != null) {
                        weapon.getSprite().setAlphaMult(0f);
                        weapon.getSprite().setColor(transparent);
                    }
                    if (weapon.getBarrelSpriteAPI() != null) {
                        weapon.getBarrelSpriteAPI().setAlphaMult(0f);
                        weapon.getBarrelSpriteAPI().setColor(transparent);
                    }
                    if (weapon.getUnderSpriteAPI() != null) {
                        weapon.getUnderSpriteAPI().setAlphaMult(0f);
                        weapon.getUnderSpriteAPI().setColor(transparent);
                    }
                    if (weapon.getGlowSpriteAPI() != null) {
                        weapon.getGlowSpriteAPI().setAlphaMult(0f);
                        weapon.getGlowSpriteAPI().setColor(transparent);
                    }
                } catch (Exception ex) {
                }
            } catch (Exception e) {
            }
        }

        private void updatePosition() {
            if (target == null) return;
//            Vector2f targetPos = target.getLocation();
            Vector2f targetPos = spawnLocation;
            float angleRad = (float) Math.toRadians(relativeAngle);
            Vector2f basePos =
                    new Vector2f(
                            targetPos.x + relativeDistance * (float) Math.cos(angleRad),
                            targetPos.y + relativeDistance * (float) Math.sin(angleRad));
//            currentFacing =
//                    (float) Math.toDegrees(Math.atan2(targetPos.y - basePos.y, targetPos.x - basePos.x));
            float slideOffset = 0f;
            if (state == State.FADE_IN) {
                float progress = stateTime / FADE_IN_TIME;
                slideOffset = -SLIDE_IN_DISTANCE * (1f - progress);
            } else if (state == State.FADE_OUT) {
                float progress = stateTime / FADE_OUT_TIME;
                slideOffset = -SLIDE_OUT_DISTANCE * progress;
            }
            float facingRad = (float) Math.toRadians(currentFacing);
//            currentPos.set(
//                    basePos.x + slideOffset * (float) Math.cos(facingRad),
//                    basePos.y + slideOffset * (float) Math.sin(facingRad));
        }

        private void updateDroneState() {
            if (demDrone == null || target == null) return;
            float angleRad = (float) Math.toRadians(currentFacing);
            float perpAngleRad = angleRad + (float) Math.PI / 2f;
            Vector2f weaponPos =
                    new Vector2f(
                            currentPos.x
                                    + WEAPON_OFFSET_Y * (float) Math.cos(angleRad)
                                    + WEAPON_OFFSET_X * (float) Math.cos(perpAngleRad),
                            currentPos.y
                                    + WEAPON_OFFSET_Y * (float) Math.sin(angleRad)
                                    + WEAPON_OFFSET_X * (float) Math.sin(perpAngleRad));
            demDrone.setOwner(source.getOwner());
            demDrone.getLocation().set(weaponPos);
            demDrone.setFacing(currentFacing);
            demDrone.getVelocity().set(target.getVelocity());
            demDrone.setCollisionRadius(0.1f);
            if (demDrone.getSpriteAPI() != null) {
                demDrone.getSpriteAPI().setAlphaMult(0f);
            }
            demDrone.getMouseTarget().set(target.getLocation());
            try {
                WeaponAPI weapon =
                        (WeaponAPI) demDrone.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
                weapon.setFacing(currentFacing);
                if (weapon.isBeam()) {
                    weapon.setKeepBeamTargetWhileChargingDown(true);
                    if (FIRING_TIME <= 2.0f) {
                        weapon.setScaleBeamGlowBasedOnDamageEffectiveness(false);
                    }
                    weapon.updateBeamFromPoints();
                }
            } catch (Exception e) {
            }
        }

        private void controlWeapons() {
            if (demDrone == null || target == null) return;
            if (state == State.FIRING) {
                demDrone.giveCommand(ShipCommand.FIRE, target.getLocation(), 0);
            }
        }

        private void cleanup(CombatEngineAPI engine) {
            if (demDrone != null) {
                engine.removeEntity(demDrone);
                demDrone = null;
            }
            engine.removePlugin(this);
        }

        @Override
        public void init(CombatEntityAPI entity) {}

        @Override
        public void advance(float amount) {
            advance(amount, null);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) {
                renderInWorldCoords(viewport);
            }
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public boolean isExpired() {
            return state == State.DONE;
        }

        @Override
        public void cleanup() {}
    }
}