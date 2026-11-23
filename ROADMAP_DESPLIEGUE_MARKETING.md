# Roadmap de Despliegue, Marketing y Tasa VE Business

**Última actualización:** 23 de noviembre de 2026  
**Rama de desarrollo actual:** `feat/v1.2-ultra`  
**Dominio oficial:** `https://tasave.net` • **Soporte:** `soporte@tasave.net`  
**Redes oficiales:** Instagram [`@tasa.ve`](https://www.instagram.com/tasa.ve/) • TikTok [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)

---

## 1. Arquitectura de Planes (Estrategia en Dos Fases)

Para evitar confusión entre planes y no sobrecargar la interfaz principal para el usuario casual, la evolución comercial de **Tasa VE** se divide en dos versiones claras:

| Nivel | Versión | Color Identidad | Precio Sugerido (Google Play) | Público Objetivo | Qué Incluye |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Gratis** | v1.2 | Neutro (Oscuro / Claro) | \$0 USD (AdMob equilibrado) | Uso ocasional / consulta rápida | Conversor BCV exacto, BCV Hoy/Mañana, USDT paralelo, **Peso Colombiano (COP) de frontera**, **Calendario SUDEBAN**, pestaña **Útiles** (*¿Con qué pago?* con descuento libre y *Dividir cuenta* en 5 monedas), Pago Mixto + Tarjeta PNG (Cobros y Carrito), **Widget Tasas en Vivo**, **botón en Barra Rápida**, hasta 5 productos en carrito, 1 perfil de cobro, 3 escaneos OCR/día (+5 o pase 12h sin anuncios vía video bonificado) y **2 conversiones por voz gratis/día**. |
| **Tasa VE Ultra** | v1.2 *(pasa a Verde en v1.3)* | **Verde Esmeralda (`#10B981`)** | **\$0.99 / mes** o **\$7.99 / año** | Usuario diario, compradores frecuentes, freelancers | **0 publicidad**, **Conversor por Voz Manos Libres ilimitado**, **Notificación fija silenciosa de tasas** (con copiar tasas, calc. rápida y Pago Móvil), **Conversor flotante desde Barra Rápida/Notificación con multi-tasa + Pago Móvil**, escáner OCR ∞, carrito ∞ + IVA/IGTF simple, perfiles de cobro ∞, **logo propio en Tarjeta PNG**, y **Widget Calculadora y Cobro Express**. |
| **Tasa VE Business** | v1.3 (Fase 2) | **Ámbar / Dorado (`#F59E0B`)** | **\$2.99 – \$3.99 / mes** o **\$24.99 – \$29.99 / año** *(7 días gratis)* | Bodegas, comercios, tiendas Instagram, delivery, servicios | **Todo Ultra incluido** + **Panel Business dedicado** (Catálogo rápido, PDF Nota de Entrega + IGTF Mixto Real, Cierre de Caja Diario, Fiados indexados y Reposición por Brecha). |

> **Regla de oro:** *Business* nunca debe cobrarse como un "extra aparte sobre Ultra". En Google Play Billing se ofertan como planes independientes donde *Business* desbloquea automáticamente todos los beneficios de *Ultra*.

---

## 2. Plan de Despliegue Técnico — Fase 1 (v1.2 en Google Play Store)

### 2.1 Checklist obligatorio antes de compilar el `.aab` de producción
1. **Unidades reales de AdMob (`app/src/main/res/values/strings.xml`):**
   - `admob_app_id`: `ca-app-pub-1192254236776922~3391721063` *(configurado)*.
   - `admob_banner_ad_unit_id`: `ca-app-pub-1192254236776922/4135771180` *(configurado)*.
   - `admob_interstitial_ad_unit_id`: `ca-app-pub-1192254236776922/2678817304` *(configurado)*.
   - `admob_rewarded_ad_unit_id`: `ca-app-pub-1192254236776922/4284589192` *(configurado)*.
   - Verificar que `https://tasave.net/app-ads.txt` esté publicado y accesible *(ya activo)*.
2. **Firebase Analytics (Telemetría Anónima Opt-Out — ACTIVO):**
   - Proyecto Firebase **`tasa-ve-julcer26`** creado con ambas apps registradas (`com.julcer26.tasave` para Play Store y `com.julcer26.tasave.ultra` para APK de pruebas).
   - Secret encriptado **`GOOGLE_SERVICES_JSON`** ya cargado en GitHub Actions; `.github/workflows/build-apk.yml` y `release.yml` inyectan `app/google-services.json` automáticamente en cada build (`app/google-services.json` está en `.gitignore`).
   - `AnalyticsTracker` respeta el interruptor *Estadísticas anónimas de uso* en *Configuración > Legal y Privacidad* y nunca envía montos ni datos personales.
3. **Suscripciones en Google Play Console:**
   - Producto de suscripción **`tasave_ultra_monthly`** con plan base mensual de **\$0.99 USD** *(activo)*.
   - Producto de suscripción **`tasave_ultra_yearly`** con plan base anual de **\$7.99 USD** *(activo)*.
4. **Firma y empaquetado Release:**
   - Generar keystore privado (`.jks`) y resguardar copia cifrada fuera del repositorio.
   - Compilar bundle firmado con `./gradlew bundleRelease` (`minifyEnabled = true` / ProGuard).
5. **Ficha de tienda y cumplimiento Play Console:**
   - URL de Política de Privacidad: `https://tasave.net/privacy-policy.html`.
   - Correo de contacto en la ficha: `soporte@tasave.net`.
   - Sitio web oficial: `https://tasave.net`.
   - Formulario **Seguridad de Datos (Data Safety):** declarar que la app no almacena cuentas en servidores propios, que el envío de reportes es opcional por Google Sheets, y que **Firebase Analytics** (solo eventos anónimos de navegación con opt-out integrado) + Google Mobile Ads + ML Kit + Play Billing procesan identificadores / diagnósticos técnicos en tránsito cifrado HTTPS.
   - Permiso `CAMERA`: justificar uso exclusivo local en primer plano para OCR de precios (sin captura/almacenamiento ni subida de fotos).
   - Permiso `RECORD_AUDIO` *(al activar Conversor por Voz)*: justificar uso exclusivo bajo pulsación directa del usuario para dictar montos y monedas (sin grabación en segundo plano ni almacenamiento de audio).
   - Si la cuenta de Play Console es personal nueva: ejecutar prueba cerrada (mínimo 12 testers durante 14 días continuos) usando el APK/Track interno antes de pasar a Producción.

### 2.2 Métricas a validar tras lanzar v1.2 (antes de publicar v1.3)
- **Estabilidad:** Crash-free users > 99.5 % en Play Console / Vitals.
- **Uso real por función (Firebase Analytics):** Propiedades `user_tier` y `app_theme`, y eventos `tasave_tab_opened`, `tasave_util_opened`, `tasave_rate_selected`, `tasave_calendar_opened`, `tasave_scanner_opened`, `tasave_plans_opened`, `tasave_ultra_cta_clicked` (`monthly`/`yearly`), `tasave_share_card` y `tasave_action` (incluyendo uso de voz y conversor flotante).
- **Monetización AdMob:** eCPM promedio del banner adaptativo, intersticial post-acción y rewarded (ratio de activación del pase 12h sin anuncios vs. +5 escaneos).
- **Retención y Conversión:** % de usuarios activos al día 7 (`D7`), pestañas/útiles más frecuentes y tasa de conversión a `Ultra` (`$0.99` vs `$7.99`).

---

## 3. Plan de Marketing, Redes y Contenido IA (Google Omni / Google Vids)

### 3.1 ASO (Posicionamiento en Google Play Store)
- **Título (máx. 30 caracteres):** `Tasa VE: Dólar BCV y Cobros`
- **Descripción corta (máx. 80 caracteres):** `Tasa BCV hoy/mañana, USDT, COP, voz manos libres, calendario SUDEBAN y cobros.`
- **Palabras clave prioritarias (Venezuela):** `tasa bcv hoy`, `dolar bcv`, `calculadora bolivar dolar`, `calculadora por voz dolar`, `peso colombiano bolivar`, `feriados bancarios sudeban`, `dividir cuenta bolivar`, `usdt paralelo`, `pago movil`, `vuelto mixto`.
- **Activos visuales recomendados (6 capturas en orden de impacto):**
  1. Conversor en vivo (BCV Hoy / Mañana + Euro BCV + USDT + chip `🇨🇴 COP` + Calendario + Micrófono por Voz).
  2. **Conversor por Voz Manos Libres** + **Conversor Flotante** sobre WhatsApp desde Barra Rápida / Notificación fija.
  3. Pestaña **Útiles** (`¿Con qué pago?` con descuento libre y `Dividir cuenta` en 5 monedas).
  4. Escáner de cámara OCR + Carrito inteligente con IVA (16 %) y Tarjeta PNG de presupuesto.
  5. Cobros / Pago Mixto con vuelto automático y Tarjeta PNG con logo propio (Ultra).
  6. Widget gratis Tasas en Vivo + Widget Ultra Calculadora y Cobro Express.

### 3.2 Motor Viral Integrado (Loop de Adquisición por WhatsApp)
Cada vez que un usuario comparte una **Tarjeta PNG**, un resumen de **Cobro / Pago Móvil** o un resultado de **Útiles** (`¿Con qué pago?`, `Dividir cuenta`) por WhatsApp, incluye formato limpio con enlaces oficiales (`https://tasave.net` y Google Play), mientras el pie de la tarjeta visual conserva `Generado con Tasa VE • https://tasave.net` incluso cuando un usuario Ultra personaliza la cabecera con su propio logo.
- **Efecto multiplicador:** quien recibe una tarjeta o resultado de cuenta dividida pregunta *"¿qué app usas?"* o instala directo.
- **Landing (`tasave.net`):** ya publicada en `docs/`; en cuanto Google Play apruebe la ficha pública, reemplazar el CTA de descarga de APK/lista de espera por el botón oficial de Google Play Store.

### 3.3 Estrategia de Contenido Corto en `@tasa.ve` (Reels, TikTok y Videos con IA — Google Omni / Vids)
- **Cuentas oficiales activas:**
  - **Instagram:** [`@tasa.ve`](https://www.instagram.com/tasa.ve/)
  - **TikTok:** [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)
- **Nota de producto y marketing:** No saturar la app con textos largos explicando trucos; el descubrimiento del **Conversor Flotante** y el **Conversor por Voz** se impulsa mediante **videos cortos visuales** (grabación real + piezas generadas con IA en **Google Omni / Google Vids**).
- **Guiones de videos verticales (15 a 25 segundos):**
  1. **"¡Háblale a tu calculadora en el súper o manejando!" (Conversor por Voz):** tocas el micrófono, dices *"38 con 50 dólares a BCV"* con las manos ocupadas → resultado en pantalla y por voz en 1 segundo (2 pruebas gratis/día, ilimitado en Ultra).
  2. **"Calcula montos de WhatsApp sin salir del chat" (Conversor Flotante Ultra):** copias el precio en WhatsApp, bajas la Barra Rápida o tocas *"Calc. rápida"* en la notificación fija → ventana flotante compacta detecta el monto y te deja copiar el resultado + tu Pago Móvil en 1 toque.
  3. **"¿Cambio USDT o pago en efectivo con descuento?"**: abre *Útiles > ¿Con qué pago?*, mete un descuento libre en % o \$ y demuestra en 5 segundos cuánto ahorras en dólares reales.
  4. **"Divide la cuenta con tus amigos en USD, EUR, Bs., USDT o COP"**: abre *Útiles > Dividir cuenta* y manda el monto por persona al grupo de WhatsApp.
  5. **"¿Mañana es lunes bancario o a cuánto amanece el BCV?"**: abre el **Calendario SUDEBAN** desde la barra superior + *BCV Hoy vs Mañana*.
  6. **"Cobra o convierte pesos en la frontera sin perderte"**: activa el chip **`🇨🇴 COP`** y convierte directo pesos ⇄ bolívares.
  7. **"No pierdas dinero con el vuelto en Pago Móvil"**: muestra *Pago Mixto* calculando \$20 en efectivo + resto en Bs. + *Tarjeta PNG* con logo propio (Ultra).
  8. **"Escanea precios en el súper sin sacar la cuenta a mano"**: apunta la cámara al hablador y suma al carrito inteligente.

---

## 4. Blueprint Técnico — Fase 2: Tasa VE Business (`v1.3`)

Cuando `v1.2` esté estable en Play Store, `v1.3` separará visualmente los dos niveles pagos y habilitará el **Panel Business** sin ensuciar la interfaz principal:

### 4.1 Cambios de Identidad y Billing
- **Ultra pasa a Verde Esmeralda (`#10B981`)** en badges, tarjetas de suscripción y comparativa de planes.
- **Business adopta el Ámbar/Dorado (`#F59E0B`)** con badge superior `BUSINESS` y acceso al **Panel Business** (botón en barra superior o pestaña dedicada solo visible/resaltada para modo negocio).
- **Nuevos IDs de Google Play Billing:** `tasave_business_monthly` y `tasave_business_yearly` (resolviendo `isUltraUser = hasUltra || hasBusiness` para que Business herede todo Ultra automáticamente).

### 4.2 Cuatro Módulos del Panel Business (para pulir antes de lanzar)
1. **Catálogo Rápido / Modo Caja:**
   - Base local (Room) de productos/servicios frecuentes (nombre, categoría, precio USD/USDT, stock opcional).
   - Tocar un ítem lo suma al instante al cobro activo; al cobrar, permite registrar la venta con 1 botón.
2. **Presupuesto / Nota de Entrega PDF + IGTF Mixto Real:**
   - Cálculo exacto de **IGTF (3 %) solo sobre la fracción pagada en efectivo divisas o Zelle**, dejando exenta la parte pagada en Bolívares (Pago Móvil / Transferencia / Punto).
   - Generación local (`android.graphics.pdf.PdfDocument`) de Nota de Entrega / Presupuesto PDF con nombre del comercio, RIF/CI, logo opcional, tasa BCV del día y datos bancarios para compartir por WhatsApp.
3. **Cierre de Caja Diario:**
   - Registro diario de ventas cobradas, totalizado por método: Efectivo USD, Pago Móvil / Bs., Zelle y Binance Pay.
   - Resumen exportable en **CSV** o mensaje formateado de WhatsApp al finalizar la jornada.
4. **Cuentas por Cobrar ("Fiados") + Reposición por Brecha:**
   - Lista de clientes con saldo pendiente indexado a USD BCV o USDT (recalcula automáticamente la deuda en Bs. cada día) y botón **"Enviar recordatorio de pago"** (Tarjeta PNG + WhatsApp).
   - **Calculadora de Reposición:** ingresas costo en USDT + margen deseado → sugiere el precio de venta en **USD BCV** y **Bs.** compensando la brecha cambiaria actual para evitar descapitalización al reponer inventario.

---

## 5. Hoja de Google Sheets para Reportes de Errores y Sugerencias

- **Hoja oficial creada:** [`Tasa VE — Reportes y Sugerencias`](https://docs.google.com/spreadsheets/d/1TALv6ZVhZAmgLjN3CHny4HaOkRqlQn8Zpat-djiKQGo/edit) (ID: `1TALv6ZVhZAmgLjN3CHny4HaOkRqlQn8Zpat-djiKQGo`).
- **Pestañas:**
  - `Reportes`: columnas `Fecha y Hora`, `Tipo` (`Error`/`Sugerencia`), `Pantalla / Módulo`, `Descripción`, `Contacto (Opcional)`, `Versión App`, `Plan`, `Dispositivo / Android` y `Estado Validación` (`Pendiente`, `En revisión`, `En desarrollo`, `Implementado`, `Descartado`).
  - `Resumen`: métricas en tiempo real con fórmulas automáticas.

### Activar envío directo desde la app (Google Apps Script — 1 minuto)
1. Abre la hoja y entra a **Extensiones → Apps Script**.
2. Pega el siguiente código y guarda:
   ```javascript
   function doPost(e) {
     const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("Reportes");
     const data = JSON.parse(e.postData.contents || "{}");
     sheet.appendRow([
       data.timestamp || new Date().toISOString(),
       data.type || "Sugerencia",
       data.module || "Otro",
       data.description || "",
       data.contact || "",
       data.appVersion || "",
       data.plan || "Gratis",
       data.device || "",
       data.status || "Pendiente"
     ]);
     return ContentService
       .createTextOutput(JSON.stringify({ ok: true }))
       .setMimeType(ContentService.MimeType.JSON);
   }
   ```
3. Haz clic en **Implementar → Nueva implementación → Tipo: Aplicación web**, selecciona **Ejecutar como: Yo** y **Quién tiene acceso: Cualquier persona**.
4. Copia la URL `/exec` generada y pégala en `app/src/main/res/values/strings.xml` dentro de `feedback_webhook_url` *(mientras esté vacío, el botón abre automáticamente el correo prellenado a `soporte@tasave.net` como respaldo)*.

