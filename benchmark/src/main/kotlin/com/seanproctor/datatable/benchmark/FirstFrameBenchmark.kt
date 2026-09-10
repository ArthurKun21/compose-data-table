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
 * Cost of showing a table for the first time: initial composition of every cell,
 * the full measure/layout pass, and drawing one frame. The table is not lazy, so
 * this scales with rows x columns regardless of what fits in the viewport.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class FirstFrameBenchmark {

    @Param("10", "100", "1000")
    var rows: Int = 0

    @Param("4", "8")
    var columns: Int = 0

    @Param("Basic", "Material3")
    lateinit var style: Style

    private lateinit var records: List<Record>
    private lateinit var columnDefinitions: List<DataColumn>

    @Setup
    fun setup() {
        records = sampleRecords(rows)
        columnDefinitions = benchmarkColumns(columns, WidthMode.Mixed, style, onSort = { _, _ -> })
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
