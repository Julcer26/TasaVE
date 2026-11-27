# Guía Definitiva para Publicar Tasa VE en Google Play Store 🚀

Esta guía contiene todos los pasos, configuraciones técnicas, requerimientos legales y directrices para publicar exitosamente **Tasa VE** en la tienda oficial de Google Play.

---

## 📋 Resumen de Cambios Técnicos Aplicados en el Código

1. **Nuevo Identificador de Paquete Profesional:**
   - Anterior: `com.example` / `com.aistudio.tasave.qzwvly` *(Rechazado automáticamente por Google Play)*.
   - Actual: **`com.julcer26.tasave`** (Válido y profesional para Play Store).
2. **Integración de Anuncios Google AdMob:**
   - Dependencia: `com.google.android.gms:play-services-ads:23.6.0`.
   - Componente: `TopBannerAd` con etiqueta obligatoria de divulgación publicitaria `"PUBLICIDAD"` para evitar infracciones de clics involuntarios de Google Play.
   - Activada configuración de pruebas (`ca-app-pub-3940256099942544...`) para evitar suspensiones de cuenta AdMob durante el desarrollo.
3. **Integración de Google Play Billing (Suscripción Tasa VE Ultra):**
   - Dependencia: `com.android.billingclient:billing-ktx:7.1.1`.
   - Productos configurados: `tasave_ultra_monthly` (`$0.99 USD/mes`) y `tasave_ultra_yearly` (`$7.99 USD/año`).
   - Funciones: Compra oficial, reconocimiento de suscripción (`acknowledge`), persistencia y botón de **Restaurar suscripción**.
   - Al activar **Tasa VE Ultra**, desaparece toda la publicidad y se desbloquean Notificación fija, Conversor Flotante multi-tasa + Pago Móvil, OCR ilimitado, Carrito ilimitado + IVA/IGTF, perfiles ilimitados, logo propio en Tarjeta PNG y Widget Ultra.
4. **Historial de Conversiones Completo:**
   - Integrado en la pantalla principal con base de datos SQLite Room local, botón "Guardar cálculo" y diálogo "Historial de Conversiones".
5. **Corrección de Errores de Fondo:**
   - Corrección en el parseo de fechas ISO con zona horaria (`2026-11-10T00:00:00-04:00`).
   - Tasas de respaldo (fallback) actualizadas a cifras realistas.
   - Soporte bidireccional en el guardado de historial (VES a Divisa y Divisa a VES).
   - Soporte de copia directa al portapapeles al pulsar las tarjetas de cotización.
6. **Documentación Legal y Cumplimiento:**
   - Política de Privacidad completa en Markdown (`PRIVACY_POLICY.md`) y HTML listo para GitHub Pages (`docs/privacy-policy.html`).
   - Términos de Servicio y Descargo de Responsabilidad Financiera (`TERMS_OF_SERVICE.md`).

---

## 🛠️ PASO 1: Habilitar la Política de Privacidad en GitHub Pages (Gratis)

Google Play exige que la URL de la Política de Privacidad esté activa públicamente:

1. Ve a tu repositorio en GitHub: `https://github.com/Julcer26/TasaVE`.
2. Haz clic en **Settings** (Configuración) > pestaña **Pages** en el menú izquierdo.
3. En **Build and deployment**:
   - Source: **Deploy from a branch**.
   - Branch: selecciona **main** y en la carpeta elige **/docs**.
   - Haz clic en **Save**.
4. En 2 minutos tu URL pública estará lista en:
   ```
   https://julcer26.github.io/TasaVE/privacy-policy.html
   ```
   *(Esta es la URL que pegarás en Google Play Console en el apartado de Política de Privacidad).*

---

## 🔑 PASO 2: Configurar Google AdMob (Publicidad)

Para comenzar a generar ingresos con la barra publicitaria superior:

1. Ingresa en [Google AdMob](https://admob.google.com/) con tu cuenta de Google.
2. Haz clic en **Apps** > **Añadir app**:
   - Plataforma: **Android**.
   - ¿La app está en una tienda de aplicaciones compatible?: Marca **No** (la vincularás después de publicarla).
   - Nombre de la app: **Tasa VE**.
3. Obtendrás tu **App ID de AdMob**, que tiene este formato:
   `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`
4. Crea un bloque de anuncios:
   - Tipo de formato: **Banner**.
   - Nombre del bloque: **Banner Superior Tasa VE**.
   - Obtendrás tu **Banner Ad Unit ID**, que tiene este formato:
     `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`
5. **Coloca tus IDs reales en el código:**
   - En `app/src/main/res/values/strings.xml`:
     ```xml
     <string name="admob_app_id">ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY</string>
     <string name="admob_banner_ad_unit_id">ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ</string>
     ```
   - En `app/src/main/AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.gms.ads.APPLICATION_ID"
         android:value="@string/admob_app_id" />
     ```
   - En `app/src/main/java/com/julcer26/tasave/ads/TopBannerAd.kt`:
     Sustituye el ID por defecto por tu Unit ID o pásalo desde `stringResource(R.string.admob_banner_ad_unit_id)`.

---

## 💳 PASO 3: Configurar Suscripción Tasa VE Ultra + Firebase Analytics

### A. Suscripciones en Google Play Console (`tasave_ultra`)
1. En tu aplicación dentro de [Google Play Console](https://play.google.com/console/), ve a **Monetizar con Play** > **Suscripciones** (`tasave_ultra`).
2. Crea los planes base vigentes:
   - **Mensual (`ultra-monthly`):** `$0.99 USD`.
   - **Anual (`ultra-yearly`):** `$7.99 USD`.
3. Mantén el ID de producto `tasave_ultra` activo y verifica la prueba cerrada con cuentas de licencia de prueba.

### B. Firebase Analytics (Seguimiento Anónimo de Uso — ACTIVO)
El proyecto Firebase **`tasa-ve-julcer26`** ya está configurado con ambas apps (`com.julcer26.tasave` para Play Store y `com.julcer26.tasave.ultra` para APK de pruebas) y el Secret encriptado `GOOGLE_SERVICES_JSON` inyectado automáticamente en GitHub Actions:
1. `AnalyticsTracker` respeta el interruptor opt-out en *Configuración > Legal y Privacidad*.
2. Eventos anónimos configurados (sin montos ni datos personales):
   - `tasave_tab_opened` (`tab_name`: Conversor / Carrito / Útiles / Cobros)
   - `tasave_util_opened` (`tool_name`: `compare_payment`, `split_bill`, `add_quick_tile`)
   - `tasave_rate_selected` (`rate_key`: `USD_BCV`, `EUR_BCV`, `USDT`, `COP`)
   - `tasave_calendar_opened` y `tasave_calendar_day_selected` (`is_bank_holiday`)
   - `tasave_scanner_opened` (`source`: `topbar` / `cart`)
   - `tasave_action` (`persistent_notif_enabled`, `quick_calc_opened`, `quick_tile_ultra`, `quick_copy_with_pm`)

---

## 🔒 PASO 4: Generar la Clave de Firma (Keystore) y Compilar el Bundle (.aab)

Google Play Store exige que las aplicaciones nuevas se suban en formato **Android App Bundle (`.aab`)** firmadas con tu clave privada de desarrollador.

### 1. Genera tu Keystore de producción con este comando:
Abre una terminal en la raíz del proyecto y ejecuta:
```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload
```
*Te pedirá una contraseña (por ejemplo `mi_clave_segura_2026`) y datos de tu organización o nombre.*
> ⚠️ **IMPORTANTE:** Guarda este archivo `my-upload-key.jks` y su contraseña en un lugar seguro (Drive/disco externo). Si pierdes esta clave, no podrás volver a actualizar la app en Play Store.

### 2. Compilar el archivo .aab de producción:
Ejecuta en tu terminal:
```bash
STORE_PASSWORD="TU_PASSWORD" KEY_PASSWORD="TU_PASSWORD" ./gradlew bundleRelease
```
El archivo de producción listo para subir a Google Play se generará en:
```
app/build/outputs/bundle/release/app-release.aab
```

---

## 🛡️ PASO 5: Sección "Seguridad de los Datos" (Data Safety) en Play Console

Google Play exige rellenar un cuestionario sobre cómo se manejan los datos. Rellénalo con las siguientes respuestas exactas para reflejar AdMob, Play Billing y Firebase Analytics:

1. **¿Tu aplicación recopila o comparte alguno de los tipos de datos de usuario obligatorios?**
   - Respuesta: **Sí** (Debido a las librerías oficiales de Google AdMob, Google Play Billing y Firebase Analytics).
2. **¿Se cifran en tránsito todos los datos de usuario que recopila la aplicación?**
   - Respuesta: **Sí** (Todas las conexiones usan HTTPS/TLS).
3. **¿Ofreces a los usuarios una forma de solicitar que se eliminen sus datos?**
   - Respuesta: **Sí** (Los usuarios pueden desactivar Firebase Analytics en *Configuración > Legal y Privacidad*, y borrar historial, carrito o perfiles desde la app o Ajustes de Android).
4. **Detalle de Datos Específicos:**
   - **Actividad en la aplicación (Interacciones con la aplicación):**
     - Recopilados: Sí (vía Firebase Analytics: solo nombres de pestañas/herramientas abiertas, nunca montos ni datos personales).
     - Compartidos: No se venden ni se comparten con anunciantes externos.
     - ¿Es obligatorio o el usuario puede elegir?: **El usuario puede elegir** (Opt-out integrado en *Configuración > Estadísticas anónimas de uso*).
     - Finalidad: **Estadísticas / Análisis** y mejora de funciones.
   - **Identificadores del dispositivo u otros IDs (Device or other IDs):**
     - Recopilados: Sí (por Google Mobile Ads y Firebase Instance ID).
     - Compartidos: Sí (con Google para fines de publicidad en el plan gratuito y prevención de fraude).
     - ¿Es obligatorio o el usuario puede elegir?: Obligatorio en la versión gratuita con anuncios (y opcional/no publicitario en Ultra).
     - Finalidad: **Publicidad o marketing**, **Prevención del fraude y seguridad**, **Estadísticas**.
   - **Información financiera (Historial de compras / suscripciones):**
     - Recopilados: Sí (Gestionados exclusivamente por Google Play Billing para **Tasa VE Ultra** `$0.99/mes` o `$7.99/año`).
     - Finalidad: **Gestión de suscripciones** y **Servicios de la aplicación**.
   - **Ubicación aproximada:**
     - Sí (derivada por IP a nivel de país/región por AdMob y Firebase Analytics; sin GPS).
   - **Permisos sensibles declarados (sin almacenamiento en servidores):**
     - `CAMERA`: Uso opcional en primer plano para OCR local de etiquetas de precios (no guarda ni sube fotos).

---

## ⚖️ PASO 6: Clasificación de Contenido y Declaraciones Legales

1. **Clasificación de Contenido (IARC):**
   - Completa el cuestionario seleccionando categoría **Utilidad / Herramienta / Finanzas**.
   - Indica que **NO** contiene violencia, lenguaje vulgar, ni apuestas con dinero real.
   - Resultado esperado: Apta para todo público (**PEGI 3** / **Everyone**).
2. **Aplicaciones Financieras / Criptomonedas:**
   - Si Google pregunta si la app ofrece préstamos, negociación bursátil directa o intercambio de criptoactivos: marca **NO**.
   - Declara: *"Tasa VE es una calculadora y herramienta de conversión referencial basada en cotizaciones públicas del Banco Central de Venezuela, TRM Colombia y promedios de mercado. No realiza transacciones financieras, custodia ni intermediación bancaria."*
3. **Público Objetivo:**
   - Selecciona **Mayores de 18 años** (o mayores de 13 años). No incluyas niños menores de 13 años en el público objetivo para evitar requisitos estrictos de la política familiar de Google.

---

## 🎨 PASO 7: Material Gráfico para la Ficha de Play Store

Prepara los siguientes recursos gráficos para la ficha de la tienda:

1. **Icono de la aplicación:**
   - Tamaño: `512 x 512 px` (Formato PNG de 32 bits con fondo transparente o sólido, máx. 1 MB).
2. **Gráfico de funciones (Feature Graphic):**
   - Tamaño: `1024 x 500 px` (Formato JPG o PNG de 24 bits, sin transparencia).
3. **Capturas de pantalla (Screenshots) y Videos Cortos (Reels / TikTok / Google Omni / Vids):**
   - Mínimo 6 capturas en resolución de teléfono (`1080 x 2400 px`):
     - Captura 1: Conversor BCV Hoy/Mañana, Euro, USDT, chip 🇨🇴 COP + botones Calendario y Cámara OCR.
     - Captura 2: **Conversor Flotante** sobre WhatsApp desde Barra Rápida / Notificación fija con Pago Móvil en 1 toque.
     - Captura 3: Pestaña **Útiles** (`¿Con qué pago?` con descuento libre y `Dividir cuenta` en 5 monedas).
     - Captura 4: Escáner de cámara OCR + Carrito Inteligente con IVA/IGTF y Tarjeta PNG.
     - Captura 5: Cobros (Pago Mixto, Tarjeta PNG con logo propio Ultra y WhatsApp).
     - Captura 6: Widget Tasas en Vivo (Gratis) + Widget Calculadora y Cobros (Ultra).
   - *Estrategia de difusión:* El flujo del **Conversor Flotante** y los trucos de **Útiles** se muestran en videos verticales cortos para Reels, TikTok (`@tasa.ve`) y piezas con **Google Omni / Google Vids**, manteniendo la app limpia y sin tarjetas largas de instrucciones.
4. **Textos para la Ficha:**
   - **Título de la app (máx. 30 caracteres):** `Tasa VE: Dólar BCV y Cobros`
   - **Descripción breve (máx. 80 caracteres):** `Tasa BCV hoy/mañana, USDT, COP frontera, calendario bancario, útiles y cobros.`
   - **Descripción completa:**
     ```
     Tasa VE es la calculadora financiera todo-en-uno para tu día a día en Venezuela.

     Consulta al instante el Dólar y Euro BCV (hoy y mañana), USDT Binance P2P y Peso Colombiano (COP) de frontera, incluso en modo offline.

     Características principales:
     • Conversor bidireccional exacto (BCV Hoy/Mañana, Euro, USDT y COP frontera).
     • Calendario Bancario: identifica lunes bancarios, feriados y tasas históricas por día.
     • Accesos Rápidos: botón en Barra Rápida, notificación fija silenciosa de tasas y Conversor Flotante con Pago Móvil en 1 toque (Ultra).
     • Pestaña Útiles: ¿Con qué pago? (USDT vs Efectivo/$ con descuento libre) y Dividir cuenta en 5 monedas.
     • Carrito inteligente con escáner de cámara OCR local e IVA/IGTF referencial.
     • Módulo Cobros: Pago Mixto, perfiles locales de Pago Móvil/Zelle/Binance y Tarjeta PNG con logo propio (Ultra) lista para WhatsApp.
     • Widgets en pantalla de inicio y Plan Ultra opcional sin anuncios ($0.99/mes o $7.99/año).

     Nota: Esta herramienta tiene carácter estrictamente informativo y de cálculo matemático referencial.
     ```

---

¡Siguiendo estos pasos tu aplicación cumplirá al 100% las normativas técnicas, comerciales y legales de Google Play Store!
