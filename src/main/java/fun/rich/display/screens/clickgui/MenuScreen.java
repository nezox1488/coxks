package fun.rich.display.screens.clickgui;

import fun.rich.display.screens.clickgui.components.implement.autobuy.autobuyui.AutoBuyGuiComponent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import fun.rich.features.module.ModuleCategory;
import fun.rich.common.animation.Easy.Direction;
import fun.rich.common.animation.Easy.EaseBackIn;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.client.sound.SoundManager;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.display.screens.clickgui.components.AbstractComponent;
import fun.rich.display.screens.clickgui.components.implement.other.BackgroundComponent;
import fun.rich.display.screens.clickgui.components.implement.other.CategoryContainerComponent;
import fun.rich.display.screens.clickgui.components.implement.other.SearchComponent;
import fun.rich.display.screens.clickgui.components.implement.other.UserComponent;
import fun.rich.display.screens.clickgui.components.implement.settings.TextComponent;
import fun.rich.utils.math.calc.Calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fun.rich.common.animation.Easy.Direction.BACKWARDS;
import static fun.rich.common.animation.Easy.Direction.FORWARDS;

@Setter
@Getter
public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private final BackgroundComponent backgroundComponent = new BackgroundComponent();
    private final UserComponent userComponent = new UserComponent();
    private final SearchComponent searchComponent = new SearchComponent();
    private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
    private final AutoBuyGuiComponent autoBuyGuiComponent = new AutoBuyGuiComponent();
    public final EaseBackIn animation = new EaseBackIn(400, 1f, 1.5f);
    public ModuleCategory category = ModuleCategory.COMBAT;
    public int x, y, width, height;

    public void initialize() {
        animation.setDirection(FORWARDS);
        categoryContainerComponent.initializeCategoryComponents();
        components.addAll(Arrays.asList(backgroundComponent, userComponent, searchComponent, categoryContainerComponent, autoBuyGuiComponent));
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        x = window.getScaledWidth() / 2 - 200;
        y = window.getScaledHeight() / 2 - 125;
        width = 400;
        height = 250;
        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, window.getScaledWidth(), window.getScaledHeight()).color(Calculate.applyOpacity(0xFF000000, 100 * getScaleAnimation())).build());
        backgroundComponent.position(x - 20, y - 15).size(width + 40, height + 30);
        userComponent.position(x + 165, y + height + 45);
        autoBuyGuiComponent.position(x - 20, y - 15).size(width + 40, height + 30);
        if (category == ModuleCategory.COMBAT || category == ModuleCategory.MOVEMENT || category == ModuleCategory.RENDER || category == ModuleCategory.PLAYER || category == ModuleCategory.MISC) {
            searchComponent.position(x + 330, y - 7.5f);
        } else {
            searchComponent.position(x + 330, y - 1000f);
            searchComponent.setText("");
        }
        categoryContainerComponent.position(x - 20, y - 15);

        Calculate.scale(context.getMatrices(), x + (float) width / 2, y + (float) height / 2, getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, mouseX, mouseY, delta));
            windowManager.render(context, mouseX, mouseY, delta);
        });
        super.render(context, mouseX, mouseY, delta);
    }

    public void openGui() {
        animation.setDirection(Direction.FORWARDS);
        animation.reset();
        mc.setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }

    public float getScaleAnimation() {
        return (float) animation.getOutput();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!windowManager.mouseClicked(mouseX, mouseY, button)) {
            components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
        windowManager.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!windowManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            components.forEach(component -> component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!windowManager.mouseScrolled(mouseX, mouseY, vertical)) {
            components.forEach(component -> component.mouseScrolled(mouseX, mouseY, vertical));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            animation.setDirection(BACKWARDS);
            return true;
        }
        if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            components.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!windowManager.charTyped(chr, modifiers)) {
            components.forEach(component -> component.charTyped(chr, modifiers));
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animation.finished(BACKWARDS)) {
            TextComponent.typing = false;
            super.close();
        }
    }
}