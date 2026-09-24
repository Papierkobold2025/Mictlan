package dev.dario.mictlan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Set;
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

    /**
     * HashSet es un método simple para almacenar valores únicos.
     * No 
     */

    private final Set<String> playerUUIDMap = new HashSet<>();

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
        Path configDir = FabricLoader.getInstance().getConfigDir();
        final boolean playerDataExists = Files.exists(Path.of(configDir.toString(), "mictlan_player_data.txt"));

        // Leer el archivo de datos del jugador si existe, o crear uno nuevo si no existe
        try {
            if (playerDataExists) {
                List<String> playerUUIDList = Files.readAllLines(Path.of(configDir.toString(), "mictlan_player_data.txt"));
                playerUUIDMap.addAll(playerUUIDList);
            } else {
                Files.createFile(Path.of(configDir.toString(), "mictlan_player_data.txt"));
            }
        } catch (IOException e) {
            LOGGER.error("[Mictlan] Datos del jugador no disponibles.", e);
        };


        
        // Registrar eventos de conexión y desconexión de jugadores

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String playerName = handler.getPlayer().getGameProfile().getName();
            String playerUUID = handler.getPlayer().getGameProfile().getId().toString();
            String playerLocation = handler.getPlayer().getPos().toString();

            // Verificar si el jugador ya se ha conectado antes
            Text playerLocationTestMessage = Text.literal("Tu ubicación actual es: " + playerLocation);
            handler.getPlayer().sendMessage(playerLocationTestMessage, false);
            if (!playerUUIDMap.contains(playerUUID)) {
                playerUUIDMap.add(playerUUID);
                //Enviar mensaje de bienvenida al jugador, en caso de que no haya jugado antes
                Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
                handler.getPlayer().sendMessage(welcomeMessage, false);
                    // Guardar el UUID del jugador en el archivo de datos
                    try{
                        Files.writeString(Path.of(FabricLoader.getInstance().getConfigDir().toString(), "mictlan_player_data.txt"), playerUUID + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        LOGGER.error("[Mictlan] No se pudo escribir los datos del jugador.", e);
                    };
            } else {
                //Enviar mensaje de bienvenida al jugador, en caso de que ya haya jugado antes
                Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
                handler.getPlayer().sendMessage(welcomeBackMessage, false);
            }

            LOGGER.info("[Mictlan] " + playerName + " se conectó.");
        });

        // Registrar evento de desconexión de jugadores
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String playerName = handler.getPlayer().getGameProfile().getName();        
            LOGGER.info("[Mictlan] " + playerName + " se desconectó.");
        });
    };
}
