package dev.dario.mictlan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.ArrayList;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.io.IOException;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.Optional;

import com.google.gson.Gson;
import net.minecraft.util.math.Box;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Punto de entrada del mod. Registra eventos de mundo, conexion de
 * jugadores y el comando /mictlan.
 */
public class MictlanMod implements ModInitializer {

    /**
     * -------------------------------------------------------------------------
     * CONSTANTES DEL MOD
     * -------------------------------------------------------------------------
     */

    /** Debe coincidir con el "id" de fabric.mod.json. */
    public static final String MOD_ID = "mictlan";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Serializador JSON compartido para jugadores y configuracion. */
    public static final Gson gson = new Gson();

    /**
     * -------------------------------------------------------------------------
     * RUTAS DE CARPETAS Y ARCHIVOS
     * -------------------------------------------------------------------------
     * Todas se calculan cuando se carga el mundo (ver ServerWorldEvents.LOAD),
     * porque antes de eso todavia no sabemos donde esta guardado el mundo.
     */

    /** world/<era>.json */
    private Path worldDataChunks;

    /** data/Chunk_<x> <z>.nbt */
    private Path mictlanCoreDataFile;

    /** entities/Chunk_<x> <z>.nbt */
    private Path mictlanCoreEntitiesFile;

    /**
     * -------------------------------------------------------------------------
     * DATOS DE LA ERA
     * -------------------------------------------------------------------------
     */

    /** Debe declararse antes de CurrentEra (orden de inicializacion). */
    public static String eraActual= "MEDIEVAL";

    /** Estado de la era en memoria; se escribe en mictlan.json. */
    public static WorldData CurrentEra = new WorldData(eraActual);

    /** Plantilla con el UUID; solo se usa para getClass() y jugadores nuevos. */
    PlayerData playerData;

    /**
     * -------------------------------------------------------------------------
     * ZONA "HOME" Y TRANSPORTE DE CHUNKS
     * -------------------------------------------------------------------------
     */

    /** Usos de /mictlan home; impar = primera esquina, par = segunda. */
    private int commandExecuted = 0;

    /** Esquina marcada en el primer /mictlan home. */
    private ChunkPos firstChunk;

    /** Esquina marcada en el segundo /mictlan home. */
    private ChunkPos secondChunk;

    /** JSON crudo de world/<era>.json. */
    private String worldChunkData = "";

    /** world/<era>.json ya parseado. */
    private WorldData worldChunkDataReturn;

    /** Dimension origen de los chunks del "home". */
    private ServerWorld mundoParaTransportar;

    /** Coordenadas X entre las dos esquinas (inclusive). */
    private ArrayList<Integer> homeChunksX = new ArrayList<>();

    /** Coordenadas Z entre las dos esquinas (inclusive). */
    private ArrayList<Integer> homeChunksZ = new ArrayList<>();

    /** Todos los chunks del "home" (producto X * Z). */
    private ArrayList<ChunkPos> totalChunkPosCount = new ArrayList<>();

    /**
     * -------------------------------------------------------------------------
     * TRANSPORTE DE ENTIDADES
     * -------------------------------------------------------------------------
     */

    /** Volumen de un chunk completo (de getBottomY a getTopY). */
    private Box caja;

    /** Entidades encontradas dentro de la caja del chunk. */
    private List<Entity> entidadesDelChunk;

    /** NBT de una sola entidad al copiarla. */
    private NbtCompound entidadesAGuardar;

    /** Raiz del .nbt de entidades: { "entidades": [ ... ] }. */
    private NbtCompound archivoEntidades;

    /**
     * -------------------------------------------------------------------------
     * INICIALIZACION DEL MOD
     * -------------------------------------------------------------------------
     */

    @Override
    public void onInitialize() {

        LOGGER.info("[Mictlan] Inicializado correctamente. Sin Mixins, sin Nexus todavia.");

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CARGA DEL MUNDO
         * -------------------------------------------------------------------------
         */
        ServerWorldEvents.LOAD.register((server,world) -> {
            WorldLoad.worldLoad(server,world,CurrentEra);
        });

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CONEXION DE JUGADORES
         * -------------------------------------------------------------------------
         */

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerConnection.playerConnection(handler);
        });

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE DESCONEXION DE USUARIO
         * -------------------------------------------------------------------------
         */
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDisconnection.playerDisconnection(handler);
        });

        /**
         * -------------------------------------------------------------------------
         * REGISTRO DE COMANDOS
         * -------------------------------------------------------------------------
         */

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("mictlan")
                /**
                 * -----------------------------------------------------------------
                 * /mictlan era
                 * -----------------------------------------------------------------
                 */
                .then(CommandManager.literal("era")
                    .executes(context-> {
                        context.getSource().sendFeedback(() -> Text.literal("Te encuentras en la era " + eraActual), false);
                        return 1;
                    })
                )
                /**
                 * -----------------------------------------------------------------
                 * /mictlan home
                 * -----------------------------------------------------------------
                 */
                // Alterna entre primera y segunda esquina de la zona.
                .then(CommandManager.literal("home")
                    .executes(context ->{
                        HelpersComandos.guardadoDeCasa(context);
                            LOGGER.info("[Mictlan] " + HelpersComandos.totalChunkPosCount);
                            context.getSource().sendFeedback(() -> Text.literal("Tu casa se ha guardado exitosamente"), false);

                            /**
                             * -----------------------------------------------------------------
                             * COPIA DE LOS CHUNKS DEL "HOME" A DISCO
                             * -----------------------------------------------------------------
                             * Por cada chunk de la zona pedimos a Minecraft sus datos en
                             * formato NBT (el mismo formato en el que el juego guarda el
                             * mundo) y los escribimos en un archivo propio dentro de
                             * <juego>/mictlan/data, para poder llevarlos a otra era despues.
                             * Las entidades van aparte, en <juego>/mictlan/entities, porque
                             * Minecraft no las guarda dentro del NBT del chunk.
                             */
                            for(ChunkPos chunks : totalChunkPosCount) {
                                mictlanCoreDataFile = Path.of(WorldLoad.mictlanCoreDataPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
                                mictlanCoreEntitiesFile = Path.of(WorldLoad.mictlanCoreEntitiesPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
                                mundoParaTransportar = context.getSource().getWorld();
                                // Entidades: se buscan por volumen (+1 porque getEnd es inclusivo).
                                caja = new Box(chunks.getStartX(), mundoParaTransportar.getBottomY(), chunks.getStartZ(), chunks.getEndX() + 1, mundoParaTransportar.getTopY(), chunks.getEndZ() +1);
                                entidadesDelChunk = mundoParaTransportar.getOtherEntities(null, caja);
                                NbtList listaEntidades = new NbtList();
                                for (Entity entity : entidadesDelChunk) {
                                        entidadesAGuardar = new NbtCompound();
                                    // false para jugadores, pasajeros y entidades removidas.
                                    if (entity.saveSelfNbt(entidadesAGuardar)) {
                                            listaEntidades.add(entidadesAGuardar);
                                    }
                                }

                                archivoEntidades = new NbtCompound();
                                archivoEntidades.put("entidades", listaEntidades);

                                try {
                                    NbtIo.writeCompressed(archivoEntidades, mictlanCoreEntitiesFile.toFile());
                                } catch (Exception e) {
                                    LOGGER.error("Entidades del Chunk " + chunks.x + " " + chunks.z + " no se pudieron guardar.", e);
                                }
                                // Vacio si el chunk nunca se ha guardado en disco.
                                Optional<NbtCompound> datosChunk = mundoParaTransportar.getChunkManager().threadedAnvilChunkStorage.getNbt(chunks).join();
                                if(datosChunk.isPresent()) {
                                    LOGGER.info("[Mictlan] " + datosChunk.get().getKeys());
                                    try{
                                        NbtIo.writeCompressed(datosChunk.get(), mictlanCoreDataFile.toFile());
                                    } catch(IOException e) {
                                        LOGGER.error("[Mictlan] Datos de Chunk no pudieron ser guardados en disco!");
                                    }
                                }
                            }
                            // Marca pegado pendiente para el proximo mundo que se cargue.
                            CurrentEra.homeChunkPasted(true);
                            Helpers.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, CurrentEra);
                        return 1;
                    })
                )
                /**
                 * -----------------------------------------------------------------
                 * /mictlan home clear
                 * -----------------------------------------------------------------
                 */
                // Brigadier fusiona esta rama "home" con la anterior.
                .then(CommandManager.literal("home")
                    .then(CommandManager.literal("clear")
                        .executes(context -> {
                            homeChunksX.clear();
                            homeChunksZ.clear();
                            totalChunkPosCount.clear();
                            // El siguiente /mictlan home vuelve a ser la primera esquina.
                            commandExecuted = 0;
                            try {
                                Files.delete(Path.of(WorldLoad.worldData.toString(), eraActual + ".json"));
                            } catch (Exception e) {
                                LOGGER.error("[Mictlan] Datos de los chunks no pueden ser borrados");
                            }
                            context.getSource().sendFeedback(() -> Text.literal("Los Chunks guardados han sido borrados exitosamente"), false);

                            return 1;
                        })
                    )
                )
            );
        });
    };
} 
