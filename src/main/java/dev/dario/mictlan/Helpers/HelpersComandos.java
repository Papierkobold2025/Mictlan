package dev.dario.mictlan.Helpers;

import java.io.IOException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.nio.file.Files;

import com.mojang.brigadier.context.CommandContext;

import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
import de.markusbordihn.easynpc.api.handler.EasyNPCMotionHandler;
import net.minecraft.world.phys.AABB;
import de.markusbordihn.easynpc.data.objective.ObjectiveDataEntry;
import de.markusbordihn.easynpc.data.objective.ObjectiveType;
import de.markusbordihn.easynpc.data.state.StateEntry;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.handler.ObjectiveHandler;
import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.NPC.ConfiguracionNPC;
import dev.dario.mictlan.Players.PlayerConnection;
import dev.dario.mictlan.World.WorldData;
import dev.dario.mictlan.World.WorldLoad;

import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.Path;

public class HelpersComandos {
    private static ChunkPos secondChunk;
    private static Path worldDataChunks = Path.of(WorldLoad.worldData.toString(), MictlanMod.eraActual + ".json");
    private static String worldChunkData = "";
    private static WorldData  worldChunkDataReturn;
    private static Path mictlanCoreDataFile;
    private static Path mictlanCoreEntitiesFile;
    private static ServerLevel mundoParaTransportar;
    private static List<Entity> entidadesDelChunk;
    private static CompoundTag entidadesAGuardar;
    private static CompoundTag archivoEntidades;
    private static AABB caja;
    private static ServerPlayer player;
    public static ChunkPos chunk;
    public static ChunkPos firstChunk;
    private static boolean npcFueMovido;
    public static ArrayList<Integer> homeChunksX = new ArrayList<>();
    public static ArrayList<Integer> homeChunksZ = new ArrayList<>();
    public static ArrayList<ChunkPos> totalChunkPosCount = new ArrayList<>();
    public UUID uuidNPC;
    private static ConfiguracionNPC movimientoNPC;

    /**
    * -------------------------------------------------------------------------
    * PRIMERA ESQUINA PARA EXTRACTO DE CHUNKS (BLOQUES/ENTIDADES)
    * -------------------------------------------------------------------------
    */

    public static void primeraEsquina(CommandContext<CommandSourceStack> context) {
        player = context.getSource().getPlayer();
        chunk = player.chunkPosition();
        homeChunksX.clear();
        homeChunksZ.clear();
        totalChunkPosCount.clear();
        firstChunk = chunk;
    }

    /**
    * -------------------------------------------------------------------------
    * SEGUNDA ESQUINA PARA EXTRACTO DE CHUNKS (BLOQUES/ENTIDADES)
    * -------------------------------------------------------------------------
    */

    public static void segundaEsquina(CommandContext<CommandSourceStack> context) {
        player = context.getSource().getPlayer();
        chunk = player.chunkPosition();
        secondChunk = chunk;
        MictlanMod.CurrentEra.mictlanHomeChunks(secondChunk, firstChunk);
        if(!Files.exists(worldDataChunks)) {
            try {
                Files.writeString(worldDataChunks, MictlanMod.gson.toJson(MictlanMod.CurrentEra));
            } catch (IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] Datos de los Chunks no pusieron ser escritos!");
            }
        }
        // Se relee de disco para usar lo que realmente quedo guardado.
        try {
            worldChunkData = Files.readString(worldDataChunks);
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos de los chunks no pueden ser leidos");
        }
        worldChunkDataReturn = MictlanMod.gson.fromJson(worldChunkData, MictlanMod.CurrentEra.getClass());
        ChunkPos homeStartPos = worldChunkDataReturn.getHomeStart();
        ChunkPos homeFinishPos = worldChunkDataReturn.getHomeFinish();

        // --- Paso 1: rango X (min/max por si las esquinas vienen en cualquier orden) ---
        int lowChunkCount = Math.min(homeStartPos.x, homeFinishPos.x);
        int highChunkCount = Math.max(homeStartPos.x, homeFinishPos.x);
        for(int chunksStart = lowChunkCount; chunksStart <= highChunkCount; chunksStart++) {
            homeChunksX.add(chunksStart);
        }

        // --- Paso 2: rango Z ---
        lowChunkCount = Math.min(homeStartPos.z, homeFinishPos.z);
        highChunkCount = Math.max(homeStartPos.z, homeFinishPos.z);
        for(int chunksFinish = lowChunkCount; chunksFinish <= highChunkCount; chunksFinish++) {
            homeChunksZ.add(chunksFinish);
        }

        // --- Paso 3: producto X * Z ---
        for( int chunkCounterX = 0; chunkCounterX < homeChunksX.size(); chunkCounterX++) {
            for(int chunkCounterZ = 0; chunkCounterZ < homeChunksZ.size(); chunkCounterZ++) {
                ChunkPos chunkAggregation = new ChunkPos (homeChunksX.get(chunkCounterX), homeChunksZ.get(chunkCounterZ));
                totalChunkPosCount.add(chunkAggregation);
            }
        }
    }

    /**
     * -------------------------------------------------------------------------
     * MARCAR NUEVA UBICACION PARA XOLOTL
     * -------------------------------------------------------------------------
    */

    public void marcarNuevaUbicacionNPC(CommandContext<CommandSourceStack> context, ResourceLocation identifier) {
        
        Optional<EasyNPC<?>> identifierYNPC= new ConfiguracionNPC(Optional.empty()).obtenerNPC(context, identifier);
        EasyNPC<?> identifierEasyNPC = identifierYNPC.get();
        movimientoNPC = new ConfiguracionNPC(identifierYNPC);
        BlockPos nuevaPosition = context.getSource().getPlayer().blockPosition();
        EasyNPCActionHandler.setState(
            identifierEasyNPC, 
            identifier,
            StateEntry.of(nuevaPosition.getX()),
            context.getSource().getPlayer()
        );
        npcFueMovido = EasyNPCMotionHandler.snapTo(identifierEasyNPC, Vec3.atBottomCenterOf(nuevaPosition));
        if(npcFueMovido) {
            movimientoNPC.xolotlYaSeMudo(true);
        } else {
            MictlanMod.LOGGER.error(identifierYNPC.toString() + " no ha podido moverse hacia la posicion nueva!");
        };
        identifierEasyNPC.getEasyNPCNavigationData().setHomePosition(nuevaPosition);
        ObjectiveHandler.addOrUpdateCustomObjective(
            identifierEasyNPC, 
            new ObjectiveDataEntry(ObjectiveType.RANDOM_STROLL_AROUND_HOME)
        );
        context.getSource().sendSuccess(() -> Component.literal("Xolotl ha emprendido su viaje y pronto estara contigo!"), false);
    } 

    /**
    * -------------------------------------------------------------------------
    * GUARDAR LOS CHUNKS CARGADOS EN RAM EN DISCO
    * -------------------------------------------------------------------------
    */

    public static void guardarCasaEnDisco(CommandContext<CommandSourceStack> context, ChunkPos chunks) {
        mictlanCoreDataFile = Path.of(WorldLoad.mictlanCoreDataPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
        mundoParaTransportar = context.getSource().getLevel();
        // Vacio si el chunk nunca se ha guardado en disco.
        Optional<CompoundTag> datosChunk = mundoParaTransportar.getChunkSource().chunkMap.read(chunks).join();
        if(datosChunk.isPresent()) {
            MictlanMod.LOGGER.info("[Mictlan] " + datosChunk.get().getAllKeys());
            try{
                NbtIo.writeCompressed(datosChunk.get(), mictlanCoreDataFile.toFile());
            } catch(IOException e) {
                MictlanMod.LOGGER.error("[Mictlan] Datos de Chunk no pudieron ser guardados en disco!");
            }
        }
    }

    /**
    * -------------------------------------------------------------------------
    * GUARDAR LAS ENTIDADES CARGADAS EN RAM EN DISCO
    * -------------------------------------------------------------------------
    */

    public static void guardarEntidadesEnDisco(ChunkPos chunks) {
        mictlanCoreEntitiesFile = Path.of(WorldLoad.mictlanCoreEntitiesPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
        // Entidades: se buscan por volumen (+1 porque getEnd es inclusivo).
        caja = new AABB(chunks.getMinBlockX(), mundoParaTransportar.getMinBuildHeight(), chunks.getMinBlockZ(), chunks.getMaxBlockX() + 1, mundoParaTransportar.getMaxBuildHeight(), chunks.getMaxBlockZ() +1);
        entidadesDelChunk = mundoParaTransportar.getEntities(null, caja);
        ListTag listaEntidades = new ListTag();
        for (Entity entity : entidadesDelChunk) {
            entidadesAGuardar = new CompoundTag();
            // false para jugadores, pasajeros y entidades removidas.
            if (entity.save(entidadesAGuardar)) {
                listaEntidades.add(entidadesAGuardar);
            }
        }
        archivoEntidades = new CompoundTag();
        archivoEntidades.put("entidades", listaEntidades);
        try {
            NbtIo.writeCompressed(archivoEntidades, mictlanCoreEntitiesFile.toFile());
        } catch (Exception e) {
            MictlanMod.LOGGER.error("Entidades del Chunk " + chunks.x + " " + chunks.z + " no se pudieron guardar.", e);
        }
    }

    /**
    * -------------------------------------------------------------------------
    * BORRAR LA ZONA HOME MARCADA
    * -------------------------------------------------------------------------
    */

    public static void borrarDatosCargados() {
        homeChunksZ.clear();
        totalChunkPosCount.clear();
        // El siguiente /mictlan home vuelve a ser la primera esquina.
        MictlanMod.commandExecuted = 0;
        try {
            Files.delete(Path.of(WorldLoad.worldData.toString(), MictlanMod.eraActual + ".json"));
        } catch (Exception e) {
            MictlanMod.LOGGER.error("[Mictlan] Datos de los chunks no pueden ser borrados");
        }
    }

    /**
    * -------------------------------------------------------------------------
    * LEER REPUTACION POR FACCION DEL PERSONAJE
    * -------------------------------------------------------------------------
    */    

    public static Integer leerReputacionPorFaccion(CommandContext<CommandSourceStack> context, String faccion) {
        String jugador = context.getSource().getPlayer().getStringUUID();
        Integer puntosDeReputacion = 0;
        HashMap<String, Integer> reputacionDelJugador = PlayerConnection.connectedPlayers.get(jugador).isPlayerReputation();
        if(reputacionDelJugador.containsKey(faccion)) {
            puntosDeReputacion = 0;
            puntosDeReputacion = reputacionDelJugador.get(faccion);
        } 
        return puntosDeReputacion;
    }
}
