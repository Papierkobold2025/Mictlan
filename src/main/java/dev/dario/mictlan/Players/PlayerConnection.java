package dev.dario.mictlan.Players;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import de.markusbordihn.easynpc.api.handler.EasyNPCEntityHandler;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.NPC.ConfiguracionNPC;
import dev.dario.mictlan.World.WorldLoad;

import java.nio.file.Files;
import net.minecraft.world.phys.Vec3;

public class PlayerConnection {
    public static ServerPlayer playerHandler;
    public static String playerUUID; 
    public static PlayerData playerData;
    public static Path playerFilePath;
    public static HashMap<String, PlayerData> connectedPlayers = new HashMap<>();
    public static Optional<EasyNPC<?>> spawnXolotl;
    public static String playerDataJson = "";
    public static ArrayList<ServerPlayer> jugadoresDisponibles = new ArrayList<>();

    /**
    * -------------------------------------------------------------------------
    * CONEXION DE UN JUGADOR
    * -------------------------------------------------------------------------
    */

    public static void playerConnection(ServerPlayer handler){
        playerHandler = handler;
        String playerName = playerHandler.getGameProfile().getName();
        //jugadoresDisponibles.add(playerHandler);
        playerUUID = playerHandler.getGameProfile().getId().toString();
        playerData = new PlayerData(handler.getStringUUID());
        playerFilePath = Path.of(WorldLoad.playerDir.toString(), playerUUID +".json");
        // Xolotl aparece en el spawn la primera vez que alguien entra.
        if(EasyNPCEntityHandler.getByCustomIdentifier(new ResourceLocation(ConfiguracionNPC.xolotlNPCIdentifier)).isEmpty()) {
            MictlanMod.LOGGER.info("[Mictlan] El NPC aun no se ha generado!");
            spawnXolotl = EasyNPCEntityHandler.spawnFromPreset(new ResourceLocation(ConfiguracionNPC.identificadoPresetXolotl), playerHandler.getServer().overworld(), Vec3.atBottomCenterOf(playerHandler.getServer().overworld().getSharedSpawnPos()), null, null);
        }
        // Primera conexion.
        if (!Files.exists(playerFilePath)) {
            Component welcomeMessage = Component.literal("Bienvenido a Mictlan, " + playerName + "!");
            playerHandler.sendSystemMessage(welcomeMessage, false);
            connectedPlayers.put(playerUUID, playerData);
            Helpers.escribirDatosDelJugador(playerUUID);
        } else {
            // Jugador existente.
            Helpers.leerDatosDelJugador(playerHandler);
            
            playerData = MictlanMod.gson.fromJson(playerDataJson, playerData.getClass());
            Component welcomeBackMessage = Component.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
            playerHandler.sendSystemMessage(welcomeBackMessage, false);
            playerData.hasPlayedBefore(true);
            playerData.setEra(MictlanMod.eraActual);
            // Reintento del kit (p. ej. inventario lleno la vez anterior).
            connectedPlayers.put(playerUUID, playerData);
            Helpers.escribirDatosDelJugador(playerUUID);
        };
        if (!Files.exists(WorldLoad.mictlanConfigFile)) {
            Helpers.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, MictlanMod.CurrentEra);
        }
        Component eraMessage = Component.literal("Te encuentras en la era " + MictlanMod.eraActual);
        playerHandler.sendSystemMessage(eraMessage, false);

        MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se conecto.");
    }
}
