package dev.dario.mictlan;

import net.minecraft.server.network.ServerPlayerEntity;
import java.nio.file.Path;
import java.nio.file.Files;
import net.minecraft.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class PlayerConnection {
    public static ServerPlayerEntity playerHandler;
    public static String playerUUID; 
    public static PlayerData playerData;
    public static Path playerFilePath;
    public static PlayerData playerDataJsonReturn;
    public static void playerConnection(ServerPlayNetworkHandler handler){
        playerHandler = handler.getPlayer();
        String playerName = playerHandler.getGameProfile().getName();
        playerUUID = playerHandler.getGameProfile().getId().toString();

        playerData = new PlayerData(playerUUID);

        playerFilePath = Path.of(WorldLoad.playerDir.toString(), playerUUID +".json");

        // Primera conexion.
        if (!Files.exists(playerFilePath)) {
            Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
            playerHandler.sendMessage(welcomeMessage, false);
            playerDataJsonReturn = playerData;
            playerDataJsonReturn.hasPlayedBefore(true);
            MictlanMod.helperEntregaKitInicial.entregaKitInicial(playerHandler);
            MictlanMod.helperEscribirDatosDelJugador.escribirDatosDelJugador();
        } else {
            // Jugador existente.
            leerDatosDelJugador(playerHandler);
            playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
            Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
            playerHandler.sendMessage(welcomeBackMessage, false);
            playerDataJsonReturn.hasPlayedBefore(true);
            boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
            playerDataJsonReturn.setEra(eraActual);
            // Reintento del kit (p. ej. inventario lleno la vez anterior).
            if(!hasReceivedStarterKit) {
                entregaKitInicial(playerHandler);
            }
            escribirDatosDelJugador();
        };
        if (!Files.exists(WorldLoad.mictlanConfigFile)) {
            helperEscribirDatosDeConfiguracion.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, CurrentEra);
        }
        Text eraMessage = Text.literal("Te encuentras en la era " + eraActual);
        playerHandler.sendMessage(eraMessage, false);

        LOGGER.info("[Mictlan] " + playerName + " se conecto.");
    }
}
