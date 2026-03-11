package fun.rich.commands.defaults;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import fun.rich.utils.client.managers.api.command.Command;
import fun.rich.utils.client.managers.api.command.argument.IArgConsumer;
import fun.rich.utils.client.managers.api.command.exception.CommandException;
import fun.rich.utils.display.interfaces.QuickImports;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Команда #legitmove — переводит Baritone в максимально легитный режим:
 * движение как у настоящего игрока (без паркура, без диагоналей, плавный поворот головы, без спринта в воде).
 */
public class LegitMoveCommand extends Command implements QuickImports {

    private static boolean legitModeActive = false;

    public static boolean isLegitModeActive() {
        return legitModeActive;
    }

    public static void setLegitModeActive(boolean active) {
        legitModeActive = active;
    }

    public LegitMoveCommand() {
        super("legitmove", "legit");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        applyLegitBaritoneSettings();
        logDirect(Formatting.GREEN + "Baritone: включён легитный режим движения (как у настоящего игрока).", Formatting.GRAY);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Включить максимально легитное движение Baritone (как у настоящего игрока). Использование: .legitmove или #legitmove";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Переводит Baritone в режим «легит»: без паркура, без диагонального спуска/подъёма,",
                "плавный поворот головы, без спринта в воде, только обычная ходьба/бег и прыжки.",
                "",
                "Использование: .legitmove или #legitmove"
        );
    }

    /**
     * Применяет настройки Baritone для максимально легитного движения (как у реального игрока).
     */
    public static void applyLegitBaritoneSettings() {
        try {
            Settings s = BaritoneAPI.getSettings();
            // Движение: только обычная ходьба/бег, без паркура и диагоналей
            s.allowSprint.value = true;           // игроки бегут при достаточном голоде
            s.allowParkour.value = false;
            s.allowParkourPlace.value = false;
            s.allowParkourAscend.value = false;
            s.allowDiagonalDescend.value = false;
            s.allowDiagonalAscend.value = false;
            s.allowJumpAt256.value = false;
            s.sprintInWater.value = false;        // в воде не спринтим, как настоящий игрок
            s.overshootTraverse.value = false;     // не «проскакивать» блок вперёд
            s.sprintAscends.value = true;         // спринт вверх по лестнице — нормально для игрока
            // Взгляд: плавный и «человеческий»
            try { s.smoothLook.value = true; } catch (Throwable ignored) {}
            try { s.smoothLookTicks.value = 8; } catch (Throwable ignored) {}
            try { s.randomLooking.value = 0.02; } catch (Throwable ignored) {}
            try { s.randomLooking113.value = 0.5; } catch (Throwable ignored) {}
            // allowBreak / allowPlace не трогаем — легит только стиль движения; майнить/строить можно через #path, #mine и т.д.
            try { s.legitMine.value = true; } catch (Throwable ignored) {}
            // Без «чит»-допущений
            s.assumeStep.value = false;
            s.assumeSafeWalk.value = false;
            s.assumeWalkOnWater.value = false;
            s.assumeWalkOnLava.value = false;
            s.allowWaterBucketFall.value = false;
        } catch (Throwable t) {
            // Часть настроек может отсутствовать в другой версии Baritone
        }
    }
}
