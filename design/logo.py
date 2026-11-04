"""
Money Tracker logosu: kanatlı, perspektifli bir para destesi.

Tek kaynaktan iki çıktı üretir:
  - design/logo.svg                                         (paylaşmak/düzenlemek için)
  - app/src/main/res/drawable/ic_launcher_foreground.xml    (Android adaptive icon ön planı)

Android vector drawable eğme (skew) dönüşümünü desteklemediği için banknotların perspektifi
burada noktalar tek tek hesaplanarak uygulanır; iki çıktı da aynı düz koordinatları kullanır.

Çalıştırma: python3 design/logo.py
"""
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SVG_OUT = ROOT / "design/logo.svg"
ICON_OUT = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"

# Adaptive icon'da yuvarlak maske güvenli alanı ~%66; kanat uçları dışarı taşmasın.
ICON_SCALE = 0.64
ICON_PIVOT = (256, 232)


# --- Basit sahne modeli -------------------------------------------------------------------

@dataclass
class Gradient:
    c0: str
    c1: str
    x0: float
    y0: float
    x1: float
    y1: float


@dataclass
class Shape:
    d: str
    fill: str | Gradient | None = None
    fill_alpha: float | None = None
    stroke: str | None = None
    stroke_width: float | None = None
    stroke_alpha: float | None = None


@dataclass
class Group:
    children: list = field(default_factory=list)
    pivot: tuple[float, float] = (0, 0)
    scale: tuple[float, float] = (1, 1)
    translate: tuple[float, float] = (0, 0)


def f(v: float) -> str:
    return f"{v:.2f}".rstrip("0").rstrip(".")


# --- Perspektifli banknot -----------------------------------------------------------------
# Banknotun üst yüzü, (x, y) "kâğıt düzlemi" koordinatlarını ekrana taşıyan bir afin dönüşümle
# çizilir: x uzun kenar boyunca, y derinlik boyunca (izleyiciye doğru).

NOTE_W, NOTE_D = 236.0, 96.0            # düzlemdeki boyutlar
U = (250.0, -46.0)                      # uzun kenarın ekrandaki yönü ve uzunluğu
V = (78.0, 80.0)                        # derinlik kenarının ekrandaki yönü ve uzunluğu
THICK = 16.0                            # banknot kalınlığı (ekranda dikey)
LAYER = 18.0                            # desteki banknotlar arası dikey mesafe
ORIGIN = (256 - (U[0] + V[0]) / 2, 226.0)   # üst banknotun arka-sol köşesi; deste x=256'da ortalı


def plane(x: float, y: float, dy: float = 0) -> tuple[float, float]:
    s, t = x / NOTE_W, y / NOTE_D
    return (ORIGIN[0] + s * U[0] + t * V[0], ORIGIN[1] + s * U[1] + t * V[1] + dy)


def poly(points) -> str:
    (x0, y0), *rest = points
    return f"M{f(x0)},{f(y0)} " + " ".join(f"L{f(x)},{f(y)}" for x, y in rest) + " Z"


def plane_path(cmds, dy=0) -> str:
    """Düzlem koordinatlarında M/L/C komutlarını ekran koordinatlarına çevirir."""
    out = []
    for op, *pts in cmds:
        if op == "Z":
            out.append("Z")
            continue
        xy = [plane(x, y, dy) for x, y in pts]
        out.append(op + " ".join(f"{f(a)},{f(b)}" for a, b in xy))
    return " ".join(out)


def plane_circle(cx, cy, r, dy=0):
    k = 0.5523 * r  # dört kübik bezierle çember; afin dönüşüm altında elips olur
    return plane_path([
        ("M", (cx + r, cy)),
        ("C", (cx + r, cy + k), (cx + k, cy + r), (cx, cy + r)),
        ("C", (cx - k, cy + r), (cx - r, cy + k), (cx - r, cy)),
        ("C", (cx - r, cy - k), (cx - k, cy - r), (cx, cy - r)),
        ("C", (cx + k, cy - r), (cx + r, cy - k), (cx + r, cy)),
        ("Z",),
    ], dy)


def note(dy: float, top: tuple[str, str], front: str, side: str, decorated: bool) -> list:
    a, b = plane(0, 0, dy), plane(NOTE_W, 0, dy)
    c, d = plane(NOTE_W, NOTE_D, dy), plane(0, NOTE_D, dy)
    down = lambda p: (p[0], p[1] + THICK)
    shapes = [
        # Ön ve sağ yan yüzler (kalınlık)
        Shape(poly([d, c, down(c), down(d)]), fill=front),
        Shape(poly([b, c, down(c), down(b)]), fill=side),
        # Yan yüzlerde kâğıt katmanı çizgisi
        Shape(f"M{f(d[0])},{f(d[1] + THICK / 2)} L{f(c[0])},{f(c[1] + THICK / 2)} "
              f"L{f(b[0])},{f(b[1] + THICK / 2)}",
              stroke="#FFFFFF", stroke_width=1.5, stroke_alpha=0.25),
        # Üst yüz
        Shape(poly([a, b, c, d]), fill=Gradient(top[0], top[1], *a, *c)),
    ]
    if not decorated:
        return shapes

    cx, cy = NOTE_W / 2, NOTE_D / 2
    band_l, band_r = cx - 14, cx + 14
    shapes += [
        # İç çerçeve
        Shape(poly([plane(12, 9, dy), plane(NOTE_W - 12, 9, dy),
                    plane(NOTE_W - 12, NOTE_D - 9, dy), plane(12, NOTE_D - 9, dy)]),
              stroke="#FFFFFF", stroke_width=3, stroke_alpha=0.45),
        Shape(plane_circle(30, 22, 6, dy), fill="#FFFFFF", fill_alpha=0.55),
        Shape(plane_circle(NOTE_W - 30, NOTE_D - 22, 6, dy), fill="#FFFFFF", fill_alpha=0.55),
        # Bant: üst yüzü boydan geçer, ön yüzden aşağı sarkar
        Shape(poly([plane(band_l, 0, dy), plane(band_r, 0, dy),
                    plane(band_r, NOTE_D, dy), plane(band_l, NOTE_D, dy)]), fill="#FFB86B"),
        Shape(poly([plane(band_l, 0, dy), plane(band_l + 5, 0, dy),
                    plane(band_l + 5, NOTE_D, dy), plane(band_l, NOTE_D, dy)]),
              fill="#FFFFFF", fill_alpha=0.35),
        Shape(poly([plane(band_l, NOTE_D, dy), plane(band_r, NOTE_D, dy),
                    down(plane(band_r, NOTE_D, dy + 2 * LAYER)), down(plane(band_l, NOTE_D, dy + 2 * LAYER))]),
              fill="#E39A4F"),
        # Amblem ve ₺
        Shape(plane_circle(cx, cy, 33, dy), fill="#179870"),
        Shape(plane_circle(cx, cy, 33, dy), stroke="#FFFFFF", stroke_width=3, stroke_alpha=0.8),
        Shape(plane_path([
            ("M", (cx - 5, cy - 18)), ("L", (cx - 5, cy + 18)),
            ("C", (cx + 8, cy + 18), (cx + 17, cy + 11), (cx + 18, cy - 2)),
            ("M", (cx - 14, cy - 3)), ("L", (cx + 6, cy - 12)),
            ("M", (cx - 14, cy + 8)), ("L", (cx + 6, cy - 1)),
        ], dy), stroke="#FFFFFF", stroke_width=6),
    ]
    return shapes


# --- Kanatlar ------------------------------------------------------------------------------

WING = ("M184,252 C150,186 96,140 30,130 C34,150 44,162 58,168 C44,176 44,190 56,198 "
        "C48,210 54,224 70,228 C66,242 78,254 96,254 C98,268 114,278 132,274 "
        "C142,286 162,288 184,276 Z")
WING_LINES = "M178,254 L64,172 M178,256 L66,204 M178,260 L82,234 M180,266 L112,262"
FEATHER_BOX = (30, 130, 184, 288)


def wing() -> Group:
    return Group([
        Shape(WING, fill=Gradient("#BFEBDC", "#8FD3BC", *FEATHER_BOX),
              stroke="#6FBFA4", stroke_width=2.5),
        Shape(WING_LINES, stroke="#7CC7AE", stroke_width=3),
        Group([
            Shape(WING, fill=Gradient("#FFFFFF", "#D6F3E9", *FEATHER_BOX)),
            Shape(WING_LINES, stroke="#B4E4D3", stroke_width=3.5),
        ], pivot=(184, 262), scale=(0.72, 0.72)),
        Group([Shape(WING, fill=Gradient("#E9FBF4", "#A9E4CF", *FEATHER_BOX))],
              pivot=(186, 266), scale=(0.42, 0.42)),
    ])


def sparkle(cx, cy, r, color, alpha=None):
    q = r * 0.2
    d = (f"M{f(cx)},{f(cy - r)} Q{f(cx + q)},{f(cy - q)} {f(cx + r)},{f(cy)} "
         f"Q{f(cx + q)},{f(cy + q)} {f(cx)},{f(cy + r)} Q{f(cx - q)},{f(cy + q)} {f(cx - r)},{f(cy)} "
         f"Q{f(cx - q)},{f(cy - q)} {f(cx)},{f(cy - r)} Z")
    return Shape(d, fill=color, fill_alpha=alpha)


def scene() -> Group:
    shadow_y = plane(NOTE_W / 2, NOTE_D, 2 * LAYER)[1] + THICK + 14
    return Group([
        Shape(f"M{f(256 - 150)},{f(shadow_y)} a150,18 0 1 0 300,0 a150,18 0 1 0 -300,0 Z",
              fill="#0B3D31", fill_alpha=0.22),
        # Sol kanat izleyiciye yakın: büyük ve alçak. Sağ kanat uzak tarafta: biraz küçük ve yukarıda.
        Group([wing()], pivot=(184, 262), scale=(1.0, 1.0), translate=(-6, 2)),
        Group([Group([wing()], pivot=(256, 0), scale=(-1, 1))], pivot=(328, 262), scale=(0.88, 0.88), translate=(14, -46)),
        *note(2 * LAYER, ("#16805F", "#0B5141"), "#0A4A3B", "#083D31", False),
        *note(LAYER, ("#2BB58A", "#137A5E"), "#0F6650", "#0B5141", False),
        *note(0, ("#6BE0B6", "#22A97F"), "#1A8C69", "#137A5E", True),
        sparkle(440, 330, 22, "#FFB86B"),
        sparkle(96, 104, 16, "#6BE0B6"),
        sparkle(470, 286, 9, "#FFFFFF", 0.9),
    ])


# --- SVG çıktısı ---------------------------------------------------------------------------

def to_svg(node, defs: list, indent="  ") -> str:
    if isinstance(node, Group):
        inner = "\n".join(to_svg(c, defs, indent + "  ") for c in node.children)
        if node.scale == (1, 1) and node.translate == (0, 0):
            return f"{indent}<g>\n{inner}\n{indent}</g>"
        (px, py), (sx, sy), (tx, ty) = node.pivot, node.scale, node.translate
        t = (f"translate({f(tx + px)} {f(ty + py)}) scale({f(sx)} {f(sy)}) translate({f(-px)} {f(-py)})")
        return f'{indent}<g transform="{t}">\n{inner}\n{indent}</g>'
    a = [f'd="{node.d}"']
    if isinstance(node.fill, Gradient):
        gid = f"g{len(defs)}"
        g = node.fill
        defs.append(f'    <linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{f(g.x0)}" y1="{f(g.y0)}" '
                    f'x2="{f(g.x1)}" y2="{f(g.y1)}"><stop offset="0" stop-color="{g.c0}"/>'
                    f'<stop offset="1" stop-color="{g.c1}"/></linearGradient>')
        a.append(f'fill="url(#{gid})"')
    else:
        a.append(f'fill="{node.fill or "none"}"')
    if node.fill_alpha is not None:
        a.append(f'fill-opacity="{node.fill_alpha}"')
    if node.stroke:
        a += [f'stroke="{node.stroke}"', f'stroke-width="{node.stroke_width}"',
              'stroke-linecap="round"', 'stroke-linejoin="round"']
    if node.stroke_alpha is not None:
        a.append(f'stroke-opacity="{node.stroke_alpha}"')
    return f"{indent}<path {' '.join(a)}/>"


def write_svg():
    defs: list[str] = []
    body = to_svg(scene(), defs)
    SVG_OUT.write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">\n'
        "  <!-- design/logo.py tarafından üretildi; elle düzenleme. -->\n"
        "  <defs>\n" + "\n".join(defs) + "\n  </defs>\n" + body + "\n</svg>\n")


# --- Android vector drawable çıktısı -------------------------------------------------------

def argb(color: str) -> str:
    return "#FF" + color.lstrip("#").upper()


def to_vd(node, indent="    ") -> str:
    if isinstance(node, Group):
        inner = "\n".join(to_vd(c, indent + "    ") for c in node.children)
        attrs = ""
        if node.scale != (1, 1):
            (px, py), (sx, sy) = node.pivot, node.scale
            attrs = (f' android:pivotX="{f(px)}" android:pivotY="{f(py)}"'
                     f' android:scaleX="{f(sx)}" android:scaleY="{f(sy)}"')
        if node.translate != (0, 0):
            attrs += f' android:translateX="{f(node.translate[0])}" android:translateY="{f(node.translate[1])}"'
        return f"{indent}<group{attrs}>\n{inner}\n{indent}</group>"
    a = [f'android:pathData="{node.d}"']
    if isinstance(node.fill, str):
        a.append(f'android:fillColor="{argb(node.fill)}"')
    if node.fill_alpha is not None:
        a.append(f'android:fillAlpha="{node.fill_alpha}"')
    if node.stroke:
        a += [f'android:strokeColor="{argb(node.stroke)}"', f'android:strokeWidth="{node.stroke_width}"',
              'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
    if node.stroke_alpha is not None:
        a.append(f'android:strokeAlpha="{node.stroke_alpha}"')
    if not isinstance(node.fill, Gradient):
        return f"{indent}<path {' '.join(a)}/>"
    g = node.fill
    return (f"{indent}<path {' '.join(a)}>\n"
            f'{indent}    <aapt:attr name="android:fillColor">\n'
            f'{indent}        <gradient android:type="linear" android:startX="{f(g.x0)}" android:startY="{f(g.y0)}" '
            f'android:endX="{f(g.x1)}" android:endY="{f(g.y1)}">\n'
            f'{indent}            <item android:offset="0" android:color="{argb(g.c0)}"/>\n'
            f'{indent}            <item android:offset="1" android:color="{argb(g.c1)}"/>\n'
            f"{indent}        </gradient>\n"
            f"{indent}    </aapt:attr>\n"
            f"{indent}</path>")


def write_icon():
    root = Group([scene()], pivot=ICON_PIVOT, scale=(ICON_SCALE, ICON_SCALE))
    # Pivot, tuvalin ortasına taşınmalı ki logo ikonun ortasında dursun.
    shift = 256 - ICON_PIVOT[1]
    ICON_OUT.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- design/logo.py tarafından üretildi; elle düzenleme. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="512" android:viewportHeight="512">\n'
        f'    <group android:translateY="{shift}">\n'
        + to_vd(root, "        ") + "\n    </group>\n</vector>\n")


if __name__ == "__main__":
    write_svg()
    write_icon()
    print(f"yazıldı: {SVG_OUT.relative_to(ROOT)}, {ICON_OUT.relative_to(ROOT)}")
