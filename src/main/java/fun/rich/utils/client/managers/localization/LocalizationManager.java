package fun.rich.utils.client.managers.localization;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class LocalizationManager {
    private static LocalizationManager instance;

    @Getter
    @Setter
    private Language currentLanguage = Language.RU;

    private final Map<Language, Map<String, String>> translations = new HashMap<>();

    private LocalizationManager() {
        initializeTranslations();
    }

    public static LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }

    private void initializeTranslations() {
        Map<String, String> ru = new HashMap<>();
        ru.put("Search", "Поиск");
        ru.put("notif.enabled", "Включена");
        ru.put("notif.disabled", "выключен");
        ru.put("notif.lowArmor", "Внимание! У вас низкая прочность брони");
        ru.put("notif.pickupItems", "Вы подобрали: ");
        ru.put("notif.pickupShulker", "Подобран шалкер с: ");
        ru.put("notif.pickupShulkerInventory", "Предметы помещены в шалкер: ");
        ru.put("desc.ServerHelper", "Помогает взаимодействовать с сервером, предоставляя полезные функции.");
        ru.put("desc.WaterSpeed", "Увеличивает скорость передвижения в воде.");
        ru.put("desc.ItemScroller", "Настраивает поведение предметов для удобства.");
        ru.put("desc.Hud", "Отображает дополнительную информацию на экране.");
        ru.put("desc.AuctionHelper", "Ищет самые выгодные товары на аукционе.");
        ru.put("desc.ProjectilePrediction", "Предсказывает траекторию полета снарядов.");
        ru.put("desc.AntiAFK", "Выполняет какое-то действие чтобы игрока не кикнуло за афк.");
        ru.put("desc.Strafe", "Помогает игроку при ходьбе.");
        ru.put("desc.TargetStrafe", "Крутится вокруг таргета в aura.");
        ru.put("desc.Jesus", "Дает возможность игроку ходить по воде.");
        ru.put("desc.ProjectileHelper", "Помогает игроку наводится на цель.");
        ru.put("desc.XRay", "Позволяет видеть сквозь блоки для поиска ресурсов.");
        ru.put("desc.TriggerBot", "Бьет сущность если игрок смотрит на нее.");
        ru.put("desc.Aura", "Автоматически атакует ближайших врагов.");
        ru.put("desc.AutoSwap", "Автоматически меняет предметы в руке.");
        ru.put("desc.AutoPilot", "Наводит камеру игрока на ценный предмет сервера ReallyWorld");
        ru.put("desc.NoFriendDamage", "Предотвращает урон по союзникам.");
        ru.put("desc.SelfDestruct", "Скрывает чит с игры, находится в разработке.");
        ru.put("desc.AutoBuy", "Автоматически скупает ресурсы с аукциона.");
        ru.put("desc.HitBoxModule", "Изменяет размер хитбоксов сущностей.");
        ru.put("desc.AntiBot", "Обнаруживает и игнорирует ботов на сервере.");
        ru.put("desc.AutoCrystal", "Автоматизирует размещение и уничтожение кристаллов.");
        ru.put("desc.AutoSprint", "Автоматически включает спринт при движении.");
        ru.put("desc.NoPush", "Предотвращает отталкивание игроков и сущностей.");
        ru.put("desc.ElytraHelper", "Улучшает управление элитрами.");
        ru.put("desc.JoinerHelper", "Облегчает процесс входа на сервер.");
        ru.put("desc.NoDelay", "Убирает задержки при выполнении действий.");
        ru.put("desc.Velocity", "Уменьшает отбрасывание от атак.");
        ru.put("desc.AutoRespawn", "Автоматически возрождает игрока после смерти.");
        ru.put("desc.NoSlow", "Устраняет замедление при определенных действиях.");
        ru.put("desc.InventoryMove", "Позволяет двигаться при открытом интерфейсе.");
        ru.put("desc.Blink", "Создает иллюзию телепортации для других игроков.");
        ru.put("desc.AutoTool", "Автоматически выбирает подходящий инструмент.");
        ru.put("desc.Fly", "Позволяет летать в режиме выживания.");
        ru.put("desc.FastBreak", "Ускоряет разрушение блоков.");
        ru.put("desc.CameraSettings", "Настраивает поведение камеры игрока.");
        ru.put("desc.BlockOverlay", "Подсвечивает блоки для лучшей видимости.");
        ru.put("desc.Esp", "Показывает местоположение сущностей через стены.");
        ru.put("desc.AutoTotem", "Автоматически использует тотемы бессмертия.");
        ru.put("desc.EnderChestPlus", "Улучшает функционал эндер-сундука.");
        ru.put("desc.FreeCam", "Предоставляет инструменты для отладки камеры.");
        ru.put("desc.ChestStealer", "Быстро забирает предметы из контейнеров.");
        ru.put("desc.AutoAccept", "Автоматически принимает запросы и добавляет друзей в регионы");
        ru.put("desc.Arrows", "Стрелки на игроков.");
        ru.put("desc.AutoLeave", "Автоматически покидает сервер при угрозе.");
        ru.put("desc.WorldTweaks", "Настраивает параметры мира для удобства.");
        ru.put("desc.NoClip", "Позволяет проходить сквозь блоки.");
        ru.put("desc.NoRender", "Отключает рендеринг определенных объектов.");
        ru.put("desc.Optimization", "Оптимизация FPS: частицы (песок, пузыри, дым), плавная камера и движение игроков.");
        ru.put("desc.TargetPearl", "Автоматически бросает жемчужины в цель.");
        ru.put("desc.NameProtect", "Скрывает имена игроков для защиты.");
        ru.put("desc.SeeInvisible", "Позволяет видеть невидимых игроков.");
        ru.put("desc.AutoArmor", "Автоматически надевает лучшую броню.");
        ru.put("desc.AutoUse", "Автоматически использует предметы.");
        ru.put("desc.ElytraTarget", "Добавляет нужную корекцию на элитрах.");
        ru.put("desc.NoInteract", "Блокирует взаимодействие с объектами.");
        ru.put("desc.CrossHair", "Настраивает отображение прицела.");
        ru.put("desc.EventParser", "Автоматически добавляет вейпоинт на евент!");
        ru.put("desc.SuperFireWork", "Усиливает фейерверки для полета.");
        ru.put("desc.Spider", "Позволяет взбираться по стенам как паук.");
        ru.put("desc.ServerRPSpoofer", "Подделывает данные для серверов.");
        ru.put("desc.LongJump", "Длинные прыжки, эквивалент HighJump.");
        ru.put("desc.ShiftTap", "Автоматически приседает при ударе.");
        ru.put("desc.AspectRatio", "Изменяет соотношение сторон экрана.");
        ru.put("desc.FreeLook", "Позволяет свободно вращать камеру без движения игрока.");
        ru.put("desc.ClickPearl", "Автоматически использует жемчужины при клике.");
        ru.put("desc.ClickFriend", "Добавляет игроков в список друзей по клику.");
        ru.put("desc.WindJump", "Усиливает прыжки с учетом направления ветра.");
        ru.put("desc.TargetESP", "Подсвечивает цели для лучшей видимости.");
        ru.put("desc.NoWeb", "Позволяет двигаться через паутину без замедления.");
        ru.put("desc.IRC", "Встроенный чат для общения.");
        ru.put("desc.Speed", "Увеличивает скорость передвижения игрока.");
        ru.put("desc.KTLeave", "Позволяет вам ливать в пвп режиме актуален на Funtime!");
        ru.put("desc.SwingAnimation", "Настраивает анимацию взмаха руки.");
        ru.put("desc.ViewModel", "Изменяет отображение модели предметов в руке.");
        ru.put("desc.AirStuck", "Фризит игрока в воздухе, предотвращая движение.");
        ru.put("desc.NoFallDamage", "Предотвращает получение урона от падения.");
        ru.put("desc.ElytraMotion", "Улучшает управление и скорость полета на элитрах.");
        ru.put("desc.WorldParticles", "Добавляет или изменяет частицы в мире для визуального эффекта.");
        ru.put("desc.BlockESP", "Подсвечивает определенные блоки через стены.");
        ru.put("desc.TimerChestESP", "Подсвечивает сундуки по таймеру над ними: красный ≥2:00, жёлтый 0:30–2:00, зелёный ≤0:30.");
        ru.put("desc.KillEffect", "Добавляет визуальные эффекты при убийстве.");
        ru.put("desc.Theme", "Цвета и темы интерфейса.");
        ru.put("desc.default", "Описание модуля отсутствует.");

        ru.put("Settings", "Настройки");

        ru.put("value.Default", "Обычный");
        ru.put("value.Legit", "Легитный");
        ru.put("value.Normal", "Обычный");
        ru.put("value.Packet", "Пакет");
        ru.put("value.Players", "Игроки");
        ru.put("value.Mobs", "Мобы");
        ru.put("value.Animals", "Животные");
        ru.put("value.Friends", "Друзья");
        ru.put("value.Armor Stand", "Стойка для брони");
        ru.put("value.Only Critical", "Только крит");
        ru.put("value.Break Shield", "Ломать щит");
        ru.put("value.UnPress Shield", "Убирать щит");
        ru.put("value.No Attack When Eat", "Не бить когда ешь");
        ru.put("value.Ignore The Walls", "Бить сквозь стены");
        ru.put("value.Fake Lag", "Фейк лаг");
        ru.put("value.Hit Chance", "Шанс попадания");
        ru.put("value.Free", "Свободно");
        ru.put("value.Focused", "Сфокусировано");
        ru.put("value.Target", "Таргет");
        ru.put("value.Not visible", "Невидимо");
        ru.put("value.Hub", "Хаб");
        ru.put("value.Main Menu", "Главное меню");
        ru.put("value.Staff", "Стафф");
        ru.put("value.Fire", "Огонь");
        ru.put("value.Bad Effects", "Плохие эффекты");
        ru.put("value.Block Overlay", "Обводка блоков");
        ru.put("value.Darkness", "Темнота");
        ru.put("value.Damage", "Урон");
        ru.put("value.Glow", "Свечение");
        ru.put("value.Watermark", "Водяной знак");
        ru.put("value.Hot Keys", "Горячие клавиши");
        ru.put("value.Target Hud", "Худ цели");
        ru.put("value.Armor Hud", "Худ брони");
        ru.put("value.Notifications", "Уведомления");
        ru.put("value.Staff List", "Список стаффа");
        ru.put("value.Potions", "Зелья");
        ru.put("value.Module Switch", "Переключение модулей");
        ru.put("value.Staff Join", "Вход стаффа");
        ru.put("value.Staff Leave", "Выход стаффа");
        ru.put("value.Item Pick Up", "Подбор предметов");
        ru.put("value.Low Armor Durability", "Низкая прочность брони");
        ru.put("value.ReallyWorld", "ReallyWorld");
        ru.put("value.HolyWorld", "HolyWorld");
        ru.put("value.FunTime", "FunTime");
        ru.put("value.Cube", "Куб");
        ru.put("value.Circle", "Круг");
        ru.put("value.Ghosts", "Призраки");
        ru.put("value.Crystals", "Кристаллы");
        ru.put("value.WhiteList", "Белый список");
        ru.put("value.CopperDungeon", "Copper Dungeon");
        ru.put("value.Player", "Игрок");
        ru.put("value.Item", "Предмет");
        ru.put("value.TNT", "ТНТ");
        ru.put("value.Box", "Бокс");
        ru.put("value.Armor", "Броня");
        ru.put("value.NameTags", "Ники");
        ru.put("value.Hand Items", "Предметы в руке");
        ru.put("value.Corner", "Угол");
        ru.put("value.Full", "Полный");
        ru.put("value.3D Box", "3D бокс");
        ru.put("value.Skeleton", "Скелет");
        ru.put("value.Bright", "Яркость");
        ru.put("value.Time", "Время");
        ru.put("value.Fog", "Туман");
        ru.put("value.2D", "2D");
        ru.put("value.3D", "3D");
        ru.put("value.Dragon Fly", "Дракон");
        ru.put("value.CommandBlock", "Командный блок");
        ru.put("value.Boat", "Лодка");
        ru.put("value.Shulker Screen", "Шалкер");
        ru.put("value.Slime Boost", "Слизень");
        ru.put("value.FunTime Soul Sand", "FunTime Песок душ");
        ru.put("value.Always", "Всегда");
        ru.put("value.Grim Old", "Grim Old");
        ru.put("value.SpookyTime", "SpookyTime");
        ru.put("value.Funsky", "Funsky");
        ru.put("value.Legit Snap", "Legit Snap");
        ru.put("value.SpookyTims", "SpookyTims");
        ru.put("value.CakeWorld", "CakeWorld");
        ru.put("value.Diamond", "Алмаз");
        ru.put("value.Emerald", "Изумруд");
        ru.put("value.Iron", "Железо");
        ru.put("value.Gold", "Золото");
        ru.put("value.Coal", "Уголь");
        ru.put("value.Redstone", "Редстоун");

        Map<String, String> en = new HashMap<>();
        en.put("Search", "Search");
        en.put("notif.enabled", "enabled");
        en.put("notif.disabled", "disabled");
        en.put("notif.lowArmor", "Warning! Your armor durability is low!");
        en.put("notif.pickupItems", "You picked up: ");
        en.put("notif.pickupShulker", "Raised shulker with: ");
        en.put("notif.pickupShulkerInventory", "Items placed in shulker: ");
        en.put("desc.ServerHelper", "Helps interact with the server, providing useful features.");
        en.put("desc.WaterSpeed", "Increases movement speed in water.");
        en.put("desc.ItemScroller", "Configures item behavior for convenience.");
        en.put("desc.Hud", "Displays additional information on screen.");
        en.put("desc.AuctionHelper", "Finds the best deals on the auction.");
        en.put("desc.ProjectilePrediction", "Predicts projectile trajectory.");
        en.put("desc.AntiAFK", "Performs actions to prevent AFK kick.");
        en.put("desc.Strafe", "Helps player while walking.");
        en.put("desc.TargetStrafe", "Circles around target in aura.");
        en.put("desc.Jesus", "Allows player to walk on water.");
        en.put("desc.ProjectileHelper", "Helps player aim at target.");
        en.put("desc.XRay", "Allows seeing through blocks to find resources.");
        en.put("desc.TriggerBot", "Attacks entity if player is looking at it.");
        en.put("desc.Aura", "Automatically attacks nearest enemies.");
        en.put("desc.AutoSwap", "Automatically swaps items in hand.");
        en.put("desc.AutoPilot", "Aims player camera at valuable item on ReallyWorld server");
        en.put("desc.NoFriendDamage", "Prevents damage to allies.");
        en.put("desc.SelfDestruct", "Hides cheat from game, in development.");
        en.put("desc.AutoBuy", "Automatically buys resources from auction.");
        en.put("desc.HitBoxModule", "Changes entity hitbox size.");
        en.put("desc.AntiBot", "Detects and ignores bots on server.");
        en.put("desc.AutoCrystal", "Automates crystal placement and destruction.");
        en.put("desc.AutoSprint", "Automatically enables sprint when moving.");
        en.put("desc.NoPush", "Prevents being pushed by players and entities.");
        en.put("desc.ElytraHelper", "Improves elytra control.");
        en.put("desc.JoinerHelper", "Eases server joining process.");
        en.put("desc.NoDelay", "Removes delays when performing actions.");
        en.put("desc.Velocity", "Reduces knockback from attacks.");
        en.put("desc.AutoRespawn", "Automatically respawns player after death.");
        en.put("desc.NoSlow", "Removes slowdown from certain actions.");
        en.put("desc.InventoryMove", "Allows movement with open interface.");
        en.put("desc.Blink", "Creates illusion of teleportation for other players.");
        en.put("desc.AutoTool", "Automatically selects suitable tool.");
        en.put("desc.Fly", "Allows flying in survival mode.");
        en.put("desc.FastBreak", "Speeds up block breaking.");
        en.put("desc.CameraSettings", "Configures player camera behavior.");
        en.put("desc.BlockOverlay", "Highlights blocks for better visibility.");
        en.put("desc.Esp", "Shows entity locations through walls.");
        en.put("desc.AutoTotem", "Automatically uses totems of undying.");
        en.put("desc.EnderChestPlus", "Improves ender chest functionality.");
        en.put("desc.FreeCam", "Provides camera debugging tools.");
        en.put("desc.ChestStealer", "Quickly takes items from containers.");
        en.put("desc.AutoAccept", "Automatically accepts requests and adds friends to regions");
        en.put("desc.Arrows", "Arrows on players.");
        en.put("desc.AutoLeave", "Automatically leaves server when threatened.");
        en.put("desc.WorldTweaks", "Configures world parameters for convenience.");
        en.put("desc.NoClip", "Allows passing through blocks.");
        en.put("desc.NoRender", "Disables rendering of certain objects.");
        en.put("desc.Optimization", "FPS optimization: particles (sand, bubbles, smoke), smooth camera and player movement.");
        en.put("desc.TargetPearl", "Automatically throws pearls at target.");
        en.put("desc.NameProtect", "Hides player names for protection.");
        en.put("desc.SeeInvisible", "Allows seeing invisible players.");
        en.put("desc.AutoArmor", "Automatically equips best armor.");
        en.put("desc.AutoUse", "Automatically uses items.");
        en.put("desc.ElytraTarget", "Adds needed correction on elytra.");
        en.put("desc.NoInteract", "Blocks interaction with objects.");
        en.put("desc.CrossHair", "Configures crosshair display.");
        en.put("desc.EventParser", "Automatically adds waypoint on event!");
        en.put("desc.SuperFireWork", "Enhances fireworks for flight.");
        en.put("desc.Spider", "Allows climbing walls like a spider.");
        en.put("desc.ServerRPSpoofer", "Fakes data for servers.");
        en.put("desc.LongJump", "Long jumps, equivalent to HighJump.");
        en.put("desc.ShiftTap", "Automatically crouches when attacking.");
        en.put("desc.AspectRatio", "Changes screen aspect ratio.");
        en.put("desc.FreeLook", "Allows free camera rotation without player movement.");
        en.put("desc.ClickPearl", "Automatically uses pearls on click.");
        en.put("desc.ClickFriend", "Adds players to friend list on click.");
        en.put("desc.WindJump", "Enhances jumps considering wind direction.");
        en.put("desc.TargetESP", "Highlights targets for better visibility.");
        en.put("desc.NoWeb", "Allows moving through cobwebs without slowdown.");
        en.put("desc.IRC", "Built-in chat for communication.");
        en.put("desc.Speed", "Increases player movement speed.");
        en.put("desc.KTLeave", "Allows you to leave in PvP mode, relevant on Funtime!");
        en.put("desc.SwingAnimation", "Configures hand swing animation.");
        en.put("desc.ViewModel", "Changes item model display in hand.");
        en.put("desc.AirStuck", "Freezes player in air, preventing movement.");
        en.put("desc.NoFallDamage", "Prevents fall damage.");
        en.put("desc.ElytraMotion", "Improves control and flight speed on elytra.");
        en.put("desc.WorldParticles", "Adds or changes particles in world for visual effect.");
        en.put("desc.BlockESP", "Highlights certain blocks through walls.");
        en.put("desc.KillEffect", "Adds visual effects on kill.");
        en.put("desc.Theme", "Interface colors and themes.");
        en.put("desc.default", "Module description missing.");

        en.put("Settings", "Settings");

        translations.put(Language.RU, ru);
        translations.put(Language.EN, en);
    }

    public String translate(String key) {
        Map<String, String> langMap = translations.get(currentLanguage);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        return key;
    }

    public String translateValue(String value) {
        if (value == null || value.isBlank()) return value;
        String key = "value." + value;
        String translated = translate(key);
        return translated.equals(key) ? value : translated;
    }

    public enum Language {
        RU, EN
    }
}
