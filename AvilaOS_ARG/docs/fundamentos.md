# Fundamentos de ÁvilaOS

## Estados del proceso
Nuevo → Listo → Ejecución → Terminado, con Ejecución → Bloqueado → Listo.
- Nuevo → Listo: solo si hay memoria suficiente en el computador.
- Ejecución → Bloqueado: semWait sin permiso o espera de E/S.
- Terminado: fin normal o deadline llegado a 0.

## PCB
ID único en todo el sistema, nombre, computador, estado, tipo, prioridad, memoria, deadline y tiempo restante.

## Semáforos
- semWait(s): s.valor--; si s.valor < 0, el proceso se bloquea en la cola de s.
- semSignal(s): s.valor++; si s.valor <= 0, se despierta un proceso de la cola de s.
- Binario (mutex): protege una sección crítica.
- De conteo: cuenta elementos o espacios libres.

## Productor-consumidor con buffer acotado
Semáforos: s = 1 (mutex), n = 0 (elementos), e = capacidad (espacios).
- Productor: semWait(e), semWait(s), añadir, semSignal(s), semSignal(n).
- Consumidor: semWait(n), semWait(s), extraer, semSignal(s), semSignal(e).
- Invertir los dos semWait del productor provoca interbloqueo.

## Dos capas de semáforos
- Java (java.util.concurrent.Semaphore): protege estructuras compartidas entre hilos reales.
- ÁvilaOS (clase propia): bloquea procesos simulados con cola visible; el hilo de Java no se duerme.