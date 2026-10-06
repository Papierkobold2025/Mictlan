package dev.dario.mictlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ConfiguracionNPC {
    public static PlayerData saludoInicial1;
    public static PlayerData jugadorInteractuando;
    public static StateEntry saludoInicial;
    private static String saludoAJugadores = "";
    private Optional<EasyNPC<?>> getNPC;
    private UUID uuidNPC;
    private boolean xolotlSeMudo;
    public static ConfiguracionNPC configuracionNPC = new ConfiguracionNPC(Optional.empty());
    public static String xolotlNPCIdentifier = "mictlan:xolotl";
    public static String identificadoPresetXolotl = "mictlan:easy_npc/preset/humanoid/xolotl.npc.snbt";
    private static String mensaje = "";
    private static ActionDataEntry messageEntry;
    private static Identifier rutaDialogos;
    private static Path rutaDialogosFile;
    private static String dialogosNPC; 

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

    public static ActionDataEntry mensajesXolotl(String mensaje) {
        messageEntry = new ActionDataEntry(ActionDataType.MESSAGE, "").withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of(mensaje)));
        return messageEntry;
    }
    public class AccionAxolotl {
        private String tipo;
        private String valor;
    }

    public static void interaccionXolotl(EasyNPC<?> npc, ServerPlayerEntity jugador) {   
        rutaDialogos = new Identifier("mictlan_medieval", "dialogos/bienvenida_xolotl.json");
        rutaDialogosFile = Path.of(rutaDialogos.getPath());
        try{
            dialogosNPC = Files.readString(rutaDialogosFile);
        } catch(IOException e) {
            MictlanMod.LOGGER.error("[mictlan] Dialogos del NPC no pudieron ser leidos!", e);
        }
        MictlanMod.gson.fromJson(dialogosNPC, AccionAxolotl.class);
        for(Integer i = 0; i < 1; i++);
        ActionDataEntry waitTime = new ActionDataEntry(ActionDataType.WAIT, "3s");
        MictlanMod.LOGGER.info("[Mictlan] " + jugador.getName().getString() + " interactuo con Xolotl.");
        // El estado vive en el NPC, asi que el saludo solo ocurre una vez.
        saludoInicial = EasyNPCActionHandler.getState(npc, new Identifier("mictlan", "saludo_inicial"));
        if (saludoInicial == null) {
            jugadoresSaludados(jugador.getServer());
            List<ActionDataEntry> lista = List.of(
                ConfiguracionNPC.mensajesXolotl("Bienvenidos " + saludoAJugadores + "!"),
                new ActionDataEntry(ActionDataType.WAIT, "3s"),
                new ActionDataEntry(ActionDataType.MESSAGE, "")
                    .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of("Mi nombre es Xolotl, soy el acompañante del Mictlan, y vengo a acompañarlos!"))),
                new ActionDataEntry(ActionDataType.WAIT, "3s"),
                new ActionDataEntry(ActionDataType.MESSAGE, "")
                    .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of("Ahora busquen una casa en este vasto mundo, cuando lleguen, escriban el comando '/mictlan home set' y yo ire a donde esten!"))),
                new ActionDataEntry(ActionDataType.WAIT, "3s"),
                new ActionDataEntry(ActionDataType.MESSAGE, "")
                    .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of("Antes de que se me olvide, tengan este libro: "))),
                new ActionDataEntry(ActionDataType.WAIT, "3s"),
                new ActionDataEntry(ActionDataType.COMMAND, "/give @a patchouli:guide_book{\"patchouli:book\":\"mictlan:codice\"}"),
                new ActionDataEntry(ActionDataType.COMMAND, "/give @a minecraft:wooden_shovel"),
                new ActionDataEntry(ActionDataType.COMMAND, "/advancement grant @a only mictlan:codice/rumor/bosque_cerezos")
            );
            EasyNPCActionHandler.schedule(npc, new Identifier("mictlan", "saludo"), 60, lista);
            MictlanMod.CurrentEra.haRecibidoSaludoInicial(true);
            EasyNPCActionHandler.setState(npc, new Identifier("mictlan", "saludo_inicial"), StateEntry.of(true), jugador);
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