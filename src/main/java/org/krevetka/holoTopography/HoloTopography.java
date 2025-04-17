package org.krevetka.holoTopography;

import org.bukkit.entity.Player; // Импорт класса Player
import org.bukkit.plugin.java.JavaPlugin;
import org.krevetka.holoTopography.commands.HoloTopographyCommand; // Импорт класса HoloTopographyCommand
import org.krevetka.holoTopography.core.Engine;
import org.bukkit.Bukkit; // Импорт класса Bukkit

public class HoloTopography extends JavaPlugin {

    private Engine engine;

    @Override
    public void onEnable() {
        getLogger().info("HoloTopography включен!");
        // Инициализация движка рендеринга
        double renderDistance = 30.0; // Настройте дистанцию рендеринга
        int particlesPerChunk = 15; // Настройте плотность частиц на "чанк" (условная единица)
        engine = new Engine(renderDistance, particlesPerChunk);

        // Регистрация команд
        getCommand("holotopo").setExecutor(new HoloTopographyCommand(this, engine));
    }

    @Override
    public void onDisable() {
        getLogger().info("HoloTopography выключен!");
        if (engine != null) {
            engine.getActiveSessions().keySet().forEach(playerId -> {
                Player player = Bukkit.getPlayer(playerId); // Используем Bukkit.getPlayer(playerId)
                if (player != null) {
                    engine.stopSession(player);
                }
            });
        }
    }

    public Engine getEngine() {
        return engine;
    }
}