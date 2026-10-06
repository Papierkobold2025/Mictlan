package dev.dario.mictlan;

import java.util.HashMap;

import net.minecraft.entity.Entity;

public class ConfiguracionReputacion {
    private Integer reputacion = 0;
    private HashMap<String, Integer> hashMapReputacionPorFaccion = new HashMap<>();
    private String playerEntity = ""; 
    public void puntosDeReputacion(Integer puntos, Entity jugador, String faccion) {
        playerEntity = jugador.getUuidAsString();
        hashMapReputacionPorFaccion = PlayerConnection.connectedPlayers.get(playerEntity).isPlayerReputation();
        if(hashMapReputacionPorFaccion.containsKey(faccion)){
            reputacion = puntos + hashMapReputacionPorFaccion.get(faccion);
        } else { 
            reputacion = puntos;        
        }
        hashMapReputacionPorFaccion.put(faccion, reputacion);
        Helpers.escribirDatosDelJugador(playerEntity);
    } 
}