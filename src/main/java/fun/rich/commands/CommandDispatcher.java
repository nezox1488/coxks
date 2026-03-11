/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package fun.rich.commands;

import net.minecraft.util.Pair;
import fun.rich.Rich;
import fun.rich.utils.client.managers.api.command.argument.ICommandArgument;
import fun.rich.utils.client.managers.api.command.exception.CommandNotEnoughArgumentsException;
import fun.rich.utils.client.managers.api.command.exception.CommandNotFoundException;
import fun.rich.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.rich.utils.client.managers.api.command.manager.ICommandManager;
import fun.rich.utils.client.managers.event.EventManager;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.events.chat.ChatEvent;
import fun.rich.events.chat.TabCompleteEvent;
import fun.rich.utils.display.interfaces.QuickLogger;
import fun.rich.commands.argument.ArgConsumer;
import fun.rich.commands.argument.CommandArguments;
import fun.rich.commands.defaults.LegitMoveCommand;
import fun.rich.commands.manager.CommandRepository;
import net.minecraft.util.Formatting;
import baritone.api.BaritoneAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static fun.rich.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class CommandDispatcher implements QuickLogger {
    private final ICommandManager manager;
    public static String prefix = ".";

    public CommandDispatcher(EventManager eventManager) {
        this.manager = Rich.getInstance().getCommandRepository();
        eventManager.register(this);
    }

    /** Префикс # для команд (например #legitmove). */
    public static final String HASH_PREFIX = "#";

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage();

        boolean forceRun = msg.startsWith(FORCE_COMMAND_PREFIX);
        if (msg.startsWith(prefix) || forceRun) {
            // Команды клиента: только . или force-префикс
            event.cancel();
            int skip = forceRun ? FORCE_COMMAND_PREFIX.length() : prefix.length();
            String commandStr = msg.substring(skip).trim();
            if (!runCommand(commandStr) && !commandStr.isEmpty()) {
                new CommandNotFoundException(CommandRepository.expand(commandStr).getLeft()).handle(null, null);
            }
            return;
        }
        if (msg.startsWith(HASH_PREFIX)) {
            // # — префикс Baritone. Перехватываем только #legitmove / #legit, остальное уходит в Baritone
            String commandStr = msg.substring(HASH_PREFIX.length()).trim();
            String first = commandStr.isEmpty() ? "" : commandStr.split("\\s+", 2)[0];
            if (first.equalsIgnoreCase("legitmove") || first.equalsIgnoreCase("legit")) {
                event.cancel();
                if (LegitMoveCommand.isLegitModeActive()) {
                    logDirect(Formatting.GRAY + "Baritone: легитный режим уже включён. Повторная отправка не отключает.", Formatting.GRAY);
                } else {
                    LegitMoveCommand.setLegitModeActive(true);
                    LegitMoveCommand.applyLegitBaritoneSettings();
                    logDirect(Formatting.GREEN + "Baritone: включён легитный режим движения.", Formatting.GRAY);
                }
            }
            // иначе не отменяем — Baritone сам обработает #help, #goto и т.д.
        }
    }

    public boolean runCommand(String msg) {
        if (msg.isEmpty()) {
            return this.runCommand("help");
        }
        Pair<String, List<ICommandArgument>> pair = CommandRepository.expand(msg);
        String command = pair.getLeft();
        String rest = msg.substring(pair.getLeft().length());
        ArgConsumer argc = new ArgConsumer(this.manager, pair.getRight());
       /* if (!argc.hasAny()) {
            Settings.Setting setting = settings.byLowerName.get(command.toLowerCase(Locale.US));
            if (setting != null) {
                logRanCommand(command, rest);
                if (setting.getValueClass() == Boolean.class) {
                    this.manager.execute(String.format("set toggle %s", setting.getName()));
                } else {
                    this.manager.execute(String.format("set %s", setting.getName()));
                }
                return true;
            }
        } else if (argc.hasExactlyOne()) {
            for (Settings.Setting setting : settings.allSettings) {
                if (setting.isJavaOnly()) {
                    continue;
                }
                if (setting.getName().equalsIgnoreCase(pair.getA())) {
                    logRanCommand(command, rest);
                    try {
                        this.manager.execute(String.format("set %s %s", setting.getName(), argc.getString()));
                    } catch (CommandNotEnoughArgumentsException ignored) {
                    } // The operation is safe
                    return true;
                }
            }
        }*/

        // If the command exists, then handle echoing the input

        return this.manager.execute(pair);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String eventPrefix = event.prefix;

        // Для Baritone-префикса (#...) вообще не лезем — пусть Baritone сам даёт подсказки
        if (!eventPrefix.startsWith(prefix)) {
            return;
        }

        // Подсказки только для клиентских команд с точкой
        String msg = eventPrefix.substring(prefix.length()).trim();
        List<ICommandArgument> args = CommandArguments.from(msg, true);
        Stream<String> stream = tabComplete(msg);
        final String p = prefix;
        if (args.size() == 1) {
            stream = stream.map(x -> p + x);
        }
        event.completions = stream.toArray(String[]::new);
    }

    public Stream<String> tabComplete(String msg) {
        try {
            List<ICommandArgument> args = CommandArguments.from(msg, true);
            ArgConsumer argc = new ArgConsumer(this.manager, args);
            if (argc.hasAtMost(2)) {
                if (argc.hasExactly(1)) {
                    return new TabCompleteHelper()
                            .addCommands(this.manager)
                            .filterPrefix(argc.getString())
                            .stream();
                }
          /*      Settings.Setting setting = settings.byLowerName.get(argc.getString().toLowerCase(Locale.US));
                if (setting != null && !setting.isJavaOnly()) {
                    if (setting.getValueClass() == Boolean.class) {
                        TabCompleteHelper helper = new TabCompleteHelper();
                        if ((Boolean) setting.value) {
                            helper.append("true", "false");
                        } else {
                            helper.append("false", "true");
                        }
                        return helper.filterPrefix(argc.getString()).stream();
                    } else {
                        return Stream.of(SettingsUtil.settingValueToString(setting));
                    }
                }*/
            }
            return this.manager.tabComplete(msg);
        } catch (CommandNotEnoughArgumentsException ignored) { // Shouldn't happen, the operation is safe
            return Stream.empty();
        }
    }
}
