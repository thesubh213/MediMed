import os
from PIL import Image, ImageDraw

def draw_emblem(draw, scale_factor, dx=0, dy=0):
    # Scale coordinates
    s = scale_factor
    
    # Outer Glow Circle:
    # Center at (256, 256) -> relative (0, 0)
    # Radius = 130
    draw.ellipse([
        (-130 + 256) * s + dx, 
        (-130 + 256) * s + dy, 
        (130 + 256) * s + dx, 
        (130 + 256) * s + dy
    ], fill=(255, 255, 255, 20))
    
    # Capsule top cap (Soft Apricot):
    # Centered at (231, 196) with radius 50.
    # Angle in PIL: 0 is East, 90 is South, 180 is West, 270 is North.
    # West to East via North is 180 to 360.
    draw.pieslice([
        (-50 + 231) * s + dx, 
        (-50 + 196) * s + dy, 
        (50 + 231) * s + dx, 
        (50 + 196) * s + dy
    ], start=180, end=360, fill=(249, 219, 189, 255))
    
    # Rectangle top body: X from 181 to 281, Y from 196 to 256
    draw.rectangle([
        (181 * s) + dx, 
        (196 * s) + dy, 
        (281 * s) + dx, 
        (256 * s) + dy
    ], fill=(249, 219, 189, 255))
    
    # Capsule bottom cap (Cotton Candy):
    # Centered at (231, 316) with radius 50.
    # West to East via South is 0 to 180.
    draw.pieslice([
        (-50 + 231) * s + dx, 
        (-50 + 316) * s + dy, 
        (50 + 231) * s + dx, 
        (50 + 316) * s + dy
    ], start=0, end=180, fill=(255, 165, 171, 255))
    
    # Rectangle bottom body: X from 181 to 281, Y from 256 to 316
    draw.rectangle([
        (181 * s) + dx, 
        (256 * s) + dy, 
        (281 * s) + dx, 
        (316 * s) + dy
    ], fill=(255, 165, 171, 255))
    
    # Center Divider: X from 181 to 281, Y from 252 to 260
    draw.rectangle([
        (181 * s) + dx, 
        (252 * s) + dy, 
        (281 * s) + dx, 
        (260 * s) + dy
    ], fill=(69, 9, 32, 153))
    
    # Pill Highlight Reflection (Quadratic Bezier):
    # M 198,171 Q 208,156 222,156 L 222,166 Q 212,166 198,171 Z
    def get_quad_bezier(p0, p1, p2, num_steps=20):
        points = []
        for i in range(num_steps + 1):
            t = i / num_steps
            x = (1-t)**2 * p0[0] + 2*(1-t)*t * p1[0] + t**2 * p2[0]
            y = (1-t)**2 * p0[1] + 2*(1-t)*t * p1[1] + t**2 * p2[1]
            points.append((x * s + dx, y * s + dy))
        return points
    
    highlight_poly = []
    highlight_poly.extend(get_quad_bezier((198, 171), (208, 156), (222, 156)))
    highlight_poly.append((222 * s + dx, 166 * s + dy))
    highlight_poly.extend(get_quad_bezier((222, 166), (212, 166), (198, 171)))
    
    draw.polygon(highlight_poly, fill=(255, 255, 255, 89))
    
    # Medical Cross Circle (Blush Rose):
    # Centered at (293, 191). Radius = 38.
    draw.ellipse([
        (-38 + 293) * s + dx, 
        (-38 + 191) * s + dy, 
        (38 + 293) * s + dx, 
        (38 + 191) * s + dy
    ], fill=(218, 98, 125, 255))
    
    # Medical Cross Bars (White):
    # Horizontal: X from 269 to 317, Y from 186 to 196
    draw.rectangle([
        (269 * s) + dx, 
        (186 * s) + dy, 
        (317 * s) + dx, 
        (196 * s) + dy
    ], fill=(255, 255, 255, 255))
    # Vertical: X from 288 to 298, Y from 167 to 215
    draw.rectangle([
        (288 * s) + dx, 
        (167 * s) + dy, 
        (298 * s) + dx, 
        (215 * s) + dy
    ], fill=(255, 255, 255, 255))

def generate_logo_png(output_path):
    # 4x supersampling
    scale = 4
    canvas_size = 512 * scale
    
    # Create image with alpha
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw centered emblem
    draw_emblem(draw, scale, dx=0, dy=0)
    
    # Downsample using Lanczos interpolation
    final_img = img.resize((512, 512), Image.Resampling.LANCZOS)
    final_img.save(output_path, "PNG")
    print(f"Generated logo icon at {output_path}")

def generate_feature_graphic_png(output_path):
    scale = 4
    width = 1024 * scale
    height = 500 * scale
    
    # 1. Create a diagonal gradient background: Night Bordeaux (#450920) to Berry Crush (#A53860)
    grad = Image.new("RGB", (256, 256))
    grad_pixels = grad.load()
    for y in range(256):
        for x in range(256):
            t = (x / 255.0 + (255.0 - y) / 255.0) / 2.0
            r = int(69 + t * (165 - 69))
            g = int(9 + t * (56 - 9))
            b = int(32 + t * (96 - 32))
            grad_pixels[x, y] = (r, g, b)
    
    img = grad.resize((width, height), Image.Resampling.BILINEAR).convert("RGBA")
    draw = ImageDraw.Draw(img)
    
    # 2. Draw subtle premium radial ring overlays
    cx = 512 * scale
    cy = 250 * scale
    
    radii = [160 * scale, 240 * scale, 340 * scale]
    for r in radii:
        draw.ellipse([
            cx - r, 
            cy - r, 
            cx + r, 
            cy + r
        ], outline=(255, 165, 171, 20), width=int(2 * scale))
        
    # 3. Draw centered emblem
    # Target center: (512, 250)
    # Original center: (256, 256)
    # Offset:
    dx = (512 - 256) * scale
    dy = (250 - 256) * scale
    draw_emblem(draw, scale, dx=dx, dy=dy)
    
    # Downsample
    final_img = img.resize((1024, 500), Image.Resampling.LANCZOS)
    final_img.save(output_path, "PNG")
    print(f"Generated feature graphic at {output_path}")

if __name__ == "__main__":
    assets_dir = os.path.dirname(os.path.abspath(__file__))
    
    logo_path = os.path.join(assets_dir, "logo.png")
    generate_logo_png(logo_path)
    
    feature_path = os.path.join(assets_dir, "feature_graphic.png")
    generate_feature_graphic_png(feature_path)
