package dev.dario.mictlan;

import java.util.HashMap;

public class ConfiguracionReputacion {
    private static Integer reputacion = 0;
    private static HashMap<String, Integer> hashmapReputacion = new HashMap<>();


    private static void PuntosDeReputacion(String jugador, Integer puntos) {
        if(hashmapReputacion.containsKey(jugador)){
            reputacion = hashmapReputacion.get(jugador);
        } else {
            reputacion = 0;
        }
        reputacion += puntos;
        hashmapReputacion.put(jugador, reputacion);
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashmapReputacion);
        Helpers.escribirDatosDelJugador(jugador);
    } 
    public void ReputacionAquelarre() {

    }
    public static void ReputacionPueblo(String jugador, Integer puntos) {
        PuntosDeReputacion(jugador, puntos);
    }
    public void ReputacionMictlan() {

    }
    
}
