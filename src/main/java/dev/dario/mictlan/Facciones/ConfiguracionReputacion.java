package dev.dario.mictlan.Facciones;

import java.util.HashMap;

import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.Players.PlayerConnection;
import net.minecraft.world.entity.Entity;

public class ConfiguracionReputacion {
    private Integer reputacion = 0;
    private HashMap<String, Integer> hashMapReputacionPorFaccion = new HashMap<>();
    private String playerEntity = ""; 
    private HashMap<String, Integer> reputacionResultante = new HashMap<>();
    public HashMap<String, Integer> puntosDeReputacion(Integer puntos, Entity jugador, String faccion) {
        playerEntity = jugador.getStringUUID();
        hashMapReputacionPorFaccion = PlayerConnection.connectedPlayers.get(playerEntity).isPlayerReputation();
        if(hashMapReputacionPorFaccion.containsKey(faccion)){
            reputacion = puntos + hashMapReputacionPorFaccion.get(faccion);
        } else { 
            reputacion = puntos;        
        } 
        hashMapReputacionPorFaccion.put(faccion, reputacion);
        Helpers.escribirDatosDelJugador(playerEntity);
        reputacionResultante.put(faccion, reputacion);
        return reputacionResultante;
    } 
}