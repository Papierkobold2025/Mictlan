package dev.dario.mictlan;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import com.google.gson.Gson;

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

    public static final String MOD_ID = "mictlan";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Gson gson = new Gson();

    public static int commandExecuted = 0;

    /**
     * -------------------------------------------------------------------------
     * DATOS DE LA ERA
     * -------------------------------------------------------------------------
     */

    public static String eraActual= "MEDIEVAL";

    public static WorldData CurrentEra = new WorldData(eraActual);

    PlayerData playerData;

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

                .then(CommandManager.literal("home")
                    .executes(context ->{
                        commandExecuted++;
                        if(commandExecuted % 2 != 0) {
                            HelpersComandos.primeraEsquina(context);
                        } else {
                            HelpersComandos.segundaEsquina(context);
                            LOGGER.info("[Mictlan] " + HelpersComandos.totalChunkPosCount);
                            context.getSource().sendFeedback(() -> Text.literal("Tu casa se ha guardado exitosamente"), false);
                            for(ChunkPos chunks : HelpersComandos.totalChunkPosCount){
                                HelpersComandos.guardarCasaEnDisco(context, chunks);
                                CurrentEra.homeChunkPasted(true);
                                Helpers.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, MictlanMod.CurrentEra);
                                HelpersComandos.guardarEntidadesEnDisco(chunks);
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

                .then(CommandManager.literal("home")
                    .then(CommandManager.literal("clear")
                        .executes(context -> {
                            HelpersComandos.borrarDatosCargados();
                            context.getSource().sendFeedback(() -> Text.literal("Los Chunks guardados han sido borrados exitosamente"), false);

                            return 1;
                        })
                    )
                )
            );
        });
    };
} 
