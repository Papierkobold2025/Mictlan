package dev.dario.mictlan.Facciones;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import dev.dario.mictlan.Core.MictlanMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public class Facciones {
    private ResourceLocation rutaPueblos = new ResourceLocation("mictlan_medieval", "facciones/pueblos.json");
    private ResourceLocation rutaAquelarre = new ResourceLocation("mictlan_medieval", "facciones/aquelarre.json");
    private ResourceLocation rutaMictlan = new ResourceLocation("mictlan_medieval", "facciones/mictlan.json");
    private List<Optional<Resource>> archivosFacciones = new ArrayList<>(); 
    private String nombre;
    private HashMap<String, Integer> matar = new HashMap<>();
    private Facciones facciones;
    private String entidadMatada;
    private FaccionPueblos faccionPueblos = new FaccionPueblos();
    private FaccionAquelarre faccionAquelarre = new FaccionAquelarre();
    private FaccionMictlan faccionMictlan = new FaccionMictlan();
    private ConfiguracionReputacion configuracionReputacion = new ConfiguracionReputacion();
    private HashMap<String, Integer> reputacionFaccion = new HashMap<>();
    private List<ResourceLocation> rutaFacciones = new ArrayList<>(List.of(rutaPueblos, rutaAquelarre, rutaMictlan));
    public void configuracionFacciones(MinecraftServer servidor, Entity jugador, LivingEntity entidad) {
        archivosFacciones.clear();
        for(ResourceLocation directorio : rutaFacciones) {
            archivosFacciones.add(servidor.getResourceManager().getResource(directorio));
        }
        for(Optional<Resource> faccion : archivosFacciones){
            if(!faccion.isEmpty()) {
                try (BufferedReader leerFacciones = faccion.get().openAsReader()) {
                    facciones = MictlanMod.gson.fromJson(leerFacciones, Facciones.class);
                    nombre = facciones.nombre;
                    matar = facciones.matar;
                } catch(IOException e) {
                    MictlanMod.LOGGER.error("[Mictlan] No se pudo obtener el objeto de Facciones.");
                    return;
                }
                entidadMatada = EntityType.getKey(entidad.getType()).toString();
                if (matar.containsKey(entidadMatada)) {
                    reputacionFaccion = configuracionReputacion.puntosDeReputacion(matar.get(entidadMatada), jugador, facciones.nombre);
/**
                    switch (nombre) {
                        case "Los Pueblos":
                            faccionPueblos.cambioReputacion(jugador, reputacionFaccion);                    
                            break;
                        case "El Aquelarre":
                            faccionAquelarre.cambioReputacion(jugador, reputacionFaccion);
                            break;
                        case "Mictlan":
                            faccionMictlan.cambioReputacion(jugador, reputacionFaccion);            
                        default:
                            break;
                    }
*/
                }
            } else {
                MictlanMod.LOGGER.error("[Mictlan] El archivo " + faccion + " esta vacio!");
            }
        }
    }
}