package dev.dario.mictlan;

import java.io.IOException;
import java.nio.file.DirectoryStream;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;


public class Helpers {

    private static PlayerData playerData;

    /**
    * -------------------------------------------------------------------------
    * PEGADO DE CHUNKS EN CONFIGURACION
    * -------------------------------------------------------------------------
    */

    public static void pegadoDeChunksEnConfig(ServerWorld mundo, WorldData respuestaArchivoDeConfiguracion, Path mictlanCoreDataPath, WorldData CurrentEra, Path mictlanConfigFile) {
        NbtList entidades;
        ChunkPos posicionChunksMundoNuevo = null;
        if(respuestaArchivoDeConfiguracion.getHomeChunksPasted()) {
            int counter = 0;
            try (DirectoryStream<Path> oldWorldDataResources = Files.newDirectoryStream(mictlanCoreDataPath, "*.nbt")) {
                for (Path archivo : oldWorldDataResources) {
                    NbtCompound datos = NbtIo.readCompressed(archivo.toFile());
                    // Posicion tomada del propio NBT, no del nombre del archivo.
                    posicionChunksMundoNuevo = new ChunkPos(datos.getInt("xPos"), datos.getInt("zPos"));

                    // Vacia el inventario de los block entities.
                    entidades = datos.getList("block_entities", NbtElement.COMPOUND_TYPE);
                    for(NbtList entities = entidades; counter < entities.size(); counter ++) {
                        entities.getCompound(counter).remove("Items");
                    }
                    mundo.getChunkManager().threadedAnvilChunkStorage.setNbt(posicionChunksMundoNuevo, datos);
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

    public static void pegadoDeEntidadesEnNuevoMundo(ServerWorld mundo, Path mictlanCoreEntitiesPath) {
        int counter = 0;
        try (DirectoryStream<Path> oldWorldEntityResources = Files.newDirectoryStream(mictlanCoreEntitiesPath, "*.nbt")){
            for (Path archivo : oldWorldEntityResources) {
                NbtCompound datos = NbtIo.readCompressed(archivo.toFile());
                NbtList recibirEntidades = datos.getList("entidades", NbtElement.COMPOUND_TYPE);
                for(NbtList recoleccionDeEntidades = recibirEntidades; counter < recoleccionDeEntidades.size(); counter++) {
                    NbtCompound entidadGuardada = recoleccionDeEntidades.getCompound(counter);
                    // Crea la entidad con su UUID, posicion y datos originales.
                    Optional<Entity> pegarEntidadesEnMundo = EntityType.getEntityFromNbt(entidadGuardada, mundo);

                    if(pegarEntidadesEnMundo.isPresent()) {
                        MictlanMod.LOGGER.info("[Mictlan] " + pegarEntidadesEnMundo.get());
                        mundo.spawnEntityAndPassengers(pegarEntidadesEnMundo.get());
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
            Files.writeString(PlayerConnection.playerFilePath, MictlanMod.gson.toJson(PlayerConnection.connectedPlayers.get(playerUUID)));
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos no escritos a disco!");
        }            
    };
    
    /**
    * -------------------------------------------------------------------------
    * LEER DATOS DEL JUGADOR
    * -------------------------------------------------------------------------
    */    
    
    public static void leerDatosDelJugador(ServerPlayerEntity jugador) {
        try{
            PlayerConnection.playerDataJson = Files.readString(PlayerConnection.playerFilePath);
        } catch(Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] No se pudieron leer contenidos del archivo del jugador!");
        }            
    };

    /**
    * -------------------------------------------------------------------------
    * ENTREGA DEL KIT INICIAL
    * -------------------------------------------------------------------------
    */

    public static void entregaKitInicial(ServerPlayerEntity jugador) {
        playerData = new PlayerData(jugador.getUuidAsString());
        try{
            ItemStack welcomeItem = new ItemStack(net.minecraft.item.Items.WOODEN_SHOVEL, 1);
            Identifier welcomeBookIdentifier = new Identifier("patchouli","guide_book");
            ItemStack welcomeBook =new ItemStack(Registries.ITEM.get(welcomeBookIdentifier));
            String playerUUID = jugador.getUuidAsString();
            welcomeBook.getOrCreateNbt().putString("patchouli:book", "mictlan:codice");
            if(PlayerConnection.playerHandler.getInventory().insertStack(welcomeItem) && PlayerConnection.playerHandler.getInventory().insertStack(welcomeBook)) {
                PlayerConnection.connectedPlayers.put(playerUUID, playerData);
            }
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] No se pudo entregar el objeto de bienvenida al jugador.", e);
        }
    }
}
