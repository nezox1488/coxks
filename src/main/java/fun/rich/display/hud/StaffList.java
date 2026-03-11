package fun.rich.display.hud;

import com.mojang.authlib.GameProfile;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.common.animation.Animation;
import fun.rich.common.repository.staff.StaffRepository;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.client.Instance;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.features.impl.render.Hud;
import fun.rich.utils.client.packet.network.Network;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaffList extends AbstractDraggable {
    public static StaffList getInstance() {
        return Instance.getDraggable(StaffList.class);
    }

    public final Map<PlayerListEntry, Animation> list = new HashMap<>();
    private final Set<String> notifiedPlayers = new HashSet<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");

    private static final Map<String, String> CHAR_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> PREFIX_COLORS = new HashMap<>();

    static {
        CHAR_TO_NAME.put("ꔀ", "player"); CHAR_TO_NAME.put("ꔄ", "hero");
        CHAR_TO_NAME.put("ꔈ", "titan"); CHAR_TO_NAME.put("ꔒ", "avenger");
        CHAR_TO_NAME.put("ꔖ", "overlord"); CHAR_TO_NAME.put("ꔠ", "magister");
        CHAR_TO_NAME.put("ꔤ", "imperator"); CHAR_TO_NAME.put("ꔨ", "dragon");
        CHAR_TO_NAME.put("ꔲ", "bull"); CHAR_TO_NAME.put("ꕒ", "rabbit");
        CHAR_TO_NAME.put("ꔶ", "tiger"); CHAR_TO_NAME.put("ꕄ", "dracula");
        CHAR_TO_NAME.put("ꕖ", "bunny"); CHAR_TO_NAME.put("ꕀ", "hydra");
        CHAR_TO_NAME.put("ꕈ", "cobra"); CHAR_TO_NAME.put("ꔁ", "media");
        CHAR_TO_NAME.put("ꔅ", "yt"); CHAR_TO_NAME.put("ꕠ", "d.helper");
        CHAR_TO_NAME.put("ꔉ", "helper"); CHAR_TO_NAME.put("ꔓ", "ml.moder");
        CHAR_TO_NAME.put("ꔗ", "moder"); CHAR_TO_NAME.put("ꔡ", "moder+");
        CHAR_TO_NAME.put("ꔥ", "st.moder"); CHAR_TO_NAME.put("ꔩ", "gl.moder");
        CHAR_TO_NAME.put("ꔳ", "ml.admin"); CHAR_TO_NAME.put("ꔷ", "admin");

        PREFIX_COLORS.put("helper", new Color(255, 255, 0).getRGB());
        PREFIX_COLORS.put("moder", new Color(0, 0, 255).getRGB());
        PREFIX_COLORS.put("admin", new Color(255, 0, 0).getRGB());
        PREFIX_COLORS.put("Vanish", new Color(255, 0, 0).getRGB());
    }

    public StaffList() {
        super("Staff List", 115, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        if (mc.world == null || mc.getNetworkHandler() == null) {
            list.clear();
            return;
        }

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        Scoreboard scoreboard = mc.world.getScoreboard();
        Set<String> addedNames = new HashSet<>();

        for (PlayerListEntry entry : playerList) {
            String name = entry.getProfile().getName();

            Team team = scoreboard.getTeam(name);
            if (team == null) continue;

            String teamPrefix = team.getPrefix().getString();
            String prefixKey = CHAR_TO_NAME.keySet().stream()
                    .filter(teamPrefix::contains)
                    .findFirst()
                    .orElse(null);

            if (prefixKey != null) {
                if (!list.containsKey(entry)) {
                    list.put(entry, new Decelerate().setMs(150).setValue(1));
                }
                addedNames.add(name.toLowerCase());
            }
        }

        for (Team team : scoreboard.getTeams()) {
            for (String name : team.getPlayerList()) {
                if (addedNames.contains(name.toLowerCase())) continue;
                if (!namePattern.matcher(name).matches()) continue;

                String teamPrefix = team.getPrefix().getString();
                String prefixKey = CHAR_TO_NAME.keySet().stream()
                        .filter(teamPrefix::contains)
                        .findFirst()
                        .orElse(null);

                if (prefixKey != null) {
                    String roleName = CHAR_TO_NAME.get(prefixKey);

                    if (list.keySet().stream().noneMatch(e -> e.getProfile().getName().equalsIgnoreCase(name))) {
                        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
                        PlayerListEntry fakeEntry = new PlayerListEntry(profile, false);

                        MutableText displayName = Text.empty();
                        displayName.append(Text.literal("[").formatted(Formatting.GRAY))
                                .append(Text.literal(roleName).formatted(Formatting.RESET))
                                .append(Text.literal("] ").formatted(Formatting.GRAY))
                                .append(Text.literal(name).formatted(Formatting.GRAY));
                        fakeEntry.setDisplayName(displayName);

                        list.put(fakeEntry, new Decelerate().setMs(150).setValue(1));

                        if (Hud.getInstance().notificationSettings.isSelected("Staff Join") && !notifiedPlayers.contains(name)) {
                            Notifications.getInstance().addList(Text.literal(name + " [V] зашел!"), 5000);
                            notifiedPlayers.add(name);
                        }
                    }
                    addedNames.add(name.toLowerCase());
                }
            }
        }

        list.entrySet().removeIf(entry -> {
            String name = entry.getKey().getProfile().getName();
            if (!addedNames.contains(name.toLowerCase())) {
                entry.getValue().setDirection(Direction.BACKWARDS);
            }

            if (entry.getValue().isFinished(Direction.BACKWARDS)) {
                notifiedPlayers.remove(name);
                return true;
            }
            return false;
        });
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);

        int staffAlpha = (int) Hud.getInstance().opacityStaffList.getValue();

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 15.5F).round(4,0,4,0).color(new Color(0,0,0, staffAlpha).getRGB()).build());
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 15.5F).round(4,0,4,0).thickness(0.1f).outlineColor(new Color(33,33,33).getRGB()).color(new Color(18,19,20, staffAlpha).getRGB()).build());

        font.drawString(matrix, "Staff List", getX() + 22, getY() + 6.5f, ColorAssist.getText());

        float sepW = getWidth() * 0.5f;
        float sepX = getX() + (getWidth() - sepW) / 2f;
        float sepY = getY() + 16;
        int c1 = ColorAssist.fade(8, 200, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        int c2 = ColorAssist.fade(8, 0, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        rectangle.render(ShapeProperties.create(matrix, sepX, sepY, sepW, 0.5f).color(c1, c1, c2, c2).build());

        float offset = 18;
        float maxWidth = 80;

        for (Map.Entry<PlayerListEntry, Animation> entry : list.entrySet()) {
            float anim = entry.getValue().getOutput().floatValue();
            if (anim <= 0.05) continue;

            String name = entry.getKey().getProfile().getName();
            String fullDisplay = entry.getKey().getDisplayName() != null ? entry.getKey().getDisplayName().getString() : name;

            String role = CHAR_TO_NAME.values().stream().filter(fullDisplay::contains).findFirst().orElse("Vanish");
            int pColor = PREFIX_COLORS.getOrDefault(role.toLowerCase(), -1);

            float curY = getY() + offset;
            Calculate.scale(matrix, getX() + getWidth()/2, curY, 1, anim, () -> {
                fontPlayer.drawString(matrix, name, getX() + 5, curY + 2, ColorAssist.getText());
                fontPlayer.drawString(matrix, role, getX() + getWidth() - fontPlayer.getStringWidth(role) - 5, curY + 2, pColor);
            });

            maxWidth = Math.max(maxWidth, fontPlayer.getStringWidth(name) + fontPlayer.getStringWidth(role) + 20);
            offset += 12 * anim;
        }

        setWidth((int) (maxWidth + 10));
        setHeight((int) offset);
    }
}