package dev.dario.mictlan;

/**
 * Clase para almacenar los datos del jugador, incluyendo su UUID, si ha jugado antes y si ha recibido el kit de inicio.
 * PlayerData
 */
public class PlayerData {
    private String playerUUID;
    private boolean hasPlayedBefore;
    private boolean hasReceivedStarterKit;

    public PlayerData(String playerUUID) {
        this.playerUUID = playerUUID;
    }
}
