package dev.dario.mictlan.Players;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.NPC.ConfiguracionNPC;
import dev.dario.mictlan.World.WorldLoad;

import java.nio.file.Files;


public class PlayerConnection {
    public static ServerPlayer playerHandler;
    public static String playerUUID; 
    public static PlayerData playerData;
    public static Path playerFilePath;
    public static HashMap<String, PlayerData> connectedPlayers = new HashMap<>();
    private static boolean xolotlPresent;
    public static Optional<EasyNPC<?>> spawnXolotl = Optional.empty();
    public static String playerDataJson = "";
    public static ArrayList<ServerPlayer> jugadoresDisponibles = new ArrayList<>();
    private static MutableComponent mictlanFormatting = Component.literal("Mictlan").withStyle(ChatFormatting.DARK_PURPLE);

    /**
    * -------------------------------------------------------------------------
    * CONEXION DE UN JUGADOR
    * -------------------------------------------------------------------------
    */

    public static PlayerData playerConnection(ServerPlayer handler){
        playerHandler = handler;
        String playerName = playerHandler.getGameProfile().getName();
        //jugadoresDisponibles.add(playerHandler);
        playerUUID = playerHandler.getGameProfile().getId().toString();
        playerData = new PlayerData(handler.getStringUUID());
        playerFilePath = Path.of(WorldLoad.playerDir.toString(), playerUUID +".json");
        // Xolotl aparece en el spawn la primera vez que alguien entra.
        xolotlPresent = ConfiguracionNPC.xolotlSpawn(handler);
        if(xolotlPresent == false) {
            MictlanMod.LOGGER.error("[Mictlan] Xolotl no pudo ser generado!");
        } else {
            MictlanMod.LOGGER.info("[Mictlan] Xolotl ha sido spawneado");
        }
        
        // Primera conexion.
        if (!Files.exists(playerFilePath)) {
            Component welcomeMessage = Component.literal("Bienvenido a ").append(mictlanFormatting).append(Component.literal(playerName + "!"));
            playerHandler.sendSystemMessage(welcomeMessage, false);
            connectedPlayers.put(playerUUID, playerData);
            Helpers.escribirDatosDelJugador(playerUUID);
        } else {
            // Jugador existente.
            Helpers.leerDatosDelJugador(playerHandler);
            // leerDatosDelJugador deja el JSON en playerDataJson.
            playerData = MictlanMod.gson.fromJson(playerDataJson, playerData.getClass());
            Component welcomeBackMessage = Component.literal("Bienvenido de nuevo a ").append(mictlanFormatting).append(playerName + "!");
            playerHandler.sendSystemMessage(welcomeBackMessage, false);
            playerData.hasPlayedBefore(true);
            playerData.setEra(MictlanMod.eraActual);
            // Se registra como conectado y se guarda con los datos actualizados.
            connectedPlayers.put(playerUUID, playerData);
            Helpers.escribirDatosDelJugador(playerUUID);
        };
        if (!Files.exists(WorldLoad.mictlanConfigFile)) {
            Helpers.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, MictlanMod.CurrentEra);
        }
        Component eraMessage = Component.literal("Te encuentras en la era ").append(Component.literal(MictlanMod.eraActual).withStyle(ChatFormatting.DARK_AQUA));
        playerHandler.sendSystemMessage(eraMessage, false);

        MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se conecto.");

        return playerData;
    }
}
