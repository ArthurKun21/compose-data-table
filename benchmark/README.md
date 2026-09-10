# Benchmarks

JVM micro-benchmarks for the data table, built on [kotlinx-benchmark](https://github.com/Kotlin/kotlinx-benchmark)
(JMH). Each benchmark composes a table into a headless `ImageComposeScene` (Skia CPU raster,
1280x800 px at density 1) and times one full frame: composition, measure/layout, and draw.
No window or display is needed, so the suite runs on CI machines and over SSH.

This module is not published and is not part of `./gradlew build` for the library modules.

## Running

```bash
# Full run: 3 x 1s warmup, 5 x 1s measurement per parameter combination (~7 minutes)
./gradlew :benchmark:benchmark

# Quick sanity run: 1 warmup, 2 x 300ms measurement
./gradlew :benchmark:smokeBenchmark

# Restrict to benchmarks whose class/method name matches a regex
./gradlew :benchmark:benchmark -PbenchmarkFilter=Scroll
./gradlew :benchmark:smokeBenchmark '-PbenchmarkFilter=FirstFrame|Recompose'
```

Results are printed to the console and written as JSON under
`benchmark/build/reports/benchmarks/<configuration>/<timestamp>/main.json`.

The plugin also builds a self-contained JMH jar, which gives access to every JMH option
(profilers, parameter overrides, custom iteration counts):

```bash
./gradlew :benchmark:mainBenchmarkJar
java -jar benchmark/build/benchmarks/main/jars/benchmark-main-jmh-*.jar \
    ColumnWidth -p widthMode=Wrap -wi 2 -i 3 -f 1 -prof stack
```

## What is measured

All benchmarks report average time per operation in milliseconds. Every parameterised
benchmark runs against both entry points: `Basic` (`BasicDataTable`, `BasicText` cells) and
`Material3` (`DataTable` inside `MaterialTheme`, M3 `Text` cells).

| Benchmark | Parameters | Operation |
|---|---|---|
| `FirstFrameBenchmark.firstFrame` | rows 10/100/1000, columns 4/8, style | Create a scene, compose the whole table, render one frame, close. The table is not lazy, so this is the cost of showing a table of that size. |
| `ColumnWidthBenchmark.firstFrame` | widthMode Fixed/Flex/Wrap/MaxIntrinsic/Mixed | Same as above at 200 rows x 6 columns, varying only the `TableColumnWidth` strategy. |
| `ScrollBenchmark.verticalScrollFrame` / `horizontalScrollFrame` | rows 100/1000, style | Apply a one-row raw scroll delta to an already composed 8 column table (fixed 200dp columns, so it overflows horizontally) and render the next frame. |
| `RecomposeBenchmark.toggleSortOrder` | rows 100/1000, style | Flip the sort direction, which hands the table a reversed row list, and render. Every cell recomposes and re-measures. |
| `RecomposeBenchmark.unrelatedStateChange` | rows 100/1000, style | Change state read only by a sibling composable and render. Measures the frame cost when the table's inputs are unchanged. |
| `PaginationBenchmark.nextPage` | pageMode FitHeight/Fixed25, style | Advance a paginated table over 10,000 records by one page and render. |

## Baseline results

Measured 2026-09-10 at library version 0.13.0 (Compose Multiplatform 1.12.0, Kotlin 2.4.20)
on an AMD Ryzen 5 9600X, Linux, Adoptium JDK 17. Average time per frame in ms, 5 x 1s
iterations after 3 x 1s warmup, one fork. First-frame numbers allocate ~215 MB per frame at
1000 x 8 and carry roughly +/-30% run-to-run noise from GC; the steady-state benchmarks
(scroll, unrelated state change) are stable to a few percent.

| Benchmark | Params | Basic | Material3 |
|---|---|---:|---:|
| FirstFrame | 10 rows x 4 cols | 24 | 41 |
| FirstFrame | 100 rows x 4 cols | 38 | 57 |
| FirstFrame | 1000 rows x 4 cols | 164 | 201 |
| FirstFrame | 10 rows x 8 cols | 27 | 43 |
| FirstFrame | 100 rows x 8 cols | 53 | 73 |
| FirstFrame | 1000 rows x 8 cols | 287 | 347 |
| ColumnWidth (200 x 6) | Fixed / Flex / Wrap / MaxIntrinsic / Mixed | 69 / 64 / 61 / 65 / 63 | - |
| Scroll vertical | 100 rows / 1000 rows | 0.69 / 1.05 | 0.72 / 1.08 |
| Scroll horizontal | 100 rows / 1000 rows | 0.63 / 0.85 | 0.64 / 0.87 |
| Recompose toggleSortOrder | 100 rows / 1000 rows | 20 / 186 | 20 / 187 |
| Recompose unrelatedStateChange | 100 rows / 1000 rows | 0.51 / 0.51 | 0.57 / 0.57 |
| Pagination nextPage (10k records) | FitHeight (14 rows) / Fixed25 | 3.4 / 5.7 | 3.5 / 5.9 |

Takeaways from this baseline:

- First-frame cost is linear in cells (about 30-35 us per cell) because every row is
  composed and measured whether or not it is visible. A JMH stack sample attributes most of
  the runnable time to Skia paragraph layout, which runs once for intrinsics and once for
  measure per cell, rather than to the table's own layout code.
- Replacing the row list (sort toggle) costs about the same as the first frame, so sorting a
  1000-row table is a ~190 ms hitch.
- Scrolling only re-runs placement, so a scroll frame is about 1 ms even at 1000 rows, with
  a mild dependence on row count from the per-row placement loop.
- A recomposition that does not touch the table's inputs skips it entirely (0.5 ms,
  independent of row count).
- The Material 3 wrapper adds a roughly constant 15-20 ms to the first frame and is free
  everywhere else.
- Column width strategy makes no measurable difference at 200 x 6.

## Adding a benchmark

Put a new `@State(Scope.Benchmark)` class in `src/main/kotlin/com/seanproctor/datatable/benchmark/`.
The `allopen` plugin makes `@State` classes open, as JMH requires. Reuse the helpers in
`Harness.kt` (`benchmarkScene`, `renderFrame`, `benchmarkColumns`, `benchmarkRows`,
`BenchmarkTable`) so results stay comparable across benchmarks.
