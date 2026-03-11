package fun.rich.display.hud;

import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.events.packet.PacketEvent;
import fun.rich.features.impl.render.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.awt.*;

public class Potions extends AbstractDraggable {
    private final List<Potion> list = new ArrayList<>();

    private static final RegistryEntry<StatusEffect>[] NEGATIVE_EFFECTS = new RegistryEntry[] {
            StatusEffects.POISON, StatusEffects.WITHER, StatusEffects.NAUSEA, StatusEffects.BLINDNESS,
            StatusEffects.HUNGER, StatusEffects.SLOWNESS, StatusEffects.MINING_FATIGUE, StatusEffects.INSTANT_DAMAGE,
            StatusEffects.WEAKNESS, StatusEffects.LEVITATION, StatusEffects.UNLUCK, StatusEffects.BAD_OMEN
    };
    private long lastEffectChange = 0;
    private RegistryEntry<StatusEffect> currentRandomEffect = StatusEffects.SPEED;

    public Potions() {
        super("Potions", 200, 40, 85, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        list.forEach(p -> p.effect.update(mc.player, null));
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEffectChange >= 1000) {
                List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
                for (Identifier id : Registries.STATUS_EFFECT.getIds()) {
                    Registries.STATUS_EFFECT.getEntry(id).ifPresent(effects::add);
                }
                if (!effects.isEmpty()) {
                    currentRandomEffect = effects.get(new Random().nextInt(effects.size()));
                    lastEffectChange = currentTime;
                }
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket effect -> {
                if (!PlayerInteractionHelper.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
                    RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                    list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectId.getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                    list.add(new Potion(new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), new Decelerate().setMs(150).setValue(1.0F)));
                }
            }
            case RemoveEntityStatusEffectS2CPacket effect -> list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effect.effect().getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
            case PlayerRespawnS2CPacket p -> list.clear();
            case GameJoinS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer iconFont = Fonts.getSize(17, Fonts.Type.ICONS);

        Hud hud = Hud.getInstance();
        int potionsAlpha = (int) hud.opacityPotions.getValue();

        if (hud.potionsBlur.isValue()) {
            blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                    .round(12).quality((int) hud.potionsBlurAmount.getValue()).color(new Color(0, 0, 0, 100).getRGB()).build());
        }
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(12)
                .thickness(0.5f)
                .outlineColor(new Color(45, 45, 45, 255).getRGB())
                .color(ColorAssist.setAlpha(hud.potionsColor.getColor(), potionsAlpha))
                .build());

        iconFont.drawString(matrix, "C", getX() + 8f, getY() + 9f, -1);
        font.drawString(matrix, getName(), getX() + 35, getY() + 6.5f, ColorAssist.getText());

        float sepW = getWidth() * 0.5f;
        float sepX = getX() + (getWidth() - sepW) / 2f;
        float sepY = getY() + 16;

        int c1 = ColorAssist.fade(8, 200, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        int c2 = ColorAssist.fade(8, 0, ColorAssist.getClientColor(), ColorAssist.getClientColor2());

        rectangle.render(ShapeProperties.create(matrix, sepX, sepY, sepW, 0.5f)
                .color(c1, c1, c2, c2).build());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 22;
        int maxWidth = 98;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            String name = "Example";
            String duration = "0:00";
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(currentRandomEffect), getX() + 4F, (int) centerY - 2, 8, 8, -1);
                fontPotion.drawString(matrix, name, getX() + 16, centerY + 1, ColorAssist.getText());
                fontPotion.drawString(matrix, duration, getX() + getWidth() - fontPotion.getStringWidth(duration) - 6, centerY + 1, -1);
            });
            maxWidth = Math.max((int) fontPotion.getStringWidth(name + duration) + 25, maxWidth);
            offset += 11;
        } else {
            for (Potion potion : list) {
                StatusEffectInstance effect = potion.effect;
                float animation = potion.anim.getOutput().floatValue();
                if (animation < 0.05) continue;

                float centerY = getY() + offset;
                String name = effect.getEffectType().value().getName().getString();
                String duration = getDuration(effect);
                int amplifier = effect.getAmplifier();

                boolean isBad = isBadEffect(effect.getEffectType());
                int alpha = (int)(255 * animation);

                if (effect.getDuration() <= 200 && effect.getDuration() > 0) {
                    alpha = (int) (155 + 100 * Math.sin(System.currentTimeMillis() / 100.0));
                }

                int color = ColorAssist.rgba(255, 255, 255, alpha);
                int nameColor = isBad ? ColorAssist.rgba(255, 80, 80, alpha) : ColorAssist.rgba(200, 200, 200, alpha);

                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), getX() + 4F, (int) centerY - 2, 8, 8, color);
                    String displayName = name + (amplifier > 0 ? " " + (amplifier + 1) : "");
                    fontPotion.drawString(matrix, displayName, getX() + 16, centerY + 1, nameColor);
                    float dWidth = fontPotion.getStringWidth(duration);
                    fontPotion.drawString(matrix, duration, getX() + getWidth() - dWidth - 6, centerY + 1, color);
                });

                maxWidth = Math.max((int) fontPotion.getStringWidth(name + duration) + 30, maxWidth);
                offset += (int) (8 * animation);
            }
        }

        setWidth(maxWidth);
        setHeight(offset + 3);
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return pe.isInfinite() || mins > 60 ? "**:**" : mins + ":" + String.format("%02d", (var1 % 1200) / 20);
    }

    private boolean isBadEffect(RegistryEntry<StatusEffect> effect) {
        for (RegistryEntry<StatusEffect> negativeEffect : NEGATIVE_EFFECTS) {
            if (effect == negativeEffect) return true;
        }
        return false;
    }

    private record Potion(StatusEffectInstance effect, fun.rich.common.animation.Animation anim) {}
}
