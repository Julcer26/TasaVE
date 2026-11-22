# Roadmap de Despliegue, Marketing y Tasa VE Business

**Última actualización:** 21 de noviembre de 2026  
**Rama de desarrollo actual:** `feat/v1.2-ultra`  
**Dominio oficial:** `https://tasave.net` • **Soporte:** `soporte@tasave.net`  
**Redes oficiales:** Instagram [`@tasa.ve`](https://www.instagram.com/tasa.ve/) • TikTok [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)

---

## 1. Arquitectura de Planes (Estrategia en Dos Fases)

Para evitar confusión entre planes y no sobrecargar la interfaz principal para el usuario casual, la evolución comercial de **Tasa VE** se divide en dos versiones claras:

| Nivel | Versión | Color Identidad | Precio Sugerido (Google Play) | Público Objetivo | Qué Incluye |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Gratis** | v1.2 | Neutro (Oscuro / Claro) | \$0 USD (AdMob equilibrado) | Uso ocasional / consulta rápida | Conversor BCV exacto, BCV Hoy/Mañana, USDT paralelo, **Peso Colombiano (COP) de frontera**, **Calendario SUDEBAN**, pestaña **Útiles** (*¿Con qué pago?*, *Inicial y cuotas*, *Comisiones*, *Dividir cuenta*), Pago Mixto + Tarjeta PNG, **Widget Tasas en Vivo**, hasta 5 productos en carrito, 1 perfil de cobro, 3 escaneos OCR/día (+5 o pase 12h sin anuncios vía video bonificado). |
| **Tasa VE Ultra** | v1.2 *(pasa a Verde en v1.3)* | **Verde Esmeralda (`#10B981`)** | **\$0.99 / mes** o **\$7.99 / año** | Usuario diario, compradores frecuentes, freelancers | **0 publicidad**, escáner OCR ∞, carrito ∞ + IVA/IGTF simple, perfiles de cobro ∞, **guardado de ruta favorita en Comisiones**, **logo propio en Tarjeta PNG de cobro**, Alertas locales de tasa y **Widget Calculadora y Cobro Express**. |
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
2. **Firebase Analytics (Telemetría Anónima Opt-Out):**
   - Descargar `google-services.json` de Firebase Console para el paquete `com.julcer26.tasave` y copiarlo en `app/google-services.json`.
   - El plugin `com.google.gms.google-services` se activa automáticamente si el archivo existe y `AnalyticsTracker` respeta el interruptor *Estadísticas anónimas de uso* en *Configuración > Legal y Privacidad*.
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
   - Si la cuenta de Play Console es personal nueva: ejecutar prueba cerrada (mínimo 12 testers durante 14 días continuos) usando el APK/Track interno antes de pasar a Producción.

### 2.2 Métricas a validar tras lanzar v1.2 (antes de publicar v1.3)
- **Estabilidad:** Crash-free users > 99.5 % en Play Console / Vitals.
- **Uso real por función (Firebase Analytics):** Eventos `tasave_tab_opened` (`Conversor`, `Carrito`, `Útiles`, `Cobros`), `tasave_util_opened` (`compare_payment`, `installments`, `commissions`, `split_bill`), `tasave_rate_selected` (incluyendo `COP`) y `tasave_calendar_opened`.
- **Monetización AdMob:** eCPM promedio del banner adaptativo, intersticial post-acción y rewarded (ratio de activación del pase 12h sin anuncios vs. +5 escaneos).
- **Retención y Conversión:** % de usuarios activos al día 7 (`D7`), pestañas/útiles más frecuentes y tasa de conversión a `Ultra` (`$0.99` vs `$7.99`).

---

## 3. Plan de Marketing y Crecimiento Orgánico

### 3.1 ASO (Posicionamiento en Google Play Store)
- **Título (máx. 30 caracteres):** `Tasa VE: Dólar BCV y Cobros`
- **Descripción corta (máx. 80 caracteres):** `Tasa BCV hoy/mañana, USDT, COP frontera, calendario SUDEBAN, útiles y cobros.`
- **Palabras clave prioritarias (Venezuela):** `tasa bcv hoy`, `dolar bcv`, `calculadora bolivar dolar`, `peso colombiano bolivar`, `feriados bancarios sudeban`, `cashea cuotas`, `paypal comision venezuela`, `usdt paralelo`, `pago movil`, `vuelto mixto`.
- **Activos visuales recomendados (6 capturas en orden de impacto):**
  1. Conversor en vivo (BCV Hoy / Mañana + Euro BCV + USDT + chip `🇨🇴 COP` + botón Calendario).
  2. Calendario SUDEBAN marcando lunes bancarios, tasa del día y cálculo rápido.
  3. Pestaña **Útiles** (`¿Con qué pago?`, `Inicial y cuotas`, `Comisiones`, `Dividir cuenta`).
  4. Escáner de cámara OCR + Carrito inteligente con IVA (16 %) y equivalencias.
  5. Cobros / Pago Mixto con vuelto automático y Tarjeta PNG lista para WhatsApp.
  6. Widget gratis Tasas en Vivo + Widget Ultra Calculadora y Cobro Express.

### 3.2 Motor Viral Integrado (Loop de Adquisición por WhatsApp)
Cada vez que un usuario comparte una **Tarjeta PNG**, un resumen de **Cobro / Pago Móvil** o un resultado de **Útiles** (`¿Con qué pago?`, `Inicial y cuotas`, `Dividir cuenta`) por WhatsApp, incluye formato limpio con enlaces oficiales (`https://tasave.net` y Google Play), mientras el pie de la tarjeta visual conserva `Generado con Tasa VE • https://tasave.net` incluso cuando un usuario Ultra personaliza la cabecera con su propio logo.
- **Efecto multiplicador:** quien recibe una tarjeta o desglose de cuotas/cuenta dividida pregunta *"¿qué app usas?"* o instala directo.
- **Landing (`tasave.net`):** ya publicada en `docs/`; en cuanto Google Play apruebe la ficha pública, reemplazar el CTA de descarga de APK/lista de espera por el botón oficial de Google Play Store.

### 3.3 Estrategia de Contenido Corto en `@tasa.ve` (Instagram Reels y TikTok)
- **Cuentas oficiales activas:**
  - **Instagram:** [`@tasa.ve`](https://www.instagram.com/tasa.ve/)
  - **TikTok:** [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)
- **Guiones de videos verticales (15 a 25 segundos)** resolviendo problemas cotidianos reales en Venezuela (sin sobreproducción, grabando la pantalla real del teléfono):
  1. **"¿Cambio USDT o pago en efectivo/Zelle?"**: abre *Útiles > ¿Con qué pago?* y demuestra en 5 segundos cuánto ahorras en dólares reales.
  2. **"Calcula tu inicial y cuotas de Cashea/Krece en bolívares al instante"**: abre *Útiles > Inicial y cuotas* con tasa BCV y compártelo por WhatsApp.
  3. **"¿Mañana es lunes bancario o a cuánto amanece el BCV?"**: abre el **Calendario SUDEBAN** desde la barra superior + *BCV Hoy vs Mañana*.
  4. **"Cobra o convierte pesos en la frontera sin perderte"**: activa el chip **`🇨🇴 COP`** y convierte directo pesos ⇄ bolívares.
  5. **"No pierdas dinero con el vuelto en Pago Móvil"**: muestra *Pago Mixto* calculando \$20 en efectivo + resto en Bs. + *Tarjeta PNG* con logo propio (Ultra).
  6. **"Escanea precios en el súper y calcula comisiones PayPal/Zinli"**: demuestra el escáner OCR local y la calculadora *Comisiones* + *Dividir cuenta* con amigos.

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

