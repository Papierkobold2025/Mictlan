package dev.dario.mictlan.Facciones;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import dev.dario.mictlan.Core.MictlanMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.Resource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class Facciones {
    private Identifier rutaPueblos = new Identifier("mictlan_medieval", "facciones/pueblos.json");
    private List<Optional<Resource>> archivosFacciones; 
    private String nombre;
    private HashMap<String, Integer> matar = new HashMap<>();
    private Facciones facciones;
    private String entidadMatada;
    private FaccionPueblos faccionPueblos = new FaccionPueblos();
    private ConfiguracionReputacion configuracionReputacion = new ConfiguracionReputacion();
    private HashMap<String, Integer> reputacionFaccion = new HashMap<>();
    public void configuracionFacciones(ServerWorld servidor, Entity jugador, LivingEntity entidad) {
        archivosFacciones.add(servidor.getServer().getResourceManager().getResource(rutaPueblos));
        for(Optional<Resource> faccion : archivosFacciones){
            try (BufferedReader leerFacciones = faccion.get().getReader()) {
                facciones = MictlanMod.gson.fromJson(leerFacciones, Facciones.class);
                nombre = facciones.nombre;
                matar = facciones.matar;
            } catch(IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] No se pudo obtener el objeto de Facciones.");
            }
        }
        entidadMatada = EntityType.getId(entidad.getType()).toString();
        if (matar.containsKey(entidadMatada)) {
            reputacionFaccion = configuracionReputacion.puntosDeReputacion(matar.get(entidadMatada), jugador, facciones.nombre);
        }
        if(reputacionFaccion.containsKey(facciones.nombre)){
            faccionPueblos.cambioReputacion(jugador, reputacionFaccion);
        }
    }
}