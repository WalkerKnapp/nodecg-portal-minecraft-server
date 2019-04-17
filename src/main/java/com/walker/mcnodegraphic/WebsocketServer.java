package com.walker.mcnodegraphic;

import com.fasterxml.jackson.core.*;
import fi.iki.elonen.NanoWSD;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebsocketServer extends NanoWSD {

    private CopyOnWriteArrayList<MCNGWebsocket> connections = new CopyOnWriteArrayList<>();
    private JsonFactory jsonFactory;

    private MCNGServer plugin;

    public WebsocketServer(int port, MCNGServer plugin) {
        super(port);
        this.jsonFactory = new JsonFactory();
        this.plugin = plugin;
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new MCNGWebsocket(this, handshake);
    }

    private void sendToAllConnections(String message) {
        for(MCNGWebsocket ws : connections) {
            try {
                ws.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendWorldInit(UUID uuid, String name, long seed, double size) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 0);
            jg.writeStringField("u", uuid.toString());
            jg.writeStringField("name", name);
            jg.writeNumberField("seed", seed);
            jg.writeNumberField("wbSize", size);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateWorldBorder(UUID uuid, double size) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 1);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("wbSize", size);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlayerInit(UUID uuid, String displayName, GameMode gameMode,
                               int xpLevel, double health, float absorpHearts,
                               Location position, Team team, int killCount, byte[] equipLevels) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 2);
            jg.writeStringField("u", uuid.toString());
            jg.writeStringField("name", displayName);
            jg.writeNumberField("gamemode", gameMode.getValue());
            jg.writeNumberField("level", xpLevel);
            jg.writeNumberField("health", health);
            jg.writeNumberField("absorp", absorpHearts);
            jg.writeNumberField("posX", position.getX());
            jg.writeNumberField("posZ", position.getZ());
            jg.writeStringField("world", position.getWorld().getUID().toString());
            jg.writeStringField("team", team.getName());
            jg.writeNumberField("kills", killCount);
            jg.writeBinaryField("equip", equipLevels);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerPosition(UUID uuid, Location position) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 3);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("posX", position.getX());
            jg.writeNumberField("posZ", position.getZ());
            jg.writeStringField("world", position.getWorld().getUID().toString());
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerTeam(UUID uuid, Team team) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 4);
            jg.writeStringField("u", uuid.toString());
            jg.writeStringField("team", team.getName());
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerGamemode(UUID uuid, GameMode gameMode) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 5);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("gamemode", gameMode.getValue());
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerHealth(UUID uuid, double health, float absorpHearts, boolean isDamage) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 6);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("health", health);
            jg.writeNumberField("absorp", absorpHearts);
            jg.writeBooleanField("damage", isDamage);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateKillCount(UUID uuid, int killCount) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 7);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("kills", killCount);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerEquipment(UUID uuid, byte[] equipLevels) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 8);
            jg.writeStringField("u", uuid.toString());
            jg.writeBinaryField("equip", equipLevels);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerLevels(UUID uuid, int xpLevel) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 9);
            jg.writeStringField("u", uuid.toString());
            jg.writeNumberField("level", xpLevel);
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlayerDeath(UUID uuid) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 10);
            jg.writeStringField("u", uuid.toString());
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPlayerDestroy(UUID uuid) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JsonGenerator jg = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeNumberField("p", 11);
            jg.writeStringField("u", uuid.toString());
            jg.writeEndObject();
            sendToAllConnections(new String(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MCNGWebsocket extends WebSocket {
        private final WebsocketServer server;

        MCNGWebsocket(WebsocketServer server, IHTTPSession handshakeReq) {
            super(handshakeReq);
            this.server = server;
        }


        @Override
        protected void onOpen() {
            server.connections.add(this);

            // Send all initialized worlds:
            for(World world : Bukkit.getWorlds()) {
                sendWorldInit(world.getUID(), world.getName(), world.getSeed(), world.getWorldBorder().getSize());
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                sendPlayerInit(player.getUniqueId(),
                        player.getDisplayName(),
                        player.getGameMode(),
                        player.getLevel(),
                        player.getHealth(),
                        ((CraftPlayer)player).getHandle().getAbsorptionHearts(),
                        player.getLocation(),
                        server.plugin.scoreboard.getTeam(player.getDisplayName()),
                        server.plugin.scoreboard.getObjective("kills").getScore(player.getDisplayName()).getScore(),
                        MCNGServer.getEquipLevels(player));
            }
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            server.connections.remove(this);
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            try (JsonParser jp = server.jsonFactory.createParser(message.getBinaryPayload())) {
                if(jp.nextToken() != JsonToken.START_OBJECT) return;
                if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                if(!"p".equals(jp.getCurrentName())) return;
                if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;

                switch(jp.getValueAsInt()) {
                    case 0:
                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"u".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_STRING) return;
                        UUID worldUUID = UUID.fromString(jp.getValueAsString());

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"x1".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int x1 = jp.getValueAsInt();

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"y1".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int y1 = jp.getValueAsInt();

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"x2".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int x2 = jp.getValueAsInt();

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"y2".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int y2 = jp.getValueAsInt();

                        server.plugin.refreshWorldMap(worldUUID, this, x1, y1, x2, y2);
                        break;
                    case 1:
                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"u".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_STRING) return;
                        UUID playerUuid = UUID.fromString(jp.getValueAsString());

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"gm".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int gamemode = jp.getValueAsInt();

                        server.plugin.setPlayerGamemode(playerUuid, gamemode);
                        break;
                    case 2:
                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"u".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_STRING) return;
                        UUID world2UUID = UUID.fromString(jp.getValueAsString());

                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"r".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_NUMBER_INT) return;
                        int radius = jp.getValueAsInt();

                        server.plugin.uhcPrestart(world2UUID, radius);
                        break;
                    case 3:
                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"u".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_STRING) return;
                        UUID world3UUID = UUID.fromString(jp.getValueAsString());

                        server.plugin.uhcStart(world3UUID);
                        break;
                    case 4:
                        if(jp.nextToken() != JsonToken.FIELD_NAME) return;
                        if(!"m".equals(jp.getCurrentName())) return;
                        if(jp.nextToken() != JsonToken.VALUE_STRING) return;
                        String chatMessage = jp.getValueAsString();

                        server.plugin.sendChatMessage(chatMessage);
                        break;
                    default: break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {

        }

        public void sendWorldInit(UUID uuid, String name, long seed, double wbSize) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                JsonGenerator jg = server.jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
                jg.writeStartObject();
                jg.writeNumberField("p", 0);
                jg.writeStringField("u", uuid.toString());
                jg.writeStringField("name", name);
                jg.writeNumberField("seed", seed);
                jg.writeNumberField("wbSize", wbSize);
                jg.writeEndObject();
                send(new String(baos.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendPlayerInit(UUID uuid, String displayName, GameMode gameMode, int xpLevel, double health, float absorpHearts, Location position, Team team, int killCount, byte[] equipLevels) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                JsonGenerator jg = server.jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
                jg.writeStartObject();
                jg.writeNumberField("p", 2);
                jg.writeStringField("u", uuid.toString());
                jg.writeStringField("name", displayName);
                jg.writeNumberField("gamemode", gameMode.getValue());
                jg.writeNumberField("level", xpLevel);
                jg.writeNumberField("health", health);
                jg.writeNumberField("absorp", absorpHearts);
                jg.writeNumberField("posX", position.getX());
                jg.writeNumberField("posZ", position.getZ());
                jg.writeStringField("world", position.getWorld().getUID().toString());
                jg.writeStringField("team", team.getName());
                jg.writeNumberField("kills", killCount);
                jg.writeBinaryField("equip", equipLevels);
                jg.writeEndObject();
                send(new String(baos.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendWorldChunk(UUID worldUUID, int locX, int locY, byte[] blocks) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                JsonGenerator jg = server.jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
                jg.writeStartObject();
                jg.writeNumberField("p", 2);
                jg.writeStringField("u", worldUUID.toString());
                jg.writeNumberField("locX", locX);
                jg.writeNumberField("locY", locY);
                jg.writeBinaryField("blocks", blocks);
                jg.writeEndObject();
                send(new String(baos.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
