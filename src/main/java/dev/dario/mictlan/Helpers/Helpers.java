package dev.dario.mictlan.Helpers;

import java.io.IOException;
import java.nio.file.DirectoryStream;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;

import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.Players.PlayerConnection;
import dev.dario.mictlan.World.WorldData;
import dev.dario.mictlan.World.WorldLoad;
import net.minecraft.world.entity.Entity;


public class Helpers {

    /**
    * -------------------------------------------------------------------------
    * PEGADO DE CHUNKS EN CONFIGURACION
    * -------------------------------------------------------------------------
    */

    public static void pegadoDeChunksEnConfig(ServerLevel mundo, WorldData respuestaArchivoDeConfiguracion, Path mictlanCoreDataPath, WorldData CurrentEra, Path mictlanConfigFile) {
        ListTag entidades;
        ChunkPos posicionChunksMundoNuevo = null;
        if(respuestaArchivoDeConfiguracion.getHomeChunksPasted()) {
            int counter = 0;
            try (DirectoryStream<Path> oldWorldDataResources = Files.newDirectoryStream(mictlanCoreDataPath, "*.nbt")) {
                for (Path archivo : oldWorldDataResources) {
                    CompoundTag datos = NbtIo.readCompressed(archivo, NbtAccounter.unlimitedHeap());
                    // Posicion tomada del propio NBT, no del nombre del archivo.
                    posicionChunksMundoNuevo = new ChunkPos(datos.getIntOr("xPos", 0), datos.getIntOr("zPos", 0));

                    // Vacia el inventario de los block entities.
                    entidades = datos.getListOrEmpty("block_entities");
                    for(ListTag entities = entidades; counter < entities.size(); counter ++) {
                        entities.getCompound(counter).ifPresent(compound -> compound.remove("Items"));
                    }
                    mundo.getChunkSource().chunkMap.write(posicionChunksMundoNuevo, datos);
                    Files.delete(archivo);
                    counter = 0;
                }
            } catch (IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] No se pudo escribir " + posicionChunksMundoNuevo + " en mundo nuevo!", e);
            }
            CurrentEra.homeChunkPasted(false);
            escribirDatosDeConfiguracion(mictlanConfigFile, CurrentEra);
        }
    };

    /**
    * -------------------------------------------------------------------------
    * PEGADO DE ENTIDADES EN EL MUNDO
    * -------------------------------------------------------------------------
    */

    public static void pegadoDeEntidadesEnNuevoMundo(ServerLevel mundo, Path mictlanCoreEntitiesPath) {
        int counter = 0;
        try (DirectoryStream<Path> oldWorldEntityResources = Files.newDirectoryStream(mictlanCoreEntitiesPath, "*.nbt")){
            for (Path archivo : oldWorldEntityResources) {
                CompoundTag datos = NbtIo.readCompressed(archivo, NbtAccounter.unlimitedHeap());
                ListTag recibirEntidades = datos.getListOrEmpty("entidades");
                for(ListTag recoleccionDeEntidades = recibirEntidades; counter < recoleccionDeEntidades.size(); counter++) {
                    CompoundTag entidadGuardada = recoleccionDeEntidades.getCompoundOrEmpty(counter);
                    // Crea la entidad con su UUID, posicion y datos originales.
                    Optional<Entity> pegarEntidadesEnMundo = EntityType.create(entidadGuardada, mundo);

                    if(pegarEntidadesEnMundo.isPresent()) {
                        MictlanMod.LOGGER.info("[Mictlan] " + pegarEntidadesEnMundo.get());
                        mundo.addFreshEntityWithPassengers(pegarEntidadesEnMundo.get());
                    }
                }
                Files.delete(archivo);
                counter = 0;
            }
        } catch (IOException e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos de entidades no pudieron ser escritas a mundo");
        }
    };
    
    /**
    * -------------------------------------------------------------------------
    * ESCRIBIR DATOS DE CONFIGURACION
    * -------------------------------------------------------------------------
    */

    public static void escribirDatosDeConfiguracion(Path mictlanConfigFile, WorldData CurrentEra) {
        try{
            Files.writeString(mictlanConfigFile, MictlanMod.gson.toJson(CurrentEra));
        } catch (IOException e) {
            MictlanMod.LOGGER.error("[Mictlan] No se pudo escribir los datos del configuracion.", e);
        }
    };

    /**
    * -------------------------------------------------------------------------
    * ESCRIBIR DATOS DE JUGADOR
    * -------------------------------------------------------------------------
    */   
    
    public static void escribirDatosDelJugador(String playerUUID) {
        try{
            Files.writeString(Path.of(WorldLoad.playerDir.toString(), playerUUID + ".json"), MictlanMod.gson.toJson(PlayerConnection.connectedPlayers.get(playerUUID)));
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos no escritos a disco!");
        }            
    };
    
    /**
    * -------------------------------------------------------------------------
    * LEER DATOS DEL JUGADOR
    * -------------------------------------------------------------------------
    */    
    
    public static void leerDatosDelJugador(ServerPlayer jugador) {
        try{
            PlayerConnection.playerDataJson = Files.readString(PlayerConnection.playerFilePath);
        } catch(Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] No se pudieron leer contenidos del archivo del jugador!");
        }            
    }
}
