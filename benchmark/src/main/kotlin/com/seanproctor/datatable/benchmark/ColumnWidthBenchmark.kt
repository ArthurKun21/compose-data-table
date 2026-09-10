package com.seanproctor.datatable.benchmark

import com.seanproctor.datatable.DataColumn
import com.seanproctor.datatable.DataTableState
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

/**
 * Same first-frame measurement as [FirstFrameBenchmark] at a fixed table size, varying
 * only the [TableColumnWidth][com.seanproctor.datatable.TableColumnWidth] strategy.
 * Intrinsic-based widths query every cell's intrinsic size before measuring it. In practice
 * text intrinsics are cached by the text layout, so the strategies measure within noise of
 * each other; this benchmark exists to catch regressions in that assumption.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ColumnWidthBenchmark {

    @Param("Fixed", "Flex", "Wrap", "MaxIntrinsic", "Mixed")
    lateinit var widthMode: WidthMode

    private val rows = 200
    private val columns = 6
    private val style = Style.Basic

    private lateinit var records: List<Record>
    private lateinit var columnDefinitions: List<DataColumn>

    @Setup
    fun setup() {
        records = sampleRecords(rows)
        columnDefinitions = benchmarkColumns(columns, widthMode, style)
    }

    @Benchmark
    fun firstFrame() {
        val state = DataTableState()
        val scene = benchmarkScene {
            BenchmarkTable(style, columnDefinitions, state) {
                benchmarkRows(records, columns, style)
            }
        }
        try {
            scene.renderFrame()
        } finally {
            scene.close()
        }
    }
}
