package dev.dario.mictlan;

import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
import de.markusbordihn.easynpc.data.action.ActionDataEntry;
import de.markusbordihn.easynpc.data.action.ActionDataType;
import de.markusbordihn.easynpc.data.state.StateEntry;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ConfiguracionNPC {
    public static Integer cantidadJugadores = PlayerConnection.jugadoresDisponibles.size();
    public static PlayerData saludoInicial1;
    public static PlayerData jugadorInteractuando;
    public static StateEntry saludoInicial;
    private static String saludoAJugadores = "";
    private static void jugadoresSaludados() {
        int i = 0;
        while (i < cantidadJugadores) {
            saludoAJugadores += PlayerConnection.jugadoresDisponibles.get(i).getName().getString() + " y ";
            i++;
        }
        // Remove the trailing " y "
        if (saludoAJugadores.endsWith(" y ")) {
            saludoAJugadores = saludoAJugadores.substring(0, saludoAJugadores.length() - 3);
        }
    }
    public static void interaccionXolotl(EasyNPC<?> npc, ServerPlayerEntity jugador) {    
        ActionDataEntry darLibro = new ActionDataEntry(ActionDataType.COMMAND, "/give @a patchouli:guide_book{\"patchouli:book\":\"mictlan:codice\"}"); 
        MictlanMod.LOGGER.info("[Mictlan] " + jugador.getName().getString() + " interactuo con Xolotl.");
        saludoInicial = EasyNPCActionHandler.getState(npc, new Identifier("mictlan", "saludo_inicial"));
        if (saludoInicial == null) {
            jugadoresSaludados();
            EasyNPCActionHandler.say(npc, "Bienvenidos " + jugador.getName().getString() + "!");
            EasyNPCActionHandler.say(npc, "Mi nombte es Xolotl, soy el acompañante del Mictlan, y vengo a acompañarlos!");
            EasyNPCActionHandler.say(npc, "Ahora busquen una casa en este vasto mundo, cuando lleguen, escriban el comando /mictlan home set y yo ire a donde esten!");
            EasyNPCActionHandler.say(npc, "Andes de que se me olviden, tengan este libro: ");
            EasyNPCActionHandler.execute(npc, darLibro, null);
            for(int i = 0; i < cantidadJugadores; i++) {
                Advancement cherryGroveAdvancement = PlayerConnection.jugadoresDisponibles.get(i).getServer().getAdvancementLoader().get(new Identifier("mictlan", "codice/rumor/bosque_cerezos"));
                PlayerConnection.jugadoresDisponibles.get(i).getAdvancementTracker().grantCriterion(cherryGroveAdvancement, "otorgado");
            }
            MictlanMod.CurrentEra.haRecibidoSaludoInicial(true);
            EasyNPCActionHandler.setState(npc, new Identifier("mictlan", "saludo_inicial"), StateEntry.of(true), jugador);
        }
    }

    public static void bienvenidaAJugadores(PlayerEntity jugador) {

    }
}