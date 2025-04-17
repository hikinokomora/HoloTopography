package org.krevetka.holoTopography;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.krevetka.holoTopography.commands.HoloTopographyCommand;
import org.krevetka.holoTopography.core.Engine;

public class HoloTopography extends JavaPlugin {

    private Engine engine;

    @Override
    public void onEnable() {
        // Сохраняем конфигурацию по умолчанию
        saveDefaultConfig();
        
        // Загружаем настройки из конфигурации
        double defaultRenderDistance = getConfig().getDouble("defaultSize", 30.0);
        int particlesPerChunk = getConfig().getInt("particlesPerChunk", 15);
        
        // Инициализация движка рендеринга
        engine = new Engine(getConfig().getDouble("defaultRenderDistance", 20.0), getConfig().getInt("particlesPerChunk", 5));

        // Регистрация команд
        getCommand("holotopo").setExecutor(new HoloTopographyCommand(this, engine));
        
        getLogger().info("HoloTopography включен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HoloTopography выключен!");
        // Здесь можно добавить логику для корректного завершения всех сессий
        if (engine != null) {
            // Останавливаем все активные сессии
            getLogger().info("Останавливаем все активные сессии визуализации...");
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        // Обновляем настройки движка после перезагрузки конфигурации
        getLogger().info("Конфигурация перезагружена");
    }

    public Engine getEngine() {
        return engine;
    }
}
