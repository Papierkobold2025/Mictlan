package dev.dario.mictlan;

/**
 * Clase para almacenar los datos del jugador, incluyendo su UUID, si ha jugado
 * antes y si ha recibido el kit de inicio.
 */
public class PlayerData {
    /** Identificador unico del jugador dentro del servidor. */
    private String playerUUID;

    /** Indica si el jugador ya habia entrado anteriormente al mundo. */
    private boolean hasPlayedBefore;

    /** Indica si el jugador ya recibio el objeto de bienvenida. */
    private boolean hasReceivedStarterKit;

    /** Indica en que era se encuentra el jugador */
    private String era;

    /** Crea un registro nuevo asociado al UUID indicado. */
    public PlayerData(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    /** Actualiza el indicador de si el jugador ya habia jugado antes. */
    public void hasPlayedBefore(boolean hasPlayedBefore) {
        this.hasPlayedBefore = hasPlayedBefore;
    }

    /** Actualiza el indicador de entrega del kit de inicio. */
    public void hasReceivedStarterKit(boolean hasReceivedStarterKit) {
        this.hasReceivedStarterKit = hasReceivedStarterKit;
    }

    public void setEra(String era) {
        this.era = era;
    }

    /** Devuelve si el jugador ya habia jugado anteriormente. */
    public boolean isHasPlayedBefore() {
        return this.hasPlayedBefore;
    }

    /** Devuelve si el jugador ya recibio el kit de inicio. */
    public boolean isHasReceivedStarterKit() {
        return this.hasReceivedStarterKit;
    }

    /** Devuelve el UUID del jugador */
    public String getPlayerUUID() {
        return this.playerUUID;
    }
}
