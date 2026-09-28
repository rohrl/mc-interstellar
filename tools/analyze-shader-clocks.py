"""Summarize retained shader-clock evidence using only the Python standard library.

Run: python tools/analyze-shader-clocks.py docs/profiles/2026-09-28-rtx-bottleneck
Clock shares describe instrumented invocation latency, not removable GPU time.
"""
import csv
import json
import math
import re
import statistics
import sys
from pathlib import Path


def clocks(path):
    rows = list(csv.DictReader(path.open(encoding="utf-8-sig")))
    result = {}
    for pass_name in ("probe", "mask"):
        for detail in (0, 1):
            group = [r for r in rows if r["pass"] == pass_name and int(r["detail"]) == detail]
            if not group:
                raise ValueError(f"Missing clock group: {path}, {pass_name}, {detail}")
            for row in group:
                if any(not math.isfinite(float(v)) for k, v in row.items() if k != "pass"):
                    raise ValueError(f"Nonfinite clock measurement: {path}")
                if int(row["invalidRays"]):
                    raise ValueError(f"Invalid clock rays: {path}")
            total = sum(float(r["totalTicks"]) for r in group)
            entry = {"active_rays": sorted({int(r["activeRays"]) for r in group}),
                     "gpu_ms_median": statistics.median(float(r["gpuMs"]) for r in group)}
            for key in ("queryInclusiveTicks", "geometrySearchTicks", "candidateShadeTicks", "orbitStepTicks", "otherTicks"):
                # Detail 0 has no nested clocks: its geometry column still includes shading.
                if not detail and key not in ("queryInclusiveTicks", "otherTicks"):
                    continue
                entry[key[:-5] + "_percent"] = (
                    100 * sum(float(r[key]) for r in group) / total if total else None)
            result[f"{pass_name}_detail{detail}"] = entry
    checks = []
    for line in Path(str(path) + ".txt").read_text().splitlines():
        if not line.startswith("pass="):
            continue
        fields = dict(re.findall(r"(\w+)=([^ ]+)", line))
        baseline = float(fields["baselineGpuMedianMs"])
        instrumented = float(fields["instrumentedColourGpuMedianMs"])
        checks.append({**fields, "overhead_percent": 100 * (instrumented / baseline - 1)})
    result["colour_and_overhead_checks"] = checks
    return result


def timings(directory):
    labels = {}
    for line in (directory / "labels.txt").read_text().splitlines():
        if "Optical benchmark completed:" in line:
            name, record = line.split("\t", 1)
            labels[record] = name
    result = {}
    current = None
    for line in (directory / "timings.txt").read_text().splitlines():
        if "Optical benchmark completed:" in line:
            current = labels[line]
            times = re.search(r"GPU pass p50=(\S+) p95=(\S+) p99=(\S+) ms; sampled frame intervals p50=(\S+) p95=(\S+) p99=(\S+)", line)
            result[current] = dict(zip(("gpu_p50", "gpu_p95", "gpu_p99", "frame_p50", "frame_p95", "frame_p99"), map(float, times.groups())))
            result[current]["stage_means"] = {}
        elif "GPU profile stage=" in line:
            stage, value = re.search(r"stage=(\S+) mean=(\S+)", line).groups()
            result[current]["stage_means"][stage] = float(value)
    for entry in result.values():
        stages = entry["stage_means"]
        if stages:
            total = sum(stages.values())
            entry["stage_mean_total"] = total
            entry["stage_mean_percent"] = {k: v / total * 100 for k, v in stages.items()}
        entry["fps_from_frame_median"] = 1000 / entry["frame_p50"]
    return result


if __name__ == "__main__":
    directory = Path(sys.argv[1])
    result = {"clocks": {view: clocks(directory / f"{view}.csv") for view in ("down", "wall")},
              "timings": timings(directory),
              "conditional_speedups_no_extra_cost": {
                  str(fraction): [1 / ((1 - fraction) + fraction / s) for s in (3.26, 3.76)]
                  for fraction in (0.5, 0.65, 0.8)}}
    print(json.dumps(result, indent=2, allow_nan=False))
