package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double DOUBLE_PI = Math.PI * 2.0;
    private static final int CIRCLE_STEPS = 40;

    private static final int MODE_NONE = 0;
    private static final int MODE_POINTS = 1;
    private static final int MODE_IMAGE = 2;
    private static final int MODE_ZAVZ = 3;
    private static final int MODE_CIRCLE = 4;
    private static final int MODE_JELLO = 5;
    private static final int MODE_LIES = 6;
    private static final int MODE_FDP = 7;
    private static final int MODE_SIMS = 8;
    private static final int MODE_BOX = 9;
    private static final int MODE_ROUND_BOX = 10;
    private static final int MODE_HEAD = 11;
    private static final int MODE_MARK = 12;

    private static final int IMAGE_RECTANGLE = 0;
    private static final int IMAGE_QUAD_STAPPLE = 1;
    private static final int IMAGE_TRIANGLE_STAPPLE = 2;
    private static final int IMAGE_TRIANGLE_STIPPLE = 3;
    private static final int IMAGE_GLOW_CIRCLE = 4;

    private static final ResourceLocation TEX_GLOW_CIRCLE = new ResourceLocation("myau/texture/targetesp/glow_circle.png");
    private static final ResourceLocation TEX_RECTANGLE = new ResourceLocation("myau/texture/targetesp/rectangle.png");
    private static final ResourceLocation TEX_QUAD_STAPPLE = new ResourceLocation("myau/texture/targetesp/quadstapple.png");
    private static final ResourceLocation TEX_TRIANGLE_STAPPLE = new ResourceLocation("myau/texture/targetesp/trianglestapple.png");
    private static final ResourceLocation TEX_TRIANGLE_STIPPLE = new ResourceLocation("myau/texture/targetesp/trianglestipple.png");

    public final ModeProperty markMode = new ModeProperty(
            "mark-mode",
            MODE_POINTS,
            new String[]{"None", "Points", "Image", "Zavz", "Circle", "Jello", "Lies", "FDP", "Sims", "Box", "RoundBox", "Head", "Mark"}
    );

    public final ColorProperty colorPrimary = new ColorProperty("color-primary", new Color(0, 90, 255).getRGB(), this::isMarkMode);
    public final ColorProperty colorSecondary = new ColorProperty("color-secondary", new Color(0, 90, 255).getRGB(), () -> this.isMarkMode() && this.markMode.getValue() == MODE_ZAVZ);

    public final ColorProperty circleStartColor = new ColorProperty("circle-start-color", Color.BLUE.getRGB(), () -> this.markMode.getValue() == MODE_CIRCLE);
    public final IntProperty circleStartAlpha = new IntProperty("circle-start-alpha", 255, 0, 255, () -> this.markMode.getValue() == MODE_CIRCLE);
    public final ColorProperty circleEndColor = new ColorProperty("circle-end-color", Color.CYAN.getRGB(), () -> this.markMode.getValue() == MODE_CIRCLE);
    public final IntProperty circleEndAlpha = new IntProperty("circle-end-alpha", 0, 0, 255, () -> this.markMode.getValue() == MODE_CIRCLE);

    public final FloatProperty pointsSpeed = new FloatProperty("points-speed", 2.0F, 0.5F, 5.0F, () -> this.markMode.getValue() == MODE_POINTS);
    public final FloatProperty pointsRadius = new FloatProperty("points-radius", 0.60F, 0.20F, 1.20F, () -> this.markMode.getValue() == MODE_POINTS);
    public final FloatProperty pointsScale = new FloatProperty("points-scale", 0.25F, 0.05F, 0.60F, () -> this.markMode.getValue() == MODE_POINTS);
    public final IntProperty pointsLayers = new IntProperty("points-layers", 3, 1, 5, () -> this.markMode.getValue() == MODE_POINTS);
    public final BooleanProperty pointsAdditive = new BooleanProperty("points-additive", true, () -> this.markMode.getValue() == MODE_POINTS);

    public final ModeProperty imageMode = new ModeProperty(
            "image-mode",
            IMAGE_RECTANGLE,
            new String[]{"Rectangle", "QuadStapple", "TriangleStapple", "TriangleStipple", "GlowCircle"},
            () -> this.markMode.getValue() == MODE_IMAGE
    );
    public final FloatProperty imageScale = new FloatProperty("image-scale", 0.6F, 0.1F, 2.0F, () -> this.markMode.getValue() == MODE_IMAGE);
    public final FloatProperty imageXOffset = new FloatProperty("image-x-offset", 0.0F, -1.5F, 1.5F, () -> this.markMode.getValue() == MODE_IMAGE);
    public final FloatProperty imageYOffset = new FloatProperty("image-y-offset", 0.0F, -0.5F, 1.5F, () -> this.markMode.getValue() == MODE_IMAGE);
    public final BooleanProperty imageAdditive = new BooleanProperty("image-additive", true, () -> this.markMode.getValue() == MODE_IMAGE);
    public final BooleanProperty imageSpin = new BooleanProperty("image-spin", false, () -> this.markMode.getValue() == MODE_IMAGE);
    public final FloatProperty imageSpinSpeed = new FloatProperty("image-spin-speed", 1.0F, 0.1F, 5.0F, () -> this.markMode.getValue() == MODE_IMAGE && this.imageSpin.getValue());
    public final BooleanProperty imageBillboard = new BooleanProperty("image-billboard", true, () -> this.markMode.getValue() == MODE_IMAGE);
    public final ColorProperty imageColor1 = new ColorProperty("image-color-1", new Color(255, 255, 255, 255).getRGB(), () -> this.markMode.getValue() == MODE_IMAGE);
    public final ColorProperty imageColor2 = new ColorProperty("image-color-2", new Color(255, 255, 255, 255).getRGB(), () -> this.markMode.getValue() == MODE_IMAGE);
    public final ColorProperty imageColor3 = new ColorProperty("image-color-3", new Color(255, 255, 255, 255).getRGB(), () -> this.markMode.getValue() == MODE_IMAGE);
    public final ColorProperty imageColor4 = new ColorProperty("image-color-4", new Color(255, 255, 255, 255).getRGB(), () -> this.markMode.getValue() == MODE_IMAGE);

    public final BooleanProperty fillInnerCircle = new BooleanProperty("fill-inner-circle", false, () -> this.markMode.getValue() == MODE_CIRCLE);
    public final BooleanProperty withHeight = new BooleanProperty("with-height", true, () -> this.markMode.getValue() == MODE_CIRCLE);
    public final BooleanProperty animateHeight = new BooleanProperty("animate-height", false, () -> this.withHeight.getValue());
    public final FloatProperty heightRangeStart = new FloatProperty("height-range-start", 0.0F, -2.0F, 2.0F, () -> this.withHeight.getValue());
    public final FloatProperty heightRangeEnd = new FloatProperty("height-range-end", 0.4F, -2.0F, 2.0F, () -> this.withHeight.getValue());
    public final FloatProperty extraWidth = new FloatProperty("extra-width", 0.0F, 0.0F, 2.0F, () -> this.markMode.getValue() == MODE_CIRCLE);
    public final BooleanProperty animateCircleY = new BooleanProperty("animate-circle-y", true, () -> this.fillInnerCircle.getValue() || this.withHeight.getValue());
    public final FloatProperty circleYRangeStart = new FloatProperty("circle-y-range-start", 0.0F, 0.0F, 2.0F, () -> this.animateCircleY.getValue());
    public final FloatProperty circleYRangeEnd = new FloatProperty("circle-y-range-end", 0.5F, 0.0F, 2.0F, () -> this.animateCircleY.getValue());
    public final FloatProperty duration = new FloatProperty("duration", 1.5F, 0.5F, 3.0F, () -> this.animateCircleY.getValue() || this.animateHeight.getValue());

    public final BooleanProperty rainbow = new BooleanProperty("mark-rainbow", false, this::isMarkMode);
    public final BooleanProperty hurt = new BooleanProperty("mark-hurt-time", true, this::isMarkMode);
    public final BooleanProperty boxOutline = new BooleanProperty("outline", true, () -> this.markMode.getValue() == MODE_BOX);

    private final Vec3[] circlePoints = new Vec3[CIRCLE_STEPS + 1];
    private final Map<Integer, Boolean> glCapMap = new HashMap<>();
    private double start = 0.0;

    public Mark() {
        super("Mark", false);

        for (int i = 0; i <= CIRCLE_STEPS; i++) {
            double theta = DOUBLE_PI * i / CIRCLE_STEPS;
            this.circlePoints[i] = new Vec3(-Math.sin(theta), 0.0, Math.cos(theta));
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        EntityLivingBase target = this.getTarget();
        if (target == null || this.markMode.getValue() == MODE_NONE) {
            return;
        }

        Color color = this.rainbow.getValue()
                ? this.rainbowColor()
                : this.withAlpha(new Color(this.colorPrimary.getValue()), 255);

        switch (this.markMode.getValue()) {
            case MODE_BOX:
                this.drawEntityBoxESP(target, (this.hurt.getValue() && target.hurtTime > 3) ? new Color(255, 50, 50, 75) : color);
                break;
            case MODE_ROUND_BOX:
                this.drawEntityBox(
                        target,
                        (this.hurt.getValue() && target.hurtTime > 3) ? new Color(37, 126, 255, 70) : new Color(255, 0, 0, 70),
                        this.boxOutline.getValue()
                );
                break;
            case MODE_HEAD:
                this.drawPlatformESP(target, (this.hurt.getValue() && target.hurtTime > 3) ? new Color(255, 50, 50, 75) : color);
                break;
            case MODE_MARK:
                this.drawPlatform(target, (this.hurt.getValue() && target.hurtTime > 3) ? new Color(37, 126, 255, 70) : color);
                break;
            case MODE_SIMS:
                this.drawCrystal(
                        target,
                        ((this.hurt.getValue() && target.hurtTime <= 0) ? new Color(80, 255, 80, 200) : new Color(255, 0, 0, 200)).getRGB(),
                        event
                );
                break;
            case MODE_ZAVZ:
                this.drawZavz(target, event, true);
                break;
            case MODE_JELLO:
                this.drawJello(target);
                break;
            case MODE_FDP:
                this.drawFDP(target, event);
                break;
            case MODE_LIES:
                this.drawLies(target, event);
                break;
            case MODE_CIRCLE:
                float heightStart = this.heightRangeStart.getValue();
                float heightEnd = this.heightRangeEnd.getValue();
                if (!this.animateHeight.getValue()) {
                    heightStart = heightEnd;
                }

                Float circleYStart = null;
                Float circleYEnd = null;
                if (this.animateCircleY.getValue()) {
                    circleYStart = this.circleYRangeStart.getValue();
                    circleYEnd = this.circleYRangeEnd.getValue();
                }

                int startColor = this.withAlpha(new Color(this.circleStartColor.getValue()), this.circleStartAlpha.getValue()).getRGB();
                int endColor = this.withAlpha(new Color(this.circleEndColor.getValue()), this.circleEndAlpha.getValue()).getRGB();

                this.drawCircle(
                        target,
                        this.duration.getValue() * 1000.0F,
                        heightStart,
                        heightEnd,
                        this.extraWidth.getValue(),
                        this.fillInnerCircle.getValue(),
                        this.withHeight.getValue(),
                        circleYStart,
                        circleYEnd,
                        startColor,
                        endColor,
                        event.getPartialTicks()
                );
                break;
            case MODE_POINTS:
                this.drawPoints(
                        target,
                        color,
                        this.pointsSpeed.getValue(),
                        this.pointsRadius.getValue(),
                        this.pointsScale.getValue(),
                        this.pointsLayers.getValue(),
                        this.pointsAdditive.getValue(),
                        this.hurt.getValue()
                );
                break;
            case MODE_IMAGE:
                this.drawImageMark(
                        target,
                        this.getImageTexture(),
                        this.withAlpha(new Color(this.imageColor1.getValue()), 255),
                        this.withAlpha(new Color(this.imageColor2.getValue()), 255),
                        this.withAlpha(new Color(this.imageColor3.getValue()), 255),
                        this.withAlpha(new Color(this.imageColor4.getValue()), 255),
                        this.imageScale.getValue(),
                        this.imageXOffset.getValue(),
                        this.imageYOffset.getValue(),
                        this.imageAdditive.getValue(),
                        this.imageSpin.getValue(),
                        this.imageSpinSpeed.getValue(),
                        this.imageBillboard.getValue(),
                        this.hurt.getValue()
                );
                break;
            default:
                break;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.markMode.getModeString()};
    }

    @Override
    public void verifyValue(String value) {
        if (this.heightRangeStart.getValue() > this.heightRangeEnd.getValue()) {
            if (this.heightRangeStart.getName().equals(value)) {
                this.heightRangeEnd.setValue(this.heightRangeStart.getValue());
            } else {
                this.heightRangeStart.setValue(this.heightRangeEnd.getValue());
            }
        }

        if (this.circleYRangeStart.getValue() > this.circleYRangeEnd.getValue()) {
            if (this.circleYRangeStart.getName().equals(value)) {
                this.circleYRangeEnd.setValue(this.circleYRangeStart.getValue());
            } else {
                this.circleYRangeStart.setValue(this.circleYRangeEnd.getValue());
            }
        }
    }

    private boolean isMarkMode() {
        int mode = this.markMode.getValue();
        return mode != MODE_NONE && mode != MODE_SIMS && mode != MODE_FDP && mode != MODE_LIES && mode != MODE_JELLO;
    }

    private EntityLivingBase getTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura == null || !killAura.isEnabled() || !killAura.isAttackAllowed()) {
            return null;
        }

        EntityLivingBase target = killAura.getTarget();
        if (!TeamUtil.isEntityLoaded(target) || target.isDead || target.deathTime > 0) {
            return null;
        }

        return target;
    }

    private ResourceLocation getImageTexture() {
        switch (this.imageMode.getValue()) {
            case IMAGE_RECTANGLE:
                return TEX_RECTANGLE;
            case IMAGE_QUAD_STAPPLE:
                return TEX_QUAD_STAPPLE;
            case IMAGE_TRIANGLE_STAPPLE:
                return TEX_TRIANGLE_STAPPLE;
            case IMAGE_TRIANGLE_STIPPLE:
                return TEX_TRIANGLE_STIPPLE;
            case IMAGE_GLOW_CIRCLE:
            default:
                return TEX_GLOW_CIRCLE;
        }
    }

    private void drawCircle(
            EntityLivingBase entity,
            float speed,
            float heightStart,
            float heightEnd,
            float size,
            boolean filled,
            boolean withHeight,
            Float circleYStart,
            Float circleYEnd,
            int startColor,
            int endColor,
            float partialTicks
    ) {
        double renderX = this.getRenderPosX();
        double renderY = this.getRenderPosY();
        double renderZ = this.getRenderPosZ();
        List<double[]> positions = new ArrayList<>();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        mc.entityRenderer.disableLightmap();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        float a1 = ((startColor >>> 24) & 255) / 255.0F;
        float r1 = ((startColor >>> 16) & 255) / 255.0F;
        float g1 = ((startColor >>> 8) & 255) / 255.0F;
        float b1 = (startColor & 255) / 255.0F;

        float a2 = ((endColor >>> 24) & 255) / 255.0F;
        float r2 = ((endColor >>> 16) & 255) / 255.0F;
        float g2 = ((endColor >>> 8) & 255) / 255.0F;
        float b2 = (endColor & 255) / 255.0F;

        float breathingT = this.breathe(speed);
        float entityHeight = (float) (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY);
        float width = 0.5F + size;

        float animatedHeight = this.lerp(0.0F, entityHeight, this.lerp(heightEnd, heightStart, breathingT));
        float animatedCircleRange = 0.0F;
        if (circleYStart != null && circleYEnd != null) {
            animatedCircleRange = this.lerp(circleYStart, circleYEnd, breathingT);
        }
        float animatedCircleY = this.lerp(0.0F, entityHeight, animatedCircleRange);

        double posX = this.interpolate(entity.posX, entity.lastTickPosX, partialTicks);
        double posY = this.interpolate(entity.posY, entity.lastTickPosY, partialTicks);
        double posZ = this.interpolate(entity.posZ, entity.lastTickPosZ, partialTicks);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();

        if (filled) {
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        }

        for (Vec3 circlePoint : this.circlePoints) {
            double px = posX + circlePoint.xCoord * width;
            double py = posY + circlePoint.yCoord + animatedCircleY;
            double pz = posZ + circlePoint.zCoord * width;
            positions.add(new double[]{px, py, pz});

            if (filled) {
                buffer.pos(px - renderX, py - renderY, pz - renderZ).color(r1, g1, b1, a1).endVertex();
            }
        }

        if (filled) {
            tessellator.draw();
        }

        if (withHeight) {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            for (int i = 0; i < positions.size() - 1; i++) {
                double[] pos = positions.get(i);
                double[] endPos = positions.get(i + 1);

                buffer.pos(pos[0] - renderX, pos[1] - renderY, pos[2] - renderZ).color(r1, g1, b1, a1).endVertex();
                buffer.pos(endPos[0] - renderX, endPos[1] - renderY, endPos[2] - renderZ).color(r1, g1, b1, a1).endVertex();
                buffer.pos(endPos[0] - renderX, endPos[1] - renderY + animatedHeight, endPos[2] - renderZ).color(r2, g2, b2, a2).endVertex();
                buffer.pos(pos[0] - renderX, pos[1] - renderY + animatedHeight, pos[2] - renderZ).color(r2, g2, b2, a2).endVertex();
            }

            tessellator.draw();
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawPoints(
            EntityLivingBase target,
            Color baseColor,
            float speed,
            float pointsRadius,
            float pointsScale,
            int pointsLayers,
            boolean pointsAdditive,
            boolean hurt
    ) {
        float partial = this.getPartialTicks();
        double x = this.interpolate(target.posX, target.lastTickPosX, partial) - this.getRenderPosX();
        double y = this.interpolate(target.posY, target.lastTickPosY, partial) - this.getRenderPosY() + target.height / 1.6F;
        double z = this.interpolate(target.posZ, target.lastTickPosZ, partial) - this.getRenderPosZ();

        Color altColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (baseColor.getAlpha() * 0.75F));

        long now = System.currentTimeMillis();
        double s = 1500.0 / Math.max(speed, 0.0001F);
        double u = (now % 1_000_000L) / s;
        double t = u + Math.sin(u) / 10.0;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        if (pointsAdditive) {
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        } else {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GlStateManager.disableAlpha();

        mc.getTextureManager().bindTexture(TEX_GLOW_CIRCLE);
        GlStateManager.enableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer vb = tess.getWorldRenderer();

        double layerYOffset = 0.0;
        boolean flip = false;
        int layers = Math.max(pointsLayers, 1);

        for (int layer = 0; layer < layers; layer++) {
            double angle = t * 360.0 * (flip ? -1.0 : 1.0);
            double end = angle + 90.0 * (flip ? -1.0 : 1.0);
            double step = flip ? -2.0 : 2.0;

            double i = angle;
            while (flip ? i >= end : i <= end) {
                double prog = this.clamp(Math.abs((i - angle) / 90.0), 0.0, 1.0);
                double rad = Math.toRadians(i);
                double pointY = layerYOffset + Math.sin(rad * 1.2) * 0.10;

                float progF = (float) prog;
                float sizeBase = (flip ? 0.15F : 0.25F)
                        * (Math.max(
                        flip ? 0.25F : 0.15F,
                        flip ? progF : (1.0F + (0.4F - progF)) / 2.0F
                ) + 0.45F);

                double size = sizeBase * (2.0F + ((1.0F - 0.5F) * 2.0F)) * pointsScale;
                double half = size / 2.0;

                Color c = prog < 0.5 ? baseColor : altColor;
                float a = (c.getAlpha() / 255.0F) * ((hurt && target.hurtTime > 3) ? 1.0F : 0.9F);
                int alpha = (int) (a * 255.0F);
                alpha = Math.max(0, Math.min(255, alpha));

                GlStateManager.pushMatrix();
                GlStateManager.translate(Math.cos(rad) * pointsRadius, pointY, Math.sin(rad) * pointsRadius);

                vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                vb.pos(-half, -half, 0.0).tex(0.0, 0.0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha).endVertex();
                vb.pos(-half, half, 0.0).tex(0.0, 1.0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha).endVertex();
                vb.pos(half, half, 0.0).tex(1.0, 1.0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha).endVertex();
                vb.pos(half, -half, 0.0).tex(1.0, 0.0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha).endVertex();
                tess.draw();

                GlStateManager.popMatrix();
                i += step;
            }

            flip = !flip;
            layerYOffset += 0.45;
        }

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private void drawImageMark(
            EntityLivingBase target,
            ResourceLocation texture,
            Color color1,
            Color color2,
            Color color3,
            Color color4,
            float scale,
            float xOffset,
            float yOffset,
            boolean additive,
            boolean spin,
            float spinSpeed,
            boolean billboard,
            boolean hurt
    ) {
        float partial = this.getPartialTicks();
        double x = this.interpolate(target.posX, target.lastTickPosX, partial) - this.getRenderPosX() + xOffset;
        double y = this.interpolate(target.posY, target.lastTickPosY, partial) - this.getRenderPosY() + target.height / 1.6F + yOffset;
        double z = this.interpolate(target.posZ, target.lastTickPosZ, partial) - this.getRenderPosZ();

        float alphaMul = (hurt && target.hurtTime > 3) ? 1.0F : 0.9F;
        int a1 = Math.max(0, Math.min(255, (int) (color1.getAlpha() * alphaMul)));
        int a2 = Math.max(0, Math.min(255, (int) (color2.getAlpha() * alphaMul)));
        int a3 = Math.max(0, Math.min(255, (int) (color3.getAlpha() * alphaMul)));
        int a4 = Math.max(0, Math.min(255, (int) (color4.getAlpha() * alphaMul)));

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        if (billboard) {
            GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        }

        if (spin) {
            long now = System.currentTimeMillis();
            float angle = ((now % 10000L) / 10000.0F) * 360.0F * spinSpeed;
            GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
        }

        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        if (additive) {
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        } else {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GlStateManager.disableAlpha();

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.enableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer vb = tess.getWorldRenderer();
        double half = scale / 2.0F;

        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vb.pos(-half, -half, 0.0).tex(0.0, 0.0).color(color1.getRed(), color1.getGreen(), color1.getBlue(), a1).endVertex();
        vb.pos(-half, half, 0.0).tex(0.0, 1.0).color(color2.getRed(), color2.getGreen(), color2.getBlue(), a2).endVertex();
        vb.pos(half, half, 0.0).tex(1.0, 1.0).color(color3.getRed(), color3.getGreen(), color3.getBlue(), a3).endVertex();
        vb.pos(half, -half, 0.0).tex(1.0, 0.0).color(color4.getRed(), color4.getGreen(), color4.getBlue(), a4).endVertex();
        tess.draw();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private void drawEntityBox(Entity entity, Color color, boolean outline) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.enableGlCap(GL11.GL_BLEND);
        this.disableGlCap(GL11.GL_TEXTURE_2D, GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        float partial = this.getPartialTicks();
        double x = this.interpolate(entity.posX, entity.lastTickPosX, partial) - this.getRenderPosX();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, partial) - this.getRenderPosY();
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, partial) - this.getRenderPosZ();

        AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                entityBox.minX - entity.posX + x - 0.05,
                entityBox.minY - entity.posY + y,
                entityBox.minZ - entity.posZ + z - 0.05,
                entityBox.maxX - entity.posX + x + 0.05,
                entityBox.maxY - entity.posY + y + 0.15,
                entityBox.maxZ - entity.posZ + z + 0.05
        );

        if (outline) {
            GL11.glLineWidth(1.0F);
            this.enableGlCap(GL11.GL_LINE_SMOOTH);
            this.glColor(color.getRed(), color.getGreen(), color.getBlue(), 95);
            this.drawSelectionBoundingBox(axisAlignedBB);
        }

        this.glColor(color.getRed(), color.getGreen(), color.getBlue(), outline ? 26 : 35);
        this.drawFilledBox(axisAlignedBB);
        this.resetColor();
        GL11.glDepthMask(true);
        this.resetCaps();
    }

    private void drawAxisAlignedBB(AxisAlignedBB axisAlignedBB, Color color) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        this.glColor(color);
        this.drawFilledBox(axisAlignedBB);
        this.resetColor();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawPlatform(double y, Color color, double size) {
        double renderY = y - this.getRenderPosY();
        double min = Math.min(size, -size);
        double max = Math.max(size, -size);
        this.drawAxisAlignedBB(new AxisAlignedBB(min, renderY, min, max, renderY + 0.02, max), color);
    }

    private void drawPlatformESP(Entity entity, Color color) {
        float partial = this.getPartialTicks();
        AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox()
                .offset(-entity.posX, -entity.posY, -entity.posZ)
                .offset(
                        this.interpolate(entity.posX, entity.lastTickPosX, partial) - this.getRenderPosX(),
                        this.interpolate(entity.posY, entity.lastTickPosY, partial) - this.getRenderPosY(),
                        this.interpolate(entity.posZ, entity.lastTickPosZ, partial) - this.getRenderPosZ()
                );

        this.drawAxisAlignedBB(new AxisAlignedBB(
                axisAlignedBB.minX,
                axisAlignedBB.maxY - 0.5,
                axisAlignedBB.minZ,
                axisAlignedBB.maxX,
                axisAlignedBB.maxY + 0.2,
                axisAlignedBB.maxZ
        ), color);
    }

    private void drawPlatform(Entity entity, Color color) {
        float partial = this.getPartialTicks();
        double deltaX = this.interpolate(entity.posX, entity.lastTickPosX, partial) - this.getRenderPosX();
        double deltaY = this.interpolate(entity.posY, entity.lastTickPosY, partial) - this.getRenderPosY();
        double deltaZ = this.interpolate(entity.posZ, entity.lastTickPosZ, partial) - this.getRenderPosZ();

        AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox()
                .offset(-entity.posX + deltaX, -entity.posY + deltaY, -entity.posZ + deltaZ);

        this.drawAxisAlignedBB(new AxisAlignedBB(
                axisAlignedBB.minX,
                axisAlignedBB.maxY + 0.2,
                axisAlignedBB.minZ,
                axisAlignedBB.maxX,
                axisAlignedBB.maxY + 0.26,
                axisAlignedBB.maxZ
        ), color);
    }

    private void enableSmoothLine(float width) {
        GL11.glDisable(3008);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2884);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glLineWidth(width);
    }

    private void disableSmoothLine() {
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3008);
        GL11.glDepthMask(true);
        GL11.glCullFace(1029);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glHint(3155, 4352);
    }

    private void startSmooth() {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
    }

    private void endSmooth() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
    }

    private void drawCrystal(EntityLivingBase entity, int color, Render3DEvent event) {
        double x = this.interpolate(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - this.getRenderPosX();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - this.getRenderPosY();
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - this.getRenderPosZ();
        float radius = 0.15F;
        int side = 4;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y + 2.0, z);
        GL11.glRotatef(-entity.width, 0.0F, 1.0F, 0.0F);

        this.glColor(color);
        this.enableSmoothLine(1.5F);

        Cylinder c = new Cylinder();
        GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
        c.setDrawStyle(100012);
        this.glColor((entity.hurtTime <= 0) ? new Color(80, 255, 80, 200) : new Color(255, 0, 0, 200));
        c.draw(0.0F, radius, 0.3F, side, 1);
        c.setDrawStyle(100012);

        GL11.glTranslated(0.0, 0.0, 0.3);
        c.draw(radius, 0.0F, 0.3F, side, 1);

        GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
        c.setDrawStyle(100011);

        GL11.glTranslated(0.0, 0.0, -0.3);
        this.glColor(color);
        c.draw(0.0F, radius, 0.3F, side, 1);
        c.setDrawStyle(100011);

        GL11.glTranslated(0.0, 0.0, 0.3);
        c.draw(radius, 0.0F, 0.3F, side, 1);

        this.disableSmoothLine();
        GL11.glPopMatrix();
    }

    private void drawZavz(EntityLivingBase entity, Render3DEvent event, boolean dual) {
        float speed = 0.1F;
        float ticks = event.getPartialTicks();

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        this.startSmooth();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);

        double x = this.interpolate(entity.posX, entity.lastTickPosX, ticks) - this.getRenderPosX();
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, ticks) - this.getRenderPosZ();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, ticks) - this.getRenderPosY();

        double radius = 0.65;
        int precision = 360;

        double startPos = this.start % 360.0;
        this.start += speed;

        for (int i = 0; i <= precision; i++) {
            double posX = x + radius * Math.cos(startPos + i * DOUBLE_PI / (precision / 2.0));
            double posZ = z + radius * Math.sin(startPos + i * DOUBLE_PI / (precision / 2.0));
            double t = Math.abs(System.currentTimeMillis() / 10.0) / 100.0 + y;

            Color gradient = this.withAlpha(
                    this.getGradientOffset(new Color(this.colorPrimary.getValue()), new Color(this.colorSecondary.getValue()), t),
                    255
            );

            this.glColor(gradient.getRed(), gradient.getGreen(), gradient.getBlue(), gradient.getAlpha());
            GL11.glVertex3d(posX, y, posZ);
            y += entity.height / precision;
            this.glColor(0, 0, 0, 0);
        }

        GL11.glEnd();
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        this.endSmooth();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();

        if (dual) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            this.startSmooth();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            GL11.glBegin(GL11.GL_LINE_STRIP);

            startPos = this.start % 360.0;
            this.start += speed;
            y = this.interpolate(entity.posY, entity.lastTickPosY, ticks) - this.getRenderPosY() + entity.height;

            for (int i = 0; i <= precision; i++) {
                double posX = x + radius * Math.cos(-(startPos + i * DOUBLE_PI / (precision / 2.0)));
                double posZ = z + radius * Math.sin(-(startPos + i * DOUBLE_PI / (precision / 2.0)));
                double t2 = Math.abs(System.currentTimeMillis() / 10.0) / 100.0 + y;

                Color gradient = this.withAlpha(
                        this.getGradientOffset(new Color(this.colorPrimary.getValue()), new Color(this.colorSecondary.getValue()), t2),
                        255
                );

                this.glColor(gradient.getRed(), gradient.getGreen(), gradient.getBlue(), gradient.getAlpha());
                GL11.glVertex3d(posX, y, posZ);
                y -= entity.height / precision;
                this.glColor(0, 0, 0, 0);
            }

            GL11.glEnd();
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            this.endSmooth();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    private void drawJello(EntityLivingBase entity) {
        int drawTime = (int) (System.currentTimeMillis() % 2000L);
        boolean drawMode = drawTime > 1000;
        double drawPercent = drawTime / 1000.0;
        drawPercent = drawMode ? drawPercent - 1.0 : 1.0 - drawPercent;
        drawPercent = this.easeInOutQuadX(drawPercent);

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double radius = bb.maxX - bb.minX;
        double height = bb.maxY - bb.minY;
        float partial = this.getPartialTicks();
        double posX = this.interpolate(entity.posX, entity.lastTickPosX, partial);
        double posY = this.interpolate(entity.posY, entity.lastTickPosY, partial);
        posY += drawMode ? -0.5 : 0.5;
        double posZ = this.interpolate(entity.posZ, entity.lastTickPosZ, partial);

        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i <= 360; i += 7) {
            points.add(new Vec3(
                    posX - Math.sin(i * Math.PI / 180.0F) * radius,
                    posY + height * drawPercent,
                    posZ + Math.cos(i * Math.PI / 180.0F) * radius
            ));
        }
        points.add(points.get(0));

        mc.entityRenderer.disableLightmap();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBegin(GL11.GL_LINE_STRIP);

        double baseMove = drawPercent > 0.5 ? 1.0 - drawPercent : drawPercent;
        double min = (height / 60.0) * 20.0 * (1.0 - baseMove) * (drawMode ? -1.0 : 1.0);

        for (int i = 0; i <= 20; i++) {
            double moveFace = (height / 60.0F) * i * baseMove;
            if (drawMode) {
                moveFace = -moveFace;
            }

            Vec3 firstPoint = points.get(0);
            GL11.glVertex3d(
                    firstPoint.xCoord - this.getRenderPosX(),
                    firstPoint.yCoord - moveFace - min - this.getRenderPosY(),
                    firstPoint.zCoord - this.getRenderPosZ()
            );

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.7F * (i / 20.0F));
            for (Vec3 vec3 : points) {
                GL11.glVertex3d(
                        vec3.xCoord - this.getRenderPosX(),
                        vec3.yCoord - moveFace - min - this.getRenderPosY(),
                        vec3.zCoord - this.getRenderPosZ()
                );
            }
            GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.0F);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void drawFDP(EntityLivingBase entity, Render3DEvent event) {
        int themeTextColor = this.getThemeColor(1).getRGB();

        int drawTime = (int) (System.currentTimeMillis() % 1500L);
        boolean drawMode = drawTime > 750;
        double drawPercent = drawTime / 750.0;
        if (!drawMode) {
            drawPercent = 1.0 - drawPercent;
        } else {
            drawPercent -= 1.0;
        }
        drawPercent = this.easeInOutQuadX(drawPercent);

        mc.entityRenderer.disableLightmap();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double radius = ((bb.maxX - bb.minX) + (bb.maxZ - bb.minZ)) * 0.5F;
        double height = bb.maxY - bb.minY;
        double x = this.interpolate(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - this.getRenderPosX();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - this.getRenderPosY() + height * drawPercent;
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - this.getRenderPosZ();

        mc.entityRenderer.disableLightmap();
        GL11.glLineWidth((float) (radius * 8.0F));
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= 360; i += 10) {
            this.glColor(themeTextColor);
            GL11.glVertex3d(x - Math.sin(i * Math.PI / 180.0F) * radius, y, z + Math.cos(i * Math.PI / 180.0F) * radius);
        }
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void drawLies(EntityLivingBase entity, Render3DEvent event) {
        Color themeTextColor = this.getThemeColor(1);

        int everyTime = 3000;
        int drawTime = (int) (System.currentTimeMillis() % everyTime);
        boolean drawMode = drawTime > (everyTime / 2);
        double drawPercent = drawTime / (everyTime / 2.0);

        if (!drawMode) {
            drawPercent = 1.0 - drawPercent;
        } else {
            drawPercent -= 1.0;
        }
        drawPercent = this.easeInOutQuadX(drawPercent);

        mc.entityRenderer.disableLightmap();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(7425);
        mc.entityRenderer.disableLightmap();

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double radius = ((bb.maxX - bb.minX) + (bb.maxZ - bb.minZ)) * 0.5F;
        double height = bb.maxY - bb.minY;
        double x = this.interpolate(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - this.getRenderPosX();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - this.getRenderPosY() + height * drawPercent;
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - this.getRenderPosZ();
        double eased = (height / 3.0) * (drawPercent > 0.5 ? 1.0 - drawPercent : drawPercent) * (drawMode ? -1.0 : 1.0);

        for (int i = 5; i <= 360; i += 5) {
            double x1 = x - Math.sin(i * Math.PI / 180.0F) * radius;
            double z1 = z + Math.cos(i * Math.PI / 180.0F) * radius;
            double x2 = x - Math.sin((i - 5) * Math.PI / 180.0F) * radius;
            double z2 = z + Math.cos((i - 5) * Math.PI / 180.0F) * radius;

            GL11.glBegin(GL11.GL_QUADS);
            this.glFloatColor(themeTextColor, 0.0F);
            GL11.glVertex3d(x1, y + eased, z1);
            GL11.glVertex3d(x2, y + eased, z2);
            this.glFloatColor(themeTextColor, 150.0F);
            GL11.glVertex3d(x2, y, z2);
            GL11.glVertex3d(x1, y, z1);
            GL11.glEnd();
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(7424);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void drawEntityBoxESP(EntityLivingBase entity, Color color) {
        float partial = this.getPartialTicks();
        double x = this.interpolate(entity.posX, entity.lastTickPosX, partial) - this.getRenderPosX();
        double y = this.interpolate(entity.posY, entity.lastTickPosY, partial) - this.getRenderPosY();
        double z = this.interpolate(entity.posZ, entity.lastTickPosZ, partial) - this.getRenderPosZ();
        AxisAlignedBB entityBox = entity.getEntityBoundingBox();

        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                entityBox.minX - entity.posX + x - 0.05,
                entityBox.minY - entity.posY + y,
                entityBox.minZ - entity.posZ + z - 0.05,
                entityBox.maxX - entity.posX + x + 0.05,
                entityBox.maxY - entity.posY + y + 0.15,
                entityBox.maxZ - entity.posZ + z + 0.05
        );

        GlStateManager.pushMatrix();
        GL11.glBlendFunc(770, 771);
        this.enableGlCap(3042);
        this.disableGlCap(3553, 2929);
        GL11.glDepthMask(false);
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-entity.rotationYawHead, 0.0, 1.0, 0.0);
        GL11.glTranslated(-x, -y, -z);
        GL11.glLineWidth(3.0F);
        this.enableGlCap(2848);
        this.glColor(0, 0, 0, 255);
        RenderGlobal.drawSelectionBoundingBox(axisAlignedBB);
        GL11.glLineWidth(1.0F);
        this.enableGlCap(2848);
        this.glColor(color.getRed(), color.getGreen(), color.getBlue(), 255);
        RenderGlobal.drawSelectionBoundingBox(axisAlignedBB);
        this.resetColor();
        GL11.glDepthMask(true);
        this.resetCaps();
        GlStateManager.popMatrix();
    }

    private void drawSelectionBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);

        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();

        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();

        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();
    }

    private void drawFilledBox(AxisAlignedBB axisAlignedBB) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        tessellator.draw();
    }

    private void glColor(int red, int green, int blue, int alpha) {
        GL11.glColor4f(red / 255.0F, green / 255.0F, blue / 255.0F, alpha / 255.0F);
    }

    private void glColor(Color color) {
        this.glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private void glColor(int hex) {
        this.glColor((hex >>> 16) & 0xFF, (hex >>> 8) & 0xFF, hex & 0xFF, (hex >>> 24) & 0xFF);
    }

    private void glFloatColor(Color color, float alpha) {
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
    }

    private void resetColor() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void resetCaps() {
        for (Map.Entry<Integer, Boolean> entry : this.glCapMap.entrySet()) {
            this.setGlState(entry.getKey(), entry.getValue());
        }
    }

    private void enableGlCap(int cap) {
        this.setGlCap(cap, true);
    }

    private void enableGlCap(int... caps) {
        for (int cap : caps) {
            this.setGlCap(cap, true);
        }
    }

    private void disableGlCap(int... caps) {
        for (int cap : caps) {
            this.setGlCap(cap, false);
        }
    }

    private void setGlCap(int cap, boolean state) {
        this.glCapMap.put(cap, GL11.glIsEnabled(cap));
        this.setGlState(cap, state);
    }

    private void setGlState(int cap, boolean state) {
        if (state) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }

    private Color getGradientOffset(Color c1, Color c2, double offsetIn) {
        double offset = offsetIn;
        if (offset > 1.0) {
            double left = offset % 1.0;
            int off = (int) offset;
            offset = (off % 2 == 0) ? left : 1.0 - left;
        }

        double inv = 1.0 - offset;
        int r = (int) (c1.getRed() * inv + c2.getRed() * offset);
        int g = (int) (c1.getGreen() * inv + c2.getGreen() * offset);
        int b = (int) (c1.getBlue() * inv + c2.getBlue() * offset);
        int a = (int) (c1.getAlpha() * inv + c2.getAlpha() * offset);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));
        return new Color(r, g, b, a);
    }

    private Color getThemeColor(int offset) {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        if (hud == null) {
            return Color.WHITE;
        }
        return hud.getColor(System.currentTimeMillis(), offset);
    }

    private Color rainbowColor() {
        Color current = new Color(Color.HSBtoRGB((System.nanoTime() + 400000L) / 10000000000F % 1.0F, 1.0F, 1.0F));
        return new Color(current.getRed() / 255.0F, current.getGreen() / 255.0F, current.getBlue() / 255.0F, current.getAlpha() / 255.0F);
    }

    private Color withAlpha(Color color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private float breathe(float duration) {
        if (duration <= 0.0F) {
            return 0.0F;
        }
        float progress = (System.currentTimeMillis() % (long) duration) / duration;
        return 0.5F * (float) (Math.sin(2.0 * Math.PI * progress) + 1.0);
    }

    private double easeInOutQuadX(double x) {
        return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
    }

    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private double interpolate(double current, double previous, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private float getPartialTicks() {
        return ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
    }

    private double getRenderPosX() {
        return ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
    }

    private double getRenderPosY() {
        return ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
    }

    private double getRenderPosZ() {
        return ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
    }
}
