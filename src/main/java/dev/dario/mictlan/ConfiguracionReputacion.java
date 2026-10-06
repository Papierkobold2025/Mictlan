package dev.dario.mictlan;

import java.util.HashMap;

import net.minecraft.entity.Entity;

public class ConfiguracionReputacion {
    private static Integer reputacion = 0;
    private static HashMap<String, Integer> hashMapReputacionPorFaccion = new HashMap<>();
    private static String playerEntity = ""; 


    public static void reputacionPorFaccion(
    String faccion, 
    Integer reputaciondeFaccion,
    String jugador) {
        reputaciondeFaccion += reputacion;
        hashMapReputacionPorFaccion.put(faccion, reputaciondeFaccion);
        PlayerConnection.connectedPlayers.get(jugador).playerReputation(hashMapReputacionPorFaccion);
        Helpers.escribirDatosDelJugador(jugador);
    }
    public static void puntosDeReputacion(Integer puntos, Entity jugador, String faccion) {
        if(PlayerConnection.connectedPlayers.get(jugador.getUuidAsString()).isPlayerReputation().containsKey(faccion)){
            reputacion = PlayerConnection.connectedPlayers.get(jugador.getUuidAsString()).isPlayerReputation().get(faccion);
        } else { 
            reputacion = 0;
        }
        reputacion += puntos;
        playerEntity = jugador.getUuidAsString();
        if("Los Pueblos".equals(faccion)) {
            reputacionPorFaccion(faccion, puntos, playerEntity);
        } else if("El Aquelarre".equals(faccion)) {
            reputacionPorFaccion(faccion, puntos, playerEntity);
        } else if("Mictlan".equals(faccion)) {
            reputacionPorFaccion(faccion, puntos, playerEntity);
        }
    } 
}
