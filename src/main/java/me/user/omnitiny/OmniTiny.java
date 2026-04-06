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
                p.sendMessage("§aРазмер изменен на " + size);
            }
            case "crawl", "lay" -> toggleLay(p);
            case "sit" -> toggleSit(p);
            case "hug" -> {
                Player target = null;
                for (Entity en : p.getNearbyEntities(2, 2, 2)) {
                    if (en instanceof Player other && other != p) {
                        target = other; break;
                    }
                }
                if (target != null) {
                    startClinging(target, p, "HUG");
                    p.sendMessage("§dВы обняли " + target.getName() + " за ногу!");
                } else {
                    p.sendMessage("§cРядом никого нет.");
                }
            }
            case "v" -> {
                if (p.getVehicle() != null) {
                    Entity vehicle = p.getVehicle();
                    vehicle.removePassenger(p);
                    if (vehicle instanceof ArmorStand) vehicle.remove();
                }
                p.setSwimming(false);
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.setPose(Pose.STANDING, true);
                p.sendMessage("§eВы встали/слезли.");
            }
        }
        return true;
    }

    private void toggleLay(Player p) {
        if (p.getPose() == Pose.SWIMMING) {
            p.setSwimming(false);
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.setPose(Pose.STANDING, true);
            p.sendMessage("§eВы встали.");
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 10, false, false));
            p.setSwimming(true);
            p.setPose(Pose.SWIMMING, true);
            p.sendMessage("§dВы легли отдыхать.");
        }
    }

    private void toggleSit(Player p) {
        if (p.getVehicle() != null) {
            p.getVehicle().remove();
            p.sendMessage("§eВы встали.");
        } else {
            double size = p.getAttribute(Attribute.GENERIC_SCALE).getValue();
            double offset = (size < 0.5) ? 0.1 : -0.7; 
            ArmorStand chair = p.getWorld().spawn(p.getLocation().add(0, offset, 0), ArmorStand.class, s -> {
                s.setVisible(false); s.setMarker(true); s.setGravity(false); s.setPersistent(false);
            });
            chair.addPassenger(p);
            p.sendMessage("§aВы присели.");
        }
    }

    @EventHandler
    public void onBedClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (e.getClickedBlock().getBlockData() instanceof Bed) {
                if (p.getAttribute(Attribute.GENERIC_SCALE).getValue() < 0.5) {
                    e.setCancelled(true);
                    Location bedLoc = e.getClickedBlock().getLocation().add(0.5, 0.4, 0.5);
                    p.teleport(bedLoc);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 10, false, false));
                    p.setSwimming(true);
                    p.setPose(Pose.SWIMMING, true);
                    p.sendMessage("§dПрилегли рядышком...");
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player p = e.getPlayer();
        double pSize = p.getAttribute(Attribute.GENERIC_SCALE).getValue();
        double tSize = target.getAttribute(Attribute.GENERIC_SCALE).getValue();

        if (p.isSneaking()) {
            if (pSize < tSize) {
                openInteractionMenu(p, target);
            } else if (pSize > tSize) {
                p.addPassenger(target);
                target.sendMessage("§6" + p.getName() + " §eвзял вас на руки!");
            }
        }
    }

    private void openInteractionMenu(Player small, Player big) {
        Inventory gui = Bukkit.createInventory(null, 9, "Действие с " + big.getName());
        String uuid = big.getUniqueId().toString();
        gui.setItem(0, createItem(Material.GOLDEN_HELMET, "§eНа голову", uuid));
        gui.setItem(1, createItem(Material.IRON_CHESTPLATE, "§bНа грудь", uuid));
        gui.setItem(2, createItem(Material.LADDER, "§aЛазать", uuid));
        gui.setItem(3, createItem(Material.LEATHER_BOOTS, "§6Обнять ногу", uuid));
        gui.setItem(8, createItem(Material.HEART_OF_THE_SEA, "§dПоцелуй", uuid));
        small.openInventory(gui);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith("Действие с")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        Player small = (Player) e.getWhoClicked();
        UUID bigId = UUID.fromString(e.getCurrentItem().getItemMeta().getLore().get(0));
        Player big = Bukkit.getPlayer(bigId);
        if (big == null) return;
        small.closeInventory();
        
        switch (e.getRawSlot()) {
            case 0 -> { big.addPassenger(small); small.setPose(Pose.SNEAKING, true); }
            case 1 -> startClinging(big, small, "CHEST");
            case 2 -> startClinging(big, small, "CLIMB");
            case 3 -> startClinging(big, small, "HUG"); 
            case 8 -> {
                small.getWorld().spawnParticle(Particle.HEART, small.getEyeLocation().add(0, 0.3, 0), 7);
            }
        }
    }

    private void startClinging(Player big, Player small, String mode) {
        ArmorStand anchor = big.getWorld().spawn(big.getLocation(), ArmorStand.class, s -> {
            s.setVisible(false); s.setMarker(true); s.setGravity(false);
        });
        anchor.addPassenger(small);
        new BukkitRunnable() {
            double climbY = 1.0;
            @Override
            public void run() {
                if (!big.isOnline() || !small.isOnline() || anchor.getPassengers().isEmpty()) {
                    anchor.remove(); this.cancel(); return;
                }
                Location loc = big.getLocation();
                Vector dir = loc.getDirection().setY(0).normalize();
                Location target;
                if (mode.equals("CHEST")) {
                    target = big.getEyeLocation().add(dir.multiply(0.3)).subtract(0, 0.7, 0);
                    small.setPose(Pose.SWIMMING, true);
                } else if (mode.equals("HUG")) {
                    target = loc.clone().add(dir.multiply(0.25)).subtract(0, 1.45, 0);
                    small.setPose(Pose.SNEAKING, true);
                } else {
                    if (small.getLocation().getPitch() < -45) climbY = Math.min(climbY + 0.05, 1.7);
                    if (small.getLocation().getPitch() > 45) climbY = Math.max(climbY - 0.05, 0.2);
                    target = loc.clone().add(dir.multiply(0.2));
                    target.setY(loc.getY() + climbY);
                }
                target.setYaw(loc.getYaw());
                anchor.teleport(target);
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
