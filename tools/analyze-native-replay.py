"""Summarize archived RTX replay/interop evidence; Python 3 standard library only."""
import json
import math
import re
import statistics
import sys
from pathlib import Path


def summarize(directory):
    rows = []
    for line in (directory / "two-view.jsonl").read_text().splitlines():
        row = json.loads(line)
        sw, hw = row["softwareSamplesMs"], row["hardwareSamplesMs"]
        # Same nearest-rank P50 convention as Probe.percentile (lower middle for 24).
        sw50, hw50 = (sorted(values)[math.ceil(len(values) * .5) - 1] for values in (sw, hw))
        if abs(sw50 - row["softwareMedianMs"]) > 1e-6 or abs(hw50 - row["hardwareMedianMs"]) > 1e-6:
            raise ValueError("Archived P50 does not match samples")
        rows.append({
            "view": row["view"], "pass": row["pass"], "alpha": row["alpha"],
            "queries": row["queries"], "hits": row["hits"],
            "softwareMedianMs": sw50,
            "hardwareMedianMs": hw50,
            "speedup": sw50 / hw50,
            "softwareRangeMs": [min(sw), max(sw)],
            "hardwareRangeMs": [min(hw), max(hw)],
            "boundaryDifferences": row["boundaryDifferences"],
            "productionDifferences": row["productionDifferences"],
        })
    interop = []
    for line in (directory / "interop.txt").read_text().splitlines():
        match = re.search(r"size=(\d+x\d+).*wallMsPerFrame=(\[.*?\]).*glTimelineMsPerFrame=(\[.*?\])", line)
        if match:
            wall, gpu = json.loads(match[2]), json.loads(match[3])
            interop.append({"size": match[1], "wallMedianMs": statistics.median(wall),
                            "wallRangeMs": [min(wall), max(wall)],
                            "glTimelineMedianMs": statistics.median(gpu),
                            "glTimelineRangeMs": [min(gpu), max(gpu)]})
    if len(rows) != 6 or len(interop) != 2:
        raise ValueError("Incomplete two-view / two-resolution evidence")
    return {"scope": "Warm replay against standalone triangle BVH; no Minecraft FPS forecast. "
                     "Interop clear/blit/ownership only; no complete backend frame.",
            "pairedQueryComparisons": sum(row["queries"] for row in rows),
            "replay": rows, "interop": interop}


if __name__ == "__main__":
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("docs/profiles/2026-09-28-rtx-native")
    output = root / "summary.json"
    output.write_text(json.dumps(summarize(root), indent=2) + "\n")
    print(output)
