"""Original NexVisuals procedural masks. Optional developer tool; never run by the mod.
Requires Pillow. All generated masks in this script are dedicated to CC0-1.0.
"""
from pathlib import Path
import math
import argparse
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
OUT = ROOT / 'assets/nexvisuals/textures/particle'
OUT.mkdir(parents=True, exist_ok=True)
N = 128
SHAPES = ('orb', 'spark', 'ring', 'slash', 'star', 'footprint', 'heart', 'pixel', 'dot', 'diamond', 'streak')
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--shapes', nargs='+', choices=SHAPES, default=SHAPES)
for shape in parser.parse_args().shapes:
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
            elif shape == 'footprint':
                toe = ((x-.035)/.24)**2 + ((y+.30)/.36)**2
                heel = ((x+.025)/.18)**2 + ((y-.40)/.22)**2
                bridge = (x/.12)**2 + ((y-.08)/.22)**2
                a = max(math.exp(-toe**3), math.exp(-heel**3), .8*math.exp(-bridge**3))
            elif shape == 'heart':
                # A signed implicit heart, upright in texture coordinates, with antialiased edges.
                hx, hy = x * 1.5, -y * 1.5 + .15
                field = (hx*hx + hy*hy - 1)**3 - hx*hx*hy**3
                a = max(0, min(1, .5 - field * 35))
            elif shape == 'pixel':
                a = max(0, min(1, (.65 - max(abs(x), abs(y))) * 70))
            elif shape == 'dot': a = max(0, min(1, (.55-r)*45))
            elif shape == 'diamond': a = max(0, min(1, (.82-abs(x)-abs(y))*45))
            elif shape == 'streak': a = math.exp(-x*x*400) * max(0, 1-abs(y))**.5
            else: a = math.exp(-x*x*170-y*y*5) + math.exp(-y*y*170-x*x*5) + .2*math.exp(-r*r*10)
            image.putpixel((px, py), (255, 255, 255, round(255 * min(1, a) * min(1, max(0, (1-r)*12)))))
    image.save(OUT / (shape + '.png'))
