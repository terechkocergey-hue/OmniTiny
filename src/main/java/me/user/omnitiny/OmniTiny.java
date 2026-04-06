package me.user.omnitiny;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

public class OmniTiny extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("scale").setExecutor(this);
        getCommand("v").setExecutor(this);
        getCommand("crawl").setExecutor(this);
        getCommand("sit").setExecutor(this);
        getCommand("lay").setExecutor(this);
        getCommand("hug").setExecutor(this);
        getCommand("pray").setExecutor(this);
        setupNoCollision();
    }

    private void setupNoCollision() {
        Scoreboard s = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = s.getTeam("tiny_physics");
        if (t == null) t = s.registerNewTeam("tiny_physics");
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        switch (cmd.getName().toLowerCase()) {
            case "scale" -> {
                double size = (args.length > 0) ? Double.parseDouble(args[0]) : 1.0;
                p.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(size);
                p.sendMessage("§aРазмер изменен!");
            }
            case "v" -> {
                if (p.getVehicle() != null) {
                    Entity v = p.getVehicle();
                    v.removePassenger(p);
                    if (v instanceof ArmorStand) v.remove();
                }
                p.setSwimming(false);
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                p.setPose(Pose.STANDING, true);
                p.sendMessage("§eВы встали/вылезли.");
            }
            case "crawl", "lay" -> forceLay(p, p.getLocation());
            case "sit" -> toggleSit(p);
            case "pray" -> p.setPose(Pose.SNEAKING, true);
        }
        return true;
    }

    private void forceLay(Player p, Location loc) {
        if (p.getVehicle() != null) { p.getVehicle().remove(); return; }
        ArmorStand anchor = p.getWorld().spawn(loc.clone().subtract(0, 1.4, 0), ArmorStand.class, s -> {
            s.setVisible(false); s.setMarker(true); s.setGravity(false);
        });
        anchor.addPassenger(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 10, false, false));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.getVehicle() == null) { anchor.remove(); this.cancel(); return; }
                p.setSwimming(true);
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void toggleSit(Player p) {
        if (p.getVehicle() != null) { p.getVehicle().remove(); return; }
        double size = p.getAttribute(Attribute.GENERIC_SCALE).getValue();
        double offset = (size < 0.5) ? 0.15 : -0.45;
        ArmorStand chair = p.getWorld().spawn(p.getLocation().add(0, offset, 0), ArmorStand.class, s -> {
            s.setVisible(false); s.setMarker(true); s.setGravity(false);
        });
        chair.addPassenger(p);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        double pSize = p.getAttribute(Attribute.GENERIC_SCALE).getValue();
        double tSize = target.getAttribute(Attribute.GENERIC_SCALE).getValue();

        if (pSize < tSize) {
            openMenu(p, target, "Малыш: Выбери действие");
        } else {
            openMenu(p, target, "Большой: Куда его посадить?");
        }
    }

    private void openMenu(Player viewer, Player other, String title) {
        Inventory gui = Bukkit.createInventory(null, 9, title);
        String uuid = other.getUniqueId().toString();
        gui.setItem(0, createItem(Material.GOLDEN_HELMET, "§eНа голову", uuid));
        gui.setItem(1, createItem(Material.LEATHER_CHESTPLATE, "§bВ карман", uuid));
        gui.setItem(2, createItem(Material.IRON_BOOTS, "§6В кроссовок", uuid));
        gui.setItem(3, createItem(Material.CHAINMAIL_CHESTPLATE, "§aНа плечо", uuid));
        gui.setItem(4, createItem(Material.OAK_STAIRS, "§cЖивой стул", uuid)); // НОВОЕ
        gui.setItem(8, createItem(Material.HEART_OF_THE_SEA, "§dПоцелуй", uuid));
        viewer.openInventory(gui);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("Выбери") || e.getView().getTitle().contains("Куда")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            
            Player actor = (Player) e.getWhoClicked();
            UUID otherId = UUID.fromString(e.getCurrentItem().getItemMeta().getLore().get(0));
            Player target = Bukkit.getPlayer(otherId);
            if (target == null) return;
            actor.closeInventory();

            Player small = (actor.getAttribute(Attribute.GENERIC_SCALE).getValue() < target.getAttribute(Attribute.GENERIC_SCALE).getValue()) ? actor : target;
            Player big = (small == actor) ? target : actor;

            switch (e.getRawSlot()) {
                case 0 -> startClinging(big, small, "HEAD");
                case 1 -> startClinging(big, small, "POCKET");
                case 2 -> startClinging(big, small, "SHOE");
                case 3 -> startClinging(big, small, "SHOULDER");
                case 4 -> startClinging(big, small, "CHAIR"); // НОВОЕ
                case 8 -> big.getWorld().spawnParticle(Particle.HEART, big.getEyeLocation(), 5);
            }
        }
    }

    private void startClinging(Player big, Player small, String mode) {
        ArmorStand anchor = big.getWorld().spawn(big.getLocation(), ArmorStand.class, s -> {
            s.setVisible(false); s.setMarker(true); s.setGravity(false);
        });
        
        if (mode.equals("CHAIR")) {
            // В режиме стула Большой садится на Маленького
            anchor.addPassenger(big);
        } else {
            // В остальных режимах Маленький садится на Большого
            anchor.addPassenger(small);
        }

        if (mode.equals("POCKET")) small.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, false, false));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!big.isOnline() || !small.isOnline() || anchor.getPassengers().isEmpty()) {
                    anchor.remove(); small.removePotionEffect(PotionEffectType.INVISIBILITY); this.cancel(); return;
                }
                Location loc = big.getLocation();
                Vector dir = loc.getDirection().setY(0).normalize();
                Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                Location tLoc;

                switch (mode) {
                    case "HEAD" -> tLoc = big.getEyeLocation().add(0, 0.35, 0);
                    case "POCKET" -> tLoc = big.getLocation().add(side.multiply(0.3)).add(0, 0.8, 0);
                    case "SHOE" -> tLoc = big.getLocation().add(dir.multiply(0.2)).subtract(0, 1.45, 0);
                    case "SHOULDER" -> tLoc = big.getEyeLocation().add(side.multiply(0.4)).subtract(0, 0.6, 0);
                    case "CHAIR" -> {
                        // Маленький ложится на землю
                        small.teleport(big.getLocation());
                        small.setSwimming(true);
                        // Большой сидит чуть выше Маленького
                        tLoc = small.getLocation().subtract(0, 0.45, 0);
                    }
                    default -> tLoc = loc;
                }
                tLoc.setYaw(big.getLocation().getYaw());
                anchor.teleport(tLoc);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private ItemStack createItem(Material m, String name, String lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta mt = i.getItemMeta();
        mt.setDisplayName(name);
        mt.setLore(List.of(lore));
        i.setItemMeta(mt);
        return i;
    }
}
