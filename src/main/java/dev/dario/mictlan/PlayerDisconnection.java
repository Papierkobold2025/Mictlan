package dev.dario.mictlan;

import java.nio.file.Path;
import java.util.HashMap;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerDisconnection {
    public static ServerPlayerEntity jugadorDesconectado;
    public static HashMap<HashMap<String, Integer>, String> estadisticasJugadores = new HashMap<>();

    /**
    * -------------------------------------------------------------------------
    * DESCONEXION DE UN JUGADOR
    * -------------------------------------------------------------------------
    */

    public static void playerDisconnection(ServerPlayNetworkHandler handler) {
            PlayerConnection.playerHandler = handler.getPlayer();
            jugadorDesconectado = handler.getPlayer();
            PlayerConnection.playerUUID = PlayerConnection.playerHandler.getGameProfile().getId().toString();
            PlayerConnection.playerFilePath = Path.of(WorldLoad.playerDir.toString(), PlayerConnection.playerUUID +".json");
            Helpers.leerDatosDelJugador(handler.getPlayer());
            // Relee el archivo del jugador y lo vuelve a guardar.
            Helpers.escribirDatosDelJugador(handler.getPlayer().getUuidAsString());
            PlayerConnection.connectedPlayers.remove(handler.getPlayer().getUuidAsString());
            String playerName = PlayerConnection.playerHandler.getGameProfile().getName();
            MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
    }
}
