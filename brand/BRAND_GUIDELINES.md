# Tasa VE — Guía de Identidad Visual y Reproducción de Marca

## 1. Concepto y Estructura del Logotipo
El isotipo oficial de **Tasa VE** sintetiza la **“V”** de Venezuela y de valor mediante un **trazo continuo doblado en dos brazos ascendentes**, coronados por el símbolo **`$`** centrado ópticamente en el valle superior:
- **Brazo izquierdo (Ámbar / Oro Tasa VE):** representa la estabilidad y el valor de referencia BCV.
- **Pliegue inferior en sombra:** aporta profundidad de cinta doblada en la base en “U” sin cortar el contorno exterior.
- **Brazo derecho continuo (Esmeralda Tasa VE):** representa crecimiento, conversión fluida y dinamismo financiero.
- **Símbolo `$` geométrico:** trazo limpio, centrado ópticamente (`x = 54.0`, `y = 42.9` en el lienzo maestro `108×108`), diseñado para permanecer 100% legible hasta en **48×48 px**.

---

## 2. Paleta Cromática Oficial
| Rol | Hexadecimal | Uso |
| :--- | :--- | :--- |
| **Fondo Principal (Obsidian Dark)** | `#0F172A` → `#1E293B` (gradiente diagonal) | Fondo de ícono, tiles de cabecera, banners y tarjetones dark |
| **Ámbar Principal (Brazo Izquierdo)** | `#FBBF24` → `#F59E0B` | Brazo izquierdo ascendente |
| **Sombra de Pliegue** | `#B45309` (`58%` opacidad) | Transición inferior en el vértice de la “V” |
| **Esmeralda Principal (Brazo Derecho)** | `#059669` → `#10B981` → `#34D399` | Brazo continuo inferior y derecho |
| **Símbolo `$` (Sobre Fondo Oscuro)** | `#FFFFFF` | Versión principal, ícono de app, web dark |
| **Símbolo `$` (Sobre Fondo Claro)** | `#0F172A` | Papelería blanca, facturas o flyers claros |

---

## 3. Reglas de Tamaño, Márgenes y Zona Segura
- **Adaptive Icon (`108×108 dp`):**
  - El isotipo completo ocupa un radio máximo de **`30.41 u`** respecto al centro `(54, 54)`.
  - Se mantiene dentro de la **zona segura estricta de Android (`66 dp`, radio `33 u`)**, garantizando **cero recortes** en máscaras circulares (Pixel), squircle (Samsung), gota o cuadrado redondeado.
- **Ícono Cuadrado / Google Play (`viewBox 16 16 76 76`):**
  - El encuadre cuadrado de `512×512` y `1024×1024` aplica un **margen de respiro del ~15%** en los cuatro lados, evitando cortes tras el redondeo automático de Google Play Console.
- **Tamaño mínimo de legibilidad:**
  - Versión completa (`logo` con tile): mínimo `24 dp` / `24 px`.
  - Emblema monocromático (notificaciones / *Themed Icons* Android 13+): incluye un canal de separación limpio (`~1.9 u`) en el pliegue inferior para distinguir ambos brazos en un solo color sólido.

---

## 4. Inventario de Archivos y Reproducción Automática
Todos los vectores y PNG se regeneran de manera exacta y determinista ejecutando:

```bash
python3 brand/generate_brand_assets.py
```

| Archivo | Formato / Tamaño | Uso Recomendado |
| :--- | :--- | :--- |
| `brand/tasave_logo_master_108.svg` | SVG (`108×108`) | Maestro vectorial adaptativo oficial |
| `brand/tasave_logo_square.svg` | SVG (`76×76` full-bleed) | Maestro vectorial para tienda, favicon y redes |
| `brand/tasave_emblem_transparent.svg` | SVG transparente (`$ blanco`) | Overlays, videos y banners oscuros |
| `brand/tasave_emblem_lightbg.svg` | SVG transparente (`$ #0F172A`) | Documentos, PDFs y fondos blancos |
| `brand/tasave_emblem_monochrome.svg` | SVG monocromo (`108×108`) | Sellos, marcas de agua, Android 13+ |
| `brand/tasave_logo_1024x1024.png` | PNG `1024×1024` | Alta resolución (redes, prensa, banners) |
| `brand/tasave_logo_512x512.png` | PNG `512×512` | Ícono de Google Play Console |
| `brand/tasave_emblem_transparent_1024x1024.png` | PNG transparente `1024×1024` | Emblema sin fondo para diseños oscuros |
| `brand/tasave_emblem_transparent_512x512.png` | PNG transparente `512×512` | Emblema sin fondo para web/UI oscura |
| `brand/tasave_emblem_lightbg_1024x1024.png` | PNG transparente `1024×1024` | Emblema sin fondo para fondos claros |
| `brand/tasave_emblem_lightbg_512x512.png` | PNG transparente `512×512` | Emblema sin fondo compacto para fondos claros |
| `brand/tasave_emblem_monochrome_512x512.png` | PNG monocromo blanco `512×512` | Stencil / marca de agua / notificaciones |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | VectorDrawable (`108dp`) | Capa frontal del Adaptive Icon |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | VectorDrawable (`108dp`) | Themed Icons Android 13+ y notificaciones |
| `app/src/main/res/drawable/ic_brand_mark.xml` | VectorDrawable (`76dp`) | Cabecera TopAppBar, Ajustes y Tarjetas PNG |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | WebP (`48..192px`) | Retrocompatibilidad Android < 8.0 |
| `docs/assets/app_icon_512x512.png` | PNG `512×512` | Landing `tasave.net` + Play Store |
| `docs/assets/feature_graphic_1024x500.png` | PNG `1024×500` | Gráfico destacado de Play Store + OpenGraph |
