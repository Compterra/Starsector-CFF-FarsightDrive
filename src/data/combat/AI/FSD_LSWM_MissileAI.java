package data.combat.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicTargeting;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class FSD_LSWM_MissileAI implements MissileAIPlugin, GuidedMissileAI{

    //////////////////////
    //     SETTINGS     //
    //////////////////////

    private final float DAMPING=0.1f;
    private final int SEARCH_CONE=360, MAX_SCATTER=30;
    private float PRECISION_RANGE=500, ECCM=2f;

    private final IntervalUtil clock = new IntervalUtil(0.15f, 0.15f);
    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target=null;
    private boolean launch=true;
    private float timer=0, check=0f, scatter=0, random, correctAngle;

    private Stack<Integer> availableSlots = new Stack<>();
    private List<MissileAPI> children = new ArrayList<>();
    private boolean slotsInitialized = false;

    public FSD_LSWM_MissileAI(MissileAPI missile, ShipAPI launchingShip){
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        this.missile = missile;

        if (missile.getSource().getVariant().getHullMods().contains("eccm")){
            ECCM=1;
        }

        PRECISION_RANGE=(float)Math.pow((3*PRECISION_RANGE),2);

        random = MathUtils.getRandomNumberInRange(-1f, 1f);

        if (!slotsInitialized) {
            for (int i = 0; i < 3; i++) {
                availableSlots.push(i);
            }
            slotsInitialized = true;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void advance(float amount) {

        if (engine.isPaused()) {return;}

        if(missile.isFizzling() || missile.isFading()){
            engine.applyDamage(missile, missile.getLocation(), missile.getHitpoints()*2, DamageType.FRAGMENTATION, 0, true, false, missile.getSource());
        }

        if (target == null
                || (target instanceof ShipAPI && ((ShipAPI)target).isHulk())
                || !engine.isEntityInPlay(target)
                || target.getCollisionClass()==CollisionClass.NONE
        ){
            if(Math.random()<0.5){
                setTarget(MagicTargeting.pickTarget(missile,
                                MagicTargeting.targetSeeking.NO_RANDOM,
                                (int)missile.getWeapon().getRange(),
                                SEARCH_CONE,
                                1,1,1,1,1,
                                true
                        )
                );
            } else {
                setTarget(MagicTargeting.pickTarget(missile,
                                MagicTargeting.targetSeeking.LOCAL_RANDOM,
                                (int)missile.getWeapon().getRange()/2,
                                SEARCH_CONE,
                                0,1,2,3,4,
                                true
                        )
                );
            }
            launch=true;
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        timer+=amount;

        if(launch || timer>=check){
            launch=false;

            timer -=check;

            float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation())/PRECISION_RANGE;
            check = Math.min(
                    0.5f,
                    Math.max(
                            0.1f,
                            dist)
            );

            scatter = ECCM * MAX_SCATTER * random * check;
        }

        correctAngle = VectorUtils.getAngle(
                missile.getLocation(),
                target.getLocation()
        );

        correctAngle+=scatter;

        float correction = MathUtils.getShortestRotation(VectorUtils.getFacing(missile.getVelocity()),correctAngle);
        if(correction>0){
            correction= -11.25f * ( (float)Math.pow(FastTrig.cos(MathUtils.FPI*correction/90)+1, 2) -4 );
        } else {
            correction= 11.25f * ( (float)Math.pow(FastTrig.cos(MathUtils.FPI*correction/90)+1, 2) -4 );
        }
        correctAngle+= correction;

        float aimAngle = MathUtils.getShortestRotation( missile.getFacing(), correctAngle);

        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }

        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }

        missile.giveCommand(ShipCommand.ACCELERATE);

        updateChildren(amount);
    }

    private void spawnChildMissile() {
        if (missile.getSource() != null && !availableSlots.isEmpty()) {
            int slotIndex = availableSlots.pop();

            MissileAPI childMissile = (MissileAPI) engine.spawnProjectile(missile.getSource(),
                    missile.getWeapon(),
                    "FSD_LSWM_Child",
                    missile.getLocation(),
                    (float) (Math.random() * 360f),
                    new Vector2f());

            while (children.size() <= slotIndex) {
                children.add(null);
            }

            children.set(slotIndex, childMissile);
            childMissile.setCustomData("LSWM_Parent", missile);

        }
    }

    private void updateChildren(float amount) {
        clock.advance(amount);

        for (int i = 0; i < children.size(); i++) {
            MissileAPI childMissile = children.get(i);
            if (childMissile != null) {
                if (childMissile.isFizzling() || childMissile.isFading() || childMissile.isExpired() ||
                        !engine.isEntityInPlay(childMissile) || childMissile.getHitpoints() <= 0) {

                    children.set(i, null);
                    availableSlots.push(i);

                }
            }
        }

        if (missile.isFizzling() || missile.isFading() || missile.isExpired() || missile.getSource() == null) {
            return;
        }

        if (!availableSlots.isEmpty() && clock.intervalElapsed()) {
            spawnChildMissile();
        }
    }

    @Override
    public CombatEntityAPI getTarget(){
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target){
        this.target = target;
    }
}