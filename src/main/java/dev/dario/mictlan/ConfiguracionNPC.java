package dev.dario.mictlan;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ConfiguracionNPC {
    private static Integer cantidadJugadores;
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

    /**
    * -------------------------------------------------------------------------
    * NOMBRES DE LOS JUGADORES CONECTADOS ("A y B y C")
    * -------------------------------------------------------------------------
    */

    private static void jugadoresSaludados() {
        cantidadJugadores = PlayerConnection.jugadoresDisponibles.size();
        int i = 0;
        while (i < cantidadJugadores) {
            saludoAJugadores += PlayerConnection.jugadoresDisponibles.get(i).getName().getString() + " y ";
            i++;
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

    public static void interaccionXolotl(EasyNPC<?> npc, ServerPlayerEntity jugador) {   
        cantidadJugadores = PlayerConnection.jugadoresDisponibles.size(); 
        MictlanMod.LOGGER.info("[Mictlan] " + jugador.getName().getString() + " interactuo con Xolotl.");
        // El estado vive en el NPC, asi que el saludo solo ocurre una vez.
        saludoInicial = EasyNPCActionHandler.getState(npc, new Identifier("mictlan", "saludo_inicial"));
        if (saludoInicial == null) {
            jugadoresSaludados();
            List<ActionDataEntry> lista = List.of(
                new ActionDataEntry(ActionDataType.MESSAGE, "")
                    .withMessageActionData(MessageActionData.DEFAULT.withTexts(List.of("Bienvenidos " + jugador.getName().getString() + "!"))),
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
        ConfiguracionNPC configuracionNPC = new ConfiguracionNPC(getNPC);
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