package dev.dario.mictlan.NPC;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.context.CommandContext;

import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
import de.markusbordihn.easynpc.api.handler.EasyNPCEntityHandler;
import de.markusbordihn.easynpc.data.action.ActionDataEntry;
import de.markusbordihn.easynpc.data.action.ActionDataType;
import de.markusbordihn.easynpc.data.action.MessageActionData;
import de.markusbordihn.easynpc.data.state.StateEntry;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.Helpers.Helpers;
import dev.dario.mictlan.Players.PlayerConnection;
import dev.dario.mictlan.Players.PlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ConfiguracionNPC {
    public static StateEntry saludoInicial;
    private static String saludoAJugadores = "";
    private Optional<EasyNPC<?>> getNPC;
    private boolean xolotlSeMudo;
    public static ConfiguracionNPC configuracionNPC = new ConfiguracionNPC(Optional.empty());
    public static String xolotlNPCIdentifier = "mictlan:xolotl";
    public static String identificadoPresetXolotl = "mictlan:easy_npc/preset/humanoid/xolotl.npc.snbt";
    private static String messageContent;
    private static Identifier rutaDialogos;
    private static Optional<Resource> rutaDialogosFile;
    private static AccionesXolotl dialogosNPC; 
    private static List<DialogosXolotl> dialogosXolotl;
    private static List<ActionDataEntry> mensajesXolotl = new ArrayList<>();
    private static Optional<EasyNPC<?>> xolotlPresence = Optional.empty();
    public static Optional<EasyNPC<?>> spawnXolotl = Optional.empty();
    private EasyNPC<?> xolotlHome;
    public static EasyNPC<?> npcInicial;
    private static boolean borrado;
    /**
    * -------------------------------------------------------------------------
    * SPAWNEO DE XOLOTL
    * -------------------------------------------------------------------------
    */

    public static Optional<EasyNPC<?>> xolotlPresence(ServerPlayer handler) {
        // Solo se crea si no hay ya un NPC con el identificador de Xolotl.
        if(EasyNPCEntityHandler.getByCustomIdentifier(Identifier.parse(ConfiguracionNPC.xolotlNPCIdentifier)).isEmpty()) {
            spawnXolotl = EasyNPCEntityHandler.spawnFromPreset(
                Identifier.parse(ConfiguracionNPC.identificadoPresetXolotl), 
                handler.level().getServer().overworld(), 
                Vec3.atBottomCenterOf(handler.getOnPos().offset(5, 5, 1)), 
                null, null
            );

            if(spawnXolotl.isPresent()) {
                xolotlPresence = spawnXolotl;
            }
        }
        return xolotlPresence;
    }

    /**
    * -------------------------------------------------------------------------
    * NOMBRES DE LOS JUGADORES CONECTADOS ("A y B y C")
    * -------------------------------------------------------------------------
    */

    private static void jugadoresSaludados(MinecraftServer mundo) {
        Set<String> jugadoresASaludar = PlayerConnection.connectedPlayers.keySet();
        for(String jugadores : jugadoresASaludar) {
            ServerPlayer jugadorConectado = mundo.getPlayerList().getPlayer(UUID.fromString(jugadores));
            saludoAJugadores += jugadorConectado.getName().getString() + " y ";
        }
        // Quita el ultimo " y ".
        if (saludoAJugadores.endsWith(" y ")) {
            saludoAJugadores = saludoAJugadores.substring(0, saludoAJugadores.length() - 3);
        }
    }

    /**
    * -------------------------------------------------------------------------
    * PRIMERA INTERACCION CON XOLOTL
    * -------------------------------------------------------------------------
    */

    /** Forma de dialogos/bienvenida_xolotl.json: { "xolotl": { "bienvenida": [...] } } */
    public class AccionesXolotl {
        private Xolotl xolotl;
    }
    public class Xolotl {
        private List<DialogosXolotl> bienvenida;
    }
    /** Un paso del dialogo. tipo: "decir", "esperar" o "comando". */
    public class DialogosXolotl {
        private String tipo;
        private String valor;
    }

    /** Arma la secuencia de acciones del saludo y la programa en el NPC. */
    public static void mensajesXolotl(EasyNPC<?> npc, List<DialogosXolotl> guardarMensajes, ServerPlayer jugador) {
        if(!(PlayerConnection.connectedPlayers.get(jugador.getStringUUID()).isHasReceivedStarterKit())) {
            npcInicial = npc;
            PlayerData playerData;
            Set<String> jugadoresDisponibles = PlayerConnection.connectedPlayers.keySet();
            jugadoresSaludados(jugador.level().getServer());
                messageContent = "Bienvenidos " + saludoAJugadores + "!";
                mensajesXolotl.add(
                    new ActionDataEntry(ActionDataType.MESSAGE, "")
                        .withMessageActionData(
                            MessageActionData.DEFAULT.withTexts(List.of(messageContent))
                        )
                    );
                for(Integer i = 0; i < guardarMensajes.size(); i++) {
                    if("decir".equals(guardarMensajes.get(i).tipo)) {
                        mensajesXolotl.add(
                            new ActionDataEntry(ActionDataType.MESSAGE, "")
                                .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of(
                                    guardarMensajes.get(i).valor
                                )))
                            );
                    } else if ("esperar".equals(guardarMensajes.get(i).tipo)){
                        mensajesXolotl.add(
                            new ActionDataEntry(
                                ActionDataType.WAIT, guardarMensajes.get(i).valor));
                    } else if ("comando".equals(guardarMensajes.get(i).tipo)){
                        mensajesXolotl.add(
                            new ActionDataEntry(
                                ActionDataType.COMMAND, guardarMensajes.get(i).valor
                            )
                        );
                    }
                }
                EasyNPCActionHandler.schedule(npc, Identifier.fromNamespaceAndPath("mictlan", "saludo"), 60, mensajesXolotl);
                // Todos los jugadores conectados durante el saludo cuentan como que ya recibieron el kit.
                for(String player : jugadoresDisponibles) {
                    playerData = PlayerConnection.connectedPlayers.get(player);
                    playerData.hasReceivedStarterKit(true);
                    MictlanMod.CurrentEra.haRecibidoSaludoInicial(true);
                    // Estado guardado en el propio NPC para no repetir el saludo.
                    EasyNPCActionHandler.setState(
                        npc, Identifier.fromNamespaceAndPath("mictlan", "saludo_inicial"), 
                        StateEntry.of(true), jugador
                    );
                    Helpers.escribirDatosDelJugador(jugador.getStringUUID());
                }
        } else {
            MictlanMod.LOGGER.info("[Mictlan] Kit y bienvenida ya fueron entregados.");
        }
    }

    /**
     * 
     * BORRAR NPC ACTUAL
     * 
     */

    public static boolean npcBorrado(Optional<EasyNPC<?>> npc) {
        if(npc != null) {
            borrado = EasyNPCEntityHandler.delete(npc.get());
            xolotlPresence = Optional.empty();
        } else {
            borrado = false;
        }
        return borrado;
    }

    /** Se llama al interactuar con Xolotl: lee los dialogos y saluda si aun no lo hizo. */
    public static void interaccionXolotl(EasyNPC<?> npc, ServerPlayer jugador) {
        rutaDialogos = Identifier.fromNamespaceAndPath("mictlan_medieval", "dialogos/bienvenida_xolotl.json");
        rutaDialogosFile = jugador.level().getServer().getResourceManager().getResource(rutaDialogos);
        try (BufferedReader leerDialogos = rutaDialogosFile.get().openAsReader()){
            dialogosNPC = MictlanMod.gson.fromJson(leerDialogos, AccionesXolotl.class);
            dialogosXolotl = dialogosNPC.xolotl.bienvenida;
        } catch(IOException e) {
            MictlanMod.LOGGER.error("[mictlan] Dialogos del NPC no pudieron ser leidos!", e);
        }
        saludoInicial = EasyNPCActionHandler.getState(npc, Identifier.fromNamespaceAndPath("mictlan", "saludo_inicial"));
        // null = el estado nunca se ha puesto, o sea, todavia no saluda.
        if(saludoInicial == null) {
           mensajesXolotl(npc, dialogosXolotl, jugador);
        }
    }

    /**
    * -------------------------------------------------------------------------
    * DATOS DEL NPC
    * -------------------------------------------------------------------------
    */

    public ConfiguracionNPC(Optional<EasyNPC<?>> npc) {
        this.getNPC = npc;
    }

    public void xolotlYaSeMudo(boolean xolotlSeMudo) {
        this.xolotlSeMudo = xolotlSeMudo;
    }

    public Optional<EasyNPC<?>> npc() {
        return this.getNPC;
    }

    public boolean isXolotlYaSeMudo() {
        return this.xolotlSeMudo;
    }

    /**
    * -------------------------------------------------------------------------
    * BUSCAR A XOLOTL Y MANDARLO A LA CASA (/mictlan home set)
    * -------------------------------------------------------------------------
    */

    public Optional<EasyNPC<?>> obtenerNPC(CommandContext<CommandSourceStack> context, ServerPlayer handler, Identifier npcAObtener) {
        xolotlHome = xolotlPresence(handler).get();
        if(!(xolotlHome == null)) {
            return xolotlPresence;
        } else {
            return null;
        }
    }

    /**
     * 
     * GETTERS PARA FACILITAR INTERACCION CON NPC
     * 
     */

    public static Optional<EasyNPC<?>> isPresentXolotl() {
        return xolotlPresence;
    }
}