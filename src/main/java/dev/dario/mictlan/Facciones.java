package dev.dario.mictlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.Resource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class Facciones {
    private Identifier rutaPueblos = new Identifier("mictlan_medieval", "facciones/pueblos.json");
    private Optional<Resource> archivoFacciones; 
    private String nombre;
    private HashMap<String, Integer> matar = new HashMap<>();
    private Facciones facciones;
    private String entidadMatada;
    public void configuracionFacciones(ServerWorld servidor, Entity jugador, LivingEntity entidad) {
        archivoFacciones = servidor.getServer().getResourceManager().getResource(rutaPueblos);
        try (BufferedReader leerFacciones = archivoFacciones.get().getReader()) {
            facciones = MictlanMod.gson.fromJson(leerFacciones, Facciones.class);
            nombre = facciones.nombre;
            matar = facciones.matar;
        } catch(IOException e) {
            MictlanMod.LOGGER.error("[Mictlan] No se pudo obtener el objeto de Facciones.");
        }
        entidadMatada = EntityType.getId(entidad.getType()).toString();
        if (matar.containsKey(entidadMatada)) {
            if("Los Pueblos".equals(facciones.nombre)) {
                ConfiguracionReputacion.ReputacionPueblo(jugador.getUuidAsString(), matar.get(entidadMatada));
            }else if("El Aquelarre".equals(facciones.nombre)){
                ConfiguracionReputacion.ReputacionAquelarre(jugador.getUuidAsString(), matar.get(entidadMatada));
            } else if("Mictlan".equals(facciones.nombre)) {
                ConfiguracionReputacion.ReputacionMictlan(jugador.getUuidAsString(), matar.get(entidadMatada));
            }
        }
    }
}