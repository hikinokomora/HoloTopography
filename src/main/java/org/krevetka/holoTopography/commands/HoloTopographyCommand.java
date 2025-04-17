package org.krevetka.holoTopography.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.krevetka.holoTopography.HoloTopography;
import org.krevetka.holoTopography.core.Engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HoloTopographyCommand implements CommandExecutor, TabCompleter {

    private final HoloTopography plugin;
    private final Engine engine;
    private final List<String> subCommands = Arrays.asList("create", "stop", "help", "settings", "info", "reload");

    public HoloTopographyCommand(HoloTopography plugin, Engine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только для игроков.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                double size = plugin.getConfig().getDouble("defaultSize", 30.0);
                if (args.length > 1) {
                    try {
                        size = Double.parseDouble(args[1]);
                        if (size <= 0 || size > plugin.getConfig().getDouble("maxSize", 50.0)) {
                            player.sendMessage(ChatColor.RED + "Неверное значение размера. Должно быть между 1 и " + 
                                    plugin.getConfig().getDouble("maxSize", 50.0) + ".");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Неверный формат числа: " + args[1]);
                        return true;
                    }
                }
                engine.createSession(player, player.getLocation(), size);
                player.sendMessage(ChatColor.GREEN + "Топографическая карта создана с радиусом " + size + " блоков.");
            }
            case "stop" -> {
                if (engine.stopSession(player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "Топографическая карта остановлена.");
                } else {
                    player.sendMessage(ChatColor.RED + "У вас нет активной карты.");
                }
            }
            case "settings" -> {
                if (args.length < 2) {
                    showSettingsMenu(player);
                    return true;
                }
                
                switch (args[1].toLowerCase()) {
                    case "particles" -> {
                        if (args.length < 3) {
                            player.sendMessage(ChatColor.YELLOW + "Текущая плотность частиц: " + 
                                    plugin.getConfig().getInt("particlesPerChunk", 15));
                            player.sendMessage(ChatColor.YELLOW + "Использование: /holotopo settings particles <1-100>");
                            return true;
                        }
                        
                        try {
                            int density = Integer.parseInt(args[2]);
                            if (density < 1 || density > 100) {
                                player.sendMessage(ChatColor.RED + "Значение должно быть между 1 и 100.");
                                return true;
                            }
                            
                            plugin.getConfig().set("particlesPerChunk", density);
                            plugin.saveConfig();
                            player.sendMessage(ChatColor.GREEN + "Плотность частиц установлена на " + density);
                            player.sendMessage(ChatColor.GREEN + "Перезапустите карту командой /holotopo create для применения изменений.");
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Неверный формат числа: " + args[2]);
                        }
                    }
                    case "size" -> {
                        if (args.length < 3) {
                            player.sendMessage(ChatColor.YELLOW + "Использование: /holotopo settings size <размер>");
                            return true;
                        }
                        
                        try {
                            float size = Float.parseFloat(args[2]);
                            if (size < 0.5f || size > 2.0f) {
                                player.sendMessage(ChatColor.RED + "Размер частиц должен быть между 0.5 и 2.0");
                                return true;
                            }
                            
                            plugin.getConfig().set("particleSize", size);
                            plugin.saveConfig();
                            player.sendMessage(ChatColor.GREEN + "Размер частиц установлен на " + size);
                            player.sendMessage(ChatColor.GREEN + "Перезапустите карту для применения изменений.");
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Неверный формат числа: " + args[2]);
                        }
                    }
                    default -> {
                        player.sendMessage(ChatColor.RED + "Неизвестная настройка. Используйте /holotopo settings");
                    }
                }
            }
            case "info" -> {
                if (engine.hasActiveSession(player.getUniqueId())) {
                    Engine.HologramInfo info = engine.getSessionInfo(player.getUniqueId());
                    player.sendMessage(ChatColor.GOLD + "=== Информация о карте ===");
                    player.sendMessage(ChatColor.YELLOW + "Размер карты: " + info.renderDistance() + " блоков");
                    player.sendMessage(ChatColor.YELLOW + "Активна: " + formatTime(System.currentTimeMillis() - info.createdAt()));
                    player.sendMessage(ChatColor.YELLOW + "Плотность частиц: " + 
                                       plugin.getConfig().getInt("particlesPerChunk", 15) + "/чанк");
                } else {
                    player.sendMessage(ChatColor.RED + "У вас нет активной карты.");
                }
            }
            case "reload" -> {
                if (player.hasPermission("holotopo.admin")) {
                    plugin.reloadConfig();
                    player.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена.");
                } else {
                    player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
                }
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Неизвестная команда. Используйте /holotopo help для помощи.");
            }
        }

        return true;
    }

    private void showSettingsMenu(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Настройки HoloTopography ===");
        player.sendMessage(ChatColor.YELLOW + "/holotopo settings particles <1-100>" + ChatColor.WHITE + " - Изменить плотность частиц");
        player.sendMessage(ChatColor.YELLOW + "/holotopo settings size <0.5-2.0>" + ChatColor.WHITE + " - Изменить размер частиц");
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== HoloTopography Help ===");
        player.sendMessage(ChatColor.YELLOW + "/holotopo create [размер]" + ChatColor.WHITE + " - Создать топографическую карту");
        player.sendMessage(ChatColor.YELLOW + "/holotopo stop" + ChatColor.WHITE + " - Остановить текущую карту");
        player.sendMessage(ChatColor.YELLOW + "/holotopo info" + ChatColor.WHITE + " - Информация о текущей карте");
        player.sendMessage(ChatColor.YELLOW + "/holotopo settings" + ChatColor.WHITE + " - Настройки отображения");
        if (player.hasPermission("holotopo.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/holotopo reload" + ChatColor.WHITE + " - Перезагрузить конфигурацию");
        }
        player.sendMessage(ChatColor.YELLOW + "/holotopo help" + ChatColor.WHITE + " - Показать это сообщение");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("settings")) {
            List<String> settingsOptions = Arrays.asList("particles", "size");
            return settingsOptions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
