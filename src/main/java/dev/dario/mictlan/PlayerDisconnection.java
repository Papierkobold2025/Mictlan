package dev.dario.mictlan;

import java.nio.file.Path;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerDisconnection {
    public static ServerPlayerEntity jugadorDesconectado;
    public static void playerDisconnection(ServerPlayNetworkHandler handler) {
            PlayerConnection.playerHandler = handler.getPlayer();
            jugadorDesconectado = handler.getPlayer();
            PlayerConnection.playerUUID = PlayerConnection.playerHandler.getGameProfile().getId().toString();
            PlayerConnection.playerFilePath = Path.of(WorldLoad.playerDir.toString(), PlayerConnection.playerUUID +".json");
            Helpers.leerDatosDelJugador(handler.getPlayer());
            // Actualiza la ultima posicion y guarda.
            PlayerConnection.playerDataJsonReturn = MictlanMod.gson.fromJson(PlayerConnection.playerDataJson, PlayerConnection.playerData.getClass());
            Helpers.escribirDatosDelJugador();
            String playerName = PlayerConnection.playerHandler.getGameProfile().getName();
            MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
    }
}
