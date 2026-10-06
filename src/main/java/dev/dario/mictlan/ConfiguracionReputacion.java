package dev.dario.mictlan;

import java.util.HashMap;

public class ConfiguracionReputacion {
    private static Integer reputacion = 0;
    private static HashMap<String, Integer> hashmapReputacion = new HashMap<>();
    private static HashMap<String, HashMap<String, Integer>> hashMapReputacionPorFaccion = new HashMap<>();


    private static HashMap<String, Integer> PuntosDeReputacion(String jugador, Integer puntos) {
        if(hashmapReputacion.containsKey(jugador)){
            reputacion = hashmapReputacion.get(jugador);
        } else {
            reputacion = 0;
        }
        reputacion += puntos;
        hashmapReputacion.put(jugador, reputacion);
        return hashmapReputacion;
    } 
    public static HashMap<String, HashMap<String, Integer>> reputacionPorFaccion(
        String faccion, 
        HashMap<String, Integer> reputaciondeFaccion) {
        hashMapReputacionPorFaccion.put(faccion, reputaciondeFaccion);
        return hashMapReputacionPorFaccion;
    }
    public static void ReputacionAquelarre(String jugador, Integer puntos) {
        reputacionPorFaccion("Aquelarre", PuntosDeReputacion(jugador, puntos));
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    public static void ReputacionPueblo(String jugador, Integer puntos) {
        reputacionPorFaccion("Pueblo", PuntosDeReputacion(jugador, puntos));
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    public static void ReputacionMictlan(String jugador, Integer puntos) {
        reputacionPorFaccion("Mictlan", PuntosDeReputacion(jugador, puntos));
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    
}
