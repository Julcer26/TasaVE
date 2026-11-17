# Tasa VE — Design System (MASTER.md)

## Identidad Visual (Fintech / Utilidad Cambiaria Venezuela)
- **Propósito:** Precisión matemática instantánea, lectura sin fatiga en supermercados/comercios y fluidez a 60/120fps.
- **Modo por defecto:** Obsidian Dark (`#0A0A0B`) con acentos Esmeralda (`#10B981`) y Ámbar Ultra/Pro (`#F59E0B`).

## Tokens Semánticos (Modo Oscuro / Modo Claro)
- **Background:** `#0A0A0B` (Dark) / `#F8FAFC` (Light)
- **Surface / Card:** `#141418` (`0x0F222226`) (Dark) / `#FFFFFF` (`0x9EFFFFFF`) (Light)
- **Surface Elevated (Nav Bar / Sheets):** `#18181D` (Dark) / `#FFFFFF` (Light)
- **Text Primary (contraste >= 7:1):** `#FFFFFF` (Dark) / `#0F172A` (Light)
- **Text Secondary (contraste >= 4.5:1):** `#94A3B8` (Dark) / `#475569` (Light)
- **Accent Primary (Conversión / BCV):** `#10B981` (`#34D399` en dark text)
- **Accent Pro / Ultra:** `#F59E0B` (`#FBBF24` highlight)
- **Semantic Info (EUR / Mercado):** `#3B82F6`
- **Semantic Warning (Brecha alta / IGTF):** `#F59E0B`
- **Semantic Danger (Eliminar / Error):** `#EF4444`

## Reglas de Oro UX & Rendimiento Compose
1. **Áreas táctiles:** mínimo `48.dp` en botones, pestañas de navegación y chips interactivos.
2. **Edge-to-Edge Real:** la barra de navegación inferior (`NavigationBar`) gestiona `WindowInsets.navigationBars` nativos; las pantallas usan el `innerPadding` del `Scaffold` sin colisiones.
3. **Animaciones en fase Layout/Draw:** usar `graphicsLayer`, `animateContentSize` y `Crossfade(tween(180))` sin `TextAlign.Justify` en bloques expandibles.
4. **Jerarquía de 3 niveles por tarjeta:** etiqueta superior, cifra principal tabular, metadato secundario.
5. **Estados vacíos accionables:** cada nueva sección (Carrito, Mercado, Cobros) guía al usuario con CTA directo en 1 toque.
