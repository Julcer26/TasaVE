# Roadmap de Despliegue, Marketing y Tasa VE Business

**Última actualización:** 19 de noviembre de 2026  
**Rama de desarrollo actual:** `feat/v1.2-ultra`  
**Dominio oficial:** `https://tasave.net` • **Soporte:** `soporte@tasave.net`  
**Redes oficiales:** Instagram [`@tasa.ve`](https://www.instagram.com/tasa.ve/) • TikTok [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)

---

## 1. Arquitectura de Planes (Estrategia en Dos Fases)

Para evitar confusión entre planes y no sobrecargar la interfaz principal para el usuario casual, la evolución comercial de **Tasa VE** se divide en dos versiones claras:

| Nivel | Versión | Color Identidad | Precio Sugerido (Google Play) | Público Objetivo | Qué Incluye |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Gratis** | v1.2 | Neutro (Oscuro / Claro) | \$0 USD (AdMob equilibrado) | Uso ocasional / consulta rápida | Conversor 4 decimales, BCV Hoy/Mañana, USDT paralelo, Brecha y Gráficos, Pago Mixto + Tarjeta PNG, hasta 5 productos en carrito, 1 perfil de cobro, 3 escaneos OCR/día (+5 o pase 12h sin anuncios vía video bonificado). |
| **Tasa VE Ultra** | v1.2 *(pasa a Verde en v1.3)* | **Verde Esmeralda (`#10B981`)** | **\$0.99 / mes** o **\$7.99 / año** | Usuario diario, compradores frecuentes, freelancers | **0 publicidad**, escáner OCR ∞, carrito ∞ + IVA/IGTF simple, perfiles de cobro ∞, **logo propio en Tarjeta PNG de cobro**, Alertas locales de tasa y Widget de pantalla de inicio. |
| **Tasa VE Business** | v1.3 (Fase 2) | **Ámbar / Dorado (`#F59E0B`)** | **\$2.99 – \$3.99 / mes** o **\$24.99 – \$29.99 / año** *(7 días gratis)* | Bodegas, comercios, tiendas Instagram, delivery, servicios | **Todo Ultra incluido** + **Panel Business dedicado** (Catálogo rápido, PDF Nota de Entrega + IGTF Mixto Real, Cierre de Caja Diario, Fiados indexados y Reposición por Brecha). |

> **Regla de oro:** *Business* nunca debe cobrarse como un "extra aparte sobre Ultra". En Google Play Billing se ofertan como planes independientes donde *Business* desbloquea automáticamente todos los beneficios de *Ultra*.

---

## 2. Plan de Despliegue Técnico — Fase 1 (v1.2 en Google Play Store)

### 2.1 Checklist obligatorio antes de compilar el `.aab` de producción
1. **Unidades reales de AdMob (`app/src/main/res/values/strings.xml`):**
   - `admob_app_id`: `ca-app-pub-1192254236776922~3391721063` *(configurado)*.
   - `admob_banner_ad_unit_id`: `ca-app-pub-1192254236776922/4135771180` *(configurado)*.
   - **Pendiente (tras superar prueba cerrada):** Crear en la consola de AdMob una unidad **Intersticial** y una **Bonificada (Rewarded)** y pegar sus IDs en `admob_interstitial_ad_unit_id` y `admob_rewarded_ad_unit_id`.
   - Verificar que `https://tasave.net/app-ads.txt` esté publicado y accesible *(ya activo)*.
2. **Suscripciones en Google Play Console:**
   - Crear producto de suscripción **`tasave_ultra_monthly`** con plan base mensual de **\$0.99 USD** (oferta opcional: 3 o 7 días de prueba gratis).
   - Crear producto de suscripción **`tasave_ultra_yearly`** con plan base anual de **\$7.99 USD** (~33 % de ahorro).
3. **Firma y empaquetado Release:**
   - Generar keystore privado (`.jks`) y resguardar copia cifrada fuera del repositorio.
   - Compilar bundle firmado con `./gradlew bundleRelease` (`minifyEnabled = true` / ProGuard).
4. **Ficha de tienda y cumplimiento Play Console:**
   - URL de Política de Privacidad: `https://tasave.net/privacy-policy.html`.
   - Correo de contacto en la ficha: `soporte@tasave.net`.
   - Sitio web oficial: `https://tasave.net`.
   - Formulario **Seguridad de Datos (Data Safety):** declarar que la app no almacena datos en servidores propios y que Google Mobile Ads + ML Kit + Play Billing procesan identificadores técnicos / diagnósticos mínimos en tránsito cifrado.
   - Permiso `CAMERA`: justificar uso exclusivo local en primer plano para OCR de precios (sin captura/almacenamiento ni subida de fotos).
   - Si la cuenta de Play Console es personal nueva: ejecutar prueba cerrada (mínimo 12 testers durante 14 días continuos) usando el APK/Track interno antes de pasar a Producción.

### 2.2 Métricas a validar tras lanzar v1.2 (antes de publicar v1.3)
- **Estabilidad:** Crash-free users > 99.5 % en Play Console / Vitals.
- **Monetización AdMob:** eCPM promedio del banner adaptativo, intersticial post-acción y rewarded (ratio de activación del pase 12h sin anuncios vs. +5 escaneos).
- **Retención y Conversión:** % de usuarios activos al día 7 (`D7`), pestañas más usadas (`Carrito`, `Cobros`, `Brecha`) y tasa de conversión a `Ultra` (`$0.99` vs `$7.99`).

---

## 3. Plan de Marketing y Crecimiento Orgánico

### 3.1 ASO (Posicionamiento en Google Play Store)
- **Título (máx. 30 caracteres):** `Tasa VE: Dólar BCV y Cobros`
- **Descripción corta (máx. 80 caracteres):** `Tasa BCV hoy/mañana, USDT, escáner OCR, carrito IVA/IGTF y cobros Pago Móvil.`
- **Palabras clave prioritarias (Venezuela):** `tasa bcv hoy`, `dolar bcv`, `calculadora bolivar dolar`, `usdt paralelo`, `pago movil`, `vuelto mixto`, `igtf`, `escaner de precios`.
- **Activos visuales recomendados (6 capturas en orden de impacto):**
  1. Calculadora en vivo (BCV Hoy / Mañana + Euro BCV + USDT en 4 decimales).
  2. Escáner de cámara OCR leyendo una etiqueta real de supermercado.
  3. Carrito inteligente con IVA (16 %) y equivalencias.
  4. Cobros / Pago Mixto con vuelto automático y Tarjeta PNG lista para WhatsApp.
  5. Brecha cambiaria + gráficos históricos (`1D` a `Todo`).
  6. Widget de pantalla de inicio + Alertas de tasa.

### 3.2 Motor Viral Integrado (Loop de Adquisición por WhatsApp)
Cada vez que un usuario comparte una **Tarjeta PNG** o un resumen de **Cobro / Pago Móvil** por WhatsApp, incluye un formato limpio con ambos enlaces oficiales (`https://tasave.net` y `https://play.google.com/store/apps/details?id=com.julcer26.tasave`), mientras el pie de la tarjeta visual conserva `Generado con Tasa VE • https://tasave.net` incluso cuando un usuario Ultra personaliza la cabecera con su propio logo.
- **Efecto multiplicador:** quien recibe una tarjeta clara con el cálculo y los datos de pago tiende a preguntar *"¿qué app usas?"* o hacer clic directo para instalarla.
- **Landing (`tasave.net`):** ya publicada en `docs/`; en cuanto Google Play apruebe la ficha pública, reemplazar el CTA de descarga de APK/lista de espera por el botón oficial de Google Play Store.

### 3.3 Estrategia de Contenido Corto en `@tasa.ve` (Instagram Reels y TikTok)
- **Cuentas oficiales activas:**
  - **Instagram:** [`@tasa.ve`](https://www.instagram.com/tasa.ve/)
  - **TikTok:** [`@tasa.ve`](https://www.tiktok.com/@tasa.ve)
- **Guiones de videos verticales (15 a 25 segundos)** resolviendo problemas cotidianos reales en Venezuela (sin sobreproducción, grabando la pantalla real del teléfono):
  1. **"No pierdas dinero con el vuelto en Pago Móvil":** muestra *Pago Mixto* calculando \$20 en efectivo + resto en Bs. en 5 segundos.
  2. **"Escanea el precio en el súper sin sacar la cuenta a mano":** apunta la cámara a un producto en \$ y ve el total instantáneo en Bs. BCV.
  3. **"Mira la tasa BCV de mañana antes que los demás":** muestra el selector *BCV Hoy vs Mañana* y las alertas automáticas.
  4. **"Cobra por WhatsApp con el logo de tu negocio":** genera la *Tarjeta PNG personalizada* (Ultra) con tus datos de Pago Móvil/Zelle/Binance en 1 toque.
  5. **Canales y alianzas micro:** grupos de emprendedores, tiendas de Instagram, repartidores/delivery y bodegas locales regalando códigos/pases promocionales en lanzamientos.

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
