package com.seanproctor.datatable.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
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
import org.openjdk.jmh.annotations.TearDown

/**
 * Cost of updating an already displayed table.
 *
 * - [toggleSortOrder] flips the sort direction, which hands the table a new row list
 *   (reversed) and a new sort indicator. Every cell is recomposed and re-measured.
 * - [unrelatedStateChange] changes state read only by a sibling composable. Ideally
 *   the table is skipped entirely, so this measures the fixed overhead of a frame in
 *   which the table's inputs did not change.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class RecomposeBenchmark {

    @Param("100", "1000")
    var rows: Int = 0

    @Param("Basic", "Material3")
    lateinit var style: Style

    private val columns = 6

    private lateinit var scene: ImageComposeScene
    private var frame = 0L

    private var sortAscending by mutableStateOf(true)
    private var unrelatedCounter by mutableIntStateOf(0)

    @Setup
    fun setup() {
        val ascending = sampleRecords(rows)
        val descending = ascending.asReversed()
        val columnDefinitions = benchmarkColumns(columns, WidthMode.Mixed, style, onSort = { _, _ -> })
        val state = DataTableState()
        scene = benchmarkScene {
            Column {
                BasicText("frame ${unrelatedCounter}")
                val ordered = if (sortAscending) ascending else descending
                BenchmarkTable(
                    style = style,
                    columns = columnDefinitions,
                    state = state,
                    sortColumnIndex = 0,
                    sortAscending = sortAscending,
                ) {
                    benchmarkRows(ordered, columns, style)
                }
            }
        }
        scene.renderFrame(nextFrameTime())
    }

    @TearDown
    fun tearDown() {
        scene.close()
    }

    @Benchmark
    fun toggleSortOrder() {
        sortAscending = !sortAscending
        scene.renderFrame(nextFrameTime())
    }

    @Benchmark
    fun unrelatedStateChange() {
        unrelatedCounter++
        scene.renderFrame(nextFrameTime())
    }

    private fun nextFrameTime(): Long = ++frame * FRAME_NANOS
}
