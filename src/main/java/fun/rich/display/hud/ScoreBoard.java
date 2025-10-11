//package fun.rich.common.managers.draggables;
//
//import net.minecraft.client.gui.DrawContext;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.scoreboard.*;
//import net.minecraft.text.MutableText;
//import net.minecraft.text.Text;
//import fun.rich.common.managers.api.draggable.AbstractDraggable;
//import fun.rich.display.api.font.FontRenderer;
//import fun.rich.display.api.font.Fonts;
//import fun.rich.display.api.shape.ShapeProperties;
//import fun.rich.util.display.color.ColorUtil;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Objects;
//
//public class ScoreBoard extends AbstractDraggable {
//    private List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
//    private ScoreboardObjective objective;
//
//    public ScoreBoard() {
//        super("Score Board", 10, 100, 100, 120,true);
//    }
//
//    @Override
//    public boolean visible() {
//        return !scoreboardEntries.isEmpty();
//    }
//
//    @Override
//    public void tick() {
//        objective = Objects.requireNonNull(mc.world).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
//        scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(objective).stream().sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).toList();
//    }
//
//    @Override
//    public void drawDraggable(DrawContext context) {
//        MatrixStack matrix = context.getMatrices();
//        FontRenderer font = Fonts.getSize(16);
//        MutableText text = Text.empty();
//        Text mainText = objective != null ? objective.getDisplayName() : Text.empty();
//
//        scoreboardEntries.forEach(entry -> text.append(Team.decorateName(Objects.requireNonNull(mc.world).getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).append("\n"));
//
//        int padding = 3;
//        int offsetText = 14;
//        int width = (int) Math.max(font.getStringWidth(text) + padding * 2 + 1,100);
//
//        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F).quality(5)
//                .round(5,0,5,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline(0)).color(ColorUtil.getRect(0.85F)).build());
//
//        blur.render(ShapeProperties.create(matrix, getX(), getY() + 16.5F, getWidth(), getHeight() - 15).quality(5)
//                .round(0,4,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline(0)).color(ColorUtil.getRect(0.6F)).build());
//
//        font.drawText(matrix, mainText, (int) (getX() + (getWidth() - font.getStringWidth(mainText)) / 2),getY() + padding + 3.5F);
//        font.drawText(matrix, text, getX() + padding,getY() + offsetText + padding);
//
//        if (getX() > mc.getWindow().getScaledWidth() / 2) setX(getX() + getWidth() - width);
//        setWidth(width);
//        setHeight((int) (font.getStringHeight(text) / 2.16 + offsetText + padding));
//    }
//}
