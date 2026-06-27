package data.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class FSD_CombatRender extends BaseCombatLayeredRenderingPlugin {


    public FSD_CombatRender() {
        super();
    }

    private static final String INSTANCEKEY_STRING = "FSD_CombatRender_Instance";

    public static FSD_CombatRender getInstance(CombatEngineAPI engine) {
        if (engine == null) {
            return null;
        }
        if (engine.getCustomData().containsKey(INSTANCEKEY_STRING)) {
            return (FSD_CombatRender) engine.getCustomData().get(INSTANCEKEY_STRING);
        } else {
            FSD_CombatRender render = new FSD_CombatRender();
            engine.addLayeredRenderingPlugin(render);
            engine.getCustomData().put(INSTANCEKEY_STRING, render);
            return render;
        }
    }

    /**
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Set<RenderData> getRenders(CombatEngineAPI engine) {
        if (engine.getCustomData().get("Gamma_CustomSpriteRender") == null) {
            engine.getCustomData().put("Gamma_CustomSpriteRender", new HashSet<RenderData>());
        }
        return (Set<RenderData>) engine.getCustomData().get("Gamma_CustomSpriteRender");
    }

    /**
     */
    public RenderData CreatRenderData(CombatEngineLayers layers, SpriteAPI sprite) {
        return new RenderData(layers, sprite);
    }

    /**
     */
    public RenderData CreatRenderData(CombatEngineLayers layers, SpriteAPI sprite, int row, int column, float height, float width) {
        return new RenderData(layers, sprite, row, column, height, width);
    }


    public static String Path;
    public static String SPRITE(String path){
        Path = path;
        return Path;
    }
    private final String RenderLayer = "graphics/missiles/FSD_ThrowShell.png";
   // SpriteAPI sprite = Global.getSettings().getSprite(RenderLayer);
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI view) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        Set<RenderData> render = getRenders(engine);
        if (render == null) {
            return;
        }
        for (RenderData data : render) {
            if (data == null) {
                continue;
            }
            if (data.getLayer() != layer) {
                continue;
            }
            data.render(layer, view);
        }
        for (shellObj shell : activeShells) {
            if (shell.alpha > 0) {
                SpriteAPI sprite = Global.getSettings().getSprite(SPRITE(Path));
                sprite.setAdditiveBlend();
                shell.render(sprite);
            }
        }
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CombatEngineLayers.class);
    }


    private static final List<shellObj> activeShells = new LinkedList<>();
    public void addShell(shellObj shell) {
        activeShells.add(shell);
    }
    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || amount <= 0) return;

        Iterator<shellObj> iterator = activeShells.iterator();
        while (iterator.hasNext()) {
            shellObj shell = iterator.next();
            shell.advance(amount);

            shell.smokeInterval.advance(amount);
            if (shell.smokeInterval.intervalElapsed()) {
                engine.addSmokeParticle(
                        shell.location,
                        new Vector2f(),
                        10f,
                        0.8f,
                        1.5f,
                        new Color(150, 150, 150, 100)
                );
            }

            if (shell.isExpired()) {
                iterator.remove();
            }
        }
    }
    public static class shellObj {
        private final Vector2f location;
        private final Vector2f velocity;
        float facing;
        float angularVelocity;
        public float life;
        float alpha = 1;
        IntervalUtil smokeInterval = new IntervalUtil(0.2f, 0.5f);
        private static final float VELOCITY_DAMPING = 0.98f;

        public shellObj(Vector2f location, Vector2f velocity, float facing, float angularVelocity) {
            this.location = location;
            this.velocity = velocity;
            this.facing = facing;
            this.angularVelocity = angularVelocity;
        }

        public void advance(float amount) {
            life -= amount;

            velocity.x *= VELOCITY_DAMPING;
            velocity.y *= VELOCITY_DAMPING;

            Vector2f v = new Vector2f(velocity);
            v.scale(amount);
            Vector2f.add(location, v, location);

            facing = Misc.normalizeAngle(angularVelocity * amount + facing);
            if(life<1){
                alpha=Math.max(0,life);
            }
        }
        public void render(SpriteAPI sprite){
            sprite.setAngle(facing - 90f);
            sprite.setAlphaMult(alpha * 0.9f);
            sprite.renderAtCenter(location.x, location.y);

        }

        boolean isExpired() {
            return life < 0;
        }
    }


    public static class RenderData {

        final CombatEngineLayers layer;
        CombatEngineAPI engine;
        SpriteAPI sprite;
        float x;
        float y;
        final float width;
        final float height;
        float RegionX = 0;
        float RegionY = 0;
        float RegionWidth;
        float RegionHeigh;
        boolean isRegion = false;
        WeaponAPI weapon;


        public RenderData(CombatEngineLayers layer, SpriteAPI sprite) {
            this.layer = layer;
            this.sprite = sprite;
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }
        int row;
        int column;
        int frame = 1;

        /**
         *
         */
        public RenderData(CombatEngineLayers layer, SpriteAPI sprite, int row_num, int column_num, float high, float width) {
            this.layer = layer;
            this.sprite = sprite;
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
            this.row = row_num;
            this.column = column_num;
            this.RegionWidth = width;
            this.RegionHeigh = high;
            isRegion = true;
        }


        public float setShellAlpha(float Shellalpha) {
            float clampedAlpha = Math.min(1.0f, Math.max(0.0f, Shellalpha));
            float visualAlpha = (float) (1.0 - Math.pow(1.0 - clampedAlpha, 2));
            sprite.setAlphaMult(visualAlpha);

            Color original = sprite.getColor();
            float desaturation = 0.5f + clampedAlpha * 0.5f;
            sprite.setColor(new Color(
                    (int)(original.getRed() * desaturation),
                    (int)(original.getGreen() * desaturation),
                    (int)(original.getBlue() * desaturation),
                    original.getAlpha()
            ));
            return Shellalpha;
        }
        public float setAlpha(float alpha) {
//            Color original = sprite.getColor();
//            float clampedAlpha = Math.min(1.0f, Math.max(0.0f, alpha));
            sprite.setAlphaMult(alpha);
            return alpha;
        }
        public int setRed(int red) {
            sprite.setColor(new Color(red, sprite.getColor().getGreen(), sprite.getColor().getBlue(), sprite.getColor().getAlpha()));
            return red;
        }
        public int setGreen(int green) {
            sprite.setColor(new Color(sprite.getColor().getRed(), green, sprite.getColor().getBlue(), sprite.getColor().getAlpha()));
            return green;
        }
        public int setBlue(int blue) {
            sprite.setColor(new Color(sprite.getColor().getRed(), sprite.getColor().getGreen(), blue, sprite.getColor().getAlpha()));
            return blue;
        }
        public int setColor(int color) {
            sprite.setColor(new Color(color));
            return color;
        }

        public CombatEngineLayers getLayer() {
            return layer;
        }

        /**
         */
        public void setRegion(float frameNumber) {
            if (isRegion) {
                float r = 0;
                for (; frameNumber > column;) {
                    frameNumber -= column;
                    r += 1;
                    if (r > row) {
                        return;
                    }
                }
                this.RegionX = (frameNumber - 1) / column;
                this.RegionY = r / row;
                sprite.setCenter((frameNumber - 0.5f) * RegionWidth, (r) * RegionHeigh);
            }
        }
        private renderPlugin plugin = null;

        public void render(CombatEngineLayers layer, ViewportAPI view) {
            if (plugin != null) {
                if (isRegion) {
                    plugin.renderOverrideRegion(sprite, x, y, RegionX, RegionY, 1f / column, 1f / row);
                } else {
                    plugin.renderOverride(sprite, x, y);
                }

            } else {
                if (isRegion) {
                    sprite.renderRegionAtCenter(x, y, RegionX, RegionY, 1f / column, 1f / row);
                } else {
                    sprite.renderAtCenter(x, y);
                }
            }
            for (shellObj shell : activeShells) {
                if (shell.alpha > 0) {
                    SpriteAPI sprite = Global.getSettings().getSprite("fx", "shell_texture");
                    sprite.setAdditiveBlend();
                    shell.render(sprite);
                }
            }
        }

        /**
         *
         * @param plugin
         */
        public void setPlugin(renderPlugin plugin) {
            this.plugin = plugin;
        }

        /**
         * @param scale
         */
        public void scale(float scale) {
            float centerx = width * 0.5f * scale;
            float centery = height * 0.5f * scale;
            sprite.setCenter(centerx, centery);
            sprite.setWidth(width * scale);
            sprite.setHeight(height * scale);
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void setLocation(Vector2f location) {
            this.x = location.x;
            this.y = location.y;
        }

        public SpriteAPI getSprite() {
            return sprite;
        }

        public void setSprite(SpriteAPI sprite) {
            this.sprite = sprite;
        }

        public void setFacing(float facing) {
            sprite.setAngle(facing);
        }
    }

    public interface renderPlugin {

        public void renderOverride(SpriteAPI sprite, float x, float y);

        public void renderOverrideRegion(SpriteAPI sprite, float x, float y, float tx, float ty, float tw, float th);
    }
}
