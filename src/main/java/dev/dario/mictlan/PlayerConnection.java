package dev.dario.mictlan;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import de.markusbordihn.easynpc.api.handler.EasyNPCEntityHandler;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;

import java.nio.file.Files;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class PlayerConnection {
    public static ServerPlayerEntity playerHandler;
    public static String playerUUID; 
    public static PlayerData playerData;
    public static Path playerFilePath;
    public static PlayerData playerDataJsonReturn;
    public static Optional<EasyNPC<?>> spawnXolotl;
    public static String playerDataJson = "";
    public static ArrayList<ServerPlayerEntity> jugadoresDisponibles = new ArrayList<>();
    public static void playerConnection(ServerPlayNetworkHandler handler){
        playerHandler = handler.getPlayer();
        String playerName = playerHandler.getGameProfile().getName();
        jugadoresDisponibles.add(playerHandler);
        playerUUID = playerHandler.getGameProfile().getId().toString();

        playerData = new PlayerData(playerUUID);

        playerFilePath = Path.of(WorldLoad.playerDir.toString(), playerUUID +".json");
        if(EasyNPCEntityHandler.getByCustomIdentifier(new Identifier(ConfiguracionNPC.xolotlNPCIdentifier)).isEmpty()) {
            MictlanMod.LOGGER.info("[Mictlan] El NPC aun no se ha generado!");
            spawnXolotl = EasyNPCEntityHandler.spawnFromPreset(new Identifier(ConfiguracionNPC.identificadoPresetXolotl), playerHandler.getServer().getOverworld(), Vec3d.ofBottomCenter(playerHandler.getServer().getOverworld().getSpawnPos()), null, null);
        }
        // Primera conexion.
        if (!Files.exists(playerFilePath)) {
            Text welcomeMessage = Text.literal("Bienvenido a Mictlan, " + playerName + "!");
            playerHandler.sendMessage(welcomeMessage, false);
            playerDataJsonReturn = playerData;
            playerDataJsonReturn.hasPlayedBefore(true);
            Helpers.entregaKitInicial(playerHandler);
            Helpers.escribirDatosDelJugador();
        } else {
            // Jugador existente.
            Helpers.leerDatosDelJugador(playerHandler);
            playerDataJsonReturn =MictlanMod.gson.fromJson(playerDataJson, playerData.getClass());
            Text welcomeBackMessage = Text.literal("Bienvenido de nuevo a Mictlan, " + playerName + "!");
            playerHandler.sendMessage(welcomeBackMessage, false);
            playerDataJsonReturn.hasPlayedBefore(true);
            boolean hasReceivedStarterKit = playerDataJsonReturn.isHasReceivedStarterKit();
            playerDataJsonReturn.setEra(MictlanMod.eraActual);
            // Reintento del kit (p. ej. inventario lleno la vez anterior).
            if(!hasReceivedStarterKit) {
                Helpers.entregaKitInicial(playerHandler);
            }
            Helpers.escribirDatosDelJugador();
        };
        if (!Files.exists(WorldLoad.mictlanConfigFile)) {
            Helpers.escribirDatosDeConfiguracion(WorldLoad.mictlanConfigFile, MictlanMod.CurrentEra);
        }
        Text eraMessage = Text.literal("Te encuentras en la era " + MictlanMod.eraActual);
        playerHandler.sendMessage(eraMessage, false);

        MictlanMod.LOGGER.info("[Mictlan] " + playerName + " se conecto.");
    }
}
