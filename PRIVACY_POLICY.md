# Política de Privacidad — Tasa VE

**Última actualización:** 18 de noviembre de 2026 (Versión 1.2.0)  
**Aplicación:** Tasa VE • **Sitio oficial:** https://tasave.net • **Contacto:** soporte@tasave.net

La aplicación móvil **Tasa VE** respeta y protege la privacidad de sus usuarios. Esta Política de Privacidad describe de forma transparente cómo se obtienen los datos de referencia, qué información se procesa localmente en su dispositivo, qué tecnologías de terceros intervienen y cuáles son sus derechos conforme a las políticas de Google Play.

---

## 1. Origen y actualización de los datos
- **Fuentes públicas de referencia:** Las cotizaciones mostradas en la aplicación (Dólar BCV hoy/mañana, Euro BCV hoy/mañana, USDT paralelo, Peso Colombiano COP/TRM y series históricas para gráficos de tendencia) se obtienen mediante consultas automatizadas a APIs públicas (`ve.dolarapi.com`, `rates.dolarvzla.com`, `co.dolarapi.com` y `open.er-api.com`), las cuales reflejan valores informativos del Banco Central de Venezuela (BCV), TRM Colombia y promedios de mercado.
- **Sin afiliación oficial:** Tasa VE es una herramienta independiente de cálculo matemático. **No está afiliada, autorizada, patrocinada ni vinculada** con el Banco Central de Venezuela (BCV), el SENIAT, la SUDEBAN, el Banco de la República de Colombia ni ninguna institución bancaria, billetera digital o casa de cambio.
- **Caché local:** Para permitir el funcionamiento sin conexión (*Modo Offline*) y actualizar el Widget de pantalla de inicio, la aplicación almacena localmente las últimas tasas consultadas junto con un historial técnico de variaciones.

---

## 2. Información que recopilamos

### A. Ausencia de recopilación en servidores propios
**Tasa VE NO recopila, no solicita ni almacena en servidores propios** cuentas de usuario, contraseñas, correos electrónicos, ubicaciones GPS ni datos personales de identidad. No se requiere registro para utilizar la aplicación.

### B. Datos almacenados exclusivamente de forma local en su dispositivo
Para ofrecer las herramientas de cálculo, carrito y cobros, la aplicación guarda en la memoria interna protegida (*sandbox*) de su teléfono Android (SQLite/Room y `SharedPreferences`):
1. **Historial de conversiones:** Montos calculados, divisas y fecha local.
2. **Carrito de compras y preferencias fiscales:** Productos agregados o editados, cantidades, precios unitarios y preferencia de cálculo referencial de IVA (16 %) e IGTF (3 %).
3. **Perfiles opcionales de cobro (Módulo Cobros — 1 gratis / ilimitados en Ultra):** Si el usuario decide guardar uno o más perfiles de **Pago Móvil**, **Cuenta Bancaria**, **Zelle** o **Binance Pay** para armar resúmenes de cobro, puede ingresar voluntariamente un alias, banco, Cédula/RIF, número de teléfono, número de cuenta (20 dígitos), nombre del titular o correo/ID digital.
   - Estos datos **permanecen únicamente en su dispositivo** y **nunca se transmiten a servidores del desarrollador**.
   - Se excluyen explícitamente del respaldo automático en la nube de Android (`android:fullBackupContent` / `dataExtractionRules`) para mayor resguardo.
4. **Caché temporal de tarjetas visuales (PNG):** Cuando el usuario genera una imagen para compartir un cálculo o resumen de cobro, el archivo PNG temporal se crea únicamente en el caché privado local (`shared_cards/`) expuesto de forma segura mediante `FileProvider`.
5. **Preferencias de interfaz y alertas:** Tema visual (Oscuro/Claro), saldo diario de escaneos gratuitos/bonificados, umbrales locales de notificación y estado de la suscripción Ultra.

---

## 3. Uso de la información
La información procesada local y técnicamente se utiliza exclusivamente para:
1. Realizar conversiones de divisas (incluyendo BCV Mañana, Peso Colombiano COP y ajustes porcentuales), cálculos estimados de IVA/IGTF, brecha cambiaria, vuelto en pagos mixtos y gráficos de tendencia.
2. Formatear resúmenes y tarjetas visuales PNG de cobro o cálculo que el usuario **elige explícitamente** copiar al portapapeles o compartir mediante otras aplicaciones (como WhatsApp).
3. Actualizar el Widget de pantalla de inicio y mostrar alertas locales de variación de tasa cuando el usuario las activa.
4. Mostrar publicidad no invasiva (banners y anuncios bonificados voluntarios) en la versión gratuita y validar el acceso a **Tasa VE Ultra**.

---

## 4. Enlaces externos y compartición nativa
- Cuando el usuario pulsa **"Enviar por WhatsApp"** o **"Tarjeta (IMG)"** en la calculadora o en el módulo Cobros, o abre enlaces de soporte/políticas/Google Play, Android transfiere la acción fuera de Tasa VE hacia la aplicación de destino elegida por el usuario, la cual se rige por sus propias políticas de privacidad. Las imágenes PNG generadas en `shared_cards/` nunca se suben a servidores del desarrollador.
- Las consultas a `ve.dolarapi.com`, `rates.dolarvzla.com`, `co.dolarapi.com` y `open.er-api.com` se realizan mediante HTTPS para descargar cotizaciones públicas, sin enviar datos de identidad ni perfiles de cobro.

---

## 5. Seguridad de los datos
- **Cifrado en tránsito:** Todas las consultas de red hacia APIs de tasas y servicios de Google se realizan exclusivamente mediante protocolo cifrado **HTTPS (SSL/TLS)**.
- **Procesamiento en el dispositivo:** El reconocimiento de precios por cámara (OCR), la generación de tarjetas PNG y la evaluación de alertas de tasas se ejecutan localmente en el teléfono.
- **Aislamiento y exclusión de backup en la nube:** Los datos locales residen en el almacenamiento privado de la app en Android y los perfiles opcionales de Pago Móvil, cuentas bancarias, Zelle y Binance Pay están excluidos de la copia de seguridad automática en la nube.
- **Control y borrado:** Puede eliminar en cualquier momento su historial, carrito o métodos de cobro dentro de la app, o borrar todos los datos desde *Ajustes de Android > Aplicaciones > Tasa VE > Almacenamiento*.

---

## 6. Publicidad, anuncios bonificados y suscripción Ultra
- **Versión gratuita (AdMob):** Incluye calculadora con 4 decimales, BCV Mañana, COP, ajustes %, Brecha y Tendencias, Pago Mixto, Tarjeta PNG, hasta **5 productos en carrito**, **1 perfil de cobro** y **3 escaneos OCR diarios**. Muestra banners publicitarios gestionados por Google Mobile Ads y ofrece la opción **voluntaria** de ver un anuncio bonificado (*Rewarded Ad*) para desbloquear **+5 escaneos adicionales** en el día.
- **Tasa VE Ultra (Suscripción mensual $0.99 USD / anual $7.99 USD):** Elimina todos los anuncios (banners y bonificados) y desbloquea de forma ilimitada todas las funciones: **escáner de cámara ilimitado**, **Carrito ilimitado + cálculo referencial de IVA/IGTF**, **perfiles de cobro ilimitados**, **Alertas locales de variación de tasa** y **Widget de pantalla de inicio**.
- **Renovación automática y cancelación:** Las suscripciones se renuevan automáticamente a través de **Google Play** al finalizar cada período salvo cancelación previa con al menos 24 horas de antelación desde *Google Play Store > Suscripciones*.
- **Seguridad de pagos:** Todas las transacciones son procesadas exclusivamente por **Google Play Billing**; el desarrollador nunca accede a tarjetas, cuentas ni credenciales financieras.

---

## 7. Permisos del sistema
Tasa VE solicita únicamente los permisos estrictamente necesarios:
1. **Cámara (`CAMERA`):** Opcional y solo en primer plano cuando el usuario abre el escáner de precios. El OCR se ejecuta localmente en el dispositivo; **no grabamos video ni almacenamos ni subimos fotografías a servidores**.
2. **Notificaciones (`POST_NOTIFICATIONS`):** Opcional en Android 13+ para usuarios que activen las alertas locales de variación de tasa.
3. **Internet y estado de red (`INTERNET`, `ACCESS_NETWORK_STATE`):** Necesarios para consultar cotizaciones públicas, mostrar anuncios en la versión gratuita y validar suscripciones en Google Play.
4. **Facturación de Google Play (`com.android.vending.BILLING`):** Necesario para gestionar la suscripción Tasa VE Ultra y la restauración de compras.

---

## 8. Uso de tecnologías de terceros
La aplicación integra los siguientes SDKs y servicios oficiales certificados:
- **Google Mobile Ads (AdMob):** Entrega de banners y anuncios bonificados opcionales en la versión gratuita y prevención de fraude publicitario; puede procesar el ID de publicidad de Android (GAID), dirección IP anonimizada y diagnósticos técnicos. ([Política de Google Ads](https://policies.google.com/technologies/ads))
- **Google Play Billing Library:** Procesamiento seguro de compras dentro de la app y suscripciones mediante tokens anónimos de confirmación. ([Términos de Google Play](https://play.google.com/intl/es_es/about/play-terms/))
- **Google ML Kit (Text Recognition):** Reconocimiento óptico de caracteres (OCR) ejecutado **100 % en el dispositivo** para leer precios con la cámara. El SDK puede intercambiar métricas técnicas mínimas de diagnóstico o descarga de modelos con Google.
- **APIs Públicas de Tasas (`ve.dolarapi.com`, `rates.dolarvzla.com`, `co.dolarapi.com`, `open.er-api.com`):** Fuentes públicas de consulta de cotizaciones e históricos de referencia.

---

## 9. Cambios en esta política
Podemos actualizar esta Política de Privacidad para reflejar mejoras técnicas, nuevas funcionalidades o requisitos normativos de Google Play. La fecha de *"Última actualización"* al inicio del documento indicará siempre la versión vigente, y los cambios relevantes se reflejarán tanto en esta página como dentro de la aplicación.

*(Privacidad de menores: Tasa VE no está dirigida a menores de 13 años —o 16 años en el EEE— y no recopila conscientemente datos de menores).*

---

## 10. Contacto
Si tiene preguntas, solicitudes de privacidad o soporte técnico, puede comunicarse directamente con el equipo de Tasa VE:
- **Correo electrónico:** soporte@tasave.net
- **Sitio web oficial:** https://tasave.net
