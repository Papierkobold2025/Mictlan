package dev.dario.mictlan.Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;

import com.google.gson.Gson;

import de.markusbordihn.easynpc.api.action.ActionRegistry;
import dev.dario.mictlan.Facciones.FaccionAquelarre;
import dev.dario.mictlan.Facciones.FaccionMictlan;
import dev.dario.mictlan.Facciones.FaccionPueblos;
import dev.dario.mictlan.Facciones.Facciones;
import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.Helpers.HelpersComandos;
import dev.dario.mictlan.NPC.ConfiguracionNPC;
import dev.dario.mictlan.Players.PlayerConnection;
import dev.dario.mictlan.Players.PlayerData;
import dev.dario.mictlan.Players.PlayerDisconnection;
import dev.dario.mictlan.World.WorldData;
import dev.dario.mictlan.World.WorldLoad;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;


/**
 * Punto de entrada del mod. Registra eventos de mundo, conexion de
 * jugadores y el comando /mictlan.
 */

@Mod(MictlanMod.MOD_ID)
public class MictlanMod {

    /**
     * -------------------------------------------------------------------------
     * CONSTANTES DEL MOD
     * -------------------------------------------------------------------------
     */

    public static final String MOD_ID = "mictlan";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Gson gson = new Gson();

    public static int commandExecuted = 0;

    List<String> listaFacciones = new ArrayList<>(List.of("Los Pueblos", "El Aquelarre", "Mictlan"));

    /**
     * -------------------------------------------------------------------------
     * DATOS DE LA ERA
     * -------------------------------------------------------------------------
     */

    public static String eraActual= "MEDIEVAL";

    public static WorldData CurrentEra = new WorldData(eraActual);

    PlayerData playerData;

    private Facciones facciones = new Facciones();

    private FaccionPueblos faccionPueblos = new FaccionPueblos();
    
    private FaccionAquelarre faccionAquelarre = new FaccionAquelarre();

    private FaccionMictlan faccionMictlan = new FaccionMictlan();

    private Integer reputacionFaccion = 0;

    /**
     * -------------------------------------------------------------------------
     * INICIALIZACION DEL MOD
     * -------------------------------------------------------------------------
     */

    public MictlanMod() {

        MinecraftForge.EVENT_BUS.addListener(this::onPlayerDisconnect);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityKilled);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerConnect);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onVillagerTrade);

        LOGGER.info("[Mictlan] Inicializado correctamente. Sin Mixins, sin Nexus todavia.");

        ActionRegistry.register(new ResourceLocation(ConfiguracionNPC.xolotlNPCIdentifier), (actionDataEntry, easyNPC, serverPlayer, arguments) ->{    
            ConfiguracionNPC.interaccionXolotl(easyNPC, serverPlayer);
        });
    }

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CARGA DEL MUNDO
         * -------------------------------------------------------------------------
         */

        private void onWorldLoad(LevelEvent.Load event)  {
            if(event.getLevel() instanceof ServerLevel world) {
                MinecraftServer server = world.getServer();
                WorldLoad.worldLoad(server,world,CurrentEra);
            }
        }

        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE CONEXION DE JUGADORES
         * -------------------------------------------------------------------------
         */

        private void onPlayerConnect(PlayerEvent.PlayerLoggedInEvent event) {
            if(event.getEntity() instanceof ServerPlayer handler){
                PlayerConnection.playerConnection(handler);
            }
        }

        /**
         * -------------------------------------------------------------------------
         * PRIMERA INTERACCION CON NPC
         * -------------------------------------------------------------------------
         */



        /**
         * -------------------------------------------------------------------------
         * REGISTROS DURANTE DESCONEXION DE USUARIO
         * -------------------------------------------------------------------------
         */
        
        private void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event){
            PlayerDisconnection.playerDisconnection(event);
        }

        /**
         * -------------------------------------------------------------------------
         * REGISTROS CUANDO EL JUGADOR MATA ALGO
         * -------------------------------------------------------------------------
         */

        private void onEntityKilled(LivingDeathEvent event) {
            Entity killer = event.getSource().getEntity();
            if(killer instanceof ServerPlayer jugador) {
                LivingEntity victim = event.getEntity();
                MinecraftServer server = jugador.getServer();
                facciones.configuracionFacciones(server, jugador, victim);
            }
        }

        /**
         * -------------------------------------------------------------------------
         * LOGICA DE TRADEO CON ALDEANOS SEGUN REPUTACION
         * -------------------------------------------------------------------------
         */

        private void onVillagerTrade(TradeWithVillagerEvent event) {
            HashMap<String, Integer> reputacionDeJugador = new HashMap<>();
            playerData = PlayerConnection.connectedPlayers.get(event.getEntity().getStringUUID());
            HashMap<String, Integer> reputacion = playerData.isPlayerReputation();
            if(event.getEntity() instanceof ServerPlayer jugador) {
                if(reputacion.containsKey("Los Pueblos")) {
                    faccionPueblos.cambioReputacion(jugador, reputacion);
                } else {
                    reputacionDeJugador.put("Los Pueblos", 0);
                    faccionPueblos.cambioReputacion(jugador, reputacionDeJugador);
                }
            }
        }

        /**
         * -------------------------------------------------------------------------
         * REGISTRO DE COMANDOS
         * -------------------------------------------------------------------------
         */

        private void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("mictlan")

                /**
                * -----------------------------------------------------------------
                * /mictlan era
                * -----------------------------------------------------------------
                */

                .then(Commands.literal("era")
                    .executes(context-> {
                        context.getSource().sendSuccess( () -> Component.literal("Te encuentras en la era " + eraActual),
                            false
                        );
                        return 1;
                    })
                )

                /**
                * -----------------------------------------------------------------
                * /mictlan home set
                * -----------------------------------------------------------------
                */                

                .then(Commands.literal("home")
                    .then(Commands.literal("set")
                        .executes(context -> {
                            HelpersComandos helpersComandos = new HelpersComandos();
                            helpersComandos.marcarNuevaUbicacionNPC(context, new ResourceLocation(ConfiguracionNPC.xolotlNPCIdentifier));
                            return 1;
                        })
                    )
                )

                /**
                * -----------------------------------------------------------------
                * /mictlan home
                * -----------------------------------------------------------------
                */

                .then(Commands.literal("home")
                    .executes(context ->{
                        commandExecuted++;
                        if(commandExecuted % 2 != 0) {
                            HelpersComandos.primeraEsquina(context);
                        } else {
                            HelpersComandos.segundaEsquina(context);
                            LOGGER.info("[Mictlan] " + HelpersComandos.totalChunkPosCount);
                            context.getSource().sendSuccess(() -> Component.literal("Tu casa se ha guardado exitosamente"), false);
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

                .then(Commands.literal("home")
                    .then(Commands.literal("clear")
                        .executes(context -> {
                            HelpersComandos.borrarDatosCargados();
                            context.getSource().sendSuccess(() -> Component.literal("Los Chunks guardados han sido borrados exitosamente"), false);

                            return 1;
                        })
                    )
                )

                /**
                 * -----------------------------------------------------------------
                 * /mictlan faccion
                 * -----------------------------------------------------------------
                 */
                          
                .then(Commands.literal("faccion")
                    .then(Commands.literal("Pueblos")
                        .executes(context -> {
                            reputacionFaccion = HelpersComandos.leerReputacionPorFaccion(context, "Los Pueblos");
                            context.getSource().sendSuccess(() -> Component.literal("Tu reputacion con la faccion de los pueblos es: " + reputacionFaccion), false);
                            return 1;
                        })
                    )
                    .then(Commands.literal("Aquelarre")
                        .executes(context -> {
                            reputacionFaccion = HelpersComandos.leerReputacionPorFaccion(context, "El Aquelarre");
                            context.getSource().sendSuccess(() -> Component.literal("Tu reputacion con la faccion del aquelarre es: " + reputacionFaccion), false);
                            return 1;
                        })
                    )
                    .then(Commands.literal("Mictlan")
                        .executes(context -> {
                            reputacionFaccion = HelpersComandos.leerReputacionPorFaccion(context, "Mictlan");
                            context.getSource().sendSuccess(() -> Component.literal("Tu reputacion en Mictlan es: " + reputacionFaccion), false);
                            return 1;
                        })
                    )
                )
                
            );
        }
} 
