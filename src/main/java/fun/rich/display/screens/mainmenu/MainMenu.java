package fun.rich.display.screens.mainmenu;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import fun.rich.utils.display.atlasfont.msdf.MsdfFont;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.systemrender.builders.Builder;
import fun.rich.Rich;
import fun.rich.mixin.client.IMinecraftClient;
import fun.rich.utils.client.text.TextAnimation;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.gif.GifRender;
import fun.rich.utils.display.scissor.ScissorAssist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.geometry.Render2D;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import fun.rich.common.animation.Easy.EaseBackIn;
import fun.rich.common.animation.Easy.Direction;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.client.managers.file.impl.account.Account;
import fun.rich.utils.client.managers.file.impl.account.AccountRepository;
import fun.rich.utils.client.managers.file.impl.ModuleFile;

public class MainMenu extends Screen implements QuickImports {
    public static MainMenu INSTANCE = new MainMenu();
    public int x, y, width, height;
    private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());
    private static final Supplier<MsdfFont> ICONS_FONT_MAIN = Suppliers.memoize(() -> MsdfFont.builder().atlas("iconmain").data("iconmain").build());
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private final TextAnimation textAnimation = new TextAnimation();
    public boolean isDarkMode = true;
    public String themeIcon = "I";
    private long transitionStartTime = 0;
    private final float transitionDuration = 0.35f;
    private boolean isTransitioning = false;
    private final Color lightBgColor = new Color(255, 255, 255, 255);
    private final Color lightButtonColor = new Color(128, 128, 128, 55);
    private final Color lightOutlineColor = new Color(86, 86, 86, 95);
    private final Color lightGradientColor = new Color(255, 255, 255, 95);
    private final Color lightTextColor = new Color(128, 128, 128, 255);
    private final Color darkBgColor = new Color(30, 30, 30, 255);
    private final Color darkButtonColor = new Color(50, 50, 50, 55);
    private final Color darkOutlineColor = new Color(100, 100, 100, 95);
    private final Color darkGradientColor = new Color(80, 80, 80, 95);
    private final Color darkTextColor = new Color(200, 200, 200, 255);
    private boolean altVisible = false;

    private final GifRender gifRender = new GifRender("minecraft:gif/backgrounds/mainmenutype1", 1);

    private AccountRepository accountRepository = Rich.getInstance().getAccountRepository();
    private String currentAccount = "None";
    private boolean typing = false;
    private String typedText = "";
    private int cursorPos = 0;
    private int selStart = -1;
    private int selEnd = -1;
    private long lastClick = 0;
    private float textXOffset = 0;
    private boolean dragging = false;
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;
    private Random rand = new Random();
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private final EaseBackIn altAnimation = new EaseBackIn(400, 1f, 1.15f);

    public MainMenu() {
        super(Text.of("MainMenu"));
    }

    public float getAltScaleAnimation() {
        return (float) altAnimation.getOutput();
    }

    @Override
    public void tick() {
        super.tick();
        textAnimation.updateText();
        for (Account account : accountRepository.accountList) {
            float target = account.starred ? 1f : 0f;
            account.starAnim += (target - account.starAnim) * 0.2f;
        }
        if (altAnimation.getDirection() == Direction.BACKWARDS && altAnimation.finished(Direction.BACKWARDS)) {
            altVisible = false;
        }
    }

    private Color interpolateColor(Color start, Color end, float t) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    private void drawAccountFace(DrawContext context, Account account, float x, float y) {
        GameProfile profile = new GameProfile(UUID.fromString(account.uuid), account.name);
        Identifier skinTexture = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile).texture();
        if (skinTexture == null) {
            skinTexture = Identifier.of("minecraft", "textures/entity/steve.png");
        }
        Render2D.drawTexture(context, skinTexture, x, y, 15, 7, 8, 8, 64, ColorAssist.getRect(1), -1);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mc.options.getGuiScale().setValue(2);
        x = window.getScaledWidth();
        y = window.getScaledHeight();
        width = window.getScaledWidth() + 2;
        height = window.getScaledHeight() + 2;
        float centerY = height / 2.0f;
        float buttonWidth = 102;
        float buttonHeight = 18.5f;
        float buttonSpacing = 21;
        float startY = centerY - 25;
        float sidePanelX =  width / 2 - 50;
        String icon = "A ";
        String texture = "textures/backorange.png";
        float t = 1.0f;
        if (isTransitioning) {
            long currentTime = System.nanoTime();
            float elapsed = (currentTime - transitionStartTime) / 1_000_000_000.0f;
            t = Math.min(elapsed / transitionDuration, 1.0f);
            if (t >= 1.0f) {
                isTransitioning = false;
            }
        }
        Color bgColor = isDarkMode ? interpolateColor(new Color(225, 225, 225, 255), new Color(25, 25, 25, 255), t) : interpolateColor(new Color(22, 22, 22, 255), new Color(225, 225, 225, 255), t);
        Color bgColor1 = isDarkMode ? interpolateColor(lightBgColor, darkBgColor, t) : interpolateColor(darkBgColor, lightBgColor, t);
        Color buttonColor = isDarkMode ? interpolateColor(lightButtonColor, darkButtonColor, t) : interpolateColor(darkButtonColor, lightButtonColor, t);
        Color outlineColor = isDarkMode ? interpolateColor(lightOutlineColor, darkOutlineColor, t) : interpolateColor(darkOutlineColor, lightOutlineColor, t);
        Color gradientColor = isDarkMode ? interpolateColor(lightGradientColor, darkGradientColor, t) : interpolateColor(darkGradientColor, lightGradientColor, t);
        Color textColor = isDarkMode ? interpolateColor(lightTextColor, darkTextColor, t) : interpolateColor(darkTextColor, lightTextColor, t);
        gifRender.render(context.getMatrices(), 0, 0, width, height);
        rectangle.render(ShapeProperties.create(context.getMatrices(), -1, -1, 230, height + 2).color(bgColor.getRGB()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), -1, -1, width + 2, height + 2).color(bgColor1.getRGB()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidePanelX, startY, buttonWidth, buttonHeight)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Single Player", sidePanelX + 28, startY + 7, textColor.getRGB());
        Builder.text().font(ICONS_FONT.get()).text("G").size(10f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 8, startY + 3);
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidePanelX, startY + buttonSpacing, buttonWidth, buttonHeight)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Multi Player", sidePanelX + 31, startY + buttonSpacing + 7, textColor.getRGB());
        Builder.text().font(ICONS_FONT_MAIN.get()).text("A").size(10f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 7, startY + buttonSpacing + 4);
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidePanelX, startY + buttonSpacing * 2, buttonWidth, buttonHeight)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Alt Screen", sidePanelX + 33, startY + buttonSpacing * 2 + 7, textColor.getRGB());
        Builder.text().font(ICONS_FONT.get()).text("L").size(13f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 7, startY + buttonSpacing * 2);
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidePanelX, startY + buttonSpacing * 3, 50, buttonHeight)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Builder.text().font(ICONS_FONT_MAIN.get()).text("C").size(15f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 19, startY + buttonSpacing * 3 + 0.5f);
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidePanelX + 52, startY + buttonSpacing * 3, 50, buttonHeight)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Builder.text().font(ICONS_FONT_MAIN.get()).text("B").size(15f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 71, startY + buttonSpacing * 3 + 0.5f);
        Builder.text().font(ICONS_FONT_MAIN.get()).text(themeIcon).size(25f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), 3, -1);
        Builder.text().font(ICONS_FONT.get()).text(icon).size(40).color(new Color(255, 101, 57, 255).getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX - 30, startY - 65);
        Builder.text().font(BOLD_FONT.get()).text("Rich Client").size(18).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 11, startY - 57);
        Builder.text().font(BOLD_FONT.get()).text("v0.1").size(10f).color(textColor.getRGB()).build()
                .render(context.getMatrices().peek().getPositionMatrix(), sidePanelX + 11, startY - 36);
        String animatedText = textAnimation.getCurrentText();
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), animatedText,  width / 2, height - 50, textColor.getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "© 2025 RichClient. All rights reserved.", width / 2, height - 10, textColor.getRGB());
        if (altVisible || altAnimation.getDirection() == Direction.BACKWARDS) {
            float altPanelX = width - 180;
            float altPanelY = startY - 80;
            float altPanelWidth = 160;
            float altPanelHeight = 210;
            context.getMatrices().push();
            Calculate.scale(context.getMatrices(), altPanelX + altPanelWidth / 2 + 250, altPanelY + altPanelHeight / 2, getAltScaleAnimation(), () -> {
                rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX + altPanelWidth / 2 - 22, altPanelY - 13.5f, 43, 12).color(bgColor.getRGB()).round(3).build());
                Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Accounts", altPanelX + altPanelWidth / 2, altPanelY - 10, textColor.getRGB());
                rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX, altPanelY, altPanelWidth, altPanelHeight).color(bgColor.getRGB()).round(7).build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX + 5, altPanelY + 185, altPanelWidth - 11, 20)
                        .thickness(2).round(5).outlineColor(outlineColor.getRGB())
                        .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX + altPanelWidth - 25, altPanelY + 187.5f, 15, 15)
                        .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                        .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
                Builder.text().font(ICONS_FONT_MAIN.get()).text("D").size(14f).color(textColor.getRGB()).build()
                        .render(context.getMatrices().peek().getPositionMatrix(), altPanelX + altPanelWidth - 24, altPanelY + 187);
                float textFieldX = altPanelX + 5;
                float textFieldY = altPanelY + 177;
                float textFieldWidth = altPanelWidth - 11;
                float textFieldHeight = 20;
                Fonts.Type fontType = Fonts.Type.DEFAULT;
                var font = Fonts.getSize(16, fontType);
                long currentTime = System.currentTimeMillis();
                boolean blink = (currentTime % 1000 < 500);
                context.enableScissor((int) (textFieldX + 3), (int) textFieldY, (int) (textFieldX + textFieldWidth - 3), (int) (textFieldY + textFieldHeight) + 5);
                if (typing && hasSelection()) {
                    int start = Math.min(selStart, selEnd);
                    int end = Math.max(selStart, selEnd);
                    float selXStart = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, start));
                    float selWidth = font.getStringWidth(typedText.substring(start, end));
                    rectangle.render(ShapeProperties.create(context.getMatrices(), selXStart, textFieldY + 13.5f, selWidth, textFieldHeight - 10).color(0xFF5585E8).build());
                }
                if (!typedText.isEmpty() || typing) {
                    font.drawString(context.getMatrices(), typedText, textFieldX + 5 - textXOffset, textFieldY + 16, textColor.getRGB());
                } else {
                    String placeholder = "Typing nickname";
                    font.drawString(context.getMatrices(), placeholder, textFieldX + 5, textFieldY + 16, textColor.getRGB());
                }
                if (typing && blink && !hasSelection()) {
                    float cursorX = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, cursorPos));
                    rectangle.render(ShapeProperties.create(context.getMatrices(), cursorX + 1, textFieldY + 15f, 0.5f, textFieldHeight - 13).color(textColor.getRGB()).build());
                }
                context.disableScissor();
                float accountSpacing = 25;
                MatrixStack matrix = context.getMatrices();
                Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
                ScissorAssist scissorManager = Rich.getInstance().getScissorManager();
                float listY = altPanelY + 5;
                float listHeight = altPanelHeight - 31;
                scissorManager.push(positionMatrix, altPanelX, listY, altPanelWidth, listHeight);
                smoothedScroll = MathHelper.lerp(0.1f, smoothedScroll, scroll);
                for (int i = 0; i < accountRepository.accountList.size(); i++) {
                    Account account = accountRepository.accountList.get(i);
                    float accY = altPanelY + 10 + i * accountSpacing - smoothedScroll;
                    if (accY + 20 >= listY && accY <= listY + listHeight) {
                        rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX + 5, accY, altPanelWidth - 11, 20)
                                .thickness(2).round(5).outlineColor(outlineColor.getRGB())
                                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
                        Color starColor = interpolateColor(textColor, new Color(255, 255, 0, textColor.getAlpha()), account.starAnim);
                        rectangle.render(ShapeProperties.create(context.getMatrices(), altPanelX + 9.5f, accY + 2.5, 16, 16)
                                .thickness(4).round(8).outlineColor(new Color(64, 64, 64, 255).getRGB())
                                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
                        Fonts.getSize(25, Fonts.Type.ICONS).drawString(context.getMatrices(), "S", altPanelX + altPanelWidth - 25, accY + 6.5f, starColor.getRGB());
                        drawAccountFace(context, account, altPanelX + 10, accY + 3);
                        Fonts.getSize(15, Fonts.Type.SEMI).drawString(context.getMatrices(), account.name, altPanelX + 28, accY + 8.5f, textColor.getRGB());
                    }
                }
                scissorManager.pop();
                if (accountRepository.accountList.size() > 7) {
                    float contentHeight = accountRepository.accountList.size() * accountSpacing;
                    float viewHeight = listHeight;
                    float maxScroll = Math.max(0, contentHeight - viewHeight);
                    scroll = MathHelper.clamp(scroll, 0, maxScroll);
                    float scrollbarWidth = 2;
                    float scrollbarX = altPanelX + altPanelWidth - scrollbarWidth - 2.5f;
                    float scrollbarY = listY + 1;
                    float scrollbarHeight = listHeight - 3;
                    rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
                            .round(1f)
                            .color(new Color(30, 30, 30, 100).getRGB())
                            .build());
                    float handleHeight = Math.max(20, viewHeight * (viewHeight / (contentHeight + viewHeight)));
                    float scrollRatio = smoothedScroll / maxScroll;
                    float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;
                    rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
                            .round(1f)
                            .color(new Color(100, 100, 100, 150).getRGB())
                            .build());
                }
                String currentText = "Current account - " + currentAccount;
                float currentWidth = font.getStringWidth(currentText) + 20;
                float currentX = altPanelX + altPanelWidth / 2 - currentWidth / 2;
                rectangle.render(ShapeProperties.create(context.getMatrices(), currentX, altPanelY + altPanelHeight + 2, currentWidth, 12).color(bgColor.getRGB()).round(3).build());
                Fonts.getSize(15, Fonts.Type.SEMI).drawCenteredString(context.getMatrices(), currentText, altPanelX + altPanelWidth / 2, altPanelY + altPanelHeight + 6, textColor.getRGB());
            });
            context.getMatrices().pop();
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float centerY = height / 2.0f;
        float buttonWidth = 102;
        float buttonHeight = 18.5f;
        float buttonSpacing = 21;
        float startY = centerY - 25;
        float sidePanelX = width / 2 - 50;
        float iconX = 3;
        float iconY = -1;
        float iconWidth = 35;
        float iconHeight = 35;
        if (button == 0) {
            if (mouseX >= iconX && mouseX <= iconX + iconWidth && mouseY >= iconY && mouseY <= iconY + iconHeight) {
                isDarkMode = !isDarkMode;
                themeIcon = isDarkMode ? "I" : "H";
                isTransitioning = true;
                transitionStartTime = System.nanoTime();
                ModuleFile moduleFile = new ModuleFile(Rich.getInstance().getModuleRepository(), Rich.getInstance().getDraggableRepository());
                try {
                    moduleFile.saveToFile(new File(MinecraftClient.getInstance().runDirectory, "richclient"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            if (mouseX >= sidePanelX && mouseX <= sidePanelX + buttonWidth && mouseY >= startY && mouseY <= startY + buttonHeight) {
                mc.setScreen(new SelectWorldScreen(this));
                return true;
            }
            if (mouseX >= sidePanelX && mouseX <= sidePanelX + buttonWidth && mouseY >= startY + buttonSpacing && mouseY <= startY + buttonSpacing + buttonHeight) {
                mc.setScreen(new MultiplayerScreen(this));
                return true;
            }
            if (mouseX >= sidePanelX && mouseX <= sidePanelX + buttonWidth && mouseY >= startY + buttonSpacing * 2 && mouseY <= startY + buttonSpacing * 2 + buttonHeight) {
                if (!altVisible || altAnimation.getDirection() == Direction.BACKWARDS) {
                    altVisible = true;
                    altAnimation.setDirection(Direction.FORWARDS);
                    altAnimation.reset();
                } else {
                    altAnimation.setDirection(Direction.BACKWARDS);
                    altAnimation.reset();
                }
                typing = false;
                clearSelection();
                return true;
            }
            if (mouseX >= sidePanelX + 52 && mouseX <= sidePanelX + 102 && mouseY >= startY + buttonSpacing * 3 && mouseY <= startY + buttonSpacing * 3 + buttonHeight) {
                mc.setScreen(new OptionsScreen(this, mc.options));
                return true;
            }
            if (mouseX >= sidePanelX && mouseX <= sidePanelX + 50 && mouseY >= startY + buttonSpacing * 3 && mouseY <= startY + buttonSpacing * 3 + buttonHeight) {
                mc.stop();
                return true;
            }
        }
        if (altVisible && altAnimation.getOutput() == 1.0f) {
            float altPanelX = width - 180;
            float altPanelY = startY - 80;
            float altPanelWidth = 160;
            float altPanelHeight = 210;
            float textFieldX = altPanelX + 5;
            float textFieldY = altPanelY + 177;
            float textFieldWidth = altPanelWidth - 25;
            float textFieldHeight = 20;
            if (button == 0 && mouseX >= textFieldX && mouseX <= textFieldX + textFieldWidth && mouseY >= textFieldY && mouseY <= textFieldY + textFieldHeight) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClick < 250) {
                    selStart = 0;
                    selEnd = typedText.length();
                } else {
                    typing = true;
                    cursorPos = getCursorIndexAt(mouseX);
                    selStart = cursorPos;
                    selEnd = cursorPos;
                    lastClick = System.currentTimeMillis();
                }
                dragging = true;
                return true;
            } else {
                typing = false;
                clearSelection();
            }
            if (button == 0 && mouseX >= altPanelX + altPanelWidth - 25 && mouseX <= altPanelX + altPanelWidth - 25 + 15 && mouseY >= altPanelY + 187.5f && mouseY <= altPanelY + 187.5f + 15) {
                String finalUsername = null;
                StringBuilder username = new StringBuilder();
                char[] vowels = {'a', 'e', 'i', 'o', 'u'};
                char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
                int attempts = 0;
                final int MAX_ATTEMPTS = 10;
                do {
                    username.setLength(0);
                    int length = 6 + rand.nextInt(5);
                    boolean startWithVowel = rand.nextBoolean();
                    for (int i = 0; i < length; i++) {
                        if (i % 2 == 0) {
                            username.append(startWithVowel ? vowels[rand.nextInt(vowels.length)] : consonants[rand.nextInt(consonants.length)]);
                        } else {
                            username.append(startWithVowel ? consonants[rand.nextInt(consonants.length)] : vowels[rand.nextInt(vowels.length)]);
                        }
                    }
                    if (rand.nextInt(100) < 30) {
                        username.append(rand.nextInt(100));
                    }
                    String tempUsername = username.substring(0, 1).toUpperCase() + username.substring(1);
                    attempts++;
                    if (!accountRepository.accountList.stream().anyMatch(account -> account.name.equals(tempUsername))) {
                        finalUsername = tempUsername;
                        break;
                    }
                } while (attempts < MAX_ATTEMPTS);
                if (finalUsername == null) {
                    finalUsername = username.substring(0, 1).toUpperCase() + username.substring(1) + (System.currentTimeMillis() % 1000);
                }
                String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + finalUsername).getBytes(StandardCharsets.UTF_8)).toString();
                accountRepository.accountList.add(new Account(finalUsername, false, false, null, offlineUuid, "0"));
                accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                typedText = "";
                cursorPos = 0;
                clearSelection();
                return true;
            }
            float accountSpacing = 25;
            float listY = altPanelY + 5;
            float listHeight = altPanelHeight - 31;
            for (int i = 0; i < accountRepository.accountList.size(); i++) {
                float accY = altPanelY + 10 + i * accountSpacing - smoothedScroll;
                float starX = altPanelX + altPanelWidth - 25;
                float starY = accY + 6.5f;
                float starSize = 15;
                if (button == 0 && mouseX >= starX && mouseX <= starX + starSize && mouseY >= starY && mouseY <= starY + starSize && accY + 20 >= listY && accY <= listY + listHeight) {
                    Account account = accountRepository.accountList.get(i);
                    account.starred = !account.starred;
                    accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                    return true;
                }
                if (button == 0 && mouseX >= altPanelX + 5 && mouseX <= altPanelX + 5 + altPanelWidth - 11 && mouseY >= accY && mouseY <= accY + 20 && accY + 20 >= listY && accY <= listY + listHeight) {
                    Account account = accountRepository.accountList.get(i);
                    currentAccount = account.name;
                    setSession(account);
                    return true;
                } else if (button == 1 && mouseX >= altPanelX + 5 && mouseX <= altPanelX + 5 + altPanelWidth - 11 && mouseY >= accY && mouseY <= accY + 20 && accY + 20 >= listY && accY <= listY + listHeight) {
                    accountRepository.accountList.remove(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setSession(Account account) {
        Session newSession;
        newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        IMinecraftClient mca = (IMinecraftClient) MinecraftClient.getInstance();
        mca.setSessionT(newSession);
        MinecraftClient.getInstance().getGameProfile().getProperties().clear();
        UserApiService apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, newSession, MinecraftClient.getInstance().runDirectory.toPath()));
        mca.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (altVisible && accountRepository.accountList.size() > 7 && altAnimation.getOutput() == 1.0f) {
            float altPanelX = width - 180;
            float altPanelY = (height / 2.0f) - 25 - 75;
            float listX = altPanelX;
            float listY = altPanelY + 5;
            float listWidth = 160;
            float listHeight = 210 - 31;
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                float contentHeight = accountRepository.accountList.size() * 25f;
                float maxScroll = Math.max(0, contentHeight - listHeight);
                scroll -= vertical * 20;
                scroll = MathHelper.clamp(scroll, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0 && altAnimation.getOutput() == 1.0f) {
            cursorPos = getCursorIndexAt(mouseX);
            selEnd = cursorPos;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing && typedText.length() < MAX_LENGTH && altAnimation.getOutput() == 1.0f) {
            deleteSelectedText();
            typedText = typedText.substring(0, cursorPos) + chr + typedText.substring(cursorPos);
            cursorPos++;
            clearSelection();
            updateTextXOffset();
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing && altAnimation.getOutput() == 1.0f) {
            if (Screen.hasControlDown()) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A:
                        selStart = 0;
                        selEnd = typedText.length();
                        return true;
                    case GLFW.GLFW_KEY_C:
                        if (hasSelection()) {
                            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), getSelectedText());
                        }
                        return true;
                    case GLFW.GLFW_KEY_V:
                        String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                        if (clipboard != null) {
                            deleteSelectedText();
                            typedText = typedText.substring(0, cursorPos) + clipboard + typedText.substring(cursorPos);
                            cursorPos += clipboard.length();
                            clearSelection();
                            updateTextXOffset();
                        }
                        return true;
                }
            }
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    if (hasSelection()) {
                        deleteSelectedText();
                    } else if (cursorPos > 0) {
                        typedText = typedText.substring(0, cursorPos - 1) + typedText.substring(cursorPos);
                        cursorPos--;
                    }
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_LEFT:
                    if (cursorPos > 0) cursorPos--;
                    updateSelectionAfterMove();
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_RIGHT:
                    if (cursorPos < typedText.length()) cursorPos++;
                    updateSelectionAfterMove();
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_ENTER:
                    if (typedText.length() >= MIN_LENGTH && typedText.length() <= MAX_LENGTH) {
                        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + typedText).getBytes(StandardCharsets.UTF_8)).toString();
                        accountRepository.accountList.add(new Account(typedText, false, false, null, offlineUuid, "0"));
                        accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                        typedText = "";
                        cursorPos = 0;
                        typing = false;
                        clearSelection();
                    }
                    return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (altVisible || altAnimation.getDirection() == Direction.BACKWARDS)) {
            altAnimation.setDirection(Direction.BACKWARDS);
            altAnimation.reset();
            typing = false;
            clearSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean hasSelection() {
        return selStart != selEnd;
    }

    private String getSelectedText() {
        int start = Math.min(selStart, selEnd);
        int end = Math.max(selStart, selEnd);
        return typedText.substring(start, end);
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            int start = Math.min(selStart, selEnd);
            int end = Math.max(selStart, selEnd);
            typedText = typedText.substring(0, start) + typedText.substring(end);
            cursorPos = start;
            clearSelection();
        }
    }

    private void clearSelection() {
        selStart = cursorPos;
        selEnd = cursorPos;
    }

    private void updateSelectionAfterMove() {
        if (Screen.hasShiftDown()) {
            if (selStart == cursorPos) selStart = cursorPos;
            selEnd = cursorPos;
        } else {
            clearSelection();
        }
    }

    private int getCursorIndexAt(double mouseX) {
        float altPanelX = width - 180;
        float altPanelY = (height / 2.0f) - 25 - 75;
        float textFieldX = altPanelX + 5;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float relX = (float) mouseX - textFieldX - 3 + textXOffset;
        int pos = 0;
        for (; pos < typedText.length(); pos++) {
            if (font.getStringWidth(typedText.substring(0, pos + 1)) > relX) {
                break;
            }
        }
        return pos;
    }

    private void updateTextXOffset() {
        float altPanelX = width - 180;
        float altPanelY = (height / 2.0f) - 25 - 75;
        float textFieldWidth = 160 - 11;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float cursorX = font.getStringWidth(typedText.substring(0, cursorPos));
        if (cursorX < textXOffset) {
            textXOffset = cursorX;
        } else if (cursorX > textXOffset + textFieldWidth - 10) {
            textXOffset = cursorX - (textFieldWidth - 10);
        }
    }
}