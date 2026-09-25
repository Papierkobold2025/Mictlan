package dev.dario.mictlan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import net.minecraft.util.WorldSavePath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Punto de entrada del mod. Fabric Loader instancia esta clase y llama a
 * onInitialize() una vez, durante la fase de carga del juego (antes de que
 * el mundo/servidor exista todavia).
 *
 * Deliberadamente vacio por ahora: el objetivo de este primer paso es
 * unicamente demostrar que el mod compila, se empaqueta y Fabric Loader
 * lo reconoce y lo carga — sin Mixins, sin dependencia de Nexus Characters,
 * sin ninguna logica propia todavia.
 */
public class MictlanMod implements ModInitializer {

    /** Debe coincidir exactamente con el "id" de fabric.mod.json. */
    public static final String MOD_ID = "mictlan";

    /**
     * Logger compartido para todo el mod. Usar siempre este en vez de
     * System.out — aparece en los logs del servidor con el prefijo del
     * mod, lo que facilita distinguir nuestros mensajes de los de
     * Minecraft o de otros mods.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    /** Directorio raiz donde se guardan todos los datos propios del mod. */
    private Path mictlanDir;

    /** Directorio reservado para los datos de los personajes. */
    private Path characterDir;

    /** Directorio donde se guarda un archivo JSON por cada jugador. */
    private Path playerDir;

    /** Directorio de configuracion del Mod Loader Fabric */ 
    private Path mictlanConfigDir;
    /** Conversor utilizado para serializar y leer los datos de los jugadores. */
    private Gson gson = new Gson();

    private String eraActual= "MEDIEVAL";

    WorldData CurrentEra = new WorldData(eraActual);

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
         * Crear directorios para almacenar datos de jugadores y personajes.
         * Se ejecuta al cargar un mundo porque en ese momento ya conocemos la
         * carpeta de guardado del servidor.
         */
        ServerWorldEvents.LOAD.register((server,world) -> {
            mictlanConfigDir = FabricLoader.getInstance().getConfigDir().resolve("mictlan");
            mictlanDir = server.getSavePath(WorldSavePath.ROOT).resolve("mictlan");
            characterDir = mictlanDir.resolve("character");
            playerDir = mictlanDir.resolve("players");
            // Mantener los directorios en una lista permite crearlos de forma uniforme.
            Path[] dirsToCreate = {characterDir, playerDir, mictlanConfigDir};
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
        });
        
        // Registrar eventos de conexión y desconexión de jugadores

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Obtener la identidad y la ubicación inicial del jugador que se conecta.
            String playerName = handler.getPlayer().getGameProfile().getName();
            String playerUUID = handler.getPlayer().getGameProfile().getId().toString();
            String playerLocation = handler.getPlayer().getPos().toString();

            // Crear una instancia de PlayerData para el jugador que se conecta
            PlayerData playerData =new PlayerData(playerUUID);

            Path playerFilePath = Path.of(playerDir.toString(), playerUUID +".json");
            Path mictlanConfigFile = Path.of(mictlanConfigDir.toString(), "mictlan.json" );

            // Mostrar la ubicación actual como dato de prueba durante esta etapa.
            Text playerLocationTestMessage = Text.literal("Tu ubicación actual es: " + playerLocation);
            handler.getPlayer().sendMessage(playerLocationTestMessage, false);
            if (!Files.exists(playerFilePath)) {
                // Enviar un mensaje de bienvenida al jugador que entra por primera vez.
                Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
                handler.getPlayer().sendMessage(welcomeMessage, false);

                // Escribir los datos iniciales del jugador en un archivo JSON.
                try{
                    Files.writeString(playerFilePath, gson.toJson(playerData));
                } catch (IOException e) {
                    LOGGER.error("[Mictlan] No se pudo escribir los datos del jugador.", e);
                };

                // Entregar el objeto inicial y actualizar el registro si se pudo guardar.
                try{
                    ItemStack WelcomeItem = new ItemStack(net.minecraft.item.Items.WOODEN_SHOVEL, 1);
                    if(handler.getPlayer().getInventory().insertStack(WelcomeItem)) {
                        playerData.hasReceivedStarterKit(true);
                        Files.writeString(playerFilePath, gson.toJson(playerData));
                    }
                } catch (Exception e) {
                    LOGGER.error("[Mictlan] No se pudo entregar el objeto de bienvenida al jugador.", e);
                }
            } else {
                // Enviar un mensaje distinto si el jugador ya tiene datos guardados.
                Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
                handler.getPlayer().sendMessage(welcomeBackMessage, false);
                playerData.hasPlayedBefore(true);
                String playerDataJson = "";
                try {
                    playerDataJson = Files.readString(playerDir.resolve(playerUUID +".json"));
                } catch (IOException e ) {
                    LOGGER.error("[Mictlan] No se pudo leer los datos del jugador.", e);
                };
                    
                // Leer el estado persistido para no entregar el kit de inicio dos veces.
                PlayerData playerDataJsonReturn = gson.fromJson(playerDataJson, playerData.getClass());
                boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
                playerDataJsonReturn.setEra(eraActual);
                try {
                    Files.writeString(playerFilePath, gson.toJson(playerDataJsonReturn));
                } catch (Exception e){
                    LOGGER.error("[Mictlan] No se pudo cargar la era actual al jugador!");
                }
                if(!hasReceivedStarterKit) {
                    // Entregar el objeto inicial pendiente y guardar el nuevo estado.
                    try{
                        ItemStack WelcomeItem = new ItemStack(net.minecraft.item.Items.WOODEN_SHOVEL, 1);
                        if(handler.getPlayer().getInventory().insertStack(WelcomeItem)) {
                            playerDataJsonReturn.hasReceivedStarterKit(true);
                            Files.writeString(playerFilePath, gson.toJson(playerDataJsonReturn));
                        }
                    } catch (Exception e) {
                        LOGGER.error("[Mictlan] No se pudo entregar el objeto de bienvenida al jugador.", e);
                    }
                }
            };
            if (!Files.exists(mictlanConfigFile)) {
                try{
                    Files.writeString(mictlanConfigFile, gson.toJson(CurrentEra));
                } catch (IOException e) {
                    LOGGER.error("[Mictlan] No se pudo escribir los datos del configuracion.", e);
                }
            }
            Text eraMessage = Text.literal("Te encuentras en la era " + eraActual);
            handler.getPlayer().sendMessage(eraMessage, false);

            LOGGER.info("[Mictlan] " + playerName + " se conecto.");
        });
        // Registrar evento de desconexión de jugadores
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String playerName = handler.getPlayer().getGameProfile().getName();        
            LOGGER.info("[Mictlan] " + playerName + " se desconecto.");
        });   

    };
}