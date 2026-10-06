package dev.dario.mictlan;

import java.util.HashMap;

import net.minecraft.entity.Entity;

public class ConfiguracionReputacion {
    private static Integer reputacion = 0;
    private static HashMap<String, Integer> hashmapReputacion = new HashMap<>();
    private static HashMap<String, Integer> hashMapReputacionPorFaccion = new HashMap<>();
    private static String playerEntity = ""; 

    public static HashMap<String, Integer> reputacionPorFaccion(
    String faccion, 
    Integer reputaciondeFaccion) {
        reputaciondeFaccion += reputacion;
        hashMapReputacionPorFaccion.put(faccion, reputaciondeFaccion);
        return hashMapReputacionPorFaccion;
    }
    public static void puntosDeReputacion(Integer puntos, Entity jugador, String faccion) {
        if(hashmapReputacion.containsKey(jugador.getUuidAsString())){
            reputacion = hashmapReputacion.get(jugador.getUuidAsString());
        } else { 
            reputacion = 0;
        }
        reputacion += puntos;
        playerEntity = jugador.getUuidAsString();
        if("Los Pueblos".equals(faccion)) {
            ConfiguracionReputacion.ReputacionAquelarre(playerEntity, puntos);
        } else if("El Aquelarre".equals(faccion)) {
            ConfiguracionReputacion.ReputacionAquelarre(playerEntity, puntos);
        } else if("Mictlan".equals(faccion)) {
            ConfiguracionReputacion.ReputacionMictlan(playerEntity, puntos);
        }
    } 

    public static void ReputacionAquelarre(String jugador, Integer puntos) {
        reputacionPorFaccion("Aquelarre", puntos);
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    public static void ReputacionPueblo(String jugador, Integer puntos) {
        reputacionPorFaccion("Pueblo", puntos);
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    public static void ReputacionMictlan(String jugador, Integer puntos) {
        reputacionPorFaccion("Mictlan", puntos);
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    
}
