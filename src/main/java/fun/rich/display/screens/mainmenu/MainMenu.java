package fun.rich.display.screens.mainmenu;

import fun.rich.Rich;
import fun.rich.display.screens.clickgui.components.implement.themes.ThemeColorsGetter;
import fun.rich.display.screens.mainmenu.altscreen.AltScreen;
import fun.rich.utils.client.text.TextAnimation;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.utils.display.gif.GifRender;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.common.animation.Easy.EaseBackIn;
import fun.rich.common.animation.Easy.Direction;
import fun.rich.utils.math.calc.Calculate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class MainMenu extends Screen implements QuickImports {
    public static MainMenu INSTANCE = new MainMenu();
    public int x, y, width, height;
    private final TextAnimation textAnimation = new TextAnimation();
    private boolean altVisible = false;
    private final GifRender gifRender = new GifRender("minecraft:gif/backgrounds/mainmenutype1", 1);
    private final EaseBackIn altAnimation = new EaseBackIn(400, 1f, 1.15f);
    private AltScreen altScreen;

    public MainMenu() {
        super(Text.of("MainMenu"));
    }

    @Override
    public void tick() {
        super.tick();
        textAnimation.updateText();
        if (altScreen != null) altScreen.tick();
        if (altAnimation.getDirection() == Direction.BACKWARDS && altAnimation.finished(Direction.BACKWARDS)) altVisible = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mc.options.getGuiScale().setValue(2);
        x = window.getScaledWidth();
        y = window.getScaledHeight();
        width = window.getScaledWidth() + 2;
        height = window.getScaledHeight() + 2;
        float cy = height / 2.0f, sy = cy - 25, sx = width / 2 - 50, bs = 21;

        gifRender.render(context.getMatrices(), 0, 0, width, height);
        image.setTexture("textures/mainmenu/backmenu.png").render(ShapeProperties.create(context.getMatrices(), 0, 0, width, height).color(-1).build());

        drawButton(context, sx, sy, 102, 18.5f, "Single Player");
        drawButton(context, sx, sy + bs, 102, 18.5f, "Multi Player");
        drawButton(context, sx, sy + bs * 2, 102, 18.5f, "Alt Screen");
        drawButton(context, sx, sy + bs * 3, 50, 18.5f, "");
        drawButton(context, sx + 52, sy + bs * 3, 50, 18.5f, "");

        Fonts.getSize(21, Fonts.Type.ICONSTYPENEW).drawCenteredString(context.getMatrices(), "i", width / 2 - 24, sy + bs + 49, ColorAssist.getText(0.35f));
        Fonts.getSize(22, Fonts.Type.ICONSTYPENEW).drawCenteredString(context.getMatrices(), "s", width / 2 + 27, sy + bs + 49, ColorAssist.getText(0.35f));

        Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Rich Client, you made the right choice.", width / 2, sy - 40, new Color(200, 200, 200).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), textAnimation.getCurrentText(), width / 2, sy - 25, new Color(200, 200, 200).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "© 2025 RichClient. All rights reserved.", width / 2, height - 7, ColorAssist.getText(0.35f));

        rectangle.render(ShapeProperties.create(context.getMatrices(), 8, height - 27, 20, 20).thickness(2).round(10).outlineColor(new Color(100, 100, 100, 95).getRGB()).color(new Color(50, 50, 50, 55).getRGB(), new Color(50, 50, 50, 55).getRGB(), new Color(80, 80, 80, 95).getRGB(), new Color(80, 80, 80, 95).getRGB()).build());

        Render2D.drawTexture(context, Identifier.of("minecraft", "textures/mainmenu/steve.png"), 9.5f, height - 25.5f, 17, 7, 32, 32, 32, new Color(0, 0, 0, 255).getRGB());

        rectangle.render(ShapeProperties.create(context.getMatrices(), 22, height - 13, 6, 6).thickness(2).round(3).outlineColor(new Color(100, 100, 100, 95).getRGB()).color(new Color(50, 50, 50, 55).getRGB(), new Color(50, 50, 50, 55).getRGB(), new Color(80, 80, 80, 95).getRGB(), new Color(80, 80, 80, 95).getRGB()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), 23, height - 12, 4, 4).round(2).color(new Color(1, 235, 1, 155).getRGB()).build());

        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Username ▸ Baflllikov", 35, height - 21.5f, ColorAssist.getText(0.35f));
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Uid ▸ 1", 35, height - 14.5f, ColorAssist.getText(0.35f));

        if (altVisible || altAnimation.getDirection() == Direction.BACKWARDS) {
            if (altScreen == null) altScreen = new AltScreen(width - 180, sy - 80);
            context.getMatrices().push();
            Calculate.scale(context.getMatrices(), width - 100, sy + 25, (float) altAnimation.getOutput(), () -> {
                altScreen.render(context, new Color(50, 50, 50, 55), new Color(100, 100, 100, 95), new Color(80, 80, 80, 95), new Color(200, 200, 200), new Color(30, 30, 30));
            });
            context.getMatrices().pop();
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext ctx, float x, float y, float w, float h, String label) {
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, h).thickness(2).round(4).outlineColor(new Color(100, 100, 100, 95).getRGB()).color(new Color(50, 50, 50, 55).getRGB(), new Color(50, 50, 50, 55).getRGB(), new Color(80, 80, 80, 95).getRGB(), new Color(80, 80, 80, 95).getRGB()).build());
        if (!label.isEmpty()) Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(ctx.getMatrices(), label, width / 2, y + 7, new Color(200, 200, 200).getRGB());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float cy = height / 2.0f, sy = cy - 25, sx = width / 2 - 50, bs = 21;
        if (btn == 0) {
            if (isIn(mx, my, sx, sy, 102, 18.5f)) { mc.setScreen(new SelectWorldScreen(this)); return true; }
            if (isIn(mx, my, sx, sy + bs, 102, 18.5f)) { mc.setScreen(new MultiplayerScreen(this)); return true; }
            if (isIn(mx, my, sx, sy + bs * 2, 102, 18.5f)) { toggleAlt(); return true; }
            if (isIn(mx, my, sx + 52, sy + bs * 3, 50, 18.5f)) { mc.setScreen(new OptionsScreen(this, mc.options)); return true; }
            if (isIn(mx, my, sx, sy + bs * 3, 50, 18.5f)) { mc.stop(); return true; }
        }
        if (altVisible && altAnimation.getOutput() == 1.0f && altScreen != null) return altScreen.mouseClicked(mx, my, btn);
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (altVisible && altAnimation.getOutput() == 1.0f && altScreen != null) return altScreen.mouseScrolled(mx, my, v);
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (altVisible && altAnimation.getOutput() == 1.0f && altScreen != null) return altScreen.mouseDragged(mx, my, btn);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (altScreen != null) altScreen.mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char c, int m) {
        if (altVisible && altAnimation.getOutput() == 1.0f && altScreen != null) return altScreen.charTyped(c);
        return super.charTyped(c, m);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (altVisible && altAnimation.getOutput() == 1.0f && altScreen != null && altScreen.keyPressed(k)) return true;
        if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && (altVisible || altAnimation.getDirection() == Direction.BACKWARDS)) {
            altAnimation.setDirection(Direction.BACKWARDS);
            altAnimation.reset();
            if (altScreen != null) altScreen.reset();
            return true;
        }
        return super.keyPressed(k, s, m);
    }

    private boolean isIn(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void toggleAlt() {
        if (!altVisible || altAnimation.getDirection() == Direction.BACKWARDS) {
            altVisible = true;
            altAnimation.setDirection(Direction.FORWARDS);
        } else {
            altAnimation.setDirection(Direction.BACKWARDS);
        }
        altAnimation.reset();
        if (altScreen != null) altScreen.reset();
    }
}