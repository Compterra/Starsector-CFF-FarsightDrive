package data.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class FSD_BEParticleData {
    private final String ParticleTexture = "graphics/fx/FSD_BEparticle.png";
    public SpriteAPI sprite = Global.getSettings().getSprite(ParticleTexture);
    public Vector2f offset = new Vector2f();
    public Vector2f vel = new Vector2f();
    public float scale = 1.0F;
    public float scaleIncreaseRate = 1.0F;
    public float turnDir = 1.0F;
    public float angle = 1.0F;
    public float maxDur;
    public FaderUtil fader;
    public float elapsed = 0.0F;
    public float baseSize;

    public FSD_BEParticleData(float baseSize, float maxDur, float endSizeMult) {
        float i = (float) Misc.random.nextInt(4);
        float j = (float)Misc.random.nextInt(4);
        this.sprite.setTexWidth(0.25F);
        this.sprite.setTexHeight(0.25F);
        this.sprite.setTexX(i * 0.25F);
        this.sprite.setTexY(j * 0.25F);
        this.sprite.setAdditiveBlend();
        this.angle = (float)Math.random() * 360.0F;
        this.maxDur = maxDur;
        this.scaleIncreaseRate = endSizeMult / maxDur;
        if (endSizeMult < 1.0F) {
            this.scaleIncreaseRate = -1.0F * endSizeMult;
        }

        this.scale = 1.0F;
        this.baseSize = baseSize;
        this.turnDir = Math.signum((float)Math.random() - 0.5F) * 20.0F * (float)Math.random();
        float driftDir = (float)Math.random() * 360.0F;
        this.vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
        this.vel.scale(0.25F * baseSize / maxDur * (1.0F + (float)Math.random() * 1.0F));
        this.fader = new FaderUtil(0.0F, 0.5F, 0.5F);
        this.fader.forceOut();
        this.fader.fadeIn();
    }

    public void advance(float amount) {
        this.scale += this.scaleIncreaseRate * amount;
        Vector2f var10000 = this.offset;
        var10000.x += this.vel.x * amount;
        var10000 = this.offset;
        var10000.y += this.vel.y * amount;
        this.angle += this.turnDir * amount;
        this.elapsed += amount;
        if (this.maxDur - this.elapsed <= this.fader.getDurationOut() + 0.1F) {
            this.fader.fadeOut();
        }

        this.fader.advance(amount);
    }
}
