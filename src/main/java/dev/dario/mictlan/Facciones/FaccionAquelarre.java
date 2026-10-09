package dev.dario.mictlan.Facciones;

import java.util.HashMap;

import net.minecraft.world.entity.Entity;

public class FaccionAquelarre {
    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion) {
        Integer reputacion = reputacionDeFaccion.get("El Aquelarre");
        if(reputacion <= 10 && reputacion >= -10) {

        } else if (reputacion <= 25) {

        } else if (reputacion >= -25) {

        } else if (reputacion <= 50) {

        } else if (reputacion >= -50) {
            
        }
    }
    
}
