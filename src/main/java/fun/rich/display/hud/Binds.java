package fun.rich.display.hud;

import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryTask;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.features.impl.misc.ServerHelper;
import fun.rich.features.impl.render.Hud;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.client.chat.StringHelper;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.events.container.SetScreenEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Binds extends AbstractDraggable {
    private final ServerHelper serverHelper;
    private final List<BindInfo> binds = new ArrayList<>();
    private final Map<Item, Long> cooldownStartTimes = new HashMap<>();
    private final Set<Item> activeCooldowns = new HashSet<>();
    private static final int BINDS_PER_ROW = 5;
    private float width;
    private float height;
    private long lastBindChange = 0;
    private java.lang.String currentRandomKey1 = "None";
    private java.lang.String currentRandomKey2 = "None";
    private java.lang.String currentRandomKey3 = "None";
    private int currentItemIndex1 = 0;
    private int currentItemIndex2 = 1;
    private int currentItemIndex3 = 2;
    private static final Item[] EXAMPLE_ITEMS = {Items.ENDER_EYE, Items.SUGAR, Items.DRIED_KELP, Items.NETHERITE_SCRAP};
    private final Animation animation = new Decelerate().setMs(300).setValue(1);

    private static class BindInfo {
        ServerHelper.KeyBind keyBind;
        java.lang.String displayName;
        int color;
        java.lang.String searchName;

        BindInfo(ServerHelper.KeyBind keyBind, java.lang.String displayName, int color, java.lang.String searchName) {
            this.keyBind = keyBind;
            this.displayName = displayName;
            this.color = color;
            this.searchName = searchName;
        }
    }

    public Binds() {
        super("Binds", 10, 180, 150, 40, true);
        this.serverHelper = ServerHelper.getInstance();
        initializeBinds();
    }

    private void initializeBinds() {
        for (ServerHelper.KeyBind keyBind : serverHelper.getKeyBindings()) {
            java.lang.String name = keyBind.setting().getName();
            java.lang.String searchName = getSearchNameForBind(name);
            int color = getColorForBind(name);
            binds.add(new BindInfo(keyBind, name, color, searchName));
        }
    }

    private java.lang.String getSearchNameForBind(java.lang.String name) {
        return switch (name) {
            case "Зелье отрыжки" -> "отрыжки";
            case "Зелье серной кислоты" -> "серная";
            case "Зелье вспышки" -> "вспышка";
            case "Зелье мочи Флеша" -> "моча флеша";
            case "Зелье победителя" -> "победителя";
            case "Зелье агента" -> "агента";
            case "Зелье медика" -> "медика";
            case "Зелье киллера" -> "киллера";
            default -> null;
        };
    }

    private int getColorForBind(java.lang.String name) {
        return switch (name) {
            case "Зелье отрыжки" -> 0xFF5D00;
            case "Зелье серной кислоты" -> 0x00C200;
            case "Зелье вспышки" -> 0xFFFFFF;
            case "Зелье мочи Флеша" -> 0x5CF7FF;
            case "Зелье победителя" -> 0x00FF00;
            case "Зелье агента" -> 0xFFFB00;
            case "Зелье медика" -> 0xFF00DE;
            case "Зелье киллера" -> 0xFF0000;
            default -> -1;
        };
    }

    private ItemStack createColoredPotion(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        if (color != -1 && item == Items.SPLASH_POTION) {
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
        }
        return stack;
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Binds") && Hud.getInstance().state && !animation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (PlayerInteractionHelper.nullCheck()) {
            animation.setDirection(Direction.BACKWARDS);
            return;
        }
        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).toList();
        if (!activeBinds.isEmpty()) {
            animation.setDirection(Direction.FORWARDS);
        } else if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
            animation.setDirection(Direction.FORWARDS);
        } else {
            animation.setDirection(Direction.BACKWARDS);
        }
        activeCooldowns.clear();
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isEmpty() && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                Item item = itemStack.getItem();
                if (!cooldownStartTimes.containsKey(item)) {
                    cooldownStartTimes.put(item, currentTime);
                }
                activeCooldowns.add(item);
            }
        }
        cooldownStartTimes.keySet().removeIf(item -> !activeCooldowns.contains(item));
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            if (currentTime - lastBindChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey1 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey2 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey3 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentItemIndex1 = (currentItemIndex1 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex2 = (currentItemIndex2 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex3 = (currentItemIndex3 + 1) % EXAMPLE_ITEMS.length;
                lastBindChange = currentTime;
            }
        }
    }

    private boolean isBindActive(BindInfo bind) {
        if (PlayerInteractionHelper.nullCheck() || !bind.keyBind.setting().isVisible() || bind.keyBind.setting().getKey() == -1 || bind.keyBind.setting().getKey() == 0) {
            return false;
        }
        if (bind.searchName != null && bind.keyBind.item() == Items.SPLASH_POTION) {
            return InventoryTask.getSlot(s -> s.getStack().getItem() == bind.keyBind.item() && InventoryTask.getCleanName(s.getStack().getName()).contains(bind.searchName.toLowerCase())) != null && !activeCooldowns.contains(bind.keyBind.item());
        }
        return InventoryTask.getSlot(s -> s.getStack().getItem() == bind.keyBind.item()) != null && !activeCooldowns.contains(bind.keyBind.item());
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        super.setScreen(e);
        if (PlayerInteractionHelper.nullCheck()) {
            animation.setDirection(Direction.BACKWARDS);
            return;
        }
        if (PlayerInteractionHelper.isChat(e.getScreen())) {
            animation.setDirection(Direction.FORWARDS);
        } else {
            List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).toList();
            if (!activeBinds.isEmpty()) {
                animation.setDirection(Direction.FORWARDS);
            } else {
                animation.setDirection(Direction.BACKWARDS);
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!Hud.getInstance().interfaceSettings.isSelected("Binds") || !Hud.getInstance().state || animation.isFinished(Direction.BACKWARDS)) return;
        MatrixStack matrix = context.getMatrices();
        float animationValue = animation.getOutput().floatValue();
        if (animationValue <= 0) return;
        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).collect(Collectors.toList());
        float padding = 2;
        float iconSize = 12;
        float spacing = 3;
        float rowHeight = iconSize + 2 * padding;
        float posX = getX();
        float posY = getY();
        int totalBinds = activeBinds.size();
        int rowCount = (int) Math.ceil((double) totalBinds / BINDS_PER_ROW);
        List<Float> rowWidths = new ArrayList<>();
        float firstRowWidth = 0;
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            rowCount = 1;
            java.lang.String name = "Example Bind";
            java.lang.String[] keys = {currentRandomKey1, currentRandomKey2, currentRandomKey3};
            float textWidth = font.getStringWidth(Arrays.stream(keys).max((a, b) -> Float.compare(font.getStringWidth(a), font.getStringWidth(b))).orElse(""));
            float bindWidth = iconSize + padding + textWidth + padding;
            float totalWidth = bindWidth * 3 + spacing * 2;
            rowWidths.add(totalWidth);
            firstRowWidth = totalWidth;
        } else {
            for (int row = 0; row < rowCount; row++) {
                float currentX = padding;
                int bindsInThisRow = Math.min(BINDS_PER_ROW, totalBinds - row * BINDS_PER_ROW);
                for (int i = 0; i < bindsInThisRow; i++) {
                    int bindIndex = row * BINDS_PER_ROW + i;
                    BindInfo bind = activeBinds.get(bindIndex);
                    java.lang.String keyName = StringHelper.getBindName(bind.keyBind.setting().getKey());
                    float textWidth = font.getStringWidth(keyName);
                    float bindWidth = iconSize + padding + textWidth + padding;
                    currentX += bindWidth + spacing;
                }
                if (bindsInThisRow > 0) {
                    currentX -= spacing;
                }
                rowWidths.add(currentX);
                if (row == 0) {
                    firstRowWidth = currentX;
                }
            }
        }
        width = firstRowWidth + padding;
        height = rowHeight * rowCount;
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            java.lang.String name = "Example Bind";
            java.lang.String[] keys = {currentRandomKey1, currentRandomKey2, currentRandomKey3};
            ItemStack[] stacks = {new ItemStack(EXAMPLE_ITEMS[currentItemIndex1]), new ItemStack(EXAMPLE_ITEMS[currentItemIndex2]), new ItemStack(EXAMPLE_ITEMS[currentItemIndex3])};
            float offsetX = padding;
            float currentY = posY;
            for (int i = 0; i < 3; i++) {
                float textWidth = Fonts.getSize(11, Fonts.Type.DEFAULT).getStringWidth(keys[i]) + 1.5f;
                float backgroundWidth = iconSize + padding + textWidth + padding;

                rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth + 1, rowHeight - 5)
                        .round(2)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(ColorAssist.getRect(1.0f))
                        .build());

                rectangle.render(ShapeProperties.create(matrix, posX + offsetX + 10, currentY, backgroundWidth - 10, rowHeight - 5)
                        .round(2, 2, 0, 0).color(ColorAssist.rgba(65, 65, 65, (int) (45 * animationValue)))
                        .build());

                matrix.push();
                matrix.translate(posX + offsetX, currentY + padding - 1.5f, 0);
                Render2D.defaultDrawStack(context, stacks[i], 0, 0, false, false, 0.5f);
                matrix.pop();
                float textX = posX + offsetX + iconSize + padding;
                float textY = currentY + padding + (iconSize - Fonts.getSize(11, Fonts.Type.DEFAULT).getStringHeight(keys[i])) / 2;
                Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(matrix, keys[i], textX - 0.5f, textY + 4f, new Color(255, 101, 57, (int)(255 * animationValue)).getRGB() );
                offsetX += backgroundWidth + spacing;
            }
        } else {
            for (int row = 0; row < rowCount; row++) {
                float rowWidth = rowWidths.get(row);
                float offsetX = row == 0 ? padding : padding + (firstRowWidth - rowWidth) / 2;
                float currentY = posY + row * rowHeight;
                int bindsInThisRow = Math.min(BINDS_PER_ROW, totalBinds - row * BINDS_PER_ROW);
                for (int i = 0; i < bindsInThisRow; i++) {
                    int bindIndex = row * BINDS_PER_ROW + i;
                    BindInfo bind = activeBinds.get(bindIndex);
                    ItemStack stack = createColoredPotion(bind.keyBind.item(), bind.color);
                    java.lang.String keyName = StringHelper.getBindName(bind.keyBind.setting().getKey());
                    float textWidth = Fonts.getSize(11, Fonts.Type.DEFAULT).getStringWidth(keyName) + 1.5f;
                    float backgroundWidth = iconSize + padding + textWidth + padding;
                    rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth + 1, rowHeight - 5)
                            .round(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ColorAssist.getRect(1.0f))
                            .build());

                    rectangle.render(ShapeProperties.create(matrix, posX + offsetX + 10, currentY, backgroundWidth - 10, rowHeight - 5)
                            .round(2, 2, 0, 0).color(ColorAssist.rgba(65, 65, 65, (int) (45 * animationValue)))
                            .build());
                    matrix.push();
                    matrix.translate(posX + offsetX, currentY + padding - 1.5f, 0);
                    Render2D.defaultDrawStack(context, stack, 0, 0, false, false, 0.5f);
                    matrix.pop();
                    float textX = posX + offsetX + iconSize + padding;
                    float textY = currentY + padding + (iconSize - Fonts.getSize(11, Fonts.Type.DEFAULT).getStringHeight(keyName)) / 2;
                    Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(matrix, keyName, textX - 0.5f, textY + 4f, new Color(255, 101, 57, (int)(255 * animationValue)).getRGB() );
                    offsetX += backgroundWidth + spacing;
                }
            }
        }
        setWidth((int) width);
        setHeight((int) height);
    }
}