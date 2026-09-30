package dev.dario.mictlan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
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
    private Gson gson = new Gson();

    /**
     * -------------------------------------------------------------------------
     * RUTAS DE CARPETAS Y ARCHIVOS
     * -------------------------------------------------------------------------
     * Todas se calculan cuando se carga el mundo (ver ServerWorldEvents.LOAD),
     * porque antes de eso todavia no sabemos donde esta guardado el mundo.
     */

    /** <mundo>/mictlan */
    private Path mictlanDir;

    /** mictlan/character */
    private Path characterDir;

    /** mictlan/players */
    private Path playerDir;

    /** players/<uuid>.json */
    private Path playerFilePath;

    /** config/mictlan */
    private Path mictlanConfigDir;

    /** mictlan/world */
    private Path worldData;

    /** world/<era>.json */
    private Path worldDataChunks;

    /** config/mictlan/mictlan.json */
    private Path mictlanConfigFile;

    /** <juego>/mictlan: raiz compartida entre mundos (sobrevive al cambio de era). */
    private Path mictlanCorePath;

    /** <juego>/mictlan/data: .nbt de los chunks del "home". */
    private Path mictlanCoreDataPath;

    /** data/Chunk_<x> <z>.nbt */
    private Path mictlanCoreDataFile;

    /** <juego>/mictlan/entities: .nbt de las entidades del "home". */
    private Path mictlanCoreEntitiesPath;

    /** entities/Chunk_<x> <z>.nbt */
    private Path mictlanCoreEntitiesFile;

    /**
     * -------------------------------------------------------------------------
     * DATOS DE LA ERA
     * -------------------------------------------------------------------------
     */

    /** Debe declararse antes de CurrentEra (orden de inicializacion). */
    private String eraActual= "MEDIEVAL";

    /** Estado de la era en memoria; se escribe en mictlan.json. */
    WorldData CurrentEra = new WorldData(eraActual);

    /** Contenido de mictlan.json leido al cargar el mundo. */
    private WorldData respuestaArchivoDeConfiguracion;

    /** JSON crudo de mictlan.json. */
    private String readMictlanConfigFile;

    /**
     * -------------------------------------------------------------------------
     * DATOS DEL JUGADOR
     * -------------------------------------------------------------------------
     */

    /** Ultimo jugador que se conecto / desconecto. */
    private ServerPlayerEntity playerHandler;

    private String playerUUID;

    /** Posicion (x, y, z) como texto. */
    private String playerLocation;

    /** Plantilla con el UUID; solo se usa para getClass() y jugadores nuevos. */
    PlayerData playerData;

    /** JSON crudo leido de players/<uuid>.json. */
    private String playerDataJson = "";

    /** Datos del jugador a escribir / leidos de disco. */
    private PlayerData playerDataJsonReturn;

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

    /** Sin uso por ahora. */
    private Path characterFilePath;

    /** Posicion destino del chunk que se pega en el mundo nuevo. */
    private ChunkPos posicionChunksMundoNuevo;

    /** "block_entities" del chunk que se esta pegando (cofres, hornos...). */
    private NbtList entidades;

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

    /** Lista "entidades" leida de un .nbt al pegar. */
    private NbtList recibirEntidades;

    /** NBT de una sola entidad al pegarla. */
    private NbtCompound entidadGuardada;

    /** Entidad reconstruida; vacio si el tipo no existe en esta version. */
    private Optional<Entity> pegarEntidadesEnMundo;

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
            if(world.getRegistryKey() == World.OVERWORLD){
                mictlanConfigDir = FabricLoader.getInstance().getConfigDir().resolve("mictlan");
                mictlanConfigFile = Path.of(mictlanConfigDir.toString(), "mictlan.json" );
                mictlanDir = server.getSavePath(WorldSavePath.ROOT).resolve("mictlan");
                characterDir = mictlanDir.resolve("character");
                playerDir = mictlanDir.resolve("players");
                worldData = mictlanDir.resolve("world");
                mictlanCorePath = FabricLoader.getInstance().getGameDir().resolve("mictlan");
                mictlanCoreDataPath = mictlanCorePath.resolve("data");
                mictlanCoreEntitiesPath = mictlanCorePath.resolve("entities");

                Path[] dirsToCreate = {characterDir, playerDir, mictlanConfigDir, worldData, mictlanCorePath, mictlanCoreDataPath, mictlanCoreEntitiesPath};
                for (Path dir : dirsToCreate) {
                    try {
                        if (!Files.exists(dir)) {
                            Files.createDirectories(dir);
                            LOGGER.info("[Mictlan] Directorio creado: " + dir.toString());
                        }
                    } catch (IOException e) {
                        LOGGER.error("[Mictlan] No se pudo crear el directorio: " + dir.toString(), e);
                    }
                }

                /**
                 * -----------------------------------------------------------------
                 * PEGADO DE CHUNKS Y ENTIDADES GUARDADOS EN EL MUNDO NUEVO
                 * -----------------------------------------------------------------
                 * Si ya hay chunks copiados con /mictlan home, los leemos de
                 * <juego>/mictlan/data (y sus entidades de <juego>/mictlan/entities)
                 * y los escribimos en el mundo que se carga.
                 */
                // Sin archivo de configuracion no hay nada que pegar.
                if(!Files.exists(mictlanConfigFile)){
                    return;
                };
                try {
                    readMictlanConfigFile = Files.readString(mictlanConfigFile);
                } catch (Exception e) {
                    LOGGER.error("[Mictlan] Archivo de configuracion no pudo ser leido", e);
                }
                respuestaArchivoDeConfiguracion = gson.fromJson(readMictlanConfigFile, CurrentEra.getClass());
                pegadoDeChunksEnConfiguracion(world);
                // Primero el terreno, despues las entidades encima.
                pegadoDeEntidadesEnNuevoMundo(world);
            }
        });

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CONEXION DE JUGADORES
         * -------------------------------------------------------------------------
         */

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            playerHandler = handler.getPlayer();
            String playerName = playerHandler.getGameProfile().getName();
            playerUUID = playerHandler.getGameProfile().getId().toString();
            playerLocation = playerHandler.getPos().toString();

            playerData = new PlayerData(playerUUID);

            playerFilePath = Path.of(playerDir.toString(), playerUUID +".json");

            // Mensaje de prueba.
            Text playerLocationTestMessage = Text.literal("Tu ubicación actual es: " + playerLocation);
            playerHandler.sendMessage(playerLocationTestMessage, false);
            playerData.playerLocation(playerLocation);

            // Primera conexion.
            if (!Files.exists(playerFilePath)) {
                Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
                playerHandler.sendMessage(welcomeMessage, false);
                playerDataJsonReturn = playerData;
                playerDataJsonReturn.hasPlayedBefore(true);
                playerDataJsonReturn.playerLocation(playerLocation);
                entregaKitInicial(playerHandler);
                escribirDatosDelJugador();
            } else {
                // Jugador existente.
                leerDatosDelJugador(playerHandler);
                playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
                Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
                playerHandler.sendMessage(welcomeBackMessage, false);
                playerDataJsonReturn.hasPlayedBefore(true);
                boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
                playerDataJsonReturn.setEra(eraActual);
                // Reintento del kit (p. ej. inventario lleno la vez anterior).
                if(!hasReceivedStarterKit) {
                    entregaKitInicial(playerHandler);
                }
                escribirDatosDelJugador();
            };
            if (!Files.exists(mictlanConfigFile)) {
                escribirDatosDeConfiguracion();
            }
            Text eraMessage = Text.literal("Te encuentras en la era " + eraActual);
            playerHandler.sendMessage(eraMessage, false);

            LOGGER.info("[Mictlan] " + playerName + " se conecto.");
        });

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE DESCONEXION DE USUARIO
         * -------------------------------------------------------------------------
         */
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerHandler = handler.getPlayer();
            playerUUID = playerHandler.getGameProfile().getId().toString();
            playerFilePath = Path.of(playerDir.toString(), playerUUID +".json");
            leerDatosDelJugador(handler.getPlayer());
            // Actualiza la ultima posicion y guarda.
            playerLocation = playerHandler.getPos().toString();
            playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
            playerDataJsonReturn.playerLocation(playerLocation);
            escribirDatosDelJugador();
            String playerName = playerHandler.getGameProfile().getName();
            LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
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
                        characterFilePath = Path.of(characterDir.toString());
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ChunkPos chunk = player.getChunkPos();
                        commandExecuted++;
                        if(commandExecuted % 2 != 0) {
                            // Primera esquina: reinicia las listas en memoria.
                            homeChunksX.clear();
                            homeChunksZ.clear();
                            totalChunkPosCount.clear();
                            firstChunk = chunk;
                        } else {
                            // Segunda esquina.
                            secondChunk = chunk;
                            worldDataChunks = Path.of(worldData.toString(), eraActual + ".json");
                            CurrentEra.mictlanHomeChunks(secondChunk, firstChunk);
                            if(!Files.exists(worldDataChunks)) {
                                try {
                                    Files.writeString(worldDataChunks, gson.toJson(CurrentEra));
                                } catch (IOException e) {
                                    LOGGER.error("[Mictlan] Datos de los Chunks no pusieron ser escritos!");
                                }
                            }
                            // Se relee de disco para usar lo que realmente quedo guardado.
                            try {
                                worldChunkData = Files.readString(worldDataChunks);
                            } catch (Exception e) {
                                LOGGER.error("[Mictlan] Datos de los chunks no pueden ser leidos");
                            }
                            worldChunkDataReturn = gson.fromJson(worldChunkData, CurrentEra.getClass());
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
                            LOGGER.info("[Mictlan] " + totalChunkPosCount);
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
                                mictlanCoreDataFile = Path.of(mictlanCoreDataPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
                                mictlanCoreEntitiesFile = Path.of(mictlanCoreEntitiesPath.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
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
                            escribirDatosDeConfiguracion();
                        }
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
                                Files.delete(Path.of(worldData.toString(), eraActual + ".json"));
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
    /**
     * -------------------------------------------------------------------------
     * HELPERS
     * -------------------------------------------------------------------------
     */

    /** Da la pala de bienvenida; solo marca el kit si cupo en el inventario. No guarda en disco. */
    private void entregaKitInicial(ServerPlayerEntity jugador) {
        try{
            ItemStack WelcomeItem = new ItemStack(net.minecraft.item.Items.WOODEN_SHOVEL, 1);
            if(playerHandler.getInventory().insertStack(WelcomeItem)) {
                playerDataJsonReturn.hasReceivedStarterKit(true);
            }
        } catch (Exception e) {
            LOGGER.error("[Mictlan] No se pudo entregar el objeto de bienvenida al jugador.", e);
        }
    };

    /** Lee playerFilePath en playerDataJson. */
    private void leerDatosDelJugador(ServerPlayerEntity jugador) {
        try{
            playerDataJson = Files.readString(playerFilePath);
        } catch(Exception e) {
            LOGGER.error("[Mictlan] No se pudieron leer contenidos del archivo del jugador!");
        }

    };

    /** Escribe playerDataJsonReturn en playerFilePath (sobrescribe). */
    private void escribirDatosDelJugador() {
        try{
            Files.writeString(playerFilePath, gson.toJson(playerDataJsonReturn));
        } catch (Exception e) {
            LOGGER.error("[Mictlan] Datos no escritos a disco!");
        }
    };

    /** Escribe CurrentEra en config/mictlan/mictlan.json. */
    private void escribirDatosDeConfiguracion() {
        try{
            Files.writeString(mictlanConfigFile, gson.toJson(CurrentEra));
        } catch (IOException e) {
            LOGGER.error("[Mictlan] No se pudo escribir los datos del configuracion.", e);
        }
    };

    /** Pega los .nbt guardados en el Overworld y limpia la bandera de pegado pendiente. */
    private void pegadoDeChunksEnConfiguracion(ServerWorld mundo) {
        if(respuestaArchivoDeConfiguracion.getHomeChunksPasted()) {
                // LOAD corre una vez por dimension; solo Overworld.
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
                        LOGGER.error("[Mictlan] No se pudo escribir " + posicionChunksMundoNuevo + " en mundo nuevo!", e);
                }
                CurrentEra.homeChunkPasted(false);
                escribirDatosDeConfiguracion();
        }
    };

    /** Recrea en el mundo las entidades de cada .nbt de entities/ y borra el archivo. */
    private void pegadoDeEntidadesEnNuevoMundo(ServerWorld mundo) {
        int counter = 0;
        try (DirectoryStream<Path> oldWorldEntityResources = Files.newDirectoryStream(mictlanCoreEntitiesPath, "*.nbt")){
            for (Path archivo : oldWorldEntityResources) {
                NbtCompound datos = NbtIo.readCompressed(archivo.toFile());
                recibirEntidades = datos.getList("entidades", NbtElement.COMPOUND_TYPE);
                for(NbtList recoleccionDeEntidades = recibirEntidades; counter < recoleccionDeEntidades.size(); counter++) {
                    entidadGuardada = recoleccionDeEntidades.getCompound(counter);
                    // Crea la entidad con su UUID, posicion y datos originales.
                    pegarEntidadesEnMundo = EntityType.getEntityFromNbt(entidadGuardada, mundo);

                    if(pegarEntidadesEnMundo.isPresent()) {
                        LOGGER.info("[Mictlan] " + pegarEntidadesEnMundo.get());
                        mundo.spawnEntityAndPassengers(pegarEntidadesEnMundo.get());
                    }
                }
                Files.delete(archivo);
                counter = 0;
            }
        } catch (IOException e) {
            LOGGER.error("[Mictlan] Datos de entidades no pudieron ser escritas a mundo");
        }
    }
} 
