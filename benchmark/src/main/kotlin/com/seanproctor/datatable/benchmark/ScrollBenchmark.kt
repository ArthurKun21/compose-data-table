package com.seanproctor.datatable.benchmark

import androidx.compose.ui.ImageComposeScene
import com.seanproctor.datatable.DataTableScrollState
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
 * Per-frame cost of scrolling an already composed table by one row (or one row height
 * horizontally). Each invocation applies a raw scroll delta, exactly as a drag or wheel
 * event would, then renders the next frame. Direction flips at either end so the
 * scroll never becomes a no-op.
 *
 * Fixed 200dp columns are used so that 8 columns (1600px) overflow the 1280px viewport
 * and horizontal scrolling has something to do.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ScrollBenchmark {

    @Param("100", "1000")
    var rows: Int = 0

    @Param("Basic", "Material3")
    lateinit var style: Style

    private val columns = 8

    private lateinit var scene: ImageComposeScene
    private lateinit var state: DataTableState
    private var frame = 0L
    private var verticalDirection = 1
    private var horizontalDirection = 1

    @Setup
    fun setup() {
        val records = sampleRecords(rows)
        val columnDefinitions = benchmarkColumns(columns, WidthMode.Fixed, style)
        state = DataTableState()
        scene = benchmarkScene {
            BenchmarkTable(style, columnDefinitions, state) {
                benchmarkRows(records, columns, style)
            }
        }
        scene.renderFrame(nextFrameTime())

        // Sanity check: make sure a scroll actually moves the content.
        state.verticalScrollState.dispatchRawDelta(-ROW_HEIGHT_PX)
        scene.renderFrame(nextFrameTime())
        check(state.verticalScrollState.offset > 0) { "vertical scroll had no effect" }
        state.horizontalScrollState.dispatchRawDelta(-ROW_HEIGHT_PX)
        scene.renderFrame(nextFrameTime())
        check(state.horizontalScrollState.offset > 0) { "horizontal scroll had no effect" }
    }

    @TearDown
    fun tearDown() {
        scene.close()
    }

    @Benchmark
    fun verticalScrollFrame() {
        verticalDirection = step(state.verticalScrollState, verticalDirection)
        scene.renderFrame(nextFrameTime())
    }

    @Benchmark
    fun horizontalScrollFrame() {
        horizontalDirection = step(state.horizontalScrollState, horizontalDirection)
        scene.renderFrame(nextFrameTime())
    }

    private fun nextFrameTime(): Long = ++frame * FRAME_NANOS

    /** Scrolls by one row in [direction], reversing at either end. Returns the direction used. */
    private fun step(scrollState: DataTableScrollState, direction: Int): Int {
        val actual = when {
            direction > 0 && !scrollState.canScrollBackward -> -1
            direction < 0 && !scrollState.canScrollForward -> 1
            else -> direction
        }
        // Negative raw delta moves the content up/left (increases offset).
        scrollState.dispatchRawDelta(-ROW_HEIGHT_PX * actual)
        return actual
    }
}
