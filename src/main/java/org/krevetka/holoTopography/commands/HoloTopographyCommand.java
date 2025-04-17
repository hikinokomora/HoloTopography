package org.krevetka.holoTopography.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.krevetka.holoTopography.HoloTopography;
import org.krevetka.holoTopography.core.Engine;
import org.bukkit.Location;

public class HoloTopographyCommand implements CommandExecutor {

    private final HoloTopography plugin;
    private final Engine engine;

    public HoloTopographyCommand(HoloTopography plugin, Engine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть выполнена только игроком.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "start":
                    Location center = player.getLocation(); // Используем текущую позицию игрока как центр
                    engine.createSession(player, center);
                    player.sendMessage("Голографическая топография запущена.");
                    return true;
                case "stop":
                    if (engine.getActiveSessions().containsKey(player.getUniqueId())) {
                        engine.stopSession(player);
                        player.sendMessage("Голографическая топография остановлена.");
                    } else {
                        player.sendMessage("Нет активной сессии голографической топографии для вас.");
                    }
                    return true;
                case "reload":
                    // TODO: Реализовать перезагрузку конфигурации (если будет)
                    player.sendMessage("Перезагрузка (не реализовано).");
                    return true;
                default:
                    player.sendMessage("Использование: /holotopo <start|stop|reload>");
                    return true;
            }
        } else {
            player.sendMessage("Использование: /holotopo <start|stop|reload>");
            return true;
        }
    }
}