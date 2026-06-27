package data.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


import javax.lang.model.util.AbstractAnnotationValueVisitor6;
import java.awt.*;
import java.util.Objects;

import static com.fs.starfarer.api.impl.campaign.plog.BasePLEntry.offset;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class FSD_LongSong implements EveryFrameWeaponEffectPlugin {
    private final Color coreColor = new Color(255, 55, 86,200);
    private final Color fringeColor = new Color(234, 207, 207,20);
    private static final float[] PARALLEL_OFFSETS1 = {-2.5f, 3f, 9.5f};
    private static final float[] PARALLEL_OFFSETS2 = {-2.5f, 3f, 9.5f};

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive()) return;
        if (ship.getSystem().getEffectLevel() < 0f) return;
        Vector2f V1 = getAdjustedVelocity(ship, 145f);
        Vector2f V2 = getAdjustedVelocity(ship, 0f);
        Vector2f V3 = getAdjustedVelocity(ship, -145f);
        if (!weapon.getShip().getCustomData().containsKey("FSD_LSdetect")) {
            weapon.getShip().setCustomData("FSD_LSdetect", true);
        }
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (Objects.equals(w.getId(), "FSD_LightLauncher")) {

                    Vector2f slotLocal = new Vector2f(w.getSlot().getLocation());
                    Vector2f slotWorld = getWorldLocation(ship, slotLocal);

                    for (float offset : PARALLEL_OFFSETS1) {
                        Vector2f modifiedLocal = new Vector2f(slotLocal.x + offset+2f, slotLocal.y + (offset / 1.3f)-7.5f);
                        Vector2f worldLocation = getWorldLocation(ship, modifiedLocal);

                        if (Objects.equals(w.getSlot().getId(), "FSD_particle2") && w.getSlot().isDecorative()) {
                            engine.addNebulaSmoothParticle(
                                    worldLocation, V1,
                                    3f, 5f, 0.25f,
                                    0.25f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.375f,
                                    coreColor);
                            engine.addNebulaSmokeParticle(
                                    worldLocation, V1,
                                    5f, 7f, 0.5f,
                                    0.1f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.6f,
                                    fringeColor);
                        }
                        if (Objects.equals(w.getSlot().getId(), "FSD_particle4") && w.getSlot().isDecorative()) {
                            engine.addNebulaSmoothParticle(
                                    getWorldLocation(ship,modifiedLocal.translate(-6.75f,+2.75f)), V2,
                                    4f, 5f, 0.25f,
                                    0.275f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.1f,
                                    coreColor);
                            engine.addNebulaSmokeParticle(
                                    getWorldLocation(ship,modifiedLocal.translate(-6.75f,+2.75f)), V2,
                                    5f, 7f, 0.5f,
                                    0.05f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.275f,
                                    fringeColor);
                        }
                    }
                    for (float offset : PARALLEL_OFFSETS2) {
                        Vector2f modifiedLocal = new Vector2f(slotLocal.x + offset+2f, slotLocal.y - (offset / 1.3f)+7.5f);
                        Vector2f worldLocation = getWorldLocation(ship, modifiedLocal);

                        if (Objects.equals(w.getSlot().getId(), "FSD_particle1") && w.getSlot().isDecorative()) {
                            engine.addNebulaSmoothParticle(
                                    worldLocation, V3,
                                    3f, 5f, 0.25f,
                                    0.25f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.375f,
                                    coreColor);
                            engine.addNebulaSmokeParticle(
                                    worldLocation, V3,
                                    5f, 7f, 0.5f,
                                    0.1f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.6f,
                                    fringeColor);
                        }
                        if (Objects.equals(w.getSlot().getId(), "FSD_particle3") && w.getSlot().isDecorative()) {
                            engine.addNebulaSmoothParticle(
                                    getWorldLocation(ship,modifiedLocal.translate(-6.75f,-2.75f)), V2,
                                    4f, 5f, 0.25f,
                                    0.275f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.1f,
                                    coreColor);
                            engine.addNebulaSmokeParticle(
                                    getWorldLocation(ship,modifiedLocal.translate(-6.75f,-2.75f)), V2,
                                    5f, 7f, 0.5f,
                                    0.05f * ship.getSystem().getEffectLevel(),
                                    ship.getSystem().getEffectLevel() * 0.275f,
                                    fringeColor);
                        }
                    }
                }
            }
       // }
    }

    private Vector2f getWorldLocation(ShipAPI ship, Vector2f localOffset) {
        Vector2f rotated = new Vector2f(localOffset);

        VectorUtils.rotate(rotated, ship.getFacing(), rotated);

        return Vector2f.add(ship.getLocation(), rotated, null);
    }

    private Vector2f getAdjustedVelocity(ShipAPI ship, float localAngle) {

        float radians = (float) Math.toRadians(localAngle);
        Vector2f direction = new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));

        VectorUtils.rotate(direction, ship.getFacing(), direction);

        direction.scale(65f);
        Vector2f.add(direction, ship.getVelocity(), direction);
        return direction;
    }


//
//    @Override
//        ShipAPI ship = weapon.getShip();
//
//                Vector2f slotLocal = new Vector2f(w.getSlot().getLocation());
//                Vector2f slotWorld = getWorldLocation(ship, slotLocal);
//
//                    Vector2f modifiedLocal = new Vector2f(slotLocal.x + offset, slotLocal.y);
//                    Vector2f worldLocation = getWorldLocation(ship, modifiedLocal);
//
//                    Vector2f velocity = getAdjustedVelocity(ship, LOCAL_ANGLE);
//
//                            3f, 6f, 3f,
//                            0.8f * weapon.getChargeLevel(),
//                            5f, 7f, 0.5f,
//                            0.1f * weapon.getChargeLevel(),
//                }
//            }
//        }
//    }
//
//        return Objects.equals(w.getId(), "FSD_LightLauncher")
//                && Objects.equals(w.getSlot().getId(), "FSD_particle2")
//                && w.getSlot().isDecorative();
//    }
//
//        Vector2f rotated = VectorUtils.rotate(localOffset, ship.getFacing(), new Vector2f());
//        return Vector2f.add(ship.getLocation(), rotated, null);
//    }
//
//        float radians = (float) Math.toRadians(localAngle);
//        Vector2f direction = new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));
//    }
}
