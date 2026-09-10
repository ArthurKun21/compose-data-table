package com.seanproctor.datatable.benchmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import com.seanproctor.datatable.material3.PaginatedDataTable
import com.seanproctor.datatable.paging.BasicPaginatedDataTable
import com.seanproctor.datatable.paging.PageSize
import com.seanproctor.datatable.paging.PaginatedDataTableState
import com.seanproctor.datatable.paging.rememberPaginatedDataTableState
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

enum class PageMode { FitHeight, Fixed25 }

/**
 * Cost of advancing a paginated table by one page over a 10,000 record data set.
 * Only the current page's rows are composed, so this should be independent of the
 * total record count and roughly proportional to the page size.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class PaginationBenchmark {

    @Param("FitHeight", "Fixed25")
    lateinit var pageMode: PageMode

    @Param("Basic", "Material3")
    lateinit var style: Style

    private val columns = 6
    private val recordCount = 10_000

    private lateinit var scene: ImageComposeScene
    private lateinit var state: PaginatedDataTableState
    private var frame = 0L

    @Setup
    fun setup() {
        val records = sampleRecords(recordCount)
        val columnDefinitions = benchmarkColumns(columns, WidthMode.Mixed, style)
        val pageSize = when (pageMode) {
            PageMode.FitHeight -> PageSize.FitHeight
            PageMode.Fixed25 -> PageSize.FixedSize(25)
        }
        // In the headless scene, effects run before the first layout, so with FitHeight the
        // library briefly reports a page size of -1 (see BasicPaginatedDataTable's
        // LaunchedEffect) and hands the content lambda a negative range. Clamp it; the
        // next frame resolves the real page size.
        fun page(from: Int, to: Int): List<Record> =
            records.subList(from.coerceIn(0, recordCount), to.coerceIn(from.coerceIn(0, recordCount), recordCount))

        scene = benchmarkScene {
            state = rememberPaginatedDataTableState(count = recordCount, pageSize = pageSize)
            when (style) {
                Style.Basic -> BasicPaginatedDataTable(
                    columns = columnDefinitions,
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    headerHeight = HEADER_HEIGHT,
                    rowHeight = ROW_HEIGHT,
                ) { from, to ->
                    benchmarkRows(page(from, to), columns, style)
                }

                Style.Material3 -> MaterialTheme {
                    PaginatedDataTable(
                        columns = columnDefinitions,
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        headerHeight = HEADER_HEIGHT,
                        rowHeight = ROW_HEIGHT,
                    ) { from, to ->
                        benchmarkRows(page(from, to), columns, style)
                    }
                }
            }
        }
        // FitHeight needs a layout pass before it knows the page size; render twice so the
        // LaunchedEffect that resizes the page has run before measurements start.
        repeat(3) { scene.renderFrame(nextFrameTime()) }
        check(state.currentPageSize > 1) { "page size was not resolved: ${state.currentPageSize}" }
    }

    @TearDown
    fun tearDown() {
        scene.close()
    }

    @Benchmark
    fun nextPage() {
        val pageCount = (recordCount + state.currentPageSize - 1) / state.currentPageSize
        state.currentPageIndex = (state.currentPageIndex + 1) % pageCount
        scene.renderFrame(nextFrameTime())
    }

    private fun nextFrameTime(): Long = ++frame * FRAME_NANOS
}
