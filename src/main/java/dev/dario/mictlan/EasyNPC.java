package dev.dario.mictlan;

import net.minecraft.advancement.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class EasyNPC {
    private static Hand manoPrincipal = Hand.MAIN_HAND;
    public static ActionResult interaccionXolotl(PlayerEntity jugador, World mundo, Hand mano, Entity entidad, EntityHitResult hitResultado) {
        if (mundo.isClient()) {
            return ActionResult.PASS;
        }
        if (!(mano == manoPrincipal)) {
            return ActionResult.PASS;
        }
        if (!entidad.getCommandTags().contains("Xolotl")) {
            return ActionResult.PASS;
        }
        for(int i =0; i < PlayerConnection.jugadoresDisponibles.size(); i++) {
            if (!(jugador.getUuid().toString().equals(PlayerConnection.jugadoresDisponibles.get(i).getUuidAsString()))){
                return ActionResult.PASS;
            }
        }
        MictlanMod.LOGGER.info("[Mictlan] " + jugador.getName().getString() + " interactuo con Xolotl.");
        jugador.sendMessage(Text.literal("<Xolotl> Bienvenidos " + PlayerConnection.jugadoresDisponibles.get(0).getName().getString() + " y " + PlayerConnection.jugadoresDisponibles.get(1).getName().getString() + "!"), false);
        jugador.sendMessage(Text.literal("<Xolotl> Mi nombte es Xolotl, soy el acompañante del Mictlan, y vengo a acompañarlos!"), false);
        for(int i =0; i < PlayerConnection.jugadoresDisponibles.size(); i++) {
            Advancement cherryGroveAdvancement = PlayerConnection.jugadoresDisponibles.get(i).getServer().getAdvancementLoader().get(new Identifier("mictlan", "codice/rumor/bosque_cerezos"));
            PlayerConnection.jugadoresDisponibles.get(i).getAdvancementTracker().grantCriterion(cherryGroveAdvancement, "otorgado");
        }
        return ActionResult.PASS;
    }
}
