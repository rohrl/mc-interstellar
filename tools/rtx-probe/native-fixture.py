"""Small analytic acceptance fixture; timings of it are not performance evidence.

184 triangles / 368 rays: cutout holes, translucent acceptance, emissive thresholds,
glint/shadow UV wrapping, red-channel text coverage, sidedness, clouds, mass tags,
and excluded returning-body tags. Expected distances follow plane locations.
"""
import struct
from pathlib import Path

root = Path('run/rtx-native/material-fixture')
root.mkdir(parents=True, exist_ok=True)
view = root / 'view-fixture'
view.mkdir(exist_ok=True)
tags = [0, -3, -4, -1, -2, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 66, 34]
alphas = [0, 13, 128, 255]
triangles = []
cases = []


def triangle(x, z, tag, uv):
    # Face normal points toward negative Z; every test ray crosses well inside.
    data = []
    for px, py in [(x, 0), (x, 2), (x + 2, 0)]:
        data.extend([px, py, z, tag, uv, .5, 4 * 4096, 4096, 1, 1, 1, 1])
    return data


for tag in tags:
    for texel, alpha in enumerate(alphas):
        x = len(cases) * 3
        cases.append((x, tag, alpha))
        triangles.append((tag in (0, -3, -4), triangle(x, 0, tag, (texel + .5) / 4)))
        triangles.append((True, triangle(x, 1, 0, .875)))
triangles.sort(key=lambda item: not item[0])
terrain = sum(item[0] for item in triangles)
with (root / 'triangles.bin').open('wb') as output:
    output.write(struct.pack('<5i', 0x49525458, 1, len(triangles), terrain, len(triangles) - terrain))
    for _, values in triangles:
        output.write(struct.pack('<36f', *values))
for name in ['atlas', 'entities', 'clouds']:
    pixels = bytes(channel for alpha in alphas for channel in (255 - alpha, 0, 0, alpha))
    (root / (name + '.bin')).write_bytes(struct.pack('<2i', 4, 1) + pixels)
for pass_id, name in [(0, 'probe'), (2, 'mask')]:
    rays = []
    for x, raw_tag, alpha in cases:
        tag = raw_tag - 64 if raw_tag >= 64 else raw_tag
        tag = 0 if tag == -4 else tag
        mode = abs(tag)
        covered = (255 - alpha if mode in (15, 16) else alpha) / 255
        accepted = mode < 32 and covered >= (.001 if mode in (9, 10) else .1)
        if mode in (5, 6) and pass_id == 0:
            accepted = False
        two_sided = mode in (2, 6) or mode >= 7 and mode % 2 == 0
        for back in (False, True):
            # Front: surface z0 at t=.25, opaque backing z1 at t=.5.
            # Back: opaque backing is back-facing; surface z0 is at t=.5.
            expected = (.5 if accepted and two_sided else -1) if back else (.25 if accepted else .5)
            rays.append(struct.pack('<8f4I', x + .5, .5, 2 if back else -1, expected,
                                    0, 0, -4 if back else 4, 1, len(rays), 0, pass_id, 7))
    (view / (name + '.bin')).write_bytes(struct.pack('<4i', 0x49525259, 1, len(rays), 48) + b''.join(rays))
(view / 'capture.txt').write_text('Analytic fixture, not a captured Minecraft view; 184 triangles and 368 total queries.\n')
print(root)
