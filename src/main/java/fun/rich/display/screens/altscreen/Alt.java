package fun.rich.display.screens.altscreen;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import fun.rich.utils.display.interfaces.QuickImports;

@Setter
@Getter
public class Alt extends Screen implements QuickImports {
    public static Alt INSTANCE = new Alt();
    public int x, y, width, height;

    public void initialize() {
    }

    public Alt() {
        super(Text.of("Alt"));
        initialize();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        x = window.getScaledWidth() / 2 - 200;
        y = window.getScaledHeight() / 2 - 125;
        width = window.getScaledWidth();
        height = window.getScaledHeight();


        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

}
