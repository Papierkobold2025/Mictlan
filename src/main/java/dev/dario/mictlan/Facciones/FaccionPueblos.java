package dev.dario.mictlan.Facciones;

import java.util.HashMap;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class FaccionPueblos {
    
    record Nivel(int minimo, int maximo, String nombre) {}
    List<Nivel> niveles = List.of(
        new Nivel(-20, -10, "Malo"),
        new Nivel(-4, 5, "Neutral"),
        new Nivel(6, 15, "Bueno"),
        new Nivel(16, 24, "Excelente")
    );
    String nivel = "Sin nivel";
    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion, MerchantOffer ofertaAldeano) {
        Integer reputacion = reputacionDeFaccion.get("Los Pueblos");
            for(Nivel n : niveles) {
            if(reputacion >= n.minimo() && reputacion <= n.maximo) {
                nivel = n.nombre();
                break;
            }
        }
        switch (nivel) {
            case "Bueno":
                ofertaAldeano.addToSpecialPriceDiff(-3);
                break;
            case "Excelente":
                ofertaAldeano.addToSpecialPriceDiff(-15);
            default:
                break;
        }

    }
}