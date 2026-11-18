# Política de Privacidad — Tasa VE

**Última actualización:** 18 de noviembre de 2026 (Versión 1.2.0)  
**Aplicación:** Tasa VE (`com.julcer26.tasave`) • **Desarrollador:** Julcer26

La aplicación móvil **Tasa VE** respeta y protege la privacidad de sus usuarios. Esta Política de Privacidad describe de forma transparente cómo se obtienen los datos de referencia, qué información se procesa localmente en su dispositivo, qué tecnologías de terceros intervienen y cuáles son sus derechos conforme a las políticas de Google Play.

---

## 1. Origen y actualización de los datos
- **Fuentes públicas de referencia:** Las cotizaciones mostradas en la aplicación (Dólar BCV, Euro BCV, USDT paralelo y series históricas para gráficos de tendencia) se obtienen mediante consultas automatizadas a APIs públicas (`ve.dolarapi.com`), las cuales reflejan valores informativos del Banco Central de Venezuela (BCV) y promedios de mercado.
- **Sin afiliación oficial:** Tasa VE es una herramienta independiente de cálculo matemático. **No está afiliada, autorizada, patrocinada ni vinculada** con el Banco Central de Venezuela (BCV), el SENIAT, la SUDEBAN ni ninguna institución bancaria o casa de cambio.
- **Caché local:** Para permitir el funcionamiento sin conexión (*Modo Offline*) y actualizar el Widget de pantalla de inicio, la aplicación almacena localmente las últimas tasas consultadas junto con un historial técnico de variaciones.

---

## 2. Información que recopilamos

### A. Ausencia de recopilación en servidores propios
**Tasa VE NO recopila, no solicita ni almacena en servidores propios** cuentas de usuario, contraseñas, correos electrónicos, ubicaciones GPS ni datos personales de identidad. No se requiere registro para utilizar la aplicación.

### B. Datos almacenados exclusivamente de forma local en su dispositivo
Para ofrecer las herramientas de cálculo, carrito y cobros, la aplicación guarda en la memoria interna protegida (*sandbox*) de su teléfono Android (SQLite/Room y `SharedPreferences`):
1. **Historial de conversiones:** Montos calculados, divisas y fecha local.
2. **Carrito de compras y preferencias fiscales:** Productos agregados o editados, cantidades, precios unitarios y preferencia de cálculo referencial de IVA (16 %) e IGTF (3 %).
3. **Perfiles opcionales de cobro (Módulo Cobros — Ultra):** Si el usuario decide guardar uno o más perfiles de **Pago Móvil** o **Cuenta Bancaria** para armar resúmenes de cobro, puede ingresar voluntariamente un alias, banco, Cédula/RIF, número de teléfono, número de cuenta (20 dígitos) y nombre del titular.
   - Estos datos **permanecen únicamente en su dispositivo** y **nunca se transmiten a servidores del desarrollador**.
   - Se excluyen explícitamente del respaldo automático en la nube de Android (`android:fullBackupContent` / `dataExtractionRules`) para mayor resguardo.
4. **Preferencias de interfaz y alertas:** Tema visual (Oscuro/Claro), umbrales locales de notificación y estado de licencias Pro/Ultra.

---

## 3. Uso de la información
La información procesada local y técnicamente se utiliza exclusivamente para:
1. Realizar conversiones de divisas, cálculos estimados de IVA/IGTF, brecha cambiaria, vuelto en pagos mixtos y gráficos de tendencia.
2. Formatear resúmenes de cobro que el usuario **elige explícitamente** copiar al portapapeles o compartir mediante otras aplicaciones (como WhatsApp).
3. Actualizar el Widget de pantalla de inicio y mostrar alertas locales de variación de tasa cuando el usuario las activa.
4. Mostrar publicidad no invasiva en la versión gratuita y validar el acceso a **Tasa VE Pro** y **Tasa VE Ultra**.

---

## 4. Enlaces externos y compartición nativa
- Cuando el usuario pulsa **"Enviar por WhatsApp"** en el módulo Cobros, o abre enlaces de soporte/políticas/Google Play, Android transfiere la acción fuera de Tasa VE hacia la aplicación de destino elegida por el usuario, la cual se rige por sus propias políticas de privacidad.
- Las consultas a `ve.dolarapi.com` se realizan mediante HTTPS para descargar cotizaciones públicas, sin enviar datos de identidad ni perfiles de cobro.

---

## 5. Seguridad de los datos
- **Cifrado en tránsito:** Todas las consultas de red hacia APIs de tasas y servicios de Google se realizan exclusivamente mediante protocolo cifrado **HTTPS (SSL/TLS)**.
- **Procesamiento en el dispositivo:** El reconocimiento de precios por cámara (OCR) y la evaluación de alertas de tasas se ejecutan localmente en el teléfono.
- **Aislamiento y exclusión de backup en la nube:** Los datos locales residen en el almacenamiento privado de la app en Android y los perfiles opcionales de Pago Móvil y cuentas bancarias están excluidos de la copia de seguridad automática en la nube.
- **Control y borrado:** Puede eliminar en cualquier momento su historial, carrito o métodos de cobro dentro de la app, o borrar todos los datos desde *Ajustes de Android > Aplicaciones > Tasa VE > Almacenamiento*.

---

## 6. Publicidad y suscripciones
- **Versión gratuita (AdMob):** Muestra banners publicitarios gestionados por Google Mobile Ads conforme a las políticas de Google Play.
- **Tasa VE Pro ($2.99 USD pago único vitalicio):** Elimina permanentemente todos los anuncios y desbloquea el escáner de cámara ilimitado.
- **Tasa VE Ultra (Suscripción mensual $1.49 USD / anual $9.99 USD):** Incluye automáticamente todos los beneficios de **Pro** (cero anuncios y escáner ilimitado) mientras la suscripción permanezca activa, además del Carrito Inteligente, Monitor de Brecha y Tendencias, Alertas locales, Widget y Módulo Cobros.
- **Renovación automática y cancelación:** Las suscripciones se renuevan automáticamente a través de **Google Play** al finalizar cada período salvo cancelación previa con al menos 24 horas de antelación desde *Google Play Store > Suscripciones*. Si el usuario ya había adquirido **Pro** previamente y luego cancela **Ultra**, conserva su licencia Pro vitalicia.
- **Seguridad de pagos:** Todas las transacciones son procesadas exclusivamente por **Google Play Billing**; el desarrollador nunca accede a tarjetas, cuentas ni credenciales financieras.

---

## 7. Permisos del sistema
Tasa VE solicita únicamente los permisos estrictamente necesarios:
1. **Cámara (`CAMERA`):** Opcional y solo en primer plano cuando el usuario abre el escáner de precios. El OCR se ejecuta localmente en el dispositivo; **no grabamos video ni almacenamos ni enviamos fotografías a servidores**.
2. **Notificaciones (`POST_NOTIFICATIONS`):** Opcional en Android 13+ para usuarios que activen las alertas locales de variación de tasa.
3. **Internet y estado de red (`INTERNET`, `ACCESS_NETWORK_STATE`):** Necesarios para consultar cotizaciones públicas, mostrar anuncios en la versión gratuita y validar compras en Google Play.
4. **Facturación de Google Play (`com.android.vending.BILLING`):** Necesario para gestionar la compra única Pro, la suscripción Ultra y la restauración de compras.

---

## 8. Uso de tecnologías de terceros
La aplicación integra los siguientes SDKs y servicios oficiales certificados:
- **Google Mobile Ads (AdMob):** Entrega de banners en la versión gratuita y prevención de fraude publicitario; puede procesar el ID de publicidad de Android (GAID), dirección IP anonimizada y diagnósticos técnicos. ([Política de Google Ads](https://policies.google.com/technologies/ads))
- **Google Play Billing Library:** Procesamiento seguro de compras dentro de la app y suscripciones mediante tokens anónimos de confirmación. ([Términos de Google Play](https://play.google.com/intl/es_es/about/play-terms/))
- **Google ML Kit (Text Recognition):** Reconocimiento óptico de caracteres (OCR) ejecutado **100 % en el dispositivo** para leer precios con la cámara. El SDK puede intercambiar métricas técnicas mínimas de diagnóstico o descarga de modelos con Google.
- **DolarAPI (`ve.dolarapi.com`):** Fuente pública de consulta de cotizaciones e históricos de referencia.

---

## 9. Cambios en esta política
Podemos actualizar esta Política de Privacidad para reflejar mejoras técnicas, nuevas funcionalidades o requisitos normativos de Google Play. La fecha de *"Última actualización"* al inicio del documento indicará siempre la versión vigente, y los cambios relevantes se reflejarán tanto en esta página como dentro de la aplicación.

*(Privacidad de menores: Tasa VE no está dirigida a menores de 13 años —o 16 años en el EEE— y no recopila conscientemente datos de menores).*

---

## 10. Contacto
Si tiene preguntas, solicitudes de privacidad o soporte técnico, puede comunicarse directamente con el desarrollador:
- **Correo electrónico:** julioingdlop@gmail.com
- **Repositorio oficial:** https://github.com/Julcer26/TasaVE
