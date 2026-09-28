// El "package" indica en que carpeta/grupo vive esta clase dentro del proyecto.
package dev.dario.mictlan;

// Los "import" traen clases de otras librerias (Fabric, Minecraft, Java)
// para poder usarlas en este archivo sin escribir su nombre completo.
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.ArrayList;
import java.nio.file.Files;
import java.io.IOException;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.util.Optional;

import com.google.gson.Gson;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Punto de entrada del mod. Fabric Loader instancia esta clase y llama a
 * onInitialize() una vez, durante la fase de carga del juego (antes de que
 * el mundo/servidor exista todavia).
 *
 * Aqui "registramos" (le decimos a Fabric) que codigo queremos ejecutar
 * cuando pasen ciertas cosas en el juego:
 *   - cuando se carga un mundo,
 *   - cuando un jugador entra o sale del servidor,
 *   - cuando alguien escribe el comando /mictlan.
 */
public class MictlanMod implements ModInitializer {

    /**
     * -------------------------------------------------------------------------
     * CONSTANTES DEL MOD
     * -------------------------------------------------------------------------
     */

    /** Debe coincidir exactamente con el "id" de fabric.mod.json. */
    public static final String MOD_ID = "mictlan";

    /**
     * Logger compartido para todo el mod. Usar siempre este en vez de
     * System.out — aparece en los logs del servidor con el prefijo del
     * mod, lo que facilita distinguir nuestros mensajes de los de
     * Minecraft o de otros mods.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Conversor utilizado para pasar objetos Java a texto JSON y viceversa. */
    private Gson gson = new Gson();

    /**
     * -------------------------------------------------------------------------
     * RUTAS DE CARPETAS Y ARCHIVOS
     * -------------------------------------------------------------------------
     * Todas se calculan cuando se carga el mundo (ver ServerWorldEvents.LOAD),
     * porque antes de eso todavia no sabemos donde esta guardado el mundo.
     */

    /** Directorio raiz donde se guardan todos los datos propios del mod: <mundo>/mictlan. */
    private Path mictlanDir;

    /** Directorio reservado para los datos de los personajes: mictlan/character. */
    private Path characterDir;

    /** Directorio donde se guarda un archivo JSON por cada jugador: mictlan/players. */
    private Path playerDir;

    /** Ruta completa al archivo JSON del jugador actual (por ejemplo: players/<uuid>.json). */
    private Path playerFilePath;

    /** Directorio de configuracion del mod dentro de la carpeta config de Fabric: config/mictlan. */
    private Path mictlanConfigDir;

    /** Directorio donde se guardan los datos del mundo (por ejemplo, las eras): mictlan/world. */
    private Path worldData;

    /** Ruta al archivo JSON donde se guardan los chunks de la era actual (por ejemplo: world/MEDIEVAL.json). */
    private Path worldDataChunks;

    /** Directorio donde se guarda una copia .nbt de cada chunk de la zona "home": mictlan/world/chunks. */
    private Path worldDataResources;

    /**
     * -------------------------------------------------------------------------
     * DATOS DE LA ERA
     * -------------------------------------------------------------------------
     */

    /**
     * Nombre de la era en la que se encuentra el mundo ahora mismo.
     * OJO: debe declararse ANTES de CurrentEra, porque Java inicializa los
     * campos en el orden en que aparecen y CurrentEra lo necesita.
     */
    private String eraActual= "MEDIEVAL";

    /** Objeto con los datos de la era actual (nombre de la era, chunks, etc.). */
    WorldData CurrentEra = new WorldData(eraActual);

    /** Reservado para guardar los chunks de la era actual (todavia no se usa). */
    private String currentEraChunks;

    /**
     * -------------------------------------------------------------------------
     * DATOS DEL JUGADOR
     * -------------------------------------------------------------------------
     */

    /** El jugador con el que estamos trabajando en este momento (el que entro, salio, etc.). */
    private ServerPlayerEntity playerHandler;

    /** Identificador unico (UUID) del jugador. No cambia aunque cambie su nombre. */
    private String playerUUID;

    /** Posicion del jugador (x, y, z) guardada como texto. */
    private String playerLocation;

    /** Datos nuevos del jugador que acaba de conectarse. */
    PlayerData playerData;

    /** Texto JSON leido del archivo del jugador (empieza vacio). */
    private String playerDataJson = "";

    /** Datos del jugador que vamos a escribir en (o que leimos de) su archivo JSON. */
    private PlayerData playerDataJsonReturn;

    /**
     * -------------------------------------------------------------------------
     * ZONA "HOME" Y TRANSPORTE DE CHUNKS
     * -------------------------------------------------------------------------
     */

    /** Cuenta cuantas veces se ha usado el comando /mictlan home. */
    private int commandExecuted = 0;

    /** Primer chunk marcado con /mictlan home (una esquina de la zona). */
    private ChunkPos firstChunk;

    /** Segundo chunk marcado con /mictlan home (la otra esquina de la zona). */
    private ChunkPos secondChunk;

    /** Texto JSON leido del archivo de chunks de la era (por ejemplo: world/MEDIEVAL.json). */
    private String worldChunkData = "";

    /** Datos de la era reconstruidos a partir de ese JSON (incluye las dos esquinas del "home"). */
    private WorldData worldChunkDataReturn;

    /** Mundo (dimension) del que se van a copiar los chunks de la zona "home". */
    private ServerWorld mundoParaTransportar;

    /**
     * Todas las coordenadas X de chunk que hay entre las dos esquinas del "home".
     * Ejemplo: si las esquinas tienen x = 2 y x = 5, la lista sera [2, 3, 4, 5].
     */
    private ArrayList<Integer> homeChunksX = new ArrayList<>();

    /** Igual que homeChunksX, pero para las coordenadas Z. */
    private ArrayList<Integer> homeChunksZ = new ArrayList<>();

    /**
     * Lista final con TODOS los chunks que forman la zona "home": cada
     * combinacion posible de una X de homeChunksX con una Z de homeChunksZ.
     * Una zona de 4 chunks de ancho por 3 de largo da 4 * 3 = 12 chunks.
     */
    private ArrayList<ChunkPos> totalChunkPosCount = new ArrayList<>();

    /**
     * -------------------------------------------------------------------------
     * INICIALIZACION DEL MOD
     * -------------------------------------------------------------------------
     */

    /**
     * Fabric Loader llama a este metodo una vez, durante la fase de carga del
     * juego (antes de que el mundo/servidor exista todavia).
     */
    @Override
    public void onInitialize() {

        // ModInitializer.onInitialize() es una API real de Fabric Loader
        // (net.fabricmc.api.ModInitializer), no client-side: corre tanto
        // en servidor dedicado como en cliente/integrated server.
        LOGGER.info("[Mictlan] Inicializado correctamente. Sin Mixins, sin Nexus todavia.");

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CARGA DEL MUNDO
         * -------------------------------------------------------------------------
         */
        // Este codigo se ejecuta cada vez que el servidor carga un mundo.
        ServerWorldEvents.LOAD.register((server,world) -> {
            // Calculamos las rutas de las carpetas que usara el mod.
            // "resolve" une una ruta con un nombre de carpeta: .../mictlan
            mictlanConfigDir = FabricLoader.getInstance().getConfigDir().resolve("mictlan");
            mictlanDir = server.getSavePath(WorldSavePath.ROOT).resolve("mictlan");
            characterDir = mictlanDir.resolve("character");
            playerDir = mictlanDir.resolve("players");
            worldData = mictlanDir.resolve("world");
            worldDataResources = worldData.resolve("chunks");

            // Mantener los directorios en una lista permite crearlos de forma uniforme.
            Path[] dirsToCreate = {characterDir, playerDir, mictlanConfigDir, worldData, worldDataResources};
            // Recorremos la lista, una carpeta a la vez.
            for (Path dir : dirsToCreate) {
                // "try/catch" sirve para atrapar errores (por ejemplo, falta de permisos)
                // sin que el servidor se caiga.
                try {
                    // Solo creamos la carpeta si todavia no existe.
                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                        LOGGER.info("[Mictlan] Directorio creado: " + dir.toString());
                    }
                } catch (IOException e) {
                    LOGGER.error("[Mictlan] No se pudo crear el directorio: " + dir.toString(), e);
                }
            }
        });
        
        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CONEXION DE JUGADORES
         * -------------------------------------------------------------------------
         */

        // Este codigo se ejecuta cada vez que un jugador entra al servidor.
        // "handler" nos da acceso al jugador que acaba de entrar.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Obtener la identidad y la ubicación inicial del jugador que se conecta.
            playerHandler = handler.getPlayer();
            String playerName = playerHandler.getGameProfile().getName();
            playerUUID = playerHandler.getGameProfile().getId().toString();
            playerLocation = playerHandler.getPos().toString();

            // Crear una instancia de PlayerData para el jugador que se conecta
            playerData = new PlayerData(playerUUID);

            // Ruta al archivo del jugador (players/<uuid>.json) y al archivo de configuracion.
            playerFilePath = Path.of(playerDir.toString(), playerUUID +".json");
            Path mictlanConfigFile = Path.of(mictlanConfigDir.toString(), "mictlan.json" );

            // Mostrar la ubicación actual como dato de prueba durante esta etapa.
            Text playerLocationTestMessage = Text.literal("Tu ubicación actual es: " + playerLocation);
            playerHandler.sendMessage(playerLocationTestMessage, false);
            // Guardamos la ubicacion dentro de los datos del jugador.
            playerData.playerLocation(playerLocation);

            // Si el archivo del jugador NO existe, es la primera vez que entra.
            if (!Files.exists(playerFilePath)) {
                // Enviar un mensaje de bienvenida al jugador que entra por primera vez.
                Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
                playerHandler.sendMessage(welcomeMessage, false);
                // Marcamos que ya jugo y guardamos su ubicacion.
                playerDataJsonReturn = playerData;
                playerDataJsonReturn.hasPlayedBefore(true);
                playerDataJsonReturn.playerLocation(playerLocation);
                // Entregar el objeto inicial y actualizar el registro si se pudo guardar.  
                entregaKitInicial(playerHandler);
                escribirDatosDelJugador();
            } else {
                // Si el archivo SI existe, el jugador ya habia entrado antes.
                // Enviar un mensaje distinto si el jugador ya tiene datos guardados.
                leerDatosDelJugador(playerHandler);
                // Convertimos ese texto JSON de vuelta a un objeto PlayerData.
                playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
                Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
                playerHandler.sendMessage(welcomeBackMessage, false);
                playerDataJsonReturn.hasPlayedBefore(true);
                // Revisamos si ya recibio el regalo de bienvenida en otra ocasion.
                boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
                // Guardamos en sus datos la era actual.
                playerDataJsonReturn.setEra(eraActual);
                // Si todavia no recibio el regalo (por ejemplo, tenia el inventario lleno), se lo damos ahora.
                if(!hasReceivedStarterKit) {
                    // Entregar el objeto inicial pendiente y guardar el nuevo estado.
                    entregaKitInicial(playerHandler);
                }
                // Guardamos los datos actualizados del jugador en su archivo.
                escribirDatosDelJugador();
            };
            // Si todavia no existe el archivo de configuracion, lo creamos con la era actual.
            if (!Files.exists(mictlanConfigFile)) {
                try{
                    Files.writeString(mictlanConfigFile, gson.toJson(CurrentEra));
                } catch (IOException e) {
                    LOGGER.error("[Mictlan] No se pudo escribir los datos del configuracion.", e);
                }
            }
            // Le decimos al jugador en que era esta.
            Text eraMessage = Text.literal("Te encuentras en la era " + eraActual);
            playerHandler.sendMessage(eraMessage, false);

            // Mensaje en la consola del servidor (el jugador no lo ve).
            LOGGER.info("[Mictlan] " + playerName + " se conecto.");
        });
        
        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE DESCONEXION DE USUARIO
         * -------------------------------------------------------------------------
         */
        // Este codigo se ejecuta cada vez que un jugador sale del servidor.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Buscamos el archivo del jugador usando su UUID.
            playerHandler = handler.getPlayer();
            playerUUID = playerHandler.getGameProfile().getId().toString();
            playerFilePath = Path.of(playerDir.toString(), playerUUID +".json");
            // Leemos lo que ya teniamos guardado de este jugador.
            leerDatosDelJugador(handler.getPlayer());
            // Tomamos la posicion donde el jugador se desconecto
            // y la guardamos en sus datos.
            playerLocation = playerHandler.getPos().toString();
            playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
            playerDataJsonReturn.playerLocation(playerLocation);
            // Escribimos los datos actualizados de vuelta en el archivo.
            escribirDatosDelJugador();
            String playerName = playerHandler.getGameProfile().getName();        
            LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
        }); 

        /**
         * -------------------------------------------------------------------------
         * REGISTRO DE COMANDOS
         * -------------------------------------------------------------------------
         */

        // Aqui creamos nuestros propios comandos de chat.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Comando principal: /mictlan
            dispatcher.register(CommandManager.literal("mictlan")
                /**
                 * -----------------------------------------------------------------
                 * /mictlan era
                 * -----------------------------------------------------------------
                 */
                // Subcomando /mictlan era -> muestra la era actual.
                .then(CommandManager.literal("era")
                    .executes(context-> {
                        context.getSource().sendFeedback(() -> Text.literal("Te encuentras en la era " + eraActual), false);
                        // Devolver 1 significa "el comando funciono".
                        return 1;
                    })
                )
                /**
                 * -----------------------------------------------------------------
                 * /mictlan home
                 * -----------------------------------------------------------------
                 */
                // Subcomando /mictlan home -> marca dos chunks (las esquinas de una zona).
                // La 1a vez guarda el primer chunk, la 2a vez guarda el segundo, y asi.
                .then(CommandManager.literal("home")
                    .executes(context ->{
                        // Apunta a la carpeta de personajes (de momento no se usa mas abajo).
                        // OJO: esto sobreescribe la ruta del archivo del jugador guardada al conectarse.
                        playerFilePath = Path.of(characterDir.toString());
                        // Obtenemos el jugador que escribio el comando y el chunk donde esta parado.
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ChunkPos chunk = player.getChunkPos();
                        // Sumamos 1 al contador de veces que se uso el comando.
                        commandExecuted++;
                        // "% 2 != 0" significa "es un numero impar" (1a, 3a, 5a vez...).
                        if(commandExecuted % 2 != 0) {
                            // Empieza una zona nueva: vaciamos las listas de la zona anterior
                            // para que no se mezclen chunks viejos con los nuevos.
                            // (Esto solo limpia la memoria; el archivo de la era en disco
                            // se sigue borrando con /mictlan home clear.)
                            homeChunksX.clear();
                            homeChunksZ.clear();
                            totalChunkPosCount.clear();
                            firstChunk = chunk;
                        } else {
                            // Numero par (2a, 4a vez...): guardamos el segundo chunk.
                            secondChunk = chunk;
                            // Archivo donde se guardan los chunks de esta era (por ejemplo: MEDIEVAL.json).
                            worldDataChunks = Path.of(worldData.toString(), eraActual + ".json");
                            // Guardamos los dos chunks dentro de los datos de la era.
                            CurrentEra.mictlanHomeChunks(secondChunk, firstChunk);
                            // Solo escribimos el archivo si todavia no existe.
                            if(!Files.exists(worldDataChunks)) {
                                try {
                                    Files.writeString(worldDataChunks, gson.toJson(CurrentEra));
                                } catch (IOException e) {
                                    LOGGER.error("[Mictlan] Datos de los Chunks no pusieron ser escritos!");
                                }
                            }
                            // Volvemos a leer el archivo de la era desde el disco.
                            // Asi trabajamos con lo que realmente quedo guardado.
                            try {
                                worldChunkData = Files.readString(worldDataChunks);
                            } catch (Exception e) {
                                LOGGER.error("[Mictlan] Datos de los chunks no pueden ser leidos");
                            }
                            // Convertimos el texto JSON de vuelta a un objeto WorldData.
                            worldChunkDataReturn = gson.fromJson(worldChunkData, CurrentEra.getClass());
                            // Sacamos las dos esquinas de la zona "home".
                            ChunkPos homeStartPos = worldChunkDataReturn.getHomeStart();
                            ChunkPos homeFinishPos = worldChunkDataReturn.getHomeFinish();

                            // --- Paso 1: todas las X entre las dos esquinas ---
                            // Usamos min/max porque el jugador pudo marcar las esquinas
                            // en cualquier orden; asi siempre contamos de menor a mayor.
                            int lowChunkCount = Math.min(homeStartPos.x, homeFinishPos.x);
                            int highChunkCount = Math.max(homeStartPos.x, homeFinishPos.x);
                            // "<=" incluye tambien el ultimo valor (la esquina misma).
                            for(int chunksStart = lowChunkCount; chunksStart <= highChunkCount; chunksStart++) {
                                homeChunksX.add(chunksStart);
                            }

                            // --- Paso 2: lo mismo, pero para las Z ---
                            // Reutilizamos las mismas variables low/high.
                            lowChunkCount = Math.min(homeStartPos.z, homeFinishPos.z);
                            highChunkCount = Math.max(homeStartPos.z, homeFinishPos.z);
                            for(int chunksFinish = lowChunkCount; chunksFinish <= highChunkCount; chunksFinish++) {
                                homeChunksZ.add(chunksFinish);
                            }

                            // --- Paso 3: combinar cada X con cada Z ---
                            // Un bucle dentro de otro recorre la zona como una cuadricula:
                            // por cada columna X, pasamos por todas las filas Z.
                            // Cada par (x, z) es un chunk de la zona "home".
                            for( int chunkCounterX = 0; chunkCounterX < homeChunksX.size(); chunkCounterX++) {
                                for(int chunkCounterZ = 0; chunkCounterZ < homeChunksZ.size(); chunkCounterZ++) {
                                    ChunkPos chunkAggregation = new ChunkPos (homeChunksX.get(chunkCounterX), homeChunksZ.get(chunkCounterZ));
                                    totalChunkPosCount.add(chunkAggregation);
                                }
                            }
                            // Mostramos en la consola la lista completa de chunks (para depurar).
                            LOGGER.info("[Mictlan] " + totalChunkPosCount);
                            context.getSource().sendFeedback(() -> Text.literal("Tu casa se ha guardado exitosamente"), false);

                            /**
                             * -----------------------------------------------------------------
                             * COPIA DE LOS CHUNKS DEL "HOME" A DISCO
                             * -----------------------------------------------------------------
                             * Por cada chunk de la zona pedimos a Minecraft sus datos en
                             * formato NBT (el mismo formato en el que el juego guarda el
                             * mundo) y los escribimos en un archivo propio dentro de
                             * mictlan/world/chunks, para poder llevarlos a otra era despues.
                             */
                            for(ChunkPos chunks : totalChunkPosCount) {
                                // Un archivo por chunk, por ejemplo: chunks/Chunk_3 -7.nbt
                                Path chunkTransportDir = Path.of(worldDataResources.toString(), "Chunk_" + chunks.x + " " + chunks.z + ".nbt");
                                // El mundo (dimension) donde estaba el jugador al usar el comando.
                                mundoParaTransportar = context.getSource().getWorld();
                                // threadedAnvilChunkStorage es la parte del servidor que lee y
                                // escribe los chunks en los archivos .mca de la region.
                                // getNbt() trabaja en otro hilo y devuelve una "promesa";
                                // join() espera aqui hasta que el resultado este listo.
                                // Devuelve un Optional: puede venir vacio si ese chunk
                                // todavia no se ha guardado nunca en disco.
                                Optional<NbtCompound> datosChunk = mundoParaTransportar.getChunkManager().threadedAnvilChunkStorage.getNbt(chunks).join();
                                // Solo escribimos el archivo si realmente hay datos.
                                if(datosChunk.isPresent()) {
                                    // Mostramos en consola las "llaves" del NBT (sections, block_entities, etc.) para depurar.
                                    LOGGER.info("[Mictlan] " + datosChunk.get().getKeys());
                                    try{
                                        // Guardamos el NBT comprimido (igual que lo hace Minecraft).
                                        NbtIo.writeCompressed(datosChunk.get(), chunkTransportDir.toFile());
                                    } catch(IOException e) {
                                        LOGGER.error("[Mictlan] Datos de Chunk no pudieron ser guardados en disco!");
                                    }
                                }
                            }
                        }
                        return 1;
                    })
                )
                /**
                 * -----------------------------------------------------------------
                 * /mictlan home clear
                 * -----------------------------------------------------------------
                 */
                // Subcomando /mictlan home clear -> borra la zona "home" guardada.
                // Aunque "home" aparece dos veces, Minecraft junta ambas ramas en un
                // solo comando: "/mictlan home" y "/mictlan home clear" funcionan las dos.
                .then(CommandManager.literal("home")
                    .then(CommandManager.literal("clear")
                        .executes(context -> {
                            // Vaciamos las listas en memoria para que el proximo
                            // /mictlan home empiece desde cero y no repita chunks.
                            homeChunksX.clear();
                            homeChunksZ.clear();
                            totalChunkPosCount.clear();
                            // Reiniciamos el contador para que el siguiente /mictlan home
                            // vuelva a ser la PRIMERA esquina (y no la segunda).
                            commandExecuted = 0;
                            // Borramos tambien el archivo de la era (por ejemplo: MEDIEVAL.json)
                            // para que la nueva zona si se pueda escribir en disco.
                            try {
                                Files.delete(Path.of(worldData.toString(), eraActual + ".json"));
                            } catch (Exception e) {
                                // Por ejemplo, si el archivo no existia todavia.
                                LOGGER.error("[Mictlan] Datos de los chunks no pueden ser borrados");
                            }
                            // Le confirmamos al jugador que se borraron los datos.
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

    /**
     * Intenta darle al jugador el regalo de bienvenida (una pala de madera).
     * Si cabe en el inventario, lo marca en playerDataJsonReturn como recibido;
     * si no cabe, no se marca y se volvera a intentar la proxima vez que entre.
     * Este metodo NO guarda en disco: despues hay que llamar a escribirDatosDelJugador().
     */
    private void entregaKitInicial(ServerPlayerEntity jugador) {
        try{
            // Creamos el objeto: 1 pala de madera.
            ItemStack WelcomeItem = new ItemStack(net.minecraft.item.Items.WOODEN_SHOVEL, 1);
            // insertStack devuelve true si se pudo meter en el inventario.
            if(playerHandler.getInventory().insertStack(WelcomeItem)) {
                playerDataJsonReturn.hasReceivedStarterKit(true);
            }
        } catch (Exception e) {
            LOGGER.error("[Mictlan] No se pudo entregar el objeto de bienvenida al jugador.", e);
        }
    };

    /**
     * Lee el archivo JSON del jugador (playerFilePath) y deja su contenido
     * como texto en playerDataJson. Quien lo llame se encarga de convertir
     * ese texto a un objeto PlayerData con gson.
     */
    private void leerDatosDelJugador(ServerPlayerEntity jugador) {
        try{
            // Leemos el texto JSON guardado en su archivo.
            playerDataJson = Files.readString(playerFilePath);
        } catch(Exception e) {
            LOGGER.error("[Mictlan] No se pudieron leer contenidos del archivo del jugador!");
        }

    };

    /**
     * Convierte playerDataJsonReturn a JSON y lo escribe en playerFilePath.
     * Si el archivo ya existe, lo reemplaza por completo.
     */
    private void escribirDatosDelJugador() {
        try{
            // gson.toJson convierte el objeto a texto JSON; writeString lo guarda en el archivo.
            Files.writeString(playerFilePath, gson.toJson(playerDataJsonReturn));
        } catch (Exception e) {
            LOGGER.error("[Mictlan] Datos no escritos a disco!");
        }
    }
}