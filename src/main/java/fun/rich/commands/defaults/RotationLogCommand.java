package fun.rich.commands.defaults;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.utils.client.managers.api.command.Command;
import fun.rich.utils.client.managers.api.command.argument.IArgConsumer;
import fun.rich.utils.client.managers.api.command.exception.CommandException;
import fun.rich.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.rich.utils.features.aura.rotations.RotationLogger;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RotationLogCommand extends Command {

    protected RotationLogCommand() {
        super("rotationlog", "rotlog");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase() : "status";
        RotationLogger logger = RotationLogger.get();

        switch (action) {
            case "on", "start", "enable" -> {
                logger.enable();
                Path file = logger.getCurrentFile();
                logDirect("RotationLogger: " + Formatting.GREEN + "ON" + Formatting.GRAY + (file != null ? (" -> " + file) : ""), Formatting.GRAY);
            }
            case "off", "stop", "disable" -> {
                logger.disable();
                logDirect("RotationLogger: " + Formatting.RED + "OFF", Formatting.GRAY);
            }
            case "status" -> {
                Path file = logger.getCurrentFile();
                logDirect("RotationLogger: " + (logger.isEnabled() ? (Formatting.GREEN + "ON") : (Formatting.RED + "OFF")) + Formatting.GRAY + (file != null ? (" -> " + file) : ""), Formatting.GRAY);
            }
            default -> logDirect("Использование: .rotationlog on|off|status", Formatting.GRAY);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAtMost(1)) {
            return new TabCompleteHelper().sortAlphabetically().append("on", "off", "status").filterPrefix(args.hasAny() ? args.getString() : "").stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Логирует yaw/pitch и флаги ротации";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда логирования ротации (yaw/pitch) в папку rotation/",
                "",
                "Использование:",
                "> rotationlog on - включить логирование",
                "> rotationlog off - выключить",
                "> rotationlog status - статус"
        );
    }
}

