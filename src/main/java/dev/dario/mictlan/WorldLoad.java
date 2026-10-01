package dev.dario.mictlan;

import java.io.IOException;

import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;


public class WorldLoad {
    public static Path mictlanConfigDir;
    public static Path mictlanConfigFile;
    public static Path mictlanDir;
    public static Path characterDir;
    public static Path playerDir;
    public static Path worldData;
    public static Path mictlanCorePath;
    public static Path mictlanCoreDataPath;
    public static Path mictlanCoreEntitiesPath;
    private static String readMictlanConfigFile;
    public static void worldLoad(MinecraftServer servidor, ServerWorld mundo, WorldData CurrentEra){
        if(mundo.getRegistryKey() == World.OVERWORLD){
            WorldData respuestaArchivoDeConfiguracion;
            mictlanConfigDir = (FabricLoader.getInstance().getConfigDir().resolve("mictlan"));
            mictlanConfigFile = Path.of(mictlanConfigDir.toString(), "mictlan.json" );
            mictlanDir = servidor.getSavePath(WorldSavePath.ROOT).resolve("mictlan");
            characterDir = mictlanDir.resolve("character");
            playerDir = mictlanDir.resolve("players");
            worldData = mictlanDir.resolve("world");
            mictlanCorePath = FabricLoader.getInstance().getGameDir().resolve("mictlan");
            mictlanCoreDataPath = mictlanCorePath.resolve("data");
            mictlanCoreEntitiesPath = mictlanCorePath.resolve("entities");

                Path[] dirsToCreate = {characterDir, playerDir, mictlanConfigDir, worldData, mictlanCorePath, mictlanCoreDataPath, mictlanCoreEntitiesPath};
                for (Path dir : dirsToCreate) {
                    try {
                        if (!Files.exists(dir)) {
                            Files.createDirectories(dir);
                            MictlanMod.LOGGER.info("[Mictlan] Directorio creado: " + dir.toString());
                        }
                    } catch (IOException e) {
                        MictlanMod.LOGGER.error("[Mictlan] No se pudo crear el directorio: " + dir.toString(), e);
                    }
                }

                /**
                * -----------------------------------------------------------------
                * PEGADO DE CHUNKS Y ENTIDADES GUARDADOS EN EL MUNDO NUEVO
                * -----------------------------------------------------------------
                * Si ya hay chunks copiados con /mictlan home, los leemos de
                * <juego>/mictlan/data (y sus entidades de <juego>/mictlan/entities)
                * y los escribimos en el mundo que se carga.
                */
                // Sin archivo de configuracion no hay nada que pegar.
                if(!Files.exists(mictlanConfigFile)){
                    return;
                };
                try {
                    readMictlanConfigFile = Files.readString(mictlanConfigFile);
                } catch (Exception e) {
                    MictlanMod.LOGGER.error("[Mictlan] Archivo de configuracion no pudo ser leido", e);
                }
                respuestaArchivoDeConfiguracion = MictlanMod.gson.fromJson(readMictlanConfigFile, CurrentEra.getClass());
                Helpers.pegadoDeChunksEnConfig(mundo, respuestaArchivoDeConfiguracion, mictlanCoreDataPath, CurrentEra, mictlanConfigFile);
                // Primero el terreno, despues las entidades encima.
                Helpers.pegadoDeEntidadesEnNuevoMundo(mundo, mictlanCoreEntitiesPath);
        }
    }
}
