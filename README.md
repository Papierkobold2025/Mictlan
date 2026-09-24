# Mictlan

Esqueleto minimo de un mod Fabric para Minecraft 1.20.1. Sin Mixins, sin
dependencia de Nexus Characters todavia. Su unico objetivo es demostrar
que el mod compila, se empaqueta y Fabric Loader lo carga.

## Antes de usarlo en serio

- Renombra `projectcore` (modid, maven_group, package Java) por el nombre
  real que decidan para el proyecto. Ahora es barato, mas adelante no.
- Revisa https://fabricmc.net/develop por si hay versiones mas nuevas de
  Yarn/Loader/Fabric API/Loom que las que dejé fijadas en `gradle.properties`
  y `build.gradle`.
- Necesitas Java 17 instalado (Minecraft 1.20.1 lo requiere).

## Como compilar

```
./gradlew build
```

El jar queda en `build/libs/`.

## Como probarlo

```
./gradlew runServer
```

La primera vez, Loom genera la carpeta `run/` con un servidor dedicado de
prueba y falla pidiendote aceptar el EULA — edita `run/eula.txt` y pon
`eula=true`, luego vuelve a correr el comando.

**Qué confirma que este paso funcionó:** en la consola del servidor, entre
los mensajes de carga de mods de Fabric, debe aparecer una línea con
`[ProjectCore] Inicializado correctamente. Sin Mixins, sin Nexus todavia.`
Si aparece, el mod compila, Fabric Loader lo reconoce y lo carga sin
errores — que es exactamente todo lo que este primer paso pretende probar.
