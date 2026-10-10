package dev.dario.mictlan.Facciones;

import java.util.HashMap;

import net.minecraft.world.entity.Entity;

/**
 * Comportamiento de la faccion "El Aquelarre" segun la reputacion del jugador.
 * Por ahora solo estan los rangos, sin consecuencias configuradas.
 */
public class FaccionAquelarre {

    /**
    * -------------------------------------------------------------------------
    * CONSECUENCIAS SEGUN REPUTACION (PENDIENTE)
    * -------------------------------------------------------------------------
    */

    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion) {
        Integer reputacion = reputacionDeFaccion.get("El Aquelarre");
        // Neutral: entre -10 y 10.
        if(reputacion <= 10 && reputacion >= -10) {

        } else if (reputacion <= 25) {

        } else if (reputacion >= -25) {

        } else if (reputacion <= 50) {

        } else if (reputacion >= -50) {

        }
    }

}
