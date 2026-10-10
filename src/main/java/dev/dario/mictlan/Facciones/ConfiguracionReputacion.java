package dev.dario.mictlan.Facciones;

import java.util.HashMap;

import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.Players.PlayerConnection;
import net.minecraft.world.entity.Entity;

/**
 * Suma (o resta) puntos de reputacion de un jugador en una faccion y guarda
 * el resultado en su archivo de jugador.
 */
public class ConfiguracionReputacion {
    /** Reputacion final de la faccion despues de aplicar los puntos. */
    private Integer reputacion = 0;

    /** Mapa "faccion -> reputacion" del jugador (referencia al de su PlayerData). */
    private HashMap<String, Integer> hashMapReputacionPorFaccion = new HashMap<>();

    /** UUID del jugador al que se le aplican los puntos. */
    private String playerEntity = "";

    /** Ultima reputacion calculada por faccion (lo que se devuelve). */
    private HashMap<String, Integer> reputacionResultante = new HashMap<>();

    /**
    * -------------------------------------------------------------------------
    * APLICAR PUNTOS DE REPUTACION A UNA FACCION
    * -------------------------------------------------------------------------
    */

    public HashMap<String, Integer> puntosDeReputacion(Integer puntos, Entity jugador, String faccion) {
        playerEntity = jugador.getStringUUID();
        // Se modifica el mapa del PlayerData directamente, no una copia.
        hashMapReputacionPorFaccion = PlayerConnection.connectedPlayers.get(playerEntity).isPlayerReputation();
        if(hashMapReputacionPorFaccion.containsKey(faccion)){
            reputacion = puntos + hashMapReputacionPorFaccion.get(faccion);
        } else {
            // Primera vez que el jugador gana/pierde reputacion con esta faccion.
            reputacion = puntos;
        }
        hashMapReputacionPorFaccion.put(faccion, reputacion);
        // Se guarda en disco en cada cambio para no perderlo si el servidor se cae.
        Helpers.escribirDatosDelJugador(playerEntity);
        reputacionResultante.put(faccion, reputacion);
        return reputacionResultante;
    }
}