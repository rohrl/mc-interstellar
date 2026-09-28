"""Summarize labelled variant checks; standard library, rejects mixed cameras per label."""
import json
import re
import statistics
import sys
from pathlib import Path

root = Path(sys.argv[1])
groups = {}
for line in (root / "timings.txt").read_text(encoding="utf-8-sig").splitlines():
    label, log = line.split("\t", 1)
    label = re.sub(r"-\d+$", "", label)
    signature = tuple(re.search(pattern, log)[1] for pattern in
                      (r"TERRAIN (\d+x\d+)", r"r/rs=([\d.]+)", r"; yaw=([\d.-]+)", r"; pitch=([\d.-]+)"))
    group = groups.setdefault(label, {"camera": signature, "gpu": [], "frame": []})
    if group["camera"] != signature:
        raise ValueError("Mixed camera conditions: " + label)
    group["gpu"].append(float(re.search(r"GPU (?:pass )?p50=([\d.]+)", log)[1]))
    group["frame"].append(float(re.search(r"frame intervals p50=([\d.]+)", log)[1]))
timings = {name: {"camera": group["camera"], "runs": len(group["frame"]),
                 "gpu_p50_ms": statistics.median(group["gpu"]),
                 "frame_p50_ms": statistics.median(group["frame"])} for name, group in groups.items()}
images = {}
for path in sorted(root.glob("*/comparison.txt")):
    raw = path.read_text(encoding="utf-8-sig")
    images[path.parent.name] = {key: float(re.search(r"\b" + key + r"=([\d.Ee+-]+)", raw)[1])
                              for key in ("width", "height", "RGB_mean_abs_255", "RGB_RMSE_255", "pixelsMaxErrorAbove16")}
print(json.dumps({"timings": timings, "images": images}, indent=2))
