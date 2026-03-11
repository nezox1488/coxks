package fun.rich.display.hud;

import fun.rich.common.animation.implement.OutBack;
import fun.rich.utils.display.color.ColorAssist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Formatting;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.client.sound.SoundManager;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.client.Instance;
import fun.rich.events.container.SetScreenEvent;
import fun.rich.events.packet.PacketEvent;
import fun.rich.features.impl.render.Hud;
import fun.rich.utils.client.managers.localization.LocalizationManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Notifications extends AbstractDraggable {

    @Override
    protected int getOutlinePadX() { return 0; }

    @Override
    protected int getOutlineWidthReduce() { return 10; }

    @Override
    protected int getOutlineOffsetY() { return 1; }
    public static Notifications getInstance() {
        return Instance.getDraggable(Notifications.class);
    }

    private final List<Notification> list = new ArrayList<>();
    private final List<Stack> stacks = new ArrayList<>();
    private long lastLowArmorNotifTime = 0;
    private static final long LOW_ARMOR_COOLDOWN_MS = 30_000;
    private String lastTotemLossPlayer = null;
    private long lastTotemLossTime = 0;
    private static final long TOTEM_GAPPLE_DEBOUNCE_MS = 2000;
    private static final String INFO_ICON_TEXTURE = "textures/features/infoicon.png";
    private static final int INFO_ICON_SIZE = 10;
    private static final int INFO_ICON_GAP = 3;

    public Notifications() {
        super("Notifications", 0, 0, 100, 15, true);
    }

    @Override
    public void tick() {
        if (getX() == 0 && getY() == 0) {
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            setX(w / 2 - 50);
            setY(h / 2 + 100);
        }
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime || (notif.text.getString().contains("Hi I'm a notification") && !PlayerInteractionHelper.isChat(mc.currentScreen)))
                notif.anim.setDirection(Direction.BACKWARDS);
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));
        if (Hud.getInstance().notificationSettings.isSelected("Low Armor Durability") && mc.player != null && System.currentTimeMillis() - lastLowArmorNotifTime > LOW_ARMOR_COOLDOWN_MS) {
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (!stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0) {
                    int remaining = stack.getMaxDamage() - stack.getDamage();
                    if (remaining <= 100) {
                        lastLowArmorNotifTime = System.currentTimeMillis();
                        String msg = LocalizationManager.getInstance().translate("notif.lowArmor");
                        addList(Text.literal(msg).formatted(Formatting.RED), 5000);
                        break;
                    }
                }
            }
        }
        while (!stacks.isEmpty()) {
            addTextIfNotEmpty(TypePickUp.INVENTORY, LocalizationManager.getInstance().translate("notif.pickupItems"));
            addTextIfNotEmpty(TypePickUp.SHULKER_INVENTORY, LocalizationManager.getInstance().translate("notif.pickupShulkerInventory"));
            addTextIfNotEmpty(TypePickUp.SHULKER, LocalizationManager.getInstance().translate("notif.pickupShulker"));
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (!PlayerInteractionHelper.nullCheck()) switch (e.getPacket()) {
            case ItemPickupAnimationS2CPacket item when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") && item.getCollectorEntityId() == Objects.requireNonNull(mc.player).getId() && Objects.requireNonNull(mc.world).getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                ItemStack itemStack = entity.getStack();
                ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
                if (component == null) {
                    Text itemText = itemStack.getName();
                    if (itemText.getContent().toString().equals("empty")) {
                        MutableText text = Text.empty().append(itemText);
                        if (itemStack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + itemStack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                        stacks.add(new Stack(TypePickUp.INVENTORY, text));
                    }
                } else component.stream().filter(s -> s.getName().getContent().toString().equals("empty")).forEach(stack -> {
                    MutableText text = Text.empty().append(stack.getName());
                    if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                    stacks.add(new Stack(TypePickUp.SHULKER, text));
                });
            }
            case EntityStatusS2CPacket status when Hud.getInstance().notificationSettings.isSelected("Totem Loss") && status.getStatus() == 35 -> {
                if (mc.world != null) {
                    Entity entity = status.getEntity(mc.world);
                    if (entity instanceof PlayerEntity player) {
                        ItemStack main = player.getMainHandStack();
                        ItemStack off = player.getOffHandStack();
                        ItemStack totem = main.getItem() == Items.TOTEM_OF_UNDYING ? main : (off.getItem() == Items.TOTEM_OF_UNDYING ? off : null);
                        boolean enchanted = totem != null && totem.hasEnchantments();
                        addTotemLossNotification(player.getName().getString(), enchanted);
                    }
                }
            }
            case EntityStatusEffectS2CPacket effect when Hud.getInstance().notificationSettings.isSelected("Gapple") -> {
                RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                if (effectId.matchesKey(StatusEffects.ABSORPTION.getKey().get()) && Objects.requireNonNull(mc.world).getEntityById(effect.getEntityId()) instanceof PlayerEntity player) {
                    int amp = effect.getAmplifier();
                    boolean enchanted = amp >= 1;
                    addGappleNotification(player.getName().getString(), enchanted);
                }
            }
            case ScreenHandlerSlotUpdateS2CPacket slot when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") -> {
                int slotId = slot.getSlot();
                ContainerComponent updatedContainer = slot.getStack().get(DataComponentTypes.CONTAINER);
                if (updatedContainer != null && slotId < Objects.requireNonNull(mc.player).currentScreenHandler.slots.size() && slot.getSyncId() == 0) {
                    ContainerComponent currentContainer = mc.player.currentScreenHandler.getSlot(slotId).getStack().get(DataComponentTypes.CONTAINER);
                    if (currentContainer != null) updatedContainer.stream().filter(stack -> currentContainer.stream().noneMatch(s -> Objects.equals(s.getComponents(), stack.getComponents()) && s.toString().equals(stack.toString()))).forEach(stack -> {
                        MutableText text = Text.empty().append(stack.getName());
                        stacks.add(new Stack(TypePickUp.SHULKER_INVENTORY, text));
                    });
                }
            }
            default -> {}
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        super.setScreen(e);
        if (e.getScreen() instanceof ChatScreen) {
            addList("Hi I'm a notification", 99999999);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(12, Fonts.Type.DEFAULT);

        float offsetY = 0;
        float offsetX = 5;
        int notifAlpha = (int) Hud.getInstance().opacityNotifications.getValue();
        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            String rawText = notification.text.getString();
            float fullTextW = font.getStringWidth(rawText);
            Text toDraw;
            String displayStr;
            if (fullTextW > MAX_TEXT_WIDTH) {
                displayStr = truncateToWidth(font, rawText, MAX_TEXT_WIDTH - font.getStringWidth("...")) + "...";
                toDraw = Text.literal(displayStr);
            } else {
                displayStr = rawText;
                toDraw = notification.text;
            }
            float finalTextW = font.getStringWidth(displayStr);
            float iconArea = INFO_ICON_SIZE + INFO_ICON_GAP;
            float finalBoxWidth = Math.min(finalTextW + offsetX * 2 + 12 + iconArea, 230);
            float startY = this.getY() + offsetY;
            float finalStartX = this.getX() + (100 - finalBoxWidth) / 2;
            float iconX = finalStartX + offsetX;
            float iconY = startY + (getHeight() + 2 - INFO_ICON_SIZE) / 2f;
            int iconColor = ColorAssist.setAlpha(new Color(64, 128, 255).getRGB(), notifAlpha);
            Calculate.setAlpha(anim, () -> {

                blur.render(ShapeProperties.create(matrix, finalStartX, startY, finalBoxWidth, getHeight() + 2).round(6).quality(12)
                        .color(new Color(0, 0, 0, notifAlpha).getRGB())
                        .build());

                rectangle.render(ShapeProperties.create(matrix, finalStartX, startY, finalBoxWidth, getHeight() + 2).round(6)
                        .thickness(0.1f)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(
                                new Color(18, 19, 20, notifAlpha).getRGB(),
                                new Color(0, 2, 5, notifAlpha).getRGB(),
                                new Color(0, 2, 5, notifAlpha).getRGB(),
                                new Color(18, 19, 20, notifAlpha).getRGB())
                        .build());

                // Иконка слева через image.getTexture
                image.setTexture(INFO_ICON_TEXTURE).setRotate(true)
                        .render(ShapeProperties.create(matrix, iconX, iconY, INFO_ICON_SIZE, INFO_ICON_SIZE).color(iconColor).build());
                font.drawText(matrix, toDraw, (int) (finalStartX + offsetX + iconArea) + 6, startY + 7F);
            });
            offsetY += (getHeight() + 3) * anim;
        }
    }

    private static String truncateToWidth(FontRenderer font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i);
            if (font.getStringWidth(sub) <= maxWidth) return sub;
        }
        return text.length() > 0 ? text.substring(0, 1) : "";
    }

    private static final int MAX_PICKUP_ITEMS = 6;
    private static final int MAX_PICKUP_TEXT_LEN = 120;
    private static final int MAX_NOTIFICATIONS = 5;
    private static final int MAX_TEXT_WIDTH = 200;

    private void addTextIfNotEmpty(TypePickUp type, String prefix) {
        List<Stack> ofType = stacks.stream().filter(s -> s.type.equals(type)).toList();
        int total = ofType.size();
        if (total == 0) return;
        ofType.forEach(stacks::remove);
        MutableText text = Text.empty();
        int count = 0;
        for (Stack stack : ofType) {
            if (count >= MAX_PICKUP_ITEMS || text.getString().length() > MAX_PICKUP_TEXT_LEN) break;
            if (count > 0) text.append(" , ");
            text.append(stack.text);
            count++;
        }
        if (total > MAX_PICKUP_ITEMS) text.append(Formatting.GRAY + " и ещё " + (total - MAX_PICKUP_ITEMS) + " шт.");
        addList(Text.empty().append(prefix).append(text), 6000);
    }

    public void addList(String text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(Text text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(String text, long removeTime, SoundEvent sound) {
        addList(Text.empty().append(text), removeTime, sound);
    }

    public void addList(Text text, long removeTime, SoundEvent sound) {
        long now = System.currentTimeMillis();
        if (list.size() >= MAX_NOTIFICATIONS) {
            list.stream().min(Comparator.comparingLong(n -> n.startTime))
                    .ifPresent(oldest -> oldest.anim.setDirection(Direction.BACKWARDS));
        }
        list.add(new Notification(text, new OutBack().setMs(400).setValue(1), now, now + removeTime));
        list.sort(Comparator.comparingLong(Notification::startTime).reversed());
        if (sound != null) SoundManager.playSound(sound);
    }

    /** Игрок (ник) Потерял Тотем бесмертия — белый текст, "Тотем бесмертия" красный если не зачарован, зелёный если зачарован */
    public void addTotemLossNotification(String playerName, boolean enchanted) {
        lastTotemLossPlayer = playerName;
        lastTotemLossTime = System.currentTimeMillis();
        MutableText text = Text.literal("Игрок (" + playerName + ") Потерял ").formatted(Formatting.WHITE)
                .append(Text.literal("Тотем бесмертия").formatted(enchanted ? Formatting.GREEN : Formatting.RED));
        addList(text, 5000);
    }

    /** Игрок (ник) Съел Золотое Зачарованное Яблоко! — белый текст, название яблока золотое. Не показывать если только что потерял тотем ( Absorption от тотема ) */
    public void addGappleNotification(String playerName, boolean enchanted) {
        if (playerName != null && playerName.equals(lastTotemLossPlayer) && System.currentTimeMillis() - lastTotemLossTime < TOTEM_GAPPLE_DEBOUNCE_MS) return;
        MutableText text = Text.literal("Игрок (" + playerName + ") Съел ").formatted(Formatting.WHITE)
                .append(Text.literal(enchanted ? "Зачарованное Золотое Яблоко" : "Золотое Яблоко").formatted(Formatting.GOLD))
                .append(Text.literal("!").formatted(Formatting.WHITE));
        addList(text, 5000);
    }

    public record Notification(Text text, Animation anim, long startTime, long removeTime) {
        public boolean isExpired() {
            return System.currentTimeMillis() > removeTime;
        }
    }

    public record Stack(TypePickUp type, MutableText text) {}

    public enum TypePickUp {
        INVENTORY, SHULKER, SHULKER_INVENTORY
    }
}