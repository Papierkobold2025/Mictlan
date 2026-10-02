package dev.dario.mictlan;

import de.markusbordihn.easynpc.api.action.EasyNPCActionHandler;
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
        MictlanMod.LOGGER.info("[Mictlan] " + jugador.getName().getString() + " interactuo con Xolotl.");
        saludoInicial = EasyNPCActionHandler.getState(npc, new Identifier("mictlan", "saludo_inicial"));
        if (saludoInicial == null) {
            jugadoresSaludados();
            EasyNPCActionHandler.say(npc, "Bienvenidos " + jugador.getName().getString() + "!");
            EasyNPCActionHandler.say(npc, "Mi nombte es Xolotl, soy el acompañante del Mictlan, y vengo a acompañarlos!");
            for(int i = 0; i < cantidadJugadores; i++) {
                Advancement cherryGroveAdvancement = PlayerConnection.jugadoresDisponibles.get(i).getServer().getAdvancementLoader().get(new Identifier("mictlan", "codice/rumor/bosque_cerezos"));
                PlayerConnection.jugadoresDisponibles.get(i).getAdvancementTracker().grantCriterion(cherryGroveAdvancement, "otorgado");
            }
            MictlanMod.CurrentEra.haRecibidoSaludoInicial(true);
            EasyNPCActionHandler.setState(npc, new Identifier("mictlan", "saludo_inicial"), new StateEntry, jugador);
        }
    }

    public static void bienvenidaAJugadores(PlayerEntity jugador) {

    }
}