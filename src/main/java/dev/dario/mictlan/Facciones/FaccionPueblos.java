package dev.dario.mictlan.Facciones;

import java.util.HashMap;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Comportamiento de la faccion "Los Pueblos": segun la reputacion del jugador,
 * los aldeanos le hacen descuento al abrir el menu de tradeo.
 */
public class FaccionPueblos {
    /** Ofertas del aldeano con el que se esta tradeando. */
    MerchantOffers merchantOffers;

    /** Rango de reputacion [minimo, maximo] (ambos inclusivos) con su nombre. */
    record Nivel(int minimo, int maximo, String nombre) {}

    /** Niveles de reputacion con Los Pueblos, de peor a mejor. */
    List<Nivel> niveles = List.of(
        new Nivel(Integer.MIN_VALUE, -5, "Malo"),
        new Nivel(-4, 5, "Neutral"),
        new Nivel(6, 15, "Bueno"),
        new Nivel(16, Integer.MAX_VALUE, "Excelente")
    );

    /** Nivel calculado en la ultima llamada a calculoReputacion. */
    String nivel = "Sin nivel";

    /**
    * -------------------------------------------------------------------------
    * APLICAR DESCUENTO SEGUN NIVEL DE REPUTACION
    * -------------------------------------------------------------------------
    */

    public void calculoReputacion(AbstractContainerMenu ofertaAldeano, Integer reputacion) {
        // Busca en que rango cae la reputacion.
        for(Nivel n : niveles) {
            if(reputacion >= n.minimo() && reputacion <= n.maximo) {
                nivel = n.nombre();
            break;
            }
            nivel = "Sin nivel";
        }
        // Valor negativo = descuento sobre la cantidad del primer objeto que pide el aldeano.
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
            // Malo y Neutral: precio normal (por ahora).
            default:
                break;
        }
    }

    /**
    * -------------------------------------------------------------------------
    * LEER REPUTACION DE LOS PUEBLOS AL ABRIR EL TRADEO
    * -------------------------------------------------------------------------
    */

    public void cambioReputacion(Entity jugador, HashMap<String, Integer> reputacionDeFaccion, AbstractContainerMenu ofertaAldeano) {
        Integer reputacion = 0;
        if(reputacionDeFaccion.containsKey("Los Pueblos")){
            reputacion = reputacionDeFaccion.get("Los Pueblos");
            calculoReputacion(ofertaAldeano, reputacion);
        } else {
            // Sin reputacion registrada: se empieza en 0 (Neutral).
            reputacionDeFaccion.put("Los Pueblos", 0);
            calculoReputacion(ofertaAldeano, reputacionDeFaccion.get("Los Pueblos"));
        }


    }

    /**
    * -------------------------------------------------------------------------
    * QUITAR DESCUENTO AL CERRAR EL TRADEO
    * -------------------------------------------------------------------------
    */

    public void reseteoReputacion(Entity jugador, AbstractContainerMenu menuAbstracto) {
        // Las ofertas son del aldeano, no del jugador: si no se limpian, el
        // descuento se acumularia cada vez que se abre el menu.
        if(menuAbstracto instanceof MerchantMenu menu) {
            MerchantOffers ofertas = menu.getOffers();
            for(MerchantOffer oferta : ofertas) {
                oferta.resetSpecialPriceDiff();
            }
        }
    }
}