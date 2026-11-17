# Política de Privacidad — Tasa VE

**Última actualización:** 17 de noviembre de 2026 (Versión 1.2.0)

La aplicación móvil **Tasa VE** (en adelante, "la Aplicación", "nosotros" o "nuestro") respeta y protege la privacidad de sus usuarios. Esta Política de Privacidad describe cómo se recopila, utiliza y protege la información al interactuar con nuestra aplicación móvil distribuida a través de Google Play Store.

---

## 1. Responsable del Tratamiento
* **Aplicación:** Tasa VE
* **Identificador de Paquete:** `com.julcer26.tasave`
* **Contacto de soporte y privacidad:** julioingdlop@gmail.com
* **Desarrollador:** Julcer26

---

## 2. Información que recopila la Aplicación

### A. Información Personal Directa
**Tasa VE NO recopila, no solicita ni almacena en servidores propios información personal identificable (PII)** como nombres, correos electrónicos, contraseñas ni ubicaciones físicas del usuario. No se requiere registro de cuenta para utilizar la aplicación.

### B. Datos Almacenados Localmente en el Dispositivo
Para brindar una experiencia óptima y permitir el funcionamiento fuera de línea (*Modo Offline*), la aplicación almacena información de manera estrictamente local en la memoria de su dispositivo:
1. **Historial de Conversiones:** Cantidades convertidas, monedas seleccionadas y fecha de cálculo (mediante base de datos SQLite/Room local).
2. **Caché y Tendencia de Tasas de Cambio:** Últimos valores registrados del Dólar BCV, Euro BCV y USDT paralelo (incluyendo historial local de variaciones para gráficos y para el Widget de pantalla de inicio).
3. **Carrito de Compras y Preferencias Fiscales:** Productos agregados al carrito, precios unitarios y preferencia de cálculo de IVA (16%) / IGTF (3%).
4. **Datos Opcionales de Pago Móvil (Módulo Cobros):** Banco, cédula/RIF y teléfono ingresados voluntariamente por el usuario para generar resúmenes de cobro. Esta información **nunca se envía a nuestros servidores**; únicamente se copia al portapapeles o se comparte mediante el menú nativo de Android (por ejemplo, WhatsApp) cuando el usuario pulsa explícitamente "Copiar" o "Enviar por WhatsApp".
5. **Preferencias Visuales y de Alertas:** Configuración de tema (Oscuro/Claro), umbrales de alertas locales y estado de licencias Pro/Ultra.

*Estos datos permanecen exclusivamente en su dispositivo móvil y pueden ser borrados en cualquier momento dentro de la app o borrando los datos de la aplicación en los ajustes de Android.*

### C. Permisos del Dispositivo (Cámara y Notificaciones)
1. **Permiso de Cámara (`CAMERA`):** Se utiliza únicamente en primer plano cuando el usuario abre el escáner de precios. El reconocimiento óptico de caracteres (OCR) se ejecuta **100% dentro del dispositivo mediante Google ML Kit**; no se graban videos, no se almacenan fotografías y no se transmiten imágenes a servidores externos.
2. **Permiso de Notificaciones (`POST_NOTIFICATIONS`):** Opcional para usuarios que activen las *Alertas de Cambio de Tasa*. Un proceso local en segundo plano (`WorkManager`) compara las cotizaciones públicas y genera una notificación local en su teléfono si la variación supera el umbral elegido.

### D. Información Recopilada por Servicios de Terceros
La aplicación integra servicios oficiales de Google para su funcionamiento, publicidad y monetización:

#### 1. Google Mobile Ads (AdMob)
La versión gratuita de la aplicación muestra anuncios en formato de banner no invasivo. AdMob puede recopilar y procesar:
- Identificadores publicitarios del dispositivo (Google Advertising ID - GAID).
- Dirección IP (anonimizada para geolocalización aproximada de país/región).
- Datos técnicos de diagnóstico, modelo de dispositivo y operadora.
- Métricas de interacción con anuncios para prevención de fraudes y entrega de publicidad relevante.
- Política de Privacidad de Google: [https://policies.google.com/technologies/ads](https://policies.google.com/technologies/ads)

#### 2. Google Play In-App Billing (Facturación de Google Play)
Si el usuario adquiere **Tasa VE Pro ($2.99 USD pago único)** o se suscribe a **Tasa VE Ultra ($1.49 USD/mes o $9.99 USD/año)**:
- Las transacciones financieras y renovaciones de suscripciones son procesadas de forma 100% segura por Google Play.
- Nosotros nunca tenemos acceso a los números de tarjetas de crédito, cuentas bancarias ni credenciales de pago.
- Recibimos únicamente un token técnico y anónimo de confirmación emitido por Google Play para verificar el estado de la compra o suscripción activa.
- Términos del Servicio de Google Play: [https://play.google.com/intl/es_es/about/play-terms/](https://play.google.com/intl/es_es/about/play-terms/)

#### 3. API de Cotizaciones Públicas (DolarAPI / BCV)
La aplicación realiza peticiones HTTP a endpoints públicos de consulta de tasas de cambio (`ve.dolarapi.com`) para obtener los promedios oficiales y de mercado. Estas consultas no envían datos de identidad del usuario.

---

## 3. Uso de la Información
La información técnica recopilada se utiliza exclusivamente para:
1. Calcular conversiones de divisas, totales de carrito, brecha cambiaria y cobros mixtos con tasas actualizadas y soporte offline.
2. Actualizar el Widget de pantalla de inicio y emitir alertas locales de variación de tasa cuando el usuario las activa.
3. Desplegar anuncios publicitarios en la versión gratuita a través de Google AdMob.
4. Validar y respetar las licencias de **Tasa VE Pro** y las suscripciones de **Tasa VE Ultra** (incluyendo la desactivación automática de anuncios y acceso ilimitado al escáner para ambos planes).
5. Mejorar la estabilidad, detectar fallos técnicos y optimizar el rendimiento.

---

## 4. Transferencia y Divulgación de Datos a Terceros
Nosotros **NO vendemos, comercializamos, alquilamos ni transferimos** datos a terceros con fines de lucro o comercialización directa. Únicamente se comparten datos técnicos con Google (AdMob y Google Play Billing) según lo descrito en la sección 2.D.

---

## 5. Privacidad de Menores (COPPA / GDPR-K)
Tasa VE no está dirigida a niños menores de 13 años (o 16 años en la Unión Europea). No recopilamos conscientemente información de menores de edad. Si un padre o tutor tiene conocimiento de que su hijo nos ha proporcionado información, puede contactarnos a través de nuestro correo electrónico para gestionarlo.

---

## 6. Seguridad de la Información
Aplicamos protocolos de seguridad estándar de la industria:
- Comunicación cifrada mediante protocolo HTTPS (SSL/TLS) para todas las consultas de red.
- Procesamiento de cámara (OCR) estrictamente local sin envío de imágenes.
- Almacenamiento seguro en entornos de aplicación en *sandbox* provistos por el sistema operativo Android.

---

## 7. Derechos del Usuario (GDPR / CCPA)
Los usuarios tienen derecho a:
- Eliminar en cualquier momento su historial de conversiones, carrito o datos de Pago Móvil desde la app o borrando los datos de almacenamiento en el menú de Aplicaciones de Android.
- Revocar en cualquier momento los permisos de Cámara o Notificaciones desde los ajustes de Android.
- Restablecer o inhabilitar el identificador de publicidad personalizada desde: *Ajustes de Android > Google > Anuncios > Restablecer ID de publicidad*.
- Restaurar sus compras o administrar/cancelar sus suscripciones desde Google Play o mediante la opción "Restaurar compras y suscripciones" en Configuración.

---

## 8. Modificaciones a esta Política
Nos reservamos el derecho de actualizar esta Política de Privacidad para reflejar cambios legales, técnicos o funcionales. Cualquier cambio será publicado en este documento y reflejado dentro de la aplicación móvil.

---

## 9. Contacto
Si tiene dudas, comentarios o solicitudes relativas a esta Política de Privacidad, por favor comuníquese a:
* **Correo electrónico:** julioingdlop@gmail.com
* **Repositorio oficial:** https://github.com/Julcer26/TasaVE
