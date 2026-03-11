package fun.rich.display.screens.clickgui.components.implement.category;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.InOutBack;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.display.screens.clickgui.components.implement.module.ModuleComponent;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.display.scissor.ScissorAssist;
import fun.rich.Rich;
import fun.rich.features.impl.render.Theme;
import fun.rich.display.screens.clickgui.MenuScreen;
import fun.rich.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryComponent extends AbstractComponent {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
    private final ModuleCategory category;
    private final Animation alphaAnimation = new InOutBack().setMs(300).setValue(1);
    private final Animation scaleAnimation = new InOutBack().setMs(300).setValue(1);
    private boolean initializedAnimations = false;
    private float scroll = 0;
    private float smoothedScroll = 0;

    private void initializeModules() {
        List<Module> modules = Rich.getInstance()
                .getModuleRepository()
                .modules();
        for (Module module : modules) {
            ModuleComponent newComponent = new ModuleComponent(module);
            if (globalModuleComponents.add(newComponent)) {
                moduleComponents.add(newComponent);
            }
        }
    }

    public void postInitialize() {
        if (!initializedAnimations) {
            if (MenuScreen.INSTANCE.getCategory().equals(category)) {
                alphaAnimation.setDirection(Direction.FORWARDS);
                scaleAnimation.setDirection(Direction.FORWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            } else {
                alphaAnimation.setDirection(Direction.BACKWARDS);
                scaleAnimation.setDirection(Direction.BACKWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            }
            initializedAnimations = true;
        }
    }

    public CategoryComponent(ModuleCategory category) {
        this.category = category;
        initializeModules();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        postInitialize();
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        globalModuleComponents.clear();
        Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
        ScissorAssist scissorManager = Rich.getInstance().getScissorManager();
        drawCategoryTab(context, context.getMatrices(), mouseX, mouseY);
        int[] offsets = calculateOffsets();
        int columnWidth = 142;
        int column = 0;
        int maxScroll = 0;
        float offsetX = 35, offsetY = 14;
        scissorManager.push(positionMatrix, menuScreen.x + offsetX - 75, menuScreen.y + offsetY + 15, menuScreen.width - offsetX + 150, menuScreen.height - offsetY - 15);
        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent component = moduleComponents.get(i);
            if (shouldRenderComponent(component)) {
                int componentHeight = component.getComponentHeight() + 9;
                component.x = menuScreen.x + 32 + (column * (columnWidth + 48));
                component.y = (float) (menuScreen.y + 35 + offsets[column] - componentHeight + smoothedScroll);
                component.width = columnWidth + 40;
                if (component.y > menuScreen.y - componentHeight && menuScreen.y + menuScreen.height + 15 > component.y) {
                    component.render(context, mouseX, mouseY, delta);
                }
                offsets[column] -= componentHeight;
                maxScroll = Math.max(maxScroll, offsets[column]);
                column = (column + 1) % 2;
            }
        }
        scissorManager.pop();
        int clamped = MathHelper.clamp(maxScroll - (menuScreen.height / 2 + 35), 0, maxScroll);
        scroll = MathHelper.clamp(scroll, -clamped, 0);
        smoothedScroll = Calculate.interpolateSmooth(2, smoothedScroll, scroll);

        if (clamped > 0) {
            float scrollbarWidth = 4;
            float scrollbarX = menuScreen.x + menuScreen.width - offsetX - scrollbarWidth + 50;
            float scrollbarY = menuScreen.y + offsetY + 22;
            float scrollbarHeight = menuScreen.height - offsetY * 2 - 14;

            rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
                    .round(2F)
                    .color(
                            new Color(35, 35, 35, 75).getRGB(),
                            new Color(45, 45, 45, 95).getRGB(),
                            new Color(45, 45, 45, 95).getRGB(),
                            new Color(35, 35, 35, 75).getRGB())
                    .build());

            float contentHeight = clamped;
            float viewHeight = menuScreen.height - offsetY * 2;
            float handleHeight = Math.max(20, viewHeight * (viewHeight / (contentHeight + viewHeight)));
            float scrollRatio = (float) (-smoothedScroll) / contentHeight;
            float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;

            rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
                    .round(2F)
                    .color(
                            new Color(44, 44, 44, 75).getRGB(),
                            new Color(101, 101, 101, 95).getRGB(),
                            new Color(101, 101, 101, 95).getRGB(),
                            new Color(44, 44, 44, 75).getRGB())
                    .build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float tabX = x;
        float tabY = y;
        float centerX = tabX + baseWidth / 2;
        float centerY = tabY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;
        if (Calculate.isHovered(mouseX, mouseY, tabX, tabY, baseWidth, baseHeight) && button == 0) {
            MenuScreen.INSTANCE.setCategory(category);
            alphaAnimation.setMs(300);
            scaleAnimation.setMs(300);
            alphaAnimation.setDirection(Direction.FORWARDS);
            scaleAnimation.setDirection(Direction.FORWARDS);
            return true;
        }
        float offsetX = 35, offsetY = 14;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX - 75, menuScreen.y + offsetY, menuScreen.width - offsetX + 150, menuScreen.height - offsetY + 15)) {
            for (int i = 0; i < moduleComponents.size(); i++) {
                ModuleComponent moduleComponent = moduleComponents.get(i);
                if (shouldRenderComponent(moduleComponent) && moduleComponent.isHover(mouseX, mouseY)) {
                    return moduleComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float tabX = x;
        float tabY = y;
        float centerX = tabX + baseWidth / 2;
        float centerY = tabY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;

        boolean isHovered = Calculate.isHovered(mouseX, mouseY, scaledX, scaledY, scaledWidth, scaledHeight);

        if (isHovered) {
            return true;
        }

        moduleComponents.forEach(moduleComponent -> moduleComponent.isHover(mouseX, mouseY));
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isHover(mouseX, mouseY)) {
                return true;
            }
        }
        return super.isHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float offsetX = 35, offsetY = 13;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX + 7, menuScreen.height - offsetY + 15)) {
            scroll += amount * 20;
        }
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.mouseScrolled(mouseX, mouseY, amount);
            }
        });
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.keyPressed(keyCode, scanCode, modifiers);
            }
        });
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.charTyped(chr, modifiers);
            }
        });
        return super.charTyped(chr, modifiers);
    }

    private void drawCategoryTab(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        alphaAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        scaleAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = alphaAnimation.getOutput().floatValue();
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        int alpha = MathHelper.clamp((int) (anim * 255), 0, 255);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float tabX = x;
        float tabY = y;
        float centerX = tabX + baseWidth / 2;
        float centerY = tabY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;

        int guiBg = Theme.getInstance() != null ? Theme.getInstance().guiBgColor.getColor() : new Color(18, 19, 20, 200).getRGB();
        int baseBg = ColorAssist.multAlpha(guiBg, 0.9f);
        rectangle.render(ShapeProperties.create(matrix, tabX, tabY, baseWidth, baseHeight)
                .round(5F)
                .color(baseBg, baseBg, baseBg, baseBg).build());

        if (!MenuScreen.INSTANCE.getCategory().equals(category) && Calculate.isHovered(mouseX, mouseY, tabX, tabY, baseWidth, baseHeight)) {
            int hoverBg = ColorAssist.multAlpha(guiBg, 0.5f);
            rectangle.render(ShapeProperties.create(matrix, tabX, tabY, baseWidth, baseHeight)
                    .round(5F)
                    .color(hoverBg, hoverBg, hoverBg, hoverBg).build());
        }

        int oldSelectedAlpha = MathHelper.clamp((int) (anim * 135), 0, 135);
        int oldSelectedBg1 = new Color(21, 21, 21, oldSelectedAlpha).getRGB();
        int oldSelectedBg2 = new Color(61, 61, 61, oldSelectedAlpha).getRGB();
        rectangle.render(ShapeProperties.create(matrix, scaledX, scaledY, scaledWidth, scaledHeight)
                .round(5F)
                .color(oldSelectedBg1, oldSelectedBg2, oldSelectedBg2, oldSelectedBg1).build());

        float iconCenterX = x + 10f;
        float iconCenterY = y + 10f + category.getIconOffsetY();
        if (ModuleCategory.COMBAT.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "A", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.MOVEMENT.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "B", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.RENDER.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "C", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.PLAYER.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "D", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.MISC.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "E", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.CONFIGS.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "F", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.AUTOBUY.equals(category)) {
            Fonts.getSize(33, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "H", iconCenterX, iconCenterY, ColorAssist.getText());
        } else if (ModuleCategory.THEME.equals(category)) {
            Fonts.getSize(21, Fonts.Type.THEME_ICON).drawCenteredString(context.getMatrices(), "G", iconCenterX, iconCenterY, ColorAssist.getText());
        }
    }

    private int[] calculateOffsets() {
        int[] offsets = new int[2];
        int column = 0;
        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent component = moduleComponents.get(i);
            if (shouldRenderComponent(component)) {
                int componentHeight = component.getComponentHeight() + 9;
                offsets[column] += componentHeight;
                column = (column + 1) % 2;
            }
        }
        return offsets;
    }

    private boolean shouldRenderComponent(ModuleComponent component) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        ModuleCategory moduleCategory = component.getModule().getCategory();
        String text = menuScreen.getSearchComponent().getText().toLowerCase();
        String moduleName = component.getModule().getVisibleName().toLowerCase();
        return (text.equalsIgnoreCase("") ? moduleCategory.equals(menuScreen.getCategory()) : moduleName.contains(text));
    }
}