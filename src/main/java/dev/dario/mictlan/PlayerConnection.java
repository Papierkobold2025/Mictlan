package dev.dario.mictlan;

import net.minecraft.server.network.ServerPlayerEntity;
import java.nio.file.Path;
import java.nio.file.Files;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public class PlayerConnection {
    public static ServerPlayerEntity playerHandler;
    public static String playerUUID; 
    public static PlayerData playerData;
    public static Path playerFilePath;
    public static PlayerData playerDataJsonReturn;
    public static String playerDataJson = "";
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
            MictlanMod.helperLeerDatosDelJugador.leerDatosDelJugador(playerHandler);
            playerDataJsonReturn =MictlanMod.gson.fromJson(playerDataJson, playerData.getClass());
            Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
            playerHandler.sendMessage(welcomeBackMessage, false);
            playerDataJsonReturn.hasPlayedBefore(true);
            boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
            playerDataJsonReturn.setEra(MictlanMod.eraActual);
            // Reintento del kit (p. ej. inventario lleno la vez anterior).
            if(!hasReceivedStarterKit) {
                MictlanMod.helperEntregaKitInicial.entregaKitInicial(playerHandler);
            }
            MictlanMod.helperEscribirDatosDelJugador.escribirDatosDelJugador();
        };
        if (!Files.exists(WorldLoad.mictlanConfigFile)) {
            MictlanMod.helperEscribirDatosDeConfiguracion.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, MictlanMod.CurrentEra);
        }
        Text eraMessage = Text.literal("Te encuentras en la era " + MictlanMod.eraActual);
        playerHandler.sendMessage(eraMessage, false);

        MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se conecto.");
    }
}
