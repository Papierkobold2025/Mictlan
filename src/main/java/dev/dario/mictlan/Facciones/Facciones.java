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

/**
 * Lee los JSON de facciones (data/mictlan_medieval/facciones/*.json) y, cuando
 * el jugador mata algo, le suma/resta la reputacion que indique cada faccion.
 */
public class Facciones {
    /** Rutas a los archivos de cada faccion dentro del datapack. */
    private ResourceLocation rutaPueblos = new ResourceLocation("mictlan_medieval", "facciones/pueblos.json");
    private ResourceLocation rutaAquelarre = new ResourceLocation("mictlan_medieval", "facciones/aquelarre.json");
    private ResourceLocation rutaMictlan = new ResourceLocation("mictlan_medieval", "facciones/mictlan.json");

    /** Archivos de faccion encontrados (vacio si el archivo no existe). */
    private List<Optional<Resource>> archivosFacciones = new ArrayList<>();

    /** Ultima faccion leida del JSON. */
    private accionMatar facciones;

    /** Id de la entidad que murio (por ejemplo: "minecraft:villager"). */
    private String entidadMatada;

    private FaccionPueblos faccionPueblos = new FaccionPueblos();
    private FaccionAquelarre faccionAquelarre = new FaccionAquelarre();
    private FaccionMictlan faccionMictlan = new FaccionMictlan();
    private ConfiguracionReputacion configuracionReputacion = new ConfiguracionReputacion();

    /** Ultima reputacion calculada por faccion. */
    private HashMap<String, Integer> reputacionFaccion = new HashMap<>();

    /** Todas las facciones que se revisan en cada muerte. */
    private List<ResourceLocation> rutaFacciones = new ArrayList<>(List.of(rutaPueblos, rutaAquelarre, rutaMictlan));

    /**
     * Forma del JSON de una faccion. Gson usa los nombres de los campos:
     * { "nombre": "Los Pueblos", "matar": { "minecraft:villager": 3, ... } }
     */
    public class accionMatar {
        String nombre = "";
        /** Entidad -> puntos de reputacion que da (o quita) matarla. */
        HashMap<String, Integer> matar = new HashMap<>();
    }

    /**
    * -------------------------------------------------------------------------
    * REPUTACION AL MATAR UNA ENTIDAD
    * -------------------------------------------------------------------------
    */

    public void configuracionFacciones(MinecraftServer servidor, Entity jugador, LivingEntity entidad) {
        accionMatar accion = new accionMatar();
        // Se releen los archivos en cada muerte (asi un /reload aplica sin reiniciar).
        archivosFacciones.clear();
        for(ResourceLocation directorio : rutaFacciones) {
            archivosFacciones.add(servidor.getResourceManager().getResource(directorio));
        }
        for(Optional<Resource> faccion : archivosFacciones){
            if(!faccion.isEmpty()) {
                try (BufferedReader leerFacciones = faccion.get().openAsReader()) {
                    facciones = MictlanMod.gson.fromJson(leerFacciones, accionMatar.class);
                    accion.nombre = facciones.nombre;
                    accion.matar = facciones.matar;
                } catch(IOException e) {
                    MictlanMod.LOGGER.error("[Mictlan] No se pudo obtener el objeto de Facciones.");
                    return;
                }
                entidadMatada = EntityType.getKey(entidad.getType()).toString();
                // Solo cambia la reputacion si esta faccion tiene puntos para esa entidad.
                if (accion.matar.containsKey(entidadMatada)) {
                    reputacionFaccion = configuracionReputacion.puntosDeReputacion(accion.matar.get(entidadMatada), jugador, facciones.nombre);
                }
            } else {
                MictlanMod.LOGGER.error("[Mictlan] El archivo " + faccion + " esta vacio!");
            }
        }
    }
}