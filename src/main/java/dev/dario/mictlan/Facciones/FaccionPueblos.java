package dev.dario.mictlan.Facciones;

import java.util.HashMap;

import net.minecraft.world.entity.Entity;

public class FaccionPueblos {
    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion) {
        Integer reputacion = reputacionDeFaccion.get("Pueblos");
        if(reputacion <= 10 && reputacion >= -10) {

        } else if (reputacion <= 25) {

        } else if (reputacion >= -25) {

        } else if (reputacion <= 50) {

        } else if (reputacion >= -50) {
            
        }
    }
}
