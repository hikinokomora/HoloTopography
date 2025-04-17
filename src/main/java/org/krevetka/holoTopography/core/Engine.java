package org.krevetka.holoTopography.core;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * Главный движок рендеринга 3D-карты
 */
public class Engine {
    private final Map<UUID, HologramSession> activeSessions = new ConcurrentHashMap<>();
    private final double renderDistance;
    private final int particlesPerChunk;
    private final String nmsVersion;

    public Engine(double renderDistance, int particlesPerChunk) {
        this.renderDistance = renderDistance;
        this.particlesPerChunk = particlesPerChunk;
        this.nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    /**
     * Инициализация голограммы для игрока
     */
    public void createSession(Player player, Location center) {
        HologramSession session = new HologramSession(
                player.getUniqueId(),
                center,
                System.currentTimeMillis()
        );
        activeSessions.put(player.getUniqueId(), session);

        // Запускаем асинхронный рендер-луп
        new RenderTask(session).runTaskTimerAsynchronously(
                Bukkit.getPluginManager().getPlugin("HoloTopography"),
                0L, 2L // Тики между обновлениями
        );
    }

    /**
     * Получить карту активных сессий
     */
    public Map<UUID, HologramSession> getActiveSessions() {
        return activeSessions;
    }

    /**
     * Остановка сессии рендеринга для игрока
     */
    public void stopSession(Player player) {
        UUID playerId = player.getUniqueId();
        HologramSession session = activeSessions.get(playerId);
        if (session != null) {
            // Находим и отменяем соответствующую RenderTask
            Bukkit.getScheduler().getPendingTasks().stream()
                    .filter(task -> task.getOwner().getName().equals("HoloTopography")) // Фильтруем задачи нашего плагина
                    .filter(task -> {
                        try {
                            java.lang.reflect.Field sessionField = task.getClass().getDeclaredField("session");
                            sessionField.setAccessible(true);
                            HologramSession taskSession = (HologramSession) sessionField.get(task);
                            return taskSession != null && taskSession.playerId().equals(playerId);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .findFirst()
                    .ifPresent(task -> task.cancel());

            activeSessions.remove(playerId);
        }
    }

    /**
     * Основная задача рендеринга
     */
    private class RenderTask extends BukkitRunnable {
        private final HologramSession session;
        private final Particle.DustOptions dustOptions =
                new Particle.DustOptions(Color.fromRGB(0x00FF00), 0.8f);

        public RenderTask(HologramSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player == null || !player.isOnline()) {
                cancel();
                activeSessions.remove(session.playerId());
                return;
            }

            // Получаем данные карты
            TerrainData data = TerrainParser.loadTerrain(
                    session.center(),
                    renderDistance,
                    particlesPerChunk
            );

            // Рендерим частицы
            renderParticles(player, data);
        }

        private void renderParticles(Player player, TerrainData data) {
            World world = player.getWorld();

            data.vectors().forEach(vector -> {
                Location loc = session.center().clone().add(vector);

                // Оптимизация: не рендерим то, что игрок не видит
                if (player.getLocation().distanceSquared(loc) > 256) return;

                // Отправка частиц через Reflection для производительности
                spawnParticle(player, loc, dustOptions);
            });
        }

        private void spawnParticle(Player player, Location loc, Particle.DustOptions options) {
            try {
                Object packet = createParticlePacket(
                        Particle.valueOf("REDSTONE"),
                        loc.getX(),
                        loc.getY(),
                        loc.getZ(),
                        0, 0, 0,
                        options.getColor().getRed(),
                        options.getColor().getGreen(),
                        options.getColor().getBlue(),
                        1
                );

                sendPacket(player, packet);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
    }

    // Рефлексивные методы для низкоуровневого рендеринга
    private Object createParticlePacket(Particle particle, double x, double y, double z,
                                        float offsetX, float offsetY, float offsetZ,
                                        int red, int green, int blue, int count) throws ReflectiveOperationException {
        Class<?> particleParamClass = getNMSClass("ParticleParam");
        Class<?> enumParticleClass = getNMSClass("EnumParticle");
        Class<?> packetPlayOutParticlesClass = getNMSClass("PacketPlayOutWorldParticles");
        Constructor<?> packetConstructor = packetPlayOutParticlesClass.getConstructor(
                enumParticleClass,
                boolean.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                int.class,
                int[].class
        );

        Object nmsParticle = enumParticleClass.getField(particle.name()).get(null);
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;

        // Для REDSTONE нужно использовать DustOptions
        if (particle == Particle.valueOf("REDSTONE")) {
            Class<?> dustOptionsNMS = getNMSClass("ParticleParamRedstone");
            Constructor<?> dustConstructor = dustOptionsNMS.getConstructor(float.class, float.class, float.class, float.class);
            Object dust = dustConstructor.newInstance(r, g, b, 1.0f);
            return packetConstructor.newInstance(dust, false, (float) x, (float) y, (float) z, offsetX, offsetY, offsetZ, 1.0f, count, new int[0]);
        } else {
            return packetConstructor.newInstance(nmsParticle, false, (float) x, (float) y, (float) z, offsetX, offsetY, offsetZ, 1.0f, count, new int[0]);
        }
    }

    private void sendPacket(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = getHandle(player);
        Object playerConnection = getField(handle.getClass(), "playerConnection").get(handle);
        Method sendPacketMethod = getMethod(playerConnection.getClass(), "sendPacket", getNMSClass("Packet"));
        sendPacketMethod.invoke(playerConnection, packet);
    }

    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + nmsVersion + "." + name);
    }

    private Object getHandle(Player player) throws ReflectiveOperationException {
        Method getHandleMethod = player.getClass().getMethod("getHandle");
        return getHandleMethod.invoke(player);
    }

    private Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    /**
     * Внутренний класс для хранения состояния сессии
     */
    private record HologramSession(
            UUID playerId,
            Location center,
            long createdAt
    ) {}

    /**
     * DTO для данных рельефа
     */
    public static class TerrainData {
        private final List<Vector> vectors;

        public TerrainData(List<Vector> vectors) {
            this.vectors = Collections.unmodifiableList(vectors);
        }

        public List<Vector> vectors() {
            return vectors;
        }
    }
}

/**
 * Класс для загрузки данных рельефа (пример реализации)
 */
class TerrainParser {
    private static final Random random = new Random();

    public static Engine.TerrainData loadTerrain(Location center, double renderDistance, int particlesPerChunk) {
        List<Vector> vectors = new ArrayList<>();
        int radius = (int) Math.ceil(renderDistance);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= renderDistance) {
                    if (random.nextInt(100) < particlesPerChunk) {
                        // Пример генерации высоты на основе расстояния и случайности
                        double y = Math.sin(distance * 0.8) * 3 + (random.nextDouble() - 0.5) * 2;
                        vectors.add(new Vector(x, y, z));
                    }
                }
            }
        }
        return new Engine.TerrainData(vectors);
    }
}