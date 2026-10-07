package dev.dario.mictlan.World;

import java.io.IOException;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

import dev.dario.mictlan.Core.MictlanMod;
import dev.dario.mictlan.Helpers.Helpers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

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

    /**
    * -------------------------------------------------------------------------
    * CREACION DE DIRECTORIOS AL CARGAR EL MUNDO
    * -------------------------------------------------------------------------
    */

    public static void worldLoad(MinecraftServer servidor, ServerLevel mundo, WorldData CurrentEra){
        // LOAD corre una vez por dimension; solo nos interesa el Overworld.
        if(mundo.dimension() == ServerLevel.OVERWORLD){
            WorldData respuestaArchivoDeConfiguracion;
            mictlanConfigDir = (FMLPaths.CONFIGDIR.get().resolve("mictlan"));
            mictlanConfigFile = Path.of(mictlanConfigDir.toString(), "mictlan.json" );
            mictlanDir = servidor.getWorldPath(LevelResource.ROOT).resolve("mictlan");
            characterDir = mictlanDir.resolve("character");
            playerDir = mictlanDir.resolve("players");
            worldData = mictlanDir.resolve("world");
            // Fuera del save del mundo para que sobreviva al cambiar de mundo.
            mictlanCorePath = FMLPaths.CONFIGDIR.get().resolve("mictlan");
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
