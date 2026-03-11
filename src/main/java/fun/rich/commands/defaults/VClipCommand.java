package fun.rich.commands.defaults;

import net.minecraft.util.Formatting;

import fun.rich.utils.client.managers.api.command.Command;
import fun.rich.utils.client.managers.api.command.argument.IArgConsumer;
import fun.rich.utils.client.managers.api.command.exception.CommandException;
import fun.rich.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.rich.utils.display.interfaces.QuickImports;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class VClipCommand extends Command implements QuickImports {

    protected VClipCommand() {
        super("vclip");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);

        if (mc.player == null || mc.world == null) {
            logDirect("Игрок не в мире!", Formatting.RED);
            return;
        }

        int offset;
        String arg = args.getString().toLowerCase(Locale.US);

        switch (arg) {
            case "up" -> offset = 1;
            case "down" -> offset = -1;
            default -> {
                try {
                    offset = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    logDirect("Использование: .vclip <число|up|down> — положительное = вверх, отрицательное = вниз", Formatting.RED);
                    return;
                }
            }
        }

        double newY = mc.player.getY() + offset;
        mc.player.setPos(mc.player.getX(), newY, mc.player.getZ());

        String direction = offset > 0 ? "вверх" : "вниз";
        logDirect("Телепортирован на " + Math.abs(offset) + " блок(ов) " + direction, Formatting.GREEN);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return new TabCompleteHelper().append("up", "down").stream();
        }
        if (args.hasExactlyOne()) {
            String arg = args.peekString();
            return new TabCompleteHelper()
                    .append("up", "down", "-10", "10")
                    .filterPrefix(arg)
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Вертикальная телепортация (вверх/вниз на N блоков)";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Телепортирует игрока вертикально на указанное количество блоков.",
                "",
                "Использование:",
                "> vclip <число> — положительное число = вверх, отрицательное = вниз",
                "> vclip up — подняться на 1 блок вверх",
                "> vclip down — опуститься на 1 блок вниз",
                "",
                "Примеры:",
                "> vclip 10 — подняться на 10 блоков",
                "> vclip -10 — опуститься на 10 блоков"
        );
    }
}
