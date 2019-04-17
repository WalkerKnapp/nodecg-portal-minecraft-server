package com.walker.mcnodegraphic;

import net.minecraft.server.v1_13_R2.EntityPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MCNGServer extends JavaPlugin implements Listener {

    public static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private WebsocketServer wsServer;
    private ExecutorService executorService;

    public Scoreboard scoreboard;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.scoreboard.registerNewObjective("kills", "playerKillCount", "Kills");
        this.scoreboard.registerNewObjective("health", "health", "Health", RenderType.HEARTS);
        Objects.requireNonNull(this.scoreboard.getObjective("health")).setDisplaySlot(DisplaySlot.PLAYER_LIST);

        executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        wsServer = new WebsocketServer(this.getConfig().getInt("websocket-port"), this);
        try {
            wsServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Repeats every 1 second
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for(World world : Bukkit.getWorlds()) {
                wsServer.updateWorldBorder(world.getUID(), world.getWorldBorder().getSize());
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                wsServer.updatePlayerEquipment(player.getUniqueId(), getEquipLevels(player));
            }
        }, 20, Integer.MAX_VALUE);
    }

    @Override
    public void onDisable(){
        wsServer.stop();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        wsServer.sendPlayerInit(player.getUniqueId(),
                player.getDisplayName(),
                player.getGameMode(),
                player.getLevel(),
                player.getHealth(),
                ((CraftPlayer)player).getHandle().getAbsorptionHearts(),
                player.getLocation(),
                scoreboard.getTeam(player.getDisplayName()),
                scoreboard.getObjective("kills").getScore(player.getDisplayName()).getScore(),
                getEquipLevels(player));
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        final Player player = event.getPlayer();
        wsServer.updatePlayerLevels(player.getUniqueId(), player.getLevel());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        wsServer.updatePlayerPosition(player.getUniqueId(), event.getTo());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        wsServer.updatePlayerPosition(player.getUniqueId(), player.getLocation());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        final Player player = event.getPlayer();
        wsServer.updatePlayerGamemode(player.getUniqueId(), event.getNewGameMode());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntityType() == EntityType.PLAYER) {
            final Player player = (Player)event.getEntity();
            wsServer.updatePlayerHealth(player.getUniqueId(), player.getHealth(), ((CraftPlayer)player).getHandle().getAbsorptionHearts(), true);
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if(event.getEntityType() == EntityType.PLAYER) {
            final Player player = (Player)event.getEntity();
            wsServer.updatePlayerHealth(player.getUniqueId(), player.getHealth(), ((CraftPlayer)player).getHandle().getAbsorptionHearts(), false);
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if(event.getEntityType() == EntityType.PLAYER) {
            final Player player = (Player)event.getEntity();
            wsServer.updatePlayerHealth(player.getUniqueId(), player.getHealth(), ((CraftPlayer)player).getHandle().getAbsorptionHearts(), false);
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        wsServer.sendPlayerDeath(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        wsServer.sendPlayerDestroy(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        final World world = event.getWorld();
        wsServer.sendWorldInit(world.getUID(), world.getName(), world.getSeed(), world.getWorldBorder().getSize());
    }

    public void setPlayerGamemode(UUID uuid, int gamemode) {
        final GameMode gameMode = GameMode.getByValue(gamemode);
        final Player player = Bukkit.getPlayer(uuid);

        if(player != null && gameMode != null) {
            player.setGameMode(gameMode);
        }
    }

    public void uhcPrestart(UUID worldUUID, int spreadRadius) {
        World uhcOverworld = Bukkit.getWorld(worldUUID);
        Objective healthObjective = scoreboard.getObjective("health");
        Objective killsObjective = scoreboard.getObjective("kills");

        if(uhcOverworld != null && healthObjective != null && killsObjective != null) {
            uhcOverworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            uhcOverworld.setTime(8000);

            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            for(Player p : Bukkit.getOnlinePlayers()) {
                killsObjective.getScore(p.getDisplayName()).setScore(0);
                if(p.getGameMode() == GameMode.SURVIVAL) {
                    p.setInvulnerable(true);
                    p.setHealth(20);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 255));
                    p.getInventory().clear();
                    p.sendTitle("UHC starting in 20 seconds", null, 10, 70, 20);
                } else {
                    p.sendTitle(null, "UHC starting in 20 seconds", 10, 70, 20);
                }
            }

            uhcOverworld.setDifficulty(Difficulty.NORMAL);

            getServer().dispatchCommand(getServer().getConsoleSender(), "/spreadplayers 0 0 400 " + spreadRadius + " true @a[gamemode=survival]");
        }
    }

    public void uhcStart(UUID worldUUID) {
        World uhcOverworld = Bukkit.getWorld(worldUUID);

        if(uhcOverworld != null) {
            uhcOverworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);

            for(Player p : Bukkit.getOnlinePlayers()) {
                if(p.getGameMode() == GameMode.SURVIVAL) {
                    p.setInvulnerable(false);
                    p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
                    p.sendTitle("Go!", null, 10, 70, 20);
                } else {
                    p.sendTitle(null, "Go!", 10, 70, 20);
                }
            }
        }
    }

    public void sendChatMessage(String message) {
        getServer().getConsoleSender().sendRawMessage(message);
    }

    public boolean refreshWorldMap(UUID uuid, WebsocketServer.MCNGWebsocket websocket, int x1, int y1, int x2, int y2) {
        World world = Bukkit.getWorld(uuid);

        if(world == null) {
            return false;
        }

        if(x1 < x2 || y1 < y2) {
            return false;
        }

        for(int t = 0; t < THREAD_COUNT; t++) {
            executorService.submit(new RefresherThread(t, x1, y1, x2, y2, world, uuid, websocket));
        }

        return true;
    }

    private class RefresherThread implements Runnable {

        final int threadOffset;
        final World world;
        final UUID uuid;
        final WebsocketServer.MCNGWebsocket websocket;
        final int x1, y1, x2, y2;

        private RefresherThread(int threadOffset, int x1, int y1, int x2, int y2, World world, UUID uuid, WebsocketServer.MCNGWebsocket websocket) {
            this.threadOffset = threadOffset;
            this.world = world;
            this.uuid = uuid;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.websocket = websocket;
        }

        @Override
        public void run() {
            for(int i = x1; i <= x2; i++) {
                for(int j = y1; j <= y2; j++) {
                    if(((i - x1) * (y2 - y1)) + (j - y1) % THREAD_COUNT == threadOffset) {
                        final Chunk chunk = world.getChunkAt(i, j);
                        boolean chunkLoaded = chunk.isLoaded();
                        final byte[] topBlocks = new byte[256];

                        if(!chunkLoaded) {
                            chunk.load(true);
                        }

                        final ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);

                        if(!chunkLoaded) {
                            chunk.unload();
                        }

                        for(int z = 0; z < 16; z++) {
                            for(int x = 0; x < 16; x++) {
                                final int h = snapshot.getHighestBlockYAt(x, z);
                                final Material m = snapshot.getBlockType(x, h, z);
                                byte cappedOrdinal = (byte) (m.ordinal() & 0xFF);
                                topBlocks[(z*16) + x] = cappedOrdinal;

                                System.out.println("Material " + m.name() + " has capped ordinal " + cappedOrdinal);
                            }
                        }

                        websocket.sendWorldChunk(uuid, i, j, topBlocks);
                    }
                }
            }
        }

    }

    public static byte[] getEquipLevels(Player player) {
        byte[] equipLevels = {-1, -1, -1, -1, -1, -1, -1};
        final PlayerInventory playerInventory = player.getInventory();
        final ItemStack helmet = playerInventory.getHelmet();
        final ItemStack chestplate = playerInventory.getChestplate();
        final ItemStack leggings = playerInventory.getLeggings();
        final ItemStack boots = playerInventory.getBoots();

        if(helmet != null) {
            final Material helmetMaterial = helmet.getType();
            switch(helmetMaterial) {
                case LEATHER_HELMET: equipLevels[0] = 0; break;
                case IRON_HELMET: equipLevels[0] = 1; break;
                case GOLDEN_HELMET: equipLevels[0] = 2; break;
                case DIAMOND_HELMET: equipLevels[0] = 3; break;
                default: break;
            }
        }

        if(chestplate != null) {
            final Material chestplateMaterial = chestplate.getType();
            switch(chestplateMaterial) {
                case LEATHER_CHESTPLATE: equipLevels[1] = 0; break;
                case IRON_CHESTPLATE: equipLevels[1] = 1; break;
                case GOLDEN_CHESTPLATE: equipLevels[1] = 2; break;
                case DIAMOND_CHESTPLATE: equipLevels[1] = 3; break;
                default: break;
            }
        }

        if(leggings != null) {
            final Material leggingsMaterial = leggings.getType();
            switch(leggingsMaterial) {
                case LEATHER_LEGGINGS: equipLevels[2] = 0; break;
                case IRON_LEGGINGS: equipLevels[2] = 1; break;
                case GOLDEN_LEGGINGS: equipLevels[2] = 2; break;
                case DIAMOND_LEGGINGS: equipLevels[2] = 3; break;
                default: break;
            }
        }

        if(boots != null) {
            final Material bootsMaterial = boots.getType();
            switch(bootsMaterial) {
                case LEATHER_BOOTS: equipLevels[3] = 0; break;
                case IRON_BOOTS: equipLevels[3] = 1; break;
                case GOLDEN_BOOTS: equipLevels[3] = 2; break;
                case DIAMOND_BOOTS: equipLevels[3] = 3; break;
                default: break;
            }
        }

        for(ItemStack stack : playerInventory.getContents()) {
            final Material stackMaterial = stack.getType();
            switch(stackMaterial) {
                case WOODEN_SWORD: if(equipLevels[4] < 0) equipLevels[4] = 0; break;
                case IRON_SWORD: if(equipLevels[4] < 1) equipLevels[4] = 1; break;
                case GOLDEN_SWORD: if(equipLevels[4] < 2) equipLevels[4] = 2; break;
                case DIAMOND_SWORD: if(equipLevels[4] < 3) equipLevels[4] = 3; break;

                case WOODEN_PICKAXE: if(equipLevels[5] < 0) equipLevels[5] = 0; break;
                case IRON_PICKAXE: if(equipLevels[5] < 1) equipLevels[5] = 1; break;
                case GOLDEN_PICKAXE: if(equipLevels[5] < 2) equipLevels[5] = 2; break;
                case DIAMOND_PICKAXE: if(equipLevels[5] < 3) equipLevels[5] = 3; break;

                case GOLDEN_APPLE:
                    if(equipLevels[6] < 0)
                        equipLevels[6]++;
                    equipLevels[6] += stack.getAmount(); break;
                default: break;
            }
        }

        return equipLevels;
    }
}
