# ÁvilaOS

Simulador de un sistema operativo distribuido procesos y sincronización con semáforos.

## Requisitos
- JDK 22 o superior (desarrollado con JDK 26)
- NetBeans IDE (proyecto Java con Ant)

## Cómo ejecutar
1. En NetBeans: File → Open Project → seleccionar la carpeta `AvilaOS_Arg` de este repositorio.
2. Run → Run Project (F6).

## Estructura
`AvilaOS_ARG/src/avilaos/` está organizado por capas: `util`, `model`, `hardware`, `kernel`, `scheduler`, `sync`, `cluster`, `metrics`, `config`, `gui`, `app`.

## Flujo de trabajo
- `main`: versión estable para entrega.
- `develop`: integración.
- `feat/...`, `fix/...`, `docs/...`: ramas de trabajo creadas desde `develop` y fusionadas mediante Pull Request.
- Commits con formato `tipo: descripción` (feat, fix, docs, chore, refactor, test).

## Equipo
- Richard Yrady
- Gianmarco Del Giudice
- Andres Arrieta