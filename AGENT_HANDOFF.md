# AGENT_HANDOFF.md — Estado Completo y Contexto Maestro de Tasa VE

> **Propósito de este documento:** Punto de entrada único para cualquier agente de desarrollo que continúe el proyecto (local o en la nube). Resume **todo lo implementado, lo descartado explícitamente por el dueño, las decisiones de producto/marketing/legal y la próxima tarea aprobada**, para evitar repetir trabajo o proponer ideas ya evaluadas.

**Última actualización:** 23 de noviembre de 2026  
**Repositorio:** `https://github.com/Julcer26/TasaVE`  
**Rama activa de trabajo:** `feat/v1.2-ultra`  
**Release de pruebas directas (APK):** `https://github.com/Julcer26/TasaVE/releases/download/v1.2.0-ultra-test/TasaVE-v1.2-ultra-test.apk`

---

## 1. Reglas de Trabajo y Preferencias del Dueño (IMPRESCINDIBLES)

1. **Entregable obligatorio tras cada cambio aprobado:** Siempre compilar, pasar tests unitarios (`./gradlew testDebugUnitTest assembleDebug`), hacer commit, push a `feat/v1.2-ultra`, esperar a que GitHub Actions publique `TasaVE-v1.2-ultra-test.apk` y entregar el enlace directo de descarga.
2. **NO saturar la app con textos explicativos ni tutoriales largos:** El usuario quiere interfaces limpias, compactas y directas. La educación de trucos (como el conversor flotante o el conversor por voz) se hace en **redes sociales y videos visuales (Reels, TikTok, Google Omni / Vids)**, NO ocupando espacio dentro de la app.
3. **Jerga PROHIBIDA en UI, web y docs:** Nunca escribir `"4 decimales"`, `"4×4"`, `"4×3"`, `"Centro Rápido"`, ni `"Obsidian Studio"` en textos visibles al usuario. Usar lenguaje natural y claro.
4. **Identidad y contacto oficial:**
   - Dominio: `https://tasave.net` (publicado desde `docs/` con GitHub Pages).
   - Correo soporte/privacidad: `soporte@tasave.net`.
   - Redes: Instagram `@tasa.ve` y TikTok `@tasa.ve`.
   - Hoja de feedback Google Sheets: ID `1TALv6ZVhZAmgLjN3CHny4HaOkRqlQn8Zpat-djiKQGo` (Apps Script webhook ya configurado en `app/src/main/res/values/strings.xml`).
   - Hoja de Bitácora y Resumen Mensual Google Sheets: ID `1TnINAzpxewiSUaZUJzSBp39rxiKoO_EOffluxSk8BrA` (`Tasa VE — Registro de Actividades y Resumen Mensual`, donde se registra cada avance y resumen mensual de uso).

---

## 2. Infraestructura y Servicios YA Configurados

| Servicio | Estado | Detalles Clave |
| :--- | :--- | :--- |
| **Firebase Analytics** | ✅ **ACTIVO y verificado** | Proyecto `tasa-ve-julcer26`. Apps registradas: `com.julcer26.tasave` (Release) y `com.julcer26.tasave.ultra` (Debug). Secret encriptado `GOOGLE_SERVICES_JSON` ya cargado en GitHub Actions; `.github/workflows/build-apk.yml` y `release.yml` inyectan `app/google-services.json` automáticamente. `app/google-services.json` está en `.gitignore`. Wrapper en `AnalyticsTracker.kt` con opt-out en Configuración. |
| **AdMob (Producción)** | ✅ **Configurado** | App ID `ca-app-pub-1192254236776922~3391721063`. Banner, Intersticial y Rewarded IDs reales ya en `strings.xml` (Debug usa IDs de prueba). `docs/app-ads.txt` publicado. |
| **Google Play Billing** | ✅ **Configurado** | `tasave_ultra_monthly` (`$0.99/mes`) y `tasave_ultra_yearly` (`$7.99/año`) en `BillingManager.kt`. En builds Debug existe selector de tier de pruebas en Configuración. |
| **APIs de Tasas (Offline-first)** | ✅ **Operativo** | `ve.dolarapi.com`, `co.dolarapi.com`, `open.er-api.com`, `rates.dolarvzla.com` (BCV Hoy, BCV Mañana, EUR BCV, USDT Binance, COP frontera e históricos) con caché local en `LocalStorageHelper`. |
| **CI/CD GitHub Actions** | ✅ **Operativo** | `.github/workflows/build-apk.yml` corre tests unitarios, inyecta `GOOGLE_SERVICES_JSON`, compila `assembleDebug` y actualiza automáticamente el pre-release `v1.2.0-ultra-test` con `TasaVE-v1.2-ultra-test.apk`. |

---

## 3. Qué Está Implementado y Validado en `feat/v1.2-ultra`

### Pestaña 1 — Conversor (`TasaVeScreen.kt`)
- Barra superior limpia: Logo, badge de plan, **Calendario Bancario** (con punto indicador si hay feriado próximo), **Escáner Cámara OCR**, **Refrescar** y **Configuración**. *(El ícono de reloj/historial fue eliminado del TopBar porque el botón `Ver historial` ya está dentro de la tarjeta de Calculadora Inteligente).*
- Chips de tasas: **BCV Hoy**, **BCV Mañana (fecha)**, **🇨🇴 COP (Peso Colombiano)**, y tarjetas en vivo **USD BCV**, **EUR BCV**, **USDT Binance**.
- Calculadora bidireccional (Divisa ⇄ Bs.), **Guardar cálculo**, **Ver historial** y **Compartir Tarjeta PNG** de cálculo.
- **Sin autocopiado molesto** al cambiar de moneda.

### Pestaña 2 — Carrito (`UltraScreens.kt`)
- Carrito de compras con escáner OCR integrado, hasta **5 productos gratis** / **ilimitado + IVA (16%) / IGTF (3%) en Ultra**.
- **Tarjeta PNG de Presupuesto / Carrito** (`ShareCardGenerator.shareCartQuoteCard`) con logo propio en Ultra y pie de marca `Generado con Tasa VE • https://tasave.net`.

### Pestaña 3 — Útiles (`UltraScreens.kt`)
- **2 herramientas prácticas** (Gratis):
  1. **¿Con qué pago? (USDT vs Efectivo/$):** permite **descuento libre en `% Porc.` o `$ Monto`** (verificado y aprobado por el usuario).
  2. **Dividir cuenta:** soporta **5 monedas seleccionables (`USD`, `EUR`, `Bs.`, `USDT`, `COP`)** y comparte el resumen por WhatsApp (verificado y aprobado por el usuario).
- **Accesos Rápidos (Sin abrir la app):** compacto, SOLO contiene:
  1. **Notificación fija de tasas (Ultra opt-in):** barra silenciosa (`PersistentRateNotificationManager.kt`) con USD/EUR/USDT y 3 botones (`Copiar tasas`, `Calc. rápida`, `Pago Móvil`). Sincronizada en segundo plano por `RateSyncWorker.kt`.
  2. **Botón en Barra Rápida (Quick Settings Tile):** `TasaVeQuickTileService.kt` (abre app en Gratis; abre **Conversor Flotante** en Ultra).
- **Conversor Flotante (`QuickCalcActivity.kt`):** ventana translúcida compacta de 4 filas con `taskAffinity=""` (no saca al usuario de WhatsApp), detección automática del portapapeles (`scheduleClipboardProbes` + `QuickAmountParser.kt` ignorando códigos bancarios como `0134` y bloques de Pago Móvil), selector de tasa + BCV Mañana, y copiado con perfil de Pago Móvil en 1 toque.
- **Widgets de Pantalla de Inicio:**
  - Gratis: `TasaVeRatesWidgetProvider.kt` (Tasas en Vivo compacto).
  - Ultra: `TasaVeUltraWidgetProvider.kt` (Tasas + Calculadora + Cobro Express).

### Pestaña 4 — Cobros (`UltraScreens.kt`)
- **Pago Mixto** (divisa + bolívares) con cálculo exacto de restante o vuelto.
- **Perfiles de cobro locales** (Pago Móvil, Cuenta Bancaria, Zelle, Binance Pay): **1 perfil gratis / ilimitados en Ultra**.
- **Tarjeta PNG de Cobro** (`ShareCardGenerator.shareCheckoutCard`) con **marca/logo personalizado en Ultra**.

---

## 4. Decisiones de Eliminación (NO RE-IMPLEMENTAR NI RE-PROPONER)

Estas funciones fueron probadas o evaluadas y **descartadas explícitamente por el dueño**:

1. **❌ Menú de 3 puntos (`ACTION_PROCESS_TEXT` / `ProcessTextActivity`) y popup de Samsung (`SecProcessTextManageAppsFragment`):** Eliminado por completo en `f1ac7a6`. En Samsung One UI 6/7/8 el sistema oculta apps de terceros en el menú contextual tras `Settings.Global["process_text_manager_apps"]` y resulta muy friccionado para el usuario. **NO volver a agregar `PROCESS_TEXT` ni popups al inicio.**
2. **❌ Tarjeta explicativa "Conversor flotante en WhatsApp" en `Útiles`:** Eliminada en `f1ac7a6` porque ocupaba demasiado espacio. Ese flujo se explicará solo en marketing/redes/videos.
3. **❌ Ícono de reloj (Historial) en la barra superior (`TopBar`):** Eliminado en `f1ac7a6` para liberar espacio arriba (el botón `Ver historial` ya existe dentro de la Calculadora).
4. **❌ Alertas automáticas de cambio de precio (`RateAlertWorker`, `AlertSettings`, tarjeta en `Útiles`):** Eliminadas por completo en `f1ac7a6` porque el dólar BCV suele mantenerse igual todo el día y no aporta valor real. *(Solo se mantiene `RateSyncWorker.kt` silencioso para refrescar la Notificación fija y Widgets; además borra el canal/tarea legado de alertas).*
5. **❌ Herramientas descartadas previamente de `Útiles`:** Se eliminaron calculadoras de cuotas/comisiones viejas (`84cd18d`). Las 2 herramientas fijas de `Útiles` son `¿Con qué pago?` y `Dividir cuenta`.
6. **❌ Conversor por Voz / Micrófono (`RECORD_AUDIO`, `SpeechRecognizer`):** Evaluado y descartado por el dueño (23/11/2026) por preferencia de interfaz limpia sin micrófono. **NO volver a agregar botón de micrófono ni permiso `RECORD_AUDIO`.**

---

## 5. Ideas Ya Exploradas y Separación con Business (NO Repetir sin Pedirlo)

Para no proponer cosas repetidas ni mezclar tiers:

- **Reservado exclusivamente para Fase 2 — Tasa VE Business (`v1.3`, color Ámbar `#F59E0B`, Ultra pasa a Verde Esmeralda `#10B981`):**
  1. Catálogo rápido / Modo Caja.
  2. Nota de entrega / Presupuesto PDF + **IGTF Mixto Real** (3% solo sobre la porción pagada en efectivo/Zelle, bolívares exentos).
  3. Cierre de Caja Diario (por método de pago + exportación CSV/WhatsApp).
  4. Cuentas por Cobrar ("Fiados" indexados) + Calculadora de Reposición por brecha.
  *(Ver detalles completos en `ROADMAP_DESPLIEGUE_MARKETING.md` sección 4).*
- **Ideas anteriores ya evaluadas para Tab 3 / Ultra que NO entusiasmaron al dueño:**
  - Cashea/Krece cuotas, tasa personalizada/promedio, tasa por fecha pasada, contador de billetes, comparador por kilo/litro, pagos fijos del mes, bóveda OCR de comprobantes, calculadora de comisiones PayPal/Zinli/Wally, presupuesto personal doble moneda, modo discreto, conversor por voz / micrófono.

---

## 6. Estado Actual y Siguientes Pasos (v1.2 Listo para Pruebas y Play Store)
- **Producto v1.2:** Completo, limpio y sincronizado (sin alertas, sin `PROCESS_TEXT`, sin micrófono).
- **Marketing, Web y Legal:** `docs/index.html`, `docs/privacy-policy.html`, `PRIVACY_POLICY.md`, `TERMS_OF_SERVICE.md`, `GOOGLE_PLAY_GUIDE.md`, `README.md`, `UltraScreens.kt` y `TasaVeScreen.kt` 100% alineados.
- **Redes (`@tasa.ve`):** Playbooks de Instagram y TikTok en Google Drive actualizados con los guiones v1.2 y prompts para Google Omni / Vids.
- **Próximo hito:** Pruebas cerradas en Google Play Console (12 testers / 14 días) y luego Fase 2 (`Tasa VE Business v1.3`).
