package dev.dario.mictlan;

import java.util.HashMap;

import net.minecraft.server.network.ServerPlayerEntity;

public class ConfiguracionReputacion {
    private Integer reputacion = 0;
    private HashMap<String, Integer> hashmapReputacion = new HashMap<>();

    public void PuntosDeReputacion(String jugador, Integer puntos) {
        hashmapReputacion.put(jugador, puntos);
        PlayerDisconnection.estadisticasJugadores.put(hashmapReputacion, jugador.toString());
    }
    public void ReputacionAquelarre() {

    }
    public void ReputacionPueblo(ServerPlayerEntity jugador) {
        PlayerConnection.connectedPlayers.get(jugador.getUuidAsString());

    }
    public void ReputacionMictlan() {

    }
    
}
