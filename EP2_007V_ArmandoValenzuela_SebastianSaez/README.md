# MpazNotifica · Comunicaciones escolares



App Android nativa para redactar, emitir y trazar comunicados escolares hacia apoderados, con permisos de emisión según el perfil. 

---

## Integrantes

| Nombre                     | Requerimientos asignados                             |
|----------------------------|------------------------------------------------------|
| Armando Valenzuela Alarcon | RF-05 · Inspectoría General emite a colegio completo |
| Sebastian Saez Quiroz      |

**Repositorio GitHub:** https://github.com/armavalenzuela/EP2_007V_AppsMobiles.git

---

## Cómo ejecutar la app

1. Descomprime el proyecto en una carpeta y ábrelo con Android Studio (`settings.gradle.kts` está en la raíz).
2. Espera a que termine la sincronización de Gradle, o usa **File > Sync Project with Gradle Files**.
3. Elige un emulador o un dispositivo Android 7.0 o superior.
4. Elige la configuración **app** y presiona **Run ▶**.

### Cómo ejecutar las pruebas unitarias

En la terminal de Android Studio:

```
.\gradlew.bat :app:testDebugUnitTest      (Windows)
./gradlew :app:testDebugUnitTest          (macOS / Linux)
```

El reporte queda en `app/build/reports/tests/testDebugUnitTest/index.html`.

---

## Acceso a la app

El inicio de sesión con credenciales (RF-01) todavía no está integrado. Por ahora, la app se usa con los botones **Modo desarrollo** de la pantalla de login, que entran directamente como:

- **Inspectoría General**
- **Profesor/a Jefe** (4° Básico A)

## Datos de prueba

Todos los datos son ficticios y están en `data/MockData.kt`:

- 3 apoderados de prueba (AP1, AP2, AP3) con canales distintos: correo, WhatsApp y SMS.
- Estudiantes, cursos y talleres ficticios.
- Teléfonos con formato de prueba (`+5690000000X`) y correos de ejemplo.

---

## Requerimientos implementados


| Código   | Requerimiento | Estado |
|----------|---|---|
| RF-01    | Inicio de sesión diferenciado por perfil | Pendiente (se usa modo desarrollo) |
| RF-04    | Selección de destinatarios según alcance | Implementado para Inspectoría General (cuatro alcances) |
| **RF-05** | **Inspectoría General emite a cualquier alcance** | **Implementado (cuatro alcances, solo para Inspectoría General)** |
| RF-08    | Estado del envío por destinatario | Implementado para Inspectoría General |
| RF-09    | Bitácora de comunicados emitidos | Parcial: lista los comunicados registrados |
| RF-11    | Registro automático de autor, fecha y destinatarios | Implementado |



## Recurso nativo: selector de archivos para adjuntar PDF

**Qué usa:** el selector de archivos del sistema (`ActivityResultContracts.OpenDocument`), filtrado a `application/pdf`.

**Por qué es pertinente:** el caso permite que una comunicación lleve un PDF opcional (RF-03), por ejemplo una circular o una autorización. El selector permite elegir el archivo desde el almacenamiento del dispositivo.

**Permisos:** no requiere permisos adicionales. El sistema entrega al app solo el archivo elegido, sin acceso general al almacenamiento. La app no usa cámara, micrófono, GPS ni datos biométricos, como exige RNF-03.

**Integración:** el archivo se copia al almacenamiento interno de la app (`filesDir`) con `AdjuntoStorage.copiarPdf()`. Antes de copiarlo se verifica que empiece con la firma `%PDF`; si no, se muestra un mensaje de error. Así el adjunto sigue disponible aunque el archivo original se mueva o se borre.

---

## Estructura del proyecto (módulo de comunicaciones)

```
app/src/main/java/com/mpaznotifica/app/
├── domain/
│   ├── EmisionColegioCompleto.kt        Emisión a colegio completo (delega en EmisionComunicado)
│   └── EnviarComunicadoUseCase.kt       Caso de uso: resuelve destinatarios, emite y guarda
├── data/
│   ├── MockData.kt                      Datos ficticios de prueba
│   ├── repository/ComunicacionRepository.kt   Interfaces y versiones en memoria
│   └── local/
│       ├── AppDatabase.kt               Base de datos Room
│       ├── ComunicacionDao.kt           Consultas e inserciones
│       ├── ComunicacionEntity.kt        Tabla comunicaciones
│       ├── EnvioEntity.kt               Tabla envios
│       ├── RoomComunicacionRepository.kt Repositorio con Room y transacción
│       └── AdjuntoStorage.kt            Copia del PDF adjunto
├── viewmodel/MpazNotificaViewModel.kt   Estado de las pantallas
└── ui/screen/comunicados/RedactarComunicadoScreen.kt  Formulario y diálogos
```

---

## Pruebas unitarias

| Clase | Pruebas | Qué verifica |
|---|---|---|
| `EmisionColegioCompletoTest` | 10 | Reglas de emisión: sesión, perfil, destinatarios sin duplicados, canal preferente, estado de envío |
| `EnviarComunicadoUseCaseTest` | 8 | Emisión de punta a punta para los cuatro alcances, con repositorios en memoria, permisos y adjunto |
| `ResolverDestinatariosTest` | 7 | Resolución de destinatarios para los cuatro alcances |
| `ComunicacionRepositoryTest` | 2 | El registro agrega comunicados y envíos sin borrar lo anterior |
| `Rf05Test` | 2 | Inspectoría ve los cuatro alcances y todos los destinos |

---

## Limitaciones conocidas

- **Login:** RF-01 no está integrado. Se usa el modo desarrollo para entrar.
- **Despacho:** simulado. No hay conexión con servidores de correo, SMS ni WhatsApp (RNF-06).
- **Modo desarrollo:** los botones de acceso directo siguen en la pantalla de login. Deben retirarse según RNF-05 cuando RF-01 esté integrado.
---

