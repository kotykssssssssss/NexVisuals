"""Original NexVisuals procedural masks. Optional developer tool; never run by the mod.
Requires Pillow. All generated masks in this script are dedicated to CC0-1.0.
"""
from pathlib import Path
import math
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
OUT = ROOT / 'assets/nexvisuals/textures/particle'
OUT.mkdir(parents=True, exist_ok=True)
N = 128
for shape in ('orb', 'spark', 'ring', 'slash', 'star'):
    image = Image.new('RGBA', (N, N))
    for py in range(N):
        for px in range(N):
            x, y = (px + .5 - N / 2) / (N / 2), (py + .5 - N / 2) / (N / 2)
            r, angle = math.hypot(x, y), math.atan2(y, x)
            if shape == 'orb': a = math.exp(-r * r * 12) * max(0, 1-r)
            elif shape == 'spark': a = math.exp(-x*x*130-y*y*4) * max(0, 1-r)
            elif shape == 'ring': a = math.exp(-((r-.73)/.045)**2) + .18*math.exp(-((r-.73)/.13)**2)
            elif shape == 'slash':
                a = math.exp(-((r-.69)/(.022+.065*(angle+math.pi)/(2*math.pi)))**2) * max(0, math.sin((angle+math.pi)*.8))**2
            else: a = math.exp(-x*x*170-y*y*5) + math.exp(-y*y*170-x*x*5) + .2*math.exp(-r*r*10)
            image.putpixel((px, py), (255, 255, 255, round(255 * min(1, a) * min(1, max(0, (1-r)*12)))))
    image.save(OUT / (shape + '.png'))

