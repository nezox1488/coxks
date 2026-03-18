package fun.rich.display.hud;

import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.math.Animation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;

import java.awt.Color;

public class TargetHud extends AbstractDraggable {

    private final Animation openAnimation = new Animation(1.4f);
    private final Animation healthAnimation = new Animation(0.55f);
    private final Animation absorptionAnimation = new Animation(0.55f);

    private LivingEntity renderTarget = null;

    public TargetHud() {
        super("Target Hud", 100, 100, 110, 35, true);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        boolean isAuraActive = Aura.getInstance().isState();
        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;

        boolean canShow = (isAuraActive && auraTarget != null && auraTarget.isAlive()) || isChatOpen;

        openAnimation.setTarget(canShow ? 1.0 : 0.0);
        openAnimation.update();

        if (canShow) {
            renderTarget = (auraTarget != null && auraTarget.isAlive()) ? auraTarget : mc.player;
        } else if (openAnimation.getValue() < 0.01) {
            renderTarget = null;
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!fun.rich.features.impl.render.Hud.getInstance().interfaceSettings.isSelected("Target Hud") || !fun.rich.features.impl.render.Hud.getInstance().state) return;
        if (renderTarget == null || openAnimation.getValue() < 0.001) return;

        MatrixStack matrix = context.getMatrices();
        float alphaPC = (float) openAnimation.getValue();
        int alpha = (int) (alphaPC * 255);

        float scale = 0.95f + (alphaPC * 0.05f);

        matrix.push();
        matrix.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
        matrix.scale(scale, scale, 1);
        matrix.translate(-(getX() + getWidth() / 2f), -(getY() + getHeight() / 2f), 0);

        renderContent(context, matrix, alpha, alphaPC);

        matrix.pop();
    }

    private void renderContent(DrawContext context, MatrixStack matrix, int alpha, float alphaPC) {
        float hpValue = getScoreboardHealth(renderTarget);

        float health = renderTarget.getHealth();
        float absorption = renderTarget.getAbsorptionAmount();
        float maxHealth = renderTarget.getMaxHealth();
        if (maxHealth <= 0) maxHealth = 20f;

        boolean isInvisible = renderTarget.isInvisible();
        boolean isFunTime = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.toLowerCase().contains("funtime");
        boolean hpUnknown = isInvisible || hpValue >= 100;

        // Красная полоска — только здоровье (без поглощения), как в ванильном Minecraft
        float totalHpForBar;
        if (isFunTime && hpUnknown) {
            totalHpForBar = maxHealth;
        } else {
            totalHpForBar = (hpValue >= 100) ? health : hpValue;
        }
        float totalFill = MathHelper.clamp(totalHpForBar / maxHealth, 0f, 1f);
        // Золотая полоска — только при эффекте Absorption (гепл = I, чарка = II+)
        boolean hasAbsorptionEffect = renderTarget.hasStatusEffect(StatusEffects.ABSORPTION) && absorption > 0.001f;
        float absorptionFill = hasAbsorptionEffect ? MathHelper.clamp(absorption / maxHealth, 0f, 1f) : 0f;

        healthAnimation.setTarget(totalFill);
        healthAnimation.update();
        absorptionAnimation.setTarget(absorptionFill);
        absorptionAnimation.update();

        float currentHealthWidth = (float) healthAnimation.getValue();
        float currentAbsorptionWidth = (float) absorptionAnimation.getValue();

        fun.rich.features.impl.render.Hud hud = fun.rich.features.impl.render.Hud.getInstance();
        int thAlpha = (int) (hud.opacityTargetHud.getValue() * alphaPC);

        if (hud.thBlur.isValue()) {
            blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                    .round(6).quality((int) hud.thBlurAmount.getValue()).color(new Color(0, 0, 0, 100).getRGB()).build());
        }

        int hudColor = hud.targetHudColor.getColor();
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(6).color(ColorAssist.setAlpha(hudColor, thAlpha)).build());

        drawArmor(context, getX() + 5, getY() + getHeight() - 2, alphaPC);

        Identifier skin = (renderTarget instanceof AbstractClientPlayerEntity p)
                ? p.getSkinTextures().texture() : Identifier.ofVanilla("textures/entity/steve.png");

        int skinOverlay = (renderTarget.hurtTime > 0)
                ? ColorAssist.setAlpha(new Color(255, 60, 60).getRGB(), (int) (180 * alphaPC))
                : ColorAssist.setAlpha(-1, alpha);

        Render2D.drawTexture(context, skin, getX() + 5, getY() + 5, 25f, 5f, 8, 8, 64,
                ColorAssist.setAlpha(new Color(30, 30, 30).getRGB(), alpha), skinOverlay);

        String hpString = (isInvisible || hpValue >= 100) ? "HP: Неизвестно" : String.format("%.1f HP", hpValue);
        Fonts.getSize(14, Fonts.Type.REGULAR).drawString(matrix, renderTarget.getName().getString(), getX() + 35, getY() + 6, ColorAssist.setAlpha(-1, alpha));
        Fonts.getSize(11, Fonts.Type.SEMI).drawString(matrix, hpString, getX() + 35, getY() + 16, ColorAssist.setAlpha(-1, alpha));

        float barX = getX() + 35;
        float barY = getY() + 26;
        float barW = getWidth() - 40;

        rectangle.render(ShapeProperties.create(matrix, barX, barY, barW, 3.2f).round(1.5f)
                .color(ColorAssist.setAlpha(new Color(25, 25, 25).getRGB(), alpha)).build());

        if (currentHealthWidth > 0.001f) {
            int c1 = ColorAssist.setAlpha(ColorAssist.fade(8, 200, ColorAssist.getClientColor(), ColorAssist.getClientColor2()), alpha);
            int c2 = ColorAssist.setAlpha(ColorAssist.fade(8, 0, ColorAssist.getClientColor(), ColorAssist.getClientColor2()), alpha);
            rectangle.render(ShapeProperties.create(matrix, barX, barY, barW * currentHealthWidth, 3.2f)
                    .round(1.5f).color(c1, c1, c2, c2).build());
        }

        if (currentAbsorptionWidth > 0.001f) {
            int gold = ColorAssist.setAlpha(new Color(255, 215, 0).getRGB(), alpha);
            float absX = barX + barW * (1f - currentAbsorptionWidth);
            rectangle.render(ShapeProperties.create(matrix, absX, barY, barW * currentAbsorptionWidth, 3.2f)
                    .round(1.5f).color(gold, gold, gold, gold).build());
        }
    }

    private float getScoreboardHealth(LivingEntity entity) {
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            try {
                Scoreboard scoreboard = player.getScoreboard();
                ScoreboardObjective obj = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (obj != null) {
                    ReadableScoreboardScore score = scoreboard.getScore(player, obj);
                    if (score != null) {
                        String text = ReadableScoreboardScore.getFormattedScore(score, obj.getNumberFormatOr(StyledNumberFormat.EMPTY)).getString();
                        return Float.parseFloat(text.replaceAll("[^0-9.]", ""));
                    }
                }
            } catch (Exception ignored) {}
        }
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private void drawArmor(DrawContext context, float x, float y, float alpha) {
        float slotSize = 12f;
        float gap = 2f;
        net.minecraft.entity.EquipmentSlot[] armorOrder = { net.minecraft.entity.EquipmentSlot.FEET, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.HEAD };
        for (net.minecraft.entity.EquipmentSlot slot : armorOrder) {
            ItemStack stack = renderTarget.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                Render2D.defaultDrawStack(context, stack, x, y, false, false, 0.65f * alpha);
            }
            if (!stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0) {
                float durability = 1f - (float) stack.getDamage() / stack.getMaxDamage();
                float barW = slotSize - 2;
                float barY = y + slotSize - 0.5f;
                rectangle.render(ShapeProperties.create(context.getMatrices(), x + 1, barY, barW, 1.2f)
                        .round(0.6f).color(ColorAssist.setAlpha(new Color(25, 25, 25).getRGB(), (int)(200 * alpha))).build());
                if (durability > 0.001f) {
                    int color = getDurabilityColor(durability);
                    rectangle.render(ShapeProperties.create(context.getMatrices(), x + 1, barY, barW * MathHelper.clamp(durability, 0f, 1f), 1.2f)
                            .round(0.6f).color(ColorAssist.setAlpha(color, (int)(255 * alpha))).build());
                }
            }
            x += slotSize + gap;
        }
        float handY = y;
        ItemStack mainHand = renderTarget.getMainHandStack();
        if (!mainHand.isEmpty()) {
            Render2D.defaultDrawStack(context, mainHand, x + 2, handY + 2, false, false, 0.65f * alpha);
            x += slotSize + gap;
        }
        ItemStack offHand = renderTarget.getOffHandStack();
        if (!offHand.isEmpty()) {
            Render2D.defaultDrawStack(context, offHand, x + 2, handY + 2, false, false, 0.65f * alpha);
        }
    }

    /** Цвет полоски прочности как в ванильном Minecraft */
    private static int getDurabilityColor(float durability) {
        if (durability > 0.6f) return new Color(85, 255, 85).getRGB();
        if (durability > 0.4f) return new Color(255, 255, 85).getRGB();
        if (durability > 0.2f) return new Color(255, 165, 0).getRGB();
        return new Color(255, 85, 85).getRGB();
    }
}
