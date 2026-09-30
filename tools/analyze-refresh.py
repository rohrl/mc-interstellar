"""Summarize an opt-in refresh CSV using recorded walking intervals.

Usage: python tools/analyze-refresh.py frames.csv legs.jsonl LABEL output.json
Each interval has label, mode, round, leg, start/end (UTC epoch milliseconds).
Start/end trims exclude input setup/release; gameplay screens only.
"""
import csv
import json
import statistics
import sys
from pathlib import Path


def stats(values):
    if not values:
        return None
    values = sorted(values)
    n = len(values)
    return {key: round(value, 4) for key, value in {
        "n": n, "mean": statistics.mean(values), "p50": values[n // 2],
        "p95": values[int((n - 1) * .95)], "p99": values[int((n - 1) * .99)],
        "max": values[-1],
    }.items()}


def summarize(csv_path, intervals_path, label):
    with Path(csv_path).open(encoding="utf-8") as source:
        frames = list(csv.DictReader(source))
    intervals = [json.loads(line) for line in Path(intervals_path).read_text(
        encoding="utf-8-sig").splitlines() if line.strip()]
    result = []
    for interval in intervals:
        if interval["label"] != label:
            continue
        rows = [row for row in frames if row.get("screen") == "game"
                and interval["start"] + 2000 < float(row["wall"]) < interval["end"] - 300]
        if not rows:
            raise ValueError(f"No gameplay samples for {interval}")
        fields = ["interval", "render", "capture", "pack", "tree", "publish",
                  "read", "transfer", "blas", "wait"]
        metrics = {field: stats([float(row[field]) for row in rows
                               if field in ("interval", "render") or float(row[field]) > 0])
                   for field in fields}
        metrics["rtxSync"] = stats([sum(float(row[field]) for field in ("read", "transfer", "blas"))
                                    for row in rows if float(row["read"]) > 0])
        metrics["queue"] = stats([float(row["queue"]) for row in rows])
        result.append({"leg": interval, "metrics": metrics,
                       "published": sum(int(row["published"]) for row in rows),
                       "cancelled": sum(int(row["cancelled"]) for row in rows),
                       "over33ms": sum(float(row["interval"]) > 1000 / 30 for row in rows),
                       "over50ms": sum(float(row["interval"]) > 50 for row in rows),
                       "positions": [[float(rows[i][axis]) for axis in ("x", "y", "z")]
                                     for i in (0, -1)]})
    if not result:
        raise ValueError(f"No intervals for {label}")
    return result


if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise SystemExit(__doc__)
    output = summarize(*sys.argv[1:4])
    Path(sys.argv[4]).write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
    for row in output:
        print(row["leg"].get("mode"), row["leg"]["round"], row["leg"]["leg"],
              row["metrics"]["interval"], "publications", row["published"])
