package dev.dario.mictlan;

import java.nio.file.Path;

public class PlayerDisconnection {
    public static void playerDisconnection() {
            PlayerConnection.playerHandler = MictlanMod.serverHandler.getPlayer();
            PlayerConnection.playerUUID = PlayerConnection.playerHandler.getGameProfile().getId().toString();
            PlayerConnection.playerFilePath = Path.of(WorldLoad.playerDir.toString(), PlayerConnection.playerUUID +".json");
            MictlanMod.helperLeerDatosDelJugador.leerDatosDelJugador(MictlanMod.serverHandler.getPlayer());
            // Actualiza la ultima posicion y guarda.
            PlayerConnection.playerDataJsonReturn = MictlanMod.gson.fromJson(PlayerConnection.playerDataJson, PlayerConnection.playerData.getClass());
            MictlanMod.helperEscribirDatosDelJugador.escribirDatosDelJugador();
            String playerName = PlayerConnection.playerHandler.getGameProfile().getName();
            MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
    }
}
