package org.krevetka.holoTopography.core;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.krevetka.holoTopography.HoloTopography;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный движок рендеринга 3D-карты
 */
public class Engine {
    private final Map<UUID, HologramSession> activeSessions = new ConcurrentHashMap<>();
    private final int particlesPerChunk;
    private final JavaPlugin plugin;

    public Engine(double defaultRenderDistance, int particlesPerChunk) {
        this.particlesPerChunk = particlesPerChunk;
        this.plugin = JavaPlugin.getPlugin(HoloTopography.class);
        initializeBlockColors();
    }

    public void startStaticMapUpdate(Player player, UUID playerId, Location initialCenter, double renderDistance, Location displayLocation) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !hasActiveSession(playerId)) {
                    cancel();
                    plugin.getLogger().info("[UPDATE_TIMER] Игрок не в сети или сессия не активна, таймер остановлен.");
                    return;
                }
                plugin.getLogger().info("[UPDATE_TIMER] Запускаем новую InitialScanTask для игрока: " + player.getName());
                new InitialScanTask(playerId, initialCenter, renderDistance, displayLocation).runTaskAsynchronously(plugin);
            }
        }.runTaskTimer(plugin, 0L, 20L); // Обновление каждую секунду (20 тиков)
    }

    /**
     * Инициализация голограммы для игрока
     */
    public void createSession(Player player, Location center, double renderDistance) {
        // Остановить текущую сессию, если она есть
        stopSession(player.getUniqueId());
        // Определяем статичное местоположение для отображения
        Location displayLocation = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        player.sendMessage(ChatColor.YELLOW + "Запущено первичное сканирование...");
        new InitialScanTask(player.getUniqueId(), center, renderDistance, displayLocation).runTaskAsynchronously(plugin);
        // Запускаем циклическое обновление карты с небольшой задержкой
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startStaticMapUpdate(player, player.getUniqueId(), center, renderDistance, displayLocation);
        }, 5L); // Задержка в 5 тиков
    }
    
    /**
     * Остановка сессии для игрока
     */
    public boolean stopSession(UUID playerId) {
        HologramSession session = activeSessions.remove(playerId);
        if (session != null && session.task() != null) {
            session.task().cancel();
            return true;
        }
        return false;
    }

    /**
     * Проверяет, есть ли активная сессия у игрока
     */
    /**
     * Проверяет, есть ли активная сессия у игрока
     */
    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * Возвращает информацию о сессии игрока
     */
    public HologramInfo getSessionInfo(UUID playerId) {
        HologramSession session = activeSessions.get(playerId);
        if (session == null) {
            return null;
        }
        return new HologramInfo(
                session.playerId(),
                session.center(),
                session.createdAt(),
                session.renderDistance() // Возможно, стоит пересмотреть, что возвращать здесь
        );
    }
    
    /**
     * Информация о голограмме (для команд и API)
     */
    public record HologramInfo(UUID playerId, Location center, long createdAt, double renderDistance) {}

    private final Map<Material, Color> blockColors = new HashMap<>();

    private void initializeBlockColors() {
        // Основные природные блоки
        blockColors.put(Material.GRASS_BLOCK, Color.fromRGB(102, 153, 51));
        blockColors.put(Material.DIRT, Color.fromRGB(139, 69, 19));
        blockColors.put(Material.STONE, Color.fromRGB(128, 128, 128));
        blockColors.put(Material.COBBLESTONE, Color.fromRGB(112, 112, 112));
        blockColors.put(Material.SAND, Color.fromRGB(255, 255, 153));
        blockColors.put(Material.GRAVEL, Color.fromRGB(169, 169, 169));
        blockColors.put(Material.CLAY, Color.fromRGB(159, 121, 93));
        blockColors.put(Material.WATER, Color.fromRGB(64, 64, 255));
        blockColors.put(Material.SANDSTONE, Color.fromRGB(210, 180, 140));
        blockColors.put(Material.RED_SAND, Color.fromRGB(255, 153, 51));
        blockColors.put(Material.RED_SANDSTONE, Color.fromRGB(205, 92, 92));
        blockColors.put(Material.SNOW, Color.fromRGB(255, 255, 255));
        blockColors.put(Material.ICE, Color.fromRGB(173, 216, 230));
        blockColors.put(Material.PACKED_ICE, Color.fromRGB(160, 224, 224));
        blockColors.put(Material.BLUE_ICE, Color.fromRGB(96, 160, 192));
        blockColors.put(Material.MUD, Color.fromRGB(79, 50, 35));
        blockColors.put(Material.MUDDY_MANGROVE_ROOTS, Color.fromRGB(91, 59, 41));

        // Растительность
        blockColors.put(Material.TALL_GRASS, Color.fromRGB(102, 153, 51));
        blockColors.put(Material.FERN, Color.fromRGB(102, 153, 51));
        blockColors.put(Material.LARGE_FERN, Color.fromRGB(102, 153, 51));
        blockColors.put(Material.POPPY, Color.fromRGB(255, 0, 0));
        blockColors.put(Material.DANDELION, Color.fromRGB(255, 255, 0));
        blockColors.put(Material.BLUE_ORCHID, Color.fromRGB(64, 64, 255));
        blockColors.put(Material.ALLIUM, Color.fromRGB(139, 0, 139));
        blockColors.put(Material.AZURE_BLUET, Color.fromRGB(240, 248, 255));
        blockColors.put(Material.RED_TULIP, Color.fromRGB(255, 0, 0));
        blockColors.put(Material.ORANGE_TULIP, Color.fromRGB(255, 165, 0));
        blockColors.put(Material.PINK_TULIP, Color.fromRGB(255, 182, 193));
        blockColors.put(Material.WHITE_TULIP, Color.fromRGB(255, 255, 255));
        blockColors.put(Material.OXEYE_DAISY, Color.fromRGB(255, 255, 255));
        blockColors.put(Material.CORNFLOWER, Color.fromRGB(100, 149, 237));
        blockColors.put(Material.LILY_OF_THE_VALLEY, Color.fromRGB(255, 255, 255));
        blockColors.put(Material.WITHER_ROSE, Color.fromRGB(0, 0, 0));
        blockColors.put(Material.SUNFLOWER, Color.fromRGB(255, 255, 0));
        blockColors.put(Material.LILAC, Color.fromRGB(171, 130, 255));
        blockColors.put(Material.ROSE_BUSH, Color.fromRGB(255, 0, 0));
        blockColors.put(Material.PEONY, Color.fromRGB(255, 182, 193));
        blockColors.put(Material.DANDELION, Color.fromRGB(255, 255, 0));
        blockColors.put(Material.LILY_PAD, Color.fromRGB(34, 139, 34));
        blockColors.put(Material.SUGAR_CANE, Color.fromRGB(224, 224, 224));
        blockColors.put(Material.KELP, Color.fromRGB(34, 139, 34));
        blockColors.put(Material.SEAGRASS, Color.fromRGB(34, 139, 34));
        blockColors.put(Material.SEA_PICKLE, Color.fromRGB(0, 255, 0));
        blockColors.put(Material.BAMBOO, Color.fromRGB(245, 245, 220));
        blockColors.put(Material.MOSS_BLOCK, Color.fromRGB(0, 128, 0));
        blockColors.put(Material.MOSS_CARPET, Color.fromRGB(0, 128, 0));
        blockColors.put(Material.VINE, Color.fromRGB(0, 100, 0));
        blockColors.put(Material.TWISTING_VINES, Color.fromRGB(148, 0, 211));
        blockColors.put(Material.CAVE_VINES, Color.fromRGB(173, 255, 47));
        blockColors.put(Material.SWEET_BERRY_BUSH, Color.fromRGB(139, 69, 19)); // Цвет куста

        // Деревья
        blockColors.put(Material.OAK_LOG, Color.fromRGB(139, 69, 19));
        blockColors.put(Material.SPRUCE_LOG, Color.fromRGB(110, 55, 15));
        blockColors.put(Material.BIRCH_LOG, Color.fromRGB(210, 180, 140));
        blockColors.put(Material.JUNGLE_LOG, Color.fromRGB(145, 105, 60));
        blockColors.put(Material.ACACIA_LOG, Color.fromRGB(179, 99, 44));
        blockColors.put(Material.DARK_OAK_LOG, Color.fromRGB(89, 48, 23));
        blockColors.put(Material.MANGROVE_LOG, Color.fromRGB(91, 59, 41));
        blockColors.put(Material.CHERRY_LOG, Color.fromRGB(160, 82, 45)); // Примерно как дуб
        blockColors.put(Material.OAK_LEAVES, Color.fromRGB(0, 128, 0));
        blockColors.put(Material.SPRUCE_LEAVES, Color.fromRGB(0, 100, 0));
        blockColors.put(Material.BIRCH_LEAVES, Color.fromRGB(154, 205, 50));
        blockColors.put(Material.JUNGLE_LEAVES, Color.fromRGB(0, 139, 0));
        blockColors.put(Material.ACACIA_LEAVES, Color.fromRGB(143, 188, 143));
        blockColors.put(Material.DARK_OAK_LEAVES, Color.fromRGB(85, 107, 47));
        blockColors.put(Material.MANGROVE_LEAVES, Color.fromRGB(0, 128, 0)); // Примерно как дуб
        blockColors.put(Material.CHERRY_LEAVES, Color.fromRGB(255, 182, 193)); // Розоватый

        // Руды (могут быть видны на поверхности)
        blockColors.put(Material.COAL_ORE, Color.fromRGB(0, 0, 0));
        blockColors.put(Material.IRON_ORE, Color.fromRGB(189, 183, 107));
        blockColors.put(Material.GOLD_ORE, Color.fromRGB(255, 215, 0));
        blockColors.put(Material.REDSTONE_ORE, Color.fromRGB(255, 0, 0));
        blockColors.put(Material.LAPIS_ORE, Color.fromRGB(25, 25, 112));
        blockColors.put(Material.DIAMOND_ORE, Color.fromRGB(0, 139, 139));
        blockColors.put(Material.EMERALD_ORE, Color.fromRGB(0, 255, 0));
        blockColors.put(Material.COPPER_ORE, Color.fromRGB(205, 127, 50));

        // Грибы (на поверхности в определенных биомах)
        blockColors.put(Material.BROWN_MUSHROOM, Color.fromRGB(139, 69, 19));
        blockColors.put(Material.RED_MUSHROOM, Color.fromRGB(255, 0, 0));
        blockColors.put(Material.MUSHROOM_STEM, Color.fromRGB(245, 245, 245));

        // Блоки, созданные игроками, которые могут быть на поверхности
        blockColors.put(Material.OAK_PLANKS, Color.fromRGB(160, 82, 45));
        blockColors.put(Material.SPRUCE_PLANKS, Color.fromRGB(139, 69, 19));
        blockColors.put(Material.BIRCH_PLANKS, Color.fromRGB(245, 245, 220));
        blockColors.put(Material.JUNGLE_PLANKS, Color.fromRGB(244, 164, 96));
        blockColors.put(Material.ACACIA_PLANKS, Color.fromRGB(255, 140, 0));
        blockColors.put(Material.DARK_OAK_PLANKS, Color.fromRGB(72, 61, 139));
        blockColors.put(Material.MANGROVE_PLANKS, Color.fromRGB(91, 59, 41));
        blockColors.put(Material.CHERRY_PLANKS, Color.fromRGB(160, 82, 45)); // Примерно как дуб

        blockColors.put(Material.STONE_BRICKS, Color.fromRGB(112, 112, 112));
        blockColors.put(Material.BRICKS, Color.fromRGB(178, 34, 34));

        // Некоторые структуры, генерируемые на поверхности
        blockColors.put(Material.COBWEB, Color.fromRGB(255, 255, 255)); // Белый
        blockColors.put(Material.BEE_NEST, Color.fromRGB(255, 215, 0)); // Золотистый
    }

    private Color getBlockColor(Material material) {
        return blockColors.getOrDefault(material, Color.fromRGB(150, 150, 150)); // Серый по умолчанию
    }

    /**
     * Сессия голограммы
     */
    record HologramSession(UUID playerId, Location center, long createdAt, double renderDistance, BukkitTask task, BukkitTask updateTask,
                           List<Pair<Vector, Material>> initialBlockData,
                           double minWorldX, double maxWorldX, double minWorldY, double maxWorldY, double minWorldZ, double maxWorldZ) {}

    private class InitialScanTask extends BukkitRunnable {
        private final UUID playerId;
        private final Location center;
        private final double renderDistance;
        private final Location displayLocation;

        public InitialScanTask(final UUID playerId, final Location center, final double renderDistance, final Location displayLocation) {
            this.playerId = playerId;
            this.center = center;
            this.renderDistance = renderDistance;
            this.displayLocation = displayLocation;
        }

        @Override
        public void run() {
            Player player = Bukkit.getPlayer(playerId);
            if  (player == null || !player.isOnline()) {
                return;
            }

            HologramSession existingSession = activeSessions.get(playerId);
            if (existingSession != null && existingSession.task() != null) {
                existingSession.task().cancel(); // Отменяем предыдущую задачу отображения
            }

            final List<Pair<Vector, Material>> blockData = new ArrayList<>();
            double minWorldX = Double.MAX_VALUE, maxWorldX = Double.MIN_VALUE;
            double minWorldY = Double.MAX_VALUE, maxWorldY = Double.MIN_VALUE;
            double minWorldZ = Double.MAX_VALUE, maxWorldZ = Double.MIN_VALUE;
            final World world = center.getWorld();

            for (int x = (int) -renderDistance; x <= renderDistance; x++) {
                for (int z = (int) -renderDistance; z <= renderDistance; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= renderDistance) {
                        int worldY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                        Block block = world.getBlockAt(center.getBlockX() + x, worldY, center.getBlockZ() + z);
                        Vector offset = new Vector(x, worldY - center.getBlockY(), z);
                        blockData.add(ImmutablePair.of(offset, block.getType()));

                        minWorldX = Math.min(minWorldX, offset.getX());
                        maxWorldX = Math.max(maxWorldX, offset.getX());
                        minWorldY = Math.min(minWorldY, offset.getY());
                        maxWorldY = Math.max(maxWorldY, offset.getY());
                        minWorldZ = Math.min(minWorldZ, offset.getZ());
                        maxWorldZ = Math.max(maxWorldZ, offset.getZ());
                    }
                }
            }

            final double finalMinWorldX = minWorldX;
            final double finalMaxWorldX = maxWorldX;
            final double finalMinWorldY = minWorldY;
            final double finalMaxWorldY = maxWorldY;
            final double finalMinWorldZ = minWorldZ;
            final double finalMaxWorldZ = maxWorldZ;
            final List<Pair<Vector, Material>> finalBlockData = new ArrayList<>(blockData);

            Bukkit.getScheduler().runTask(plugin, () -> {
                DisplayMapTask displayTask = new DisplayMapTask(playerId, displayLocation, finalBlockData, finalMinWorldX, finalMaxWorldX, finalMinWorldY, finalMaxWorldY, finalMinWorldZ, finalMaxWorldZ);
                BukkitTask task = displayTask.runTaskTimer(plugin, 0L, 1L);
                activeSessions.put(playerId, new HologramSession(playerId, center, System.currentTimeMillis(), renderDistance, task, null,
                        finalBlockData, finalMinWorldX, finalMaxWorldX, finalMinWorldY, finalMaxWorldY, finalMinWorldZ, finalMaxWorldZ));
                plugin.getLogger().info("[SCAN] Новая карта отображается для игрока: " + player.getName());
            });
        }
    }

    private class DisplayMapTask extends BukkitRunnable {
        private final UUID playerId;
        private final Location displayLocation;
        private final List<Pair<Vector, Material>> blockData;
        private final double minWorldX;
        private final double maxWorldX;
        private final double minWorldY;
        private final double maxWorldY;
        private final double minWorldZ;
        private final double maxWorldZ;

        public DisplayMapTask(final UUID playerId, final Location displayLocation, final List<Pair<Vector, Material>> blockData,
                              final double minWorldX, final double maxWorldX, final double minWorldY, final double maxWorldY,
                              final double minWorldZ, final double maxWorldZ) {
            this.playerId = playerId;
            this.displayLocation = displayLocation;
            this.blockData = blockData;
            this.minWorldX = minWorldX;
            this.maxWorldX = maxWorldX;
            this.minWorldY = minWorldY;
            this.maxWorldY = maxWorldY;
            this.minWorldZ = minWorldZ;
            this.maxWorldZ = maxWorldZ;
        }

        @Override
        public void run() {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                cancel();
                activeSessions.remove(playerId);
                return;
            }

            final Location displayCenter = displayLocation;
            final double displayWidth = 10;
            final double displayHeight = 5;
            final double displayDepth = 10;
            final World world = player.getWorld();
            final float particleSize = (float) plugin.getConfig().getDouble("particleSize", 0.8);
            final Engine engine = JavaPlugin.getPlugin(HoloTopography.class).getEngine(); // Получаем экземпляр Engine для доступа к getBlockColor

//            player.sendMessage(ChatColor.YELLOW + "Начало отображения " + blockData.size() + " частиц...");
            int particlesSpawned = 0;

            for (final Pair<Vector, Material> data : blockData) { // Итерируемся по списку пар
                final Vector offset = data.getKey();
                final Material material = data.getValue();

                final double rangeX = maxWorldX - minWorldX;
                final double rangeY = maxWorldY - minWorldY;
                final double rangeZ = maxWorldZ - minWorldZ;

                double normalizedX = (rangeX == 0) ? 0.5 : (offset.getX() - minWorldX) / rangeX;
                double normalizedY = (rangeY == 0) ? 0.5 : (offset.getY() - minWorldY) / rangeY;
                double normalizedZ = (rangeZ == 0) ? 0.5 : (offset.getZ() - minWorldZ) / rangeZ;

                final double displayX = displayCenter.getX() - displayWidth / 2 + normalizedX * displayWidth;
                final double displayY = displayCenter.getY() + normalizedY * displayHeight;
                final double displayZ = displayCenter.getZ() - displayDepth / 2 + normalizedZ * displayDepth;

                final Location particleLocation = new Location(world, displayX, displayY, displayZ);
                final Color particleColor = engine.getBlockColor(material); // Получаем цвет на основе типа блока

                try {
                    player.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, particleSize, new Particle.DustOptions(particleColor, particleSize));
                    particlesSpawned++;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Ошибка спавна частицы: " + e.getMessage());
                    e.printStackTrace();
                }
            }
//            player.sendMessage(ChatColor.GREEN + "Отображено " + particlesSpawned + " частиц с учетом цвета блоков.");
        }

    }

    /**
     * Задача рендеринга топографии
     */
    private class RenderTask extends BukkitRunnable {
        private final UUID playerId;
        private final Location center;
        private final double renderDistance;

        public RenderTask(UUID playerId, Location center, double renderDistance) {
            this.playerId = playerId;
            this.center = center;
            this.renderDistance = renderDistance;
        }
        
        @Override
        public void run() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                this.cancel();
                activeSessions.remove(playerId);
                return;
            }
            
            // Получаем текущий мир игрока
            World world = player.getWorld();
            
            // Логируем информацию о мире для диагностики
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Мир: " + world.getName() + 
                                      ", Мин. высота: " + world.getMinHeight() + 
                                      ", Макс. высота: " + world.getMaxHeight());
            }
            
            // Определяем границы рендеринга
            int minX = (int) (center.getBlockX() - renderDistance);
            int maxX = (int) (center.getBlockX() + renderDistance);
            int minZ = (int) (center.getBlockZ() - renderDistance);
            int maxZ = (int) (center.getBlockZ() + renderDistance);
            
            // Максимальное расстояние показа частиц
            double maxParticleDistance = plugin.getConfig().getDouble("particleRenderDistance", 60.0);
            float particleSize = (float) plugin.getConfig().getDouble("particleSize", 0.8);
            
            // Используем значительно меньший шаг для гарантии отображения
            int step = Math.max(1, (int) (renderDistance / 10)); 
            
            // Счетчик для отладки
            int particleCount = 0;
            int blocksScanned = 0;
            
            // Основной алгоритм сканирования
            for (int x = minX; x <= maxX; x += step) {
                for (int z = minZ; z <= maxZ; z += step) {
                    blocksScanned++;
                    
                    // Проверка на нахождение в радиусе
                    double distanceFromCenter = Math.sqrt(Math.pow(x - center.getBlockX(), 2) + 
                                                       Math.pow(z - center.getBlockZ(), 2));
                    if (distanceFromCenter > renderDistance) continue;
                    
                    // Ищем блок для отображения
                    int y;
                    try {
                        // Сначала пробуем получить высоту
                        y = world.getHighestBlockYAt(x, z);
                        
                        // Проверка на недопустимую высоту
                        if (y < 1) {
                            // Иногда метод getHighestBlockYAt возвращает -1, если чанк не полностью загружен
                            // В этом случае пробуем использовать фиксированную высоту
                            y = center.getBlockY();
                        }
                    } catch (Exception e) {
                        // В случае ошибки используем высоту игрока
                        plugin.getLogger().warning("Ошибка при определении высоты блока: " + e.getMessage());
                        y = center.getBlockY();
                    }
                    
                    // Проверяем расстояние от игрока для оптимизации
                    Location blockLoc = new Location(world, x, y, z);
                    double playerDistance = blockLoc.distance(player.getLocation());
                    
                    // Увеличиваем дистанцию отображения для большей видимости
                    if (playerDistance > maxParticleDistance) continue;
                    
                    // Расчёт цвета в зависимости от высоты
                    // Учитываем потенциальные проблемы с определением диапазона высот
                    double normalizedHeight;
                    try {
                        normalizedHeight = (double) (y - world.getMinHeight()) / 
                                          Math.max(1, world.getMaxHeight() - world.getMinHeight());
                        
                        // Защита от некорректных значений
                        normalizedHeight = Math.max(0, Math.min(1, normalizedHeight));
                    } catch (Exception e) {
                        // Если что-то пошло не так, используем значение по умолчанию
                        normalizedHeight = 0.5;
                    }
                    
                    // Создаем final копии для лямбда-выражения
                    final int finalX = x;
                    final int finalY = y;
                    final int finalZ = z;
                    
                    // Увеличиваем счетчик частиц
                    particleCount++;
                    
                    // Ограничиваем максимальное количество частиц для производительности
                    if (particleCount >= 500 && plugin.getConfig().getBoolean("limitParticles", true)) {
                        break;
                    }
                }
                
                // Проверка лимита частиц
                if (particleCount >= 500 && plugin.getConfig().getBoolean("limitParticles", true)) {
                    break;
                }
            }
            
            // Отладочные сообщения
            if (particleCount == 0) {
                plugin.getLogger().info("Не удалось создать частицы для игрока " + player.getName() + 
                                       ". Проверено блоков: " + blocksScanned);
                player.sendActionBar(ChatColor.RED + "Не удалось создать топографическую карту");
            } else {
                player.sendActionBar(ChatColor.GREEN + "Топографическая карта: " + particleCount + " точек");
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Создано " + particleCount + " частиц для игрока " + player.getName() + 
                                           " из " + blocksScanned + " проверенных блоков");
                }
            }
        }
    }
}
