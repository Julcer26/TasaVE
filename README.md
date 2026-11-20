<p align="center">
  <img src="brand/tasave_emblem_transparent_512x512.png" alt="Logo Tasa VE" width="140" />
</p>

<h1 align="center">Tasa VE</h1>

<p align="center">
  <strong>Calculadora financiera para Venezuela: Dólar y Euro BCV (4 decimales), BCV Mañana, USDT, Escáner OCR, Carrito con IVA/IGTF y Cobros por Pago Móvil.</strong>
</p>

<p align="center">
  <a href="https://tasave.net"><img src="https://img.shields.io/badge/Web-tasave.net-10B981?style=for-the-badge" alt="Sitio Web Oficial" /></a>
  <img src="https://img.shields.io/badge/Android-Kotlin%20%2B%20Jetpack%20Compose-059669?style=for-the-badge" alt="Android Jetpack Compose" />
  <img src="https://img.shields.io/badge/Version-1.2.0-EA8C00?style=for-the-badge" alt="Versión 1.2.0" />
</p>

---

## Características Principales

- **Conversor Multi-Tasa en Tiempo Real:** Dólar BCV y Euro BCV en **4 decimales exactos**, vista anticipada de **BCV Mañana**, USDT paralelo y Modo Offline con caché local.
- **Widgets Obsidian Studio en Inicio:** Widget gratuito **Tasas en Vivo** (`4×2` con BCV Hoy/Mañana y Brecha) y **Centro Rápido Ultra** (`4×3` con pestañas de 1 toque: Tasas + Escáner/Carrito, Calculadora Clásica `4×4` y Cobro Express con copia directa).
- **Escáner de Precios por Cámara (OCR Local):** Lee etiquetas y habladores directamente en el dispositivo con ML Kit (sin subir fotos a servidores).
- **Carrito de Mercado Inteligente:** Controla tu presupuesto en USD y Bolívares con cálculo opcional de **IVA (16%)** e **IGTF (3%)**.
- **Brecha Cambiaria y Tendencias:** Comparativa oficial vs. paralelo y gráficos históricos interactivos.
- **Cobros, Pago Mixto y Tarjeta PNG:** Calcula el vuelto en pagos mixtos y comparte tarjetas visuales en PNG con tus datos de Pago Móvil, Zelle o Binance Pay (y logo propio en **Ultra**).
- **Privacidad Local y Feedback Directo:** Sin cuentas ni registro; tus perfiles de cobro permanecen exclusivamente en tu dispositivo, con centro de reportes/sugerencias integrado en Configuración.

---

## Identidad Visual y Assets de Marca

La identidad oficial de **Tasa VE** utiliza un isotipo **3D esculpido** en **Ámbar Profundo** (`#EA8C00 → #6B2B00`) y **Esmeralda Profundo** (`#022C22 → #10B981`) con símbolo **`$`** flotante sobre **Deep Obsidian** (`#060A12 → #111B2D`).

- Guía completa: [`brand/BRAND_GUIDELINES.md`](brand/BRAND_GUIDELINES.md)
- Landing page & Marketing: [`docs/`](docs/index.html) ([tasave.net](https://tasave.net))

Para regenerar todos los maestros SVG, PNG (`512×512`, `1024×1024`), Android `VectorDrawable`, íconos `mipmap-*.webp` y el gráfico de Google Play (`1024×500`):

```bash
python3 brand/generate_brand_assets.py
```

---

## Compilación y Pruebas (Android)

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

> **Contacto:** [soporte@tasave.net](mailto:soporte@tasave.net) • **Web:** [tasave.net](https://tasave.net) • **Instagram:** [@tasa.ve](https://www.instagram.com/tasa.ve/) • **TikTok:** [@tasa.ve](https://www.tiktok.com/@tasa.ve)
