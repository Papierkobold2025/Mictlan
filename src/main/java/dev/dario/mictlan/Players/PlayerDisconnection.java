package dev.dario.mictlan.Players;

import java.nio.file.Path;
import java.util.HashMap;

import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.World.WorldLoad;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class PlayerDisconnection {
    public static ServerPlayer jugadorDesconectado;
    public static HashMap<HashMap<String, Integer>, String> estadisticasJugadores = new HashMap<>();

    /**
    * -------------------------------------------------------------------------
    * DESCONEXION DE UN JUGADOR
    * -------------------------------------------------------------------------
    */

    public static void playerDisconnection(PlayerEvent.PlayerLoggedOutEvent handler) {
        ServerPlayer jugador;
        if(handler.getEntity() instanceof ServerPlayer player){
            PlayerConnection.playerHandler = player;
            jugador = player;
        } else {
            return;
        }
            jugadorDesconectado = jugador;
            PlayerConnection.playerUUID = jugador.getGameProfile().getId().toString();
            PlayerConnection.playerFilePath = Path.of(
                WorldLoad.playerDir.toString(), 
                PlayerConnection.playerUUID +".json"
            );
            Helpers.leerDatosDelJugador(jugador);
            // Relee el archivo del jugador y lo vuelve a guardar.
            Helpers.escribirDatosDelJugador(jugador.getStringUUID());
            PlayerConnection.connectedPlayers.remove(jugador.getStringUUID());
            //String playerName = PlayerConnection.playerHandler.getGameProfile().getName();
            //MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
    }
}
