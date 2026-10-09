package dev.dario.mictlan.Facciones;

import java.util.HashMap;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class FaccionPueblos {
    MerchantOffers merchantOffers;
    record Nivel(int minimo, int maximo, String nombre) {}
    List<Nivel> niveles = List.of(
        new Nivel(Integer.MIN_VALUE, -5, "Malo"),
        new Nivel(-4, 5, "Neutral"),
        new Nivel(6, 15, "Bueno"),
        new Nivel(16, Integer.MAX_VALUE, "Excelente")
    );
    String nivel = "Sin nivel";
    public void calculoReputacion(AbstractContainerMenu ofertaAldeano, Integer reputacion) {
        for(Nivel n : niveles) {
            if(reputacion >= n.minimo() && reputacion <= n.maximo) {
                nivel = n.nombre();
            break;
            }
            nivel = "Sin nivel";
        }
        switch (nivel) {
            case "Bueno":
                if(ofertaAldeano instanceof MerchantMenu merchantMenu) {
                    merchantOffers = merchantMenu.getOffers();
                    for(MerchantOffer offer : merchantOffers) {
                        offer.addToSpecialPriceDiff(-3);
                    }
                }
                break;
            case "Excelente":
                if(ofertaAldeano instanceof MerchantMenu merchantMenu) {
                    merchantOffers = merchantMenu.getOffers();
                    for(MerchantOffer offer : merchantOffers) {
                        offer.addToSpecialPriceDiff(-15);
                    }
                }
            break;
            default:
                break;
        }
    }
    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion, AbstractContainerMenu ofertaAldeano) {
        Integer reputacion = 0;
        if(reputacionDeFaccion.containsKey("Los Pueblos")){
            reputacion = reputacionDeFaccion.get("Los Pueblos");
            calculoReputacion(ofertaAldeano, reputacion);
        } else {
            reputacionDeFaccion.put("Los Pueblos", 0);
            calculoReputacion(ofertaAldeano, reputacionDeFaccion.get("Los Pueblos"));
        }


    }
}