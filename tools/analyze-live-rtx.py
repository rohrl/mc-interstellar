"""Summarize labelled F12 runs; Python standard library only.

Each input line is LABEL-N<TAB>the complete benchmark completion log line.
The archived labels distinguish frozen-simulation regression checks from live runs.
"""
import json
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path


def summarize(paths):
    groups = defaultdict(list)
    for path in paths:
        for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
            label, log = line.split("\t", 1)
            label = re.sub(r"-\d+$", "", label)
            expected_pitch = 35.91 if "down" in label else 0.91
            pitch = float(re.search(r"; pitch=([\d.-]+)", log)[1])
            if abs(pitch - expected_pitch) > 0.001:
                raise ValueError("Camera mismatch: " + label)
            gpu = re.search(r"GPU (?:pass )?p50=([\d.]+) p95=([\d.]+)", log)
            frame = re.search(r"frame intervals p50=([\d.]+) p95=([\d.]+)", log)
            if not gpu or not frame:
                raise ValueError("Incomplete benchmark: " + label)
            groups[label].append([float(v) for v in (*gpu.groups(), *frame.groups())])
    result = {}
    for label, runs in groups.items():
        values = [statistics.median(column) for column in zip(*runs)]
        result[label] = dict(zip(("gpu_p50_ms", "gpu_p95_ms", "frame_p50_ms", "frame_p95_ms"), values))
        result[label].update(runs=len(runs), fps_from_median_interval=1000 / values[2])
    for view in ("down", "wall"):
        before, after = result.get("before-" + view), result.get("after-" + view)
        if before and after:
            result["regression-" + view] = {
                key + "_change_percent": 100 * (after[key] / before[key] - 1)
                for key in ("gpu_p50_ms", "frame_p50_ms")
            }
    return result


if __name__ == "__main__":
    print(json.dumps(summarize(sys.argv[1:]), indent=2))
