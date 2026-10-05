package dev.dario.mictlan;

import java.util.HashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class HelperReputacion {
    private static TagKey<EntityType<?>> listaEntidades = TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier("mictlan_medieval", "faccion/pueblos"));
    private static HashMap<String, Integer> listaReputacion = new HashMap<>();
    private static Integer puntos = 0;
    public static void BajarReputacion(ServerWorld servidor, Entity jugador, LivingEntity entidad) {
        if(entidad.getType().isIn(listaEntidades)) {
            String entidadMatada = EntityType.getId(entidad.getType()).toString();
            puntos = (listaReputacion.get(entidadMatada));

        }
    }
}
