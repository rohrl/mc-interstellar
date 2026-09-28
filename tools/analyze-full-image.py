"""Summarize paired frozen full-image comparisons; Python standard library only.

Usage: py -3 tools/analyze-full-image.py docs/profiles/2026-09-28-rtx-image
Prints JSON. Timings are frozen optical frames, not live Minecraft FPS.
"""
import json
import re
import statistics
import sys
from pathlib import Path


def summarize(path):
    raw = path.read_text(encoding="utf-8-sig")
    values = {}
    for key in ("OpenGL_wall_ms", "RTX_wall_ms", "RTX_Vulkan_gpu_ms",
                "OpenGL_GLtimeline_ms", "RTX_GLtimeline_ms"):
        found = re.search(r"^" + key + r"=(\[.*\])$", raw, re.MULTILINE)
        if found:
            samples = json.loads(found.group(1))
            values[key] = {"median": statistics.median(samples),
                           "min": min(samples), "max": max(samples),
                           "samples": len(samples)}
    software = values["OpenGL_wall_ms"]["median"]
    hardware = values["RTX_wall_ms"]["median"]
    result = {"case": str(path.parent.name), "timings": values,
              "wall_speedup": software / hardware,
              "wall_reduction_percent": 100 * (1 - hardware / software)}
    for key in ("width", "height", "RGB_mean_abs_255", "RGB_RMSE_255",
                "changedPixels", "pixelsMaxErrorAbove16"):
        result[key] = float(re.search(r"\b" + key + r"=([\d.Ee+-]+)", raw).group(1))
    return result


if __name__ == "__main__":
    files = sorted(Path(sys.argv[1]).rglob("comparison.txt"))
    if not files:
        raise SystemExit("No comparison.txt files found")
    print(json.dumps([summarize(path) for path in files], indent=2))
