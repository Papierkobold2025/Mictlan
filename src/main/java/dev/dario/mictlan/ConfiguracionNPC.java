package dev.dario.mictlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
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
import de.markusbordihn.easynpc.data.npc.SavedNPCEntityEntry;
import de.markusbordihn.easynpc.data.state.StateEntry;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ConfiguracionNPC {
    public static StateEntry saludoInicial;
    private static String saludoAJugadores = "";
    private Optional<EasyNPC<?>> getNPC;
    private UUID uuidNPC;
    private boolean xolotlSeMudo;
    public static ConfiguracionNPC configuracionNPC = new ConfiguracionNPC(Optional.empty());
    public static String xolotlNPCIdentifier = "mictlan:xolotl";
    public static String identificadoPresetXolotl = "mictlan:easy_npc/preset/humanoid/xolotl.npc.snbt";
    private static String messageContent;
    private static Identifier rutaDialogos;
    private static Optional<Resource> rutaDialogosFile;
    private static AccionesXolotl dialogosNPC; 
    private static List<DialogosXolotl> dialogosXolotl;
    private static List<ActionDataEntry> mensajesXolotl;

    /**
    * -------------------------------------------------------------------------
    * NOMBRES DE LOS JUGADORES CONECTADOS ("A y B y C")
    * -------------------------------------------------------------------------
    */

    private static void jugadoresSaludados(MinecraftServer mundo) {
        Set<String> jugadoresASaludar = PlayerConnection.connectedPlayers.keySet();
        for(String jugadores : jugadoresASaludar) {
            ServerPlayerEntity jugadorConectado = mundo.getPlayerManager().getPlayer(UUID.fromString(jugadores));
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

    public class AccionesXolotl {
        private Xolotl xolotl;
    }
    public class Xolotl {
        private List<DialogosXolotl> bienvenida;
    }
    public class DialogosXolotl {
        private String tipo;
        private String valor;
    }

    public static void mensajesXolotl(EasyNPC<?> npc, List<DialogosXolotl> guardarMensajes, ServerPlayerEntity jugador) {
        jugadoresSaludados(jugador.getServer());
            messageContent = "Bienvenidos " + saludoAJugadores + "!";
            mensajesXolotl.add(
                new ActionDataEntry(ActionDataType.MESSAGE, "")
                    .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of(messageContent))));
            for(Integer i = 0; i < guardarMensajes.size(); i++) {
                if("decir".equals(guardarMensajes.get(i).tipo)) {
                    mensajesXolotl.add(
                        new ActionDataEntry(ActionDataType.MESSAGE, "")
                            .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of(
                                guardarMensajes.get(i).valor))));
                } else if ("esperar".equals(guardarMensajes.get(i).tipo)){
                    mensajesXolotl.add(
                        new ActionDataEntry(
                            ActionDataType.WAIT, guardarMensajes.get(i).valor));
                } else if ("comando".equals(guardarMensajes.get(i).tipo)){
                    mensajesXolotl.add(
                        new ActionDataEntry(
                            ActionDataType.COMMAND, guardarMensajes.get(i).valor));
                }
            }
            EasyNPCActionHandler.schedule(npc, new Identifier("mictlan", "saludo"), 60, mensajesXolotl);
            MictlanMod.CurrentEra.haRecibidoSaludoInicial(true);
            EasyNPCActionHandler.setState(
                npc, new Identifier("mictlan", "saludo_inicial"), 
                StateEntry.of(true), jugador);
    }

    public static void interaccionXolotl(EasyNPC<?> npc, ServerPlayerEntity jugador) {   
        rutaDialogos = new Identifier("mictlan_medieval", "dialogos/bienvenida_xolotl.json");
        rutaDialogosFile = jugador.getServer().getResourceManager().getResource(rutaDialogos);
        try (BufferedReader leerDialogos = rutaDialogosFile.get().getReader()){
            dialogosNPC = MictlanMod.gson.fromJson(leerDialogos, AccionesXolotl.class);
            dialogosXolotl = dialogosNPC.xolotl.bienvenida;
        } catch(IOException e) {
            MictlanMod.LOGGER.error("[mictlan] Dialogos del NPC no pudieron ser leidos!", e);
        }
        saludoInicial = EasyNPCActionHandler.getState(npc, new Identifier("mictlan", "saludo_inicial"));
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

    /**
    * -------------------------------------------------------------------------
    * BUSCAR A XOLOTL Y MANDARLO A LA CASA (/mictlan home set)
    * -------------------------------------------------------------------------
    */

    public void obtenerNPC(CommandContext<ServerCommandSource> context, Identifier npcAObtener) {
        HelpersComandos nuevaPosicionNPC = new HelpersComandos();
        Collection<SavedNPCEntityEntry> entidadNPC = EasyNPCEntityHandler.getByCustomIdentifier(npcAObtener);
        for(SavedNPCEntityEntry datosNPC : entidadNPC) {
            // Si hay varios con el mismo identificador, se queda el ultimo.
            uuidNPC = datosNPC.entityUUID();
        }
        getNPC = EasyNPCEntityHandler.find(uuidNPC, context.getSource().getWorld());
        if(!getNPC.isEmpty()) {
            nuevaPosicionNPC.marcarNuevaUbicacionNPC(context, new Identifier(xolotlNPCIdentifier), getNPC);
        }
    }

    /**
    * -------------------------------------------------------------------------
    * GETTERS
    * -------------------------------------------------------------------------
    */

    public Optional<EasyNPC<?>> npc() {
        return this.getNPC;
    }

    public boolean isXolotlYaSeMudo() {
        return this.xolotlSeMudo;
    }
}