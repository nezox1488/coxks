package fun.rich.display.screens.clickgui.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;

import fun.rich.features.module.ModuleCategory;
import fun.rich.display.screens.clickgui.MenuScreen;
import fun.rich.display.screens.clickgui.components.implement.category.CategoryComponent;
import fun.rich.display.screens.clickgui.components.implement.settings.TextComponent;
import fun.rich.utils.interactions.inv.InventoryFlowManager;
import fun.rich.display.screens.clickgui.components.AbstractComponent;

import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class CategoryContainerComponent extends AbstractComponent {
    private static final int ICON_SIZE = 20;
    private static final int GAP = 1;
    /** Высота панели категорий: больше = выше над меню (в пикселях) */
    private static final int MENU_OFFSET = 8;

    private final List<CategoryComponent> categoryComponents = new ArrayList<>();

    public void initializeCategoryComponents() {
        categoryComponents.clear();
        for (ModuleCategory category : ModuleCategory.values()) {
            if (category == ModuleCategory.AUTOBUY) continue;
            categoryComponents.add(new CategoryComponent(category));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        int totalWidth = categoryComponents.size() * ICON_SIZE + Math.max(0, categoryComponents.size() - 1) * GAP;
        float startX = menuScreen.x + (menuScreen.width - totalWidth) / 2f;
        float iconY = menuScreen.y - ICON_SIZE - GAP - MENU_OFFSET;

        for (int i = 0; i < categoryComponents.size(); i++) {
            CategoryComponent component = categoryComponents.get(i);
            component.x = startX + i * (ICON_SIZE + GAP);
            component.y = iconY;
            component.width = ICON_SIZE;
            component.height = ICON_SIZE;
            component.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void tick() {
        if (TextComponent.typing || SearchComponent.typing) InventoryFlowManager.unPressMoveKeys();
        else InventoryFlowManager.updateMoveKeys();
        categoryComponents.forEach(AbstractComponent::tick);
        super.tick();
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        categoryComponents.forEach(categoryComponent -> categoryComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
}
