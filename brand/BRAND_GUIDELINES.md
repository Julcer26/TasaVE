# Tasa VE — Guía de Identidad Visual y Reproducción de Marca (Edición 3D Esculpida)

## 1. Concepto y Estructura del Logotipo
El isotipo oficial de **Tasa VE** sintetiza la **“V”** de Venezuela y de valor mediante un **trazo continuo doblado en dos brazos ascendentes** con acabado **3D esculpido**, coronados por el símbolo **`$`** flotante centrado ópticamente en el valle superior:
- **Brazo izquierdo (Ámbar Profundo Volumétrico):** representa la estabilidad y el valor de referencia BCV, con luz especular a 45° (`#FDE68A`) y sombreado interior cálido (`#451A03`).
- **Pliegue inferior en sombra oclusiva:** curva suave (`#260C00` / `#290D00`) que aporta profundidad real de cinta doblada en la base en “U” sin cortar el contorno exterior.
- **Brazo derecho continuo (Esmeralda Profundo Volumétrico):** representa crecimiento, conversión fluida y dinamismo financiero, con bisel exterior oscuro (`#011A13`) y arista superior iluminada (`#34D399` → `#A7F3D0`).
- **Símbolo `$` flotante:** trazo geométrico con degradado vertical (`#FFFFFF → #CBD5E1` en oscuro, `#0F172A → #334155` en claro) y doble sombra proyectada (ambiente + direccional), legible hasta en **32×32 px**.

---

## 2. Paleta Cromática Oficial
| Rol | Hexadecimal | Uso |
| :--- | :--- | :--- |
| **Fondo Principal (Deep Obsidian)** | `#060A12` → `#0A111F` → `#111B2D` | Fondo de ícono, tiles de cabecera (`#070C16`), banners y gráficos Play Store |
| **Aura Ambiental Central** | `#065F46` (`22%`) → `#0F172A` (`6%`) | Resplandor radial sutil tras el emblema en la versión ícono |
| **Ámbar Profundo (Brazo Izquierdo)** | `#EA8C00` → `#C96400` → `#9A4100` → `#6B2B00` | Brazo izquierdo con brillo especular `#FDE68A` y borde interior `#451A03` |
| **Sombra de Pliegue 3D** | `#3B1400` (`0%`) → `#290D00` (`82%`) + `#260C00` | Oclusión curva en el vértice de la “V” bajo el brazo esmeralda |
| **Esmeralda Profundo (Brazo Derecho)** | `#022C22` → `#064E3B` → `#047857` → `#059669` → `#10B981` | Brazo continuo con bisel derecho `#011A13` y arista iluminada `#34D399` / `#A7F3D0` |
| **Símbolo `$` (Sobre Fondo Oscuro)** | `#FFFFFF` → `#F1F5F9` → `#CBD5E1` + sombra `#020617` | Ícono de app, web dark, tarjetas PNG y redes |
| **Símbolo `$` (Sobre Fondo Claro)** | `#0F172A` → `#1E293B` → `#334155` | Papelería blanca, facturas o flyers claros |

---

## 3. Reglas de Tamaño, Márgenes y Zona Segura
- **Adaptive Icon (`108×108 dp`):**
  - El isotipo completo ocupa un radio máximo de **`30.41 u`** respecto al centro `(54, 54)`.
  - Se mantiene dentro de la **zona segura estricta de Android (`66 dp`, radio `33 u`, ~80% central)**, garantizando **cero recortes** en máscaras circulares (Pixel), squircle (Samsung), gota o cuadrado redondeado.
- **Ícono Cuadrado / Google Play (`viewBox 0 0 512 512`):**
  - El encuadre cuadrado de `512×512` y `1024×1024` aplica un **margen de respiro del ~15%** en los cuatro lados, evitando recortes en los bordes redondeados de Google Play Console.
- **Implementación VectorDrawable Android:**
  - `ic_launcher_foreground.xml` e `ic_brand_mark.xml` replican el acabado 3D mediante `<clip-path>` nativo (API 21+, compatible con `minSdk 24`), biseles especulares internos y sombras escalonadas multicapa sin depender de filtros SVG no soportados por Android.
- **Tamaño mínimo de legibilidad:**
  - Versión completa (`logo` con tile): mínimo `24 dp` / `24 px`.
  - Emblema monocromático (*Themed Icons* Android 13+ y notificaciones): incluye un canal de separación limpio (`~1.9 u`) en el pliegue inferior.

---

## 4. Inventario de Archivos y Reproducción Automática
Todos los vectores, PNG, VectorDrawables, WebP y gráficos de tienda se regeneran de forma exacta y determinista ejecutando:

```bash
python3 brand/generate_brand_assets.py
```

| Archivo | Formato / Tamaño | Uso Recomendado |
| :--- | :--- | :--- |
| `brand/tasave_logo_master_108.svg` | SVG (`108×108`) | Maestro vectorial adaptativo 3D oficial |
| `brand/tasave_logo_square.svg` | SVG (`512×512` full-bleed) | Maestro vectorial 3D para tienda, favicon y redes |
| `brand/tasave_emblem_transparent.svg` | SVG transparente (`$ claro`) | Overlays, videos y banners oscuros |
| `brand/tasave_emblem_lightbg.svg` | SVG transparente (`$ #0F172A`) | Documentos, PDFs y fondos blancos |
| `brand/tasave_emblem_monochrome.svg` | SVG monocromo (`108×108`) | Sellos, marcas de agua, Android 13+ |
| `brand/tasave_logo_1024x1024.png` | PNG `1024×1024` | Alta resolución (redes, prensa, banners) |
| `brand/tasave_logo_512x512.png` | PNG `512×512` | Ícono de Google Play Console |
| `brand/tasave_emblem_transparent_1024x1024.png` | PNG transparente `1024×1024` | Emblema sin fondo para diseños oscuros |
| `brand/tasave_emblem_transparent_512x512.png` | PNG transparente `512×512` | Emblema sin fondo para web/UI oscura |
| `brand/tasave_emblem_lightbg_1024x1024.png` | PNG transparente `1024×1024` | Emblema sin fondo para fondos claros |
| `brand/tasave_emblem_lightbg_512x512.png` | PNG transparente `512×512` | Emblema sin fondo compacto para fondos claros |
| `brand/tasave_emblem_monochrome_512x512.png` | PNG monocromo blanco `512×512` | Stencil / marca de agua / notificaciones |
| `app/src/main/res/drawable/ic_launcher_background.xml` | VectorDrawable (`108dp`) | Fondo Deep Obsidian + aura esmeralda |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | VectorDrawable (`108dp`) | Capa frontal 3D del Adaptive Icon |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | VectorDrawable (`108dp`) | Themed Icons Android 13+ y notificaciones |
| `app/src/main/res/drawable/ic_brand_mark.xml` | VectorDrawable (`76dp`) | Cabecera TopAppBar, Ajustes y Tarjetas PNG |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | WebP (`48..192px`) | Retrocompatibilidad Android < 8.0 |
| `docs/assets/app_icon_512x512.png` | PNG `512×512` | Landing `tasave.net` + Play Store |
| `docs/assets/logo_tasave_transparent_512.png` | PNG transparente `512×512` | Simulador web y lockups en `tasave.net` |
| `docs/assets/feature_graphic_1024x500.png` | PNG `1024×500` | Gráfico destacado de Play Store + OpenGraph |
| `docs/assets/hero_showcase.jpg` | JPG `1200×900` | Hero visual principal de `tasave.net` |
| `docs/assets/features_showcase.jpg` | JPG `1408×792` | Banner visual de módulos en `tasave.net` |
