package dev.txadmin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/** Layar peringatan ala txAdmin: judul besar, suara, hitung mundur, lalu bisa ditutup dengan Shift. */
public final class Warnings implements Listener {
    private final TxAdmin p;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, Integer> left = new HashMap<>();

    public Warnings(TxAdmin p) { this.p = p; }

    public void show(Player t, String admin, String reason) {
        dismiss(t);
        UUID id = t.getUniqueId();
        int wait = Math.max(0, p.getConfig().getInt("warn.dismiss-seconds", 10));
        t.showTitle(Title.title(p.plain("warn-title"), p.plain("warn-subtitle", "reason", reason),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(300), Duration.ofMillis(500))));
        t.sendMessage(p.plain("warn-chat", "admin", admin, "reason", reason));
        p.actions.sound(t, "sounds.warn");
        left.put(id, wait);
        tasks.put(id, Bukkit.getScheduler().runTaskTimer(p, () -> {
            if (!t.isOnline()) { cancel(id); return; }
            int l = left.getOrDefault(id, 0);
            if (l > 0) {
                t.sendActionBar(p.plain("warn-wait", "seconds", String.valueOf(l)));
                left.put(id, l - 1);
            } else {
                t.sendActionBar(p.plain("warn-dismiss"));
            }
        }, 0L, 20L));
    }

    private void cancel(UUID id) {
        BukkitTask t = tasks.remove(id);
        if (t != null) t.cancel();
        left.remove(id);
    }

    public void dismiss(Player t) {
        if (!tasks.containsKey(t.getUniqueId())) return;
        cancel(t.getUniqueId());
        t.clearTitle();
        t.sendActionBar(Component.empty());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (e.isSneaking() && tasks.containsKey(id) && left.getOrDefault(id, 1) == 0) dismiss(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { cancel(e.getPlayer().getUniqueId()); }
}
