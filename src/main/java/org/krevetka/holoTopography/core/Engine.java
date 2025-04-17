package org.krevetka.holoTopography.core;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
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
    }

    /**
     * Инициализация голограммы для игрока
     */
    public void createSession(Player player, Location center, double renderDistance) {
        // Остановить текущую сессию, если она есть
        stopSession(player.getUniqueId());
        Location displayLocation = player.getLocation().add(player.getLocation().getDirection().multiply(3)); // Пример: 5 блоков перед игроком
        player.sendMessage(ChatColor.YELLOW + "Запущиено Сканирование...");
        new InitialScanTask(player.getUniqueId(), center, renderDistance, displayLocation).runTaskAsynchronously(plugin);
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
    
    /**
     * Сессия голограммы
     */
    record HologramSession(UUID playerId, Location center, long createdAt, double renderDistance, BukkitTask task) {}

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

            player.sendMessage(ChatColor.YELLOW + "Начало сканирования...");
            final List<Vector> relativeOffsets = new ArrayList<>();
            double minWorldX = Double.MAX_VALUE, maxWorldX = Double.MIN_VALUE;
            double minWorldY = Double.MAX_VALUE, maxWorldY = Double.MIN_VALUE;
            double minWorldZ = Double.MAX_VALUE, maxWorldZ = Double.MIN_VALUE;
            final World world = center.getWorld();

            for (int x = (int) -renderDistance; x <= renderDistance; x++) {
                for (int z = (int) -renderDistance; z <= renderDistance; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= renderDistance) {
                        int worldY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                        Vector offset = new Vector(x, worldY - center.getBlockY(), z);
                        relativeOffsets.add(offset);

                        minWorldX = Math.min(minWorldX, offset.getX());
                        maxWorldX = Math.max(maxWorldX, offset.getX());
                        minWorldY = Math.min(minWorldY, offset.getY());
                        maxWorldY = Math.max(maxWorldY, offset.getY());
                        minWorldZ = Math.min(minWorldZ, offset.getZ());
                        maxWorldZ = Math.max(maxWorldZ, offset.getZ());
                    }
                }
            }

            player.sendMessage(ChatColor.YELLOW + "Сканирование завершено, найдено " + relativeOffsets.size() + " точек.");

            final double finalMinWorldX = minWorldX;
            final double finalMaxWorldX = maxWorldX;
            final double finalMinWorldY = minWorldY;
            final double finalMaxWorldY = maxWorldY;
            final double finalMinWorldZ = minWorldZ;
            final double finalMaxWorldZ = maxWorldZ;
            final List<Vector> finalRelativeOffsets = new ArrayList<>(relativeOffsets);

            Bukkit.getScheduler().runTask(plugin, () -> {
                DisplayMapTask displayTask = new DisplayMapTask(playerId, displayLocation, finalRelativeOffsets, finalMinWorldX, finalMaxWorldX, finalMinWorldY, finalMaxWorldY, finalMinWorldZ, finalMaxWorldZ);
                BukkitTask task = displayTask.runTaskTimer(plugin, 0L, 1L);
                activeSessions.put(playerId, new HologramSession(playerId, center, System.currentTimeMillis(), renderDistance, task));
            });
        }
    }

    private class DisplayMapTask extends BukkitRunnable {
        private final UUID playerId;
        private final Location displayLocation;
        private final List<Vector> relativeOffsets;
        private final double minWorldX;
        private final double maxWorldX;
        private final double minWorldY;
        private final double maxWorldY;
        private final double minWorldZ;
        private final double maxWorldZ;

        public DisplayMapTask(final UUID playerId, final Location displayLocation, final List<Vector> relativeOffsets,
                              final double minWorldX, final double maxWorldX, final double minWorldY, final double maxWorldY,
                              final double minWorldZ, final double maxWorldZ) {
            this.playerId = playerId;
            this.displayLocation = displayLocation;
            this.relativeOffsets = relativeOffsets;
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

            player.sendMessage(ChatColor.YELLOW + "Начало отображения " + relativeOffsets.size() + " частиц...");
            int particlesSpawned = 0;

            for (final Vector offset : relativeOffsets) {
                final double normalizedX = (offset.getX() - minWorldX) / (maxWorldX - minWorldX);
                final double normalizedY = (offset.getY() - minWorldY) / (maxWorldY - minWorldY);
                final double normalizedZ = (offset.getZ() - minWorldZ) / (maxWorldZ - minWorldZ);

                final double displayX = displayCenter.getX() - displayWidth / 2 + normalizedX * displayWidth;
                final double displayY = displayCenter.getY() + normalizedY * displayHeight;
                final double displayZ = displayCenter.getZ() - displayDepth / 2 + normalizedZ * displayDepth;

                final Location particleLocation = new Location(world, displayX, displayY, displayZ);

                // Дополнительная отладка координат
//                player.sendMessage(ChatColor.GRAY + "Попытка спавна в: " + particleLocation.getX() + ", " + particleLocation.getY() + ", " + particleLocation.getZ());

                try {
                    player.spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, particleSize, new Particle.DustOptions(Color.GREEN, particleSize));
                    particlesSpawned++;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Ошибка спавна частицы: " + e.getMessage());
                    e.printStackTrace(); // Вывести Stack Trace в консоль
                }
            }
            player.sendMessage(ChatColor.GREEN + "Отображено " + particlesSpawned + " частиц.");
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
                    
                    Color color = getHeightColor(normalizedHeight);
                    
                    // Создаем final копии для лямбда-выражения
                    final int finalX = x;
                    final int finalY = y;
                    final int finalZ = z;
                    final Color finalColor = color;
                    
                    // Увеличиваем счетчик частиц
                    particleCount++;
                    
                    // Отображаем частицу синхронно в основном потоке сервера
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // Первый метод - использовать DUST с настраиваемым цветом
                            Particle.DustOptions dustOptions = new Particle.DustOptions(finalColor, particleSize);
                            player.spawnParticle(Particle.DUST_COLOR_TRANSITION, 
                                                finalX + 0.5, finalY + 1.0, finalZ + 0.5, 
                                                1, 0, 0, 0, 0, dustOptions);
                        } catch (Exception e1) {
                            try {
                                // Резервный вариант - обычные цветные частицы
                                player.spawnParticle(Particle.DUST, 
                                                   finalX + 0.5, finalY + 1.0, finalZ + 0.5, 
                                                   1, 0, 0, 0, 0);
                            } catch (Exception e2) {
                                try {
                                    // Последняя попытка - использовать гарантированно существующий тип частиц
                                    player.spawnParticle(Particle.END_ROD, 
                                                       finalX + 0.5, finalY + 1.0, finalZ + 0.5, 
                                                       1, 0, 0, 0, 0);
                                } catch (Exception e3) {
                                    plugin.getLogger().warning("Все попытки создать частицы провалились: " + e3.getMessage());
                                }
                            }
                        }
                    });
                    
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
        
        /**
         * Получение цвета для высоты (от синего к красному через градации)
         */
        private Color getHeightColor(double normalizedHeight) {
            // Градиент: синий -> голубой -> зеленый -> желтый -> красный
            if (normalizedHeight < 0.2) {
                // Синий -> Голубой
                double factor = normalizedHeight / 0.2;
                return Color.fromRGB(0, (int) (factor * 255), 255);
            } else if (normalizedHeight < 0.4) {
                // Голубой -> Зеленый
                double factor = (normalizedHeight - 0.2) / 0.2;
                return Color.fromRGB(0, 255, 255 - (int) (factor * 255));
            } else if (normalizedHeight < 0.6) {
                // Зеленый -> Желтый
                double factor = (normalizedHeight - 0.4) / 0.2;
                return Color.fromRGB((int) (factor * 255), 255, 0);
            } else if (normalizedHeight < 0.8) {
                // Желтый -> Красный
                double factor = (normalizedHeight - 0.6) / 0.2;
                return Color.fromRGB(255, 255 - (int) (factor * 255), 0);
            } else {
                // Красный -> Темно-красный
                double factor = (normalizedHeight - 0.8) / 0.2;
                return Color.fromRGB(255 - (int) (factor * 128), 0, 0);
            }
        }
    }
}
