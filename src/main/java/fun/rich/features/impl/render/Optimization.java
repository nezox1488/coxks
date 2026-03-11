package fun.rich.features.impl.render;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.Instance;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Optimization extends Module {

    public static Optimization getInstance() {
        return Instance.get(Optimization.class);
    }

    /** Отключает тяжёлые частицы для экономии FPS */
    public MultiSelectSetting particles = new MultiSelectSetting("Частицы", "Оптимизация частиц")
            .value("Песок и пыль", "Пузыри (душ, вода)", "Дым и огонь")
            .selected();

    /** Плавная камера и прицел (как при высоком FPS) */
    public MultiSelectSetting smooth = new MultiSelectSetting("Плавность", "Плавное движение")
            .value("Плавная камера", "Плавные игроки")
            .selected("Плавная камера", "Плавные игроки");

    /** Сила плавности: меньше = плавнее (больше интерполяции) */
    public SliderSettings smoothStrength = new SliderSettings("Сила плавности", "Чем меньше — тем плавнее камера и игроки")
            .range(0.05f, 0.95f).setValue(0.25f).visible(() -> smooth.getSelected().stream().anyMatch(s -> !s.isEmpty()));

    public Optimization() {
        super("Optimization", "Optimization", ModuleCategory.RENDER);
        setup(particles, smooth, smoothStrength);
    }

    public boolean shouldOptimizeSandDust() {
        return isState() && particles.isSelected("Песок и пыль");
    }

    public boolean shouldOptimizeBubbles() {
        return isState() && particles.isSelected("Пузыри (душ, вода)");
    }

    public boolean shouldOptimizeSmokeFire() {
        return isState() && particles.isSelected("Дым и огонь");
    }

    public boolean isSmoothCameraEnabled() {
        return isState() && smooth.isSelected("Плавная камера");
    }

    public boolean isSmoothPlayersEnabled() {
        return isState() && smooth.isSelected("Плавные игроки");
    }

    /** Коэффициент интерполяции для плавности: 1488 = без изменений (стандартный tickDelta) */
    public float getSmoothInterpolationFactor() {
        return smoothStrength.getValue();
    }

    /** Сглаженный tickDelta для плавного рендера других игроков (обновляется из миксина) */
    private static float smoothRenderDelta = 0.5f;

    public static void updateSmoothRenderDelta(float tickDelta) {
        Optimization opt = getInstance();
        if (opt != null && opt.isSmoothPlayersEnabled()) {
            float strength = opt.getSmoothInterpolationFactor();
            smoothRenderDelta += (tickDelta - smoothRenderDelta) * Math.max(0.1f, strength);
        } else {
            smoothRenderDelta = tickDelta;
        }
    }

    public static float getSmoothRenderDelta(float vanillaTickDelta) {
        Optimization opt = getInstance();
        if (opt != null && opt.isSmoothPlayersEnabled()) {
            return smoothRenderDelta;
        }
        return vanillaTickDelta;
    }
}
