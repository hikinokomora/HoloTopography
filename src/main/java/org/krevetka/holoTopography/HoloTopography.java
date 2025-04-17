package org.krevetka.holoTopography;

import org.bukkit.plugin.java.JavaPlugin;
import org.krevetka.holoTopography.commands.HoloTopographyCommand;
import org.krevetka.holoTopography.core.Engine;

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
        // Здесь можно добавить логику для корректного завершения всех сессий
    }

    public Engine getEngine() {
        return engine;
    }
}