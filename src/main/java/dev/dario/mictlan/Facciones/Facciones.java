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
    private List<Optional<Resource>> archivosFacciones = new ArrayList<>(); 
    private String nombre;
    private HashMap<String, Integer> matar = new HashMap<>();
    private Facciones facciones;
    private String entidadMatada;
    private FaccionPueblos faccionPueblos = new FaccionPueblos();
    private ConfiguracionReputacion configuracionReputacion = new ConfiguracionReputacion();
    private HashMap<String, Integer> reputacionFaccion = new HashMap<>();
    public void configuracionFacciones(MinecraftServer servidor, Entity jugador, LivingEntity entidad) {
        archivosFacciones.clear();
        archivosFacciones.add(servidor.getResourceManager().getResource(rutaPueblos));
        for(Optional<Resource> faccion : archivosFacciones){
            try (BufferedReader leerFacciones = faccion.get().openAsReader()) {
                facciones = MictlanMod.gson.fromJson(leerFacciones, Facciones.class);
                nombre = facciones.nombre;
                matar = facciones.matar;
            } catch(IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] No se pudo obtener el objeto de Facciones.");
            }
        }
        entidadMatada = EntityType.getKey(entidad.getType()).toString();
        if (matar.containsKey(entidadMatada)) {
            reputacionFaccion = configuracionReputacion.puntosDeReputacion(matar.get(entidadMatada), jugador, facciones.nombre);
        }
        if(reputacionFaccion.containsKey(facciones.nombre)){
            faccionPueblos.cambioReputacion(jugador, reputacionFaccion);
        }
    }
    public HashMap<String, Integer> isReputacionFaccion() {
        return this.reputacionFaccion;
    }
}