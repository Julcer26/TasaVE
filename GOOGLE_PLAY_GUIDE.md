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
3. **Integración de Google Play Billing (IAP $2.99 USD):**
   - Dependencia: `com.android.billingclient:billing-ktx:7.1.1`.
   - Producto configurado: `tasave_pro_remove_ads` (Compra única / In-App Product).
   - Funciones: Compra oficial, reconocimiento de compra (`acknowledge`), persistencia y botón de **Restaurar Compras**.
   - Al comprar la versión Pro, el banner de publicidad superior desaparece automáticamente y se muestra la insignia dorada **PRO**.
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

## 💳 PASO 3: Configurar el Producto de $2.99 USD en Google Play Console

Para habilitar la compra de eliminación de anuncios:

1. En tu aplicación dentro de [Google Play Console](https://play.google.com/console/):
2. Ve al menú lateral: **Monetizar con Play** > **Productos integrados en la aplicación** (In-app products).
3. Haz clic en **Crear producto**:
   - **ID de producto:** `tasave_pro_remove_ads` *(Debe ser exactamente este ID, ya que está configurado en el código)*.
   - **Nombre:** `Tasa VE Pro — Sin Publicidad`.
   - **Descripción:** `Elimina permanentemente los anuncios publicitarios en Tasa VE mediante un pago único de por vida.`
   - **Estado:** Activo.
   - **Precio:** Configura el precio base en **$2.99 USD** (Google Play calculará automáticamente la conversión de divisas en los países admitidos).
4. Guarda y activa el producto.

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

Google Play exige rellenar un cuestionario sobre cómo se manejan los datos. Rellénalo con las siguientes respuestas exactas para evitar revisiones rechazadas:

1. **¿Tu aplicación recopila o comparte alguno de los tipos de datos de usuario obligatorios?**
   - Respuesta: **Sí** (Debido a las librerías oficiales de Google AdMob y Google Play Billing).
2. **¿Se cifran en tránsito todos los datos de usuario que recopila la aplicación?**
   - Respuesta: **Sí** (Todas las conexiones usan HTTPS).
3. **¿Ofreces a los usuarios una forma de solicitar que se eliminen sus datos?**
   - Respuesta: **Sí** (Los usuarios pueden borrar el historial de cálculos directamente desde la app o borrando los datos de la app).
4. **Detalle de Datos Específicos:**
   - **Identificadores del dispositivo u otros IDs (Device or other IDs):**
     - Recopilados: Sí (por Google Mobile Ads).
     - Compartidos: Sí (con Google para fines de publicidad y análisis).
     - Procesamiento efímero: No.
     - ¿Es obligatorio o el usuario puede elegir?: Obligatorio para la versión gratuita con anuncios.
     - Finalidad: **Publicidad o marketing**, **Prevención del fraude y seguridad**, **Estadísticas**.
   - **Información financiera (Datos de compras):**
     - Recopilados: Sí (Gestionados por Google Play Billing para la compra de $2.99 USD).
     - Finalidad: **Gestión de la cuenta** y **Servicios de la aplicación**.
   - **Ubicación aproximada:**
     - Sí (recopilada automáticamente por AdMob a nivel de país para servir anuncios pertinentes).

---

## ⚖️ PASO 6: Clasificación de Contenido y Declaraciones Legales

1. **Clasificación de Contenido (IARC):**
   - Completa el cuestionario seleccionando categoría **Utilidad / Herramienta / Finanzas**.
   - Indica que **NO** contiene violencia, lenguaje vulgar, ni apuestas con dinero real.
   - Resultado esperado: Apta para todo público (**PEGI 3** / **Everyone**).
2. **Aplicaciones Financieras / Criptomonedas:**
   - Si Google pregunta si la app ofrece préstamos, negociación bursátil directa o intercambio de criptoactivos: marca **NO**.
   - Declara: *"Tasa VE es una calculadora y herramienta de conversión referencial basada en cotizaciones públicas del Banco Central de Venezuela y promedios de mercado. No realiza transacciones financieras, custodia ni intermediación bancaria."*
3. **Público Objetivo:**
   - Selecciona **Mayores de 18 años** (o mayores de 13 años). No incluyas niños menores de 13 años en el público objetivo para evitar requisitos estrictos de la política familiar de Google.

---

## 🎨 PASO 7: Material Gráfico para la Ficha de Play Store

Prepara los siguientes recursos gráficos para la ficha de la tienda:

1. **Icono de la aplicación:**
   - Tamaño: `512 x 512 px` (Formato PNG de 32 bits con fondo transparente o sólido, máx. 1 MB).
2. **Gráfico de funciones (Feature Graphic):**
   - Tamaño: `1024 x 500 px` (Formato JPG o PNG de 24 bits, sin transparencia).
3. **Capturas de pantalla (Screenshots):**
   - Mínimo 4 capturas en resolución de teléfono (por ejemplo `1080 x 2400 px`).
   - Sugerencia:
     - Captura 1: Pantalla principal con la calculadora y cotizaciones del BCV y USDT.
     - Captura 2: Live Multi-Rate Comparison (Equivalencias en tiempo real de USD, EUR y USDT).
     - Captura 3: Historial de conversiones inteligente.
     - Captura 4: Modo Oscuro vs Modo Claro.
4. **Textos para la Ficha:**
   - **Título de la app (máx. 30 caracteres):** `Tasa VE: Dólar BCV y USDT`
   - **Descripción breve (máx. 80 caracteres):** `Cotizador y calculadora de tasas de cambio en tiempo real para Venezuela.`
   - **Descripción completa:**
     ```
     Tasa VE es la calculadora y cotizadora de divisas más elegante, rápida y confiable para Venezuela.

     Consulta al instante el valor oficial del Dólar BCV, Euro BCV y USDT paralelo con un diseño moderno, fluido y soporte offline para calcular incluso sin conexión a Internet.

     Características principales:
     • Tasas oficiales del Banco Central de Venezuela (BCV) en tiempo real.
     • Cotización del Euro y USDT P2P de mercado.
     • Calculadora inteligente bidireccional (Bolívares a Divisas y Divisas a Bolívares).
     • Tabla comparativa de equivalencias en todas las monedas en simultáneo.
     • Historial de conversiones integrado con respaldo local.
     • Funcionamiento offline con memoria caché inteligente.
     • Interfaz moderna con modo oscuro y modo claro optimizados.
     • Opción Tasa VE Pro: desbloqueo de por vida para disfrutar de una experiencia 100% libre de publicidad.

     Nota: Esta herramienta tiene carácter estrictamente informativo y de cálculo matemático referencial.
     ```

---

¡Siguiendo estos pasos tu aplicación cumplirá al 100% las normativas técnicas, comerciales y legales de Google Play Store!
