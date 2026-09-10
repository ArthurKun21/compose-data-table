package com.seanproctor.datatable.benchmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.seanproctor.datatable.BasicDataTable
import com.seanproctor.datatable.DataColumn
import com.seanproctor.datatable.DataTableScope
import com.seanproctor.datatable.DataTableState
import com.seanproctor.datatable.TableColumnWidth
import com.seanproctor.datatable.material3.DataTable
import kotlin.random.Random

/** Viewport used by every benchmark scene, in px at density 1. */
const val SCENE_WIDTH = 1280
const val SCENE_HEIGHT = 800

val HEADER_HEIGHT = 56.dp
val ROW_HEIGHT = 52.dp

/** Row height in px at density 1, used as the scroll step. */
const val ROW_HEIGHT_PX = 52f

/** 60 fps frame period, used to advance the scene clock between rendered frames. */
const val FRAME_NANOS = 16_666_667L

/** Which public entry point is exercised. */
enum class Style { Basic, Material3 }

/** Column width strategy used for every column of a table. */
enum class WidthMode { Fixed, Flex, Wrap, MaxIntrinsic, Mixed }

data class Record(
    val id: Int,
    val name: String,
    val email: String,
    val amount: Double,
    val status: String,
) {
    fun cellText(column: Int): String = when (column % 5) {
        0 -> id.toString()
        1 -> name
        2 -> email
        3 -> amount.toString()
        else -> status
    }
}

private val firstNames = listOf("Ada", "Grace", "Linus", "Margaret", "Dennis", "Barbara", "Ken", "Radia", "Alan", "Frances")
private val lastNames = listOf("Lovelace", "Hopper", "Torvalds", "Hamilton", "Ritchie", "Liskov", "Thompson", "Perlman", "Turing", "Allen")
private val statuses = listOf("Active", "Pending", "Archived", "Suspended")

/** Deterministic sample data so every fork sees the same strings. */
fun sampleRecords(count: Int): List<Record> {
    val random = Random(42)
    return List(count) { index ->
        val first = firstNames[random.nextInt(firstNames.size)]
        val last = lastNames[random.nextInt(lastNames.size)]
        Record(
            id = index + 1,
            name = "$first $last",
            email = "${first.lowercase()}.${last.lowercase()}$index@example.com",
            amount = random.nextInt(0, 1_000_000) / 100.0,
            status = statuses[random.nextInt(statuses.size)],
        )
    }
}

fun widthFor(mode: WidthMode, column: Int): TableColumnWidth = when (mode) {
    WidthMode.Fixed -> TableColumnWidth.Fixed(200.dp)
    WidthMode.Flex -> TableColumnWidth.Flex(1f)
    WidthMode.Wrap -> TableColumnWidth.Wrap
    WidthMode.MaxIntrinsic -> TableColumnWidth.MaxIntrinsic
    WidthMode.Mixed -> when (column % 4) {
        0 -> TableColumnWidth.Fixed(120.dp)
        1 -> TableColumnWidth.Wrap
        2 -> TableColumnWidth.Flex(1f)
        else -> TableColumnWidth.MaxIntrinsic.flexible(1f)
    }
}

@Composable
fun CellText(style: Style, text: String) {
    when (style) {
        Style.Basic -> BasicText(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Style.Material3 -> Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun benchmarkColumns(
    count: Int,
    mode: WidthMode,
    style: Style,
    onSort: ((Int, Boolean) -> Unit)? = null,
): List<DataColumn> = List(count) { column ->
    DataColumn(
        alignment = if (column % 5 == 3) Alignment.CenterEnd else Alignment.CenterStart,
        width = widthFor(mode, column),
        onSort = onSort,
    ) {
        CellText(style, "Column $column")
    }
}

private val evenRow = Color.White
private val oddRow = Color(0xFFF5F5F5)

fun DataTableScope.benchmarkRows(records: List<Record>, columnCount: Int, style: Style) {
    records.forEachIndexed { index, record ->
        row {
            backgroundColor = if (index % 2 == 0) evenRow else oddRow
            onClick = { }
            repeat(columnCount) { column ->
                cell { CellText(style, record.cellText(column)) }
            }
        }
    }
}

/** Renders either the core [BasicDataTable] or the Material 3 [DataTable] with equivalent settings. */
@Composable
fun BenchmarkTable(
    style: Style,
    columns: List<DataColumn>,
    state: DataTableState,
    sortColumnIndex: Int? = null,
    sortAscending: Boolean = true,
    content: DataTableScope.() -> Unit,
) {
    when (style) {
        Style.Basic -> BasicDataTable(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            state = state,
            headerHeight = HEADER_HEIGHT,
            rowHeight = ROW_HEIGHT,
            sortColumnIndex = sortColumnIndex,
            sortAscending = sortAscending,
            content = content,
        )

        Style.Material3 -> MaterialTheme {
            DataTable(
                columns = columns,
                modifier = Modifier.fillMaxSize(),
                state = state,
                headerHeight = HEADER_HEIGHT,
                rowHeight = ROW_HEIGHT,
                sortColumnIndex = sortColumnIndex,
                sortAscending = sortAscending,
                content = content,
            )
        }
    }
}

/** A headless Skia-backed scene the size of a typical desktop window. */
fun benchmarkScene(content: @Composable () -> Unit): ImageComposeScene =
    ImageComposeScene(
        width = SCENE_WIDTH,
        height = SCENE_HEIGHT,
        density = Density(1f),
        content = content,
    )

/** Runs composition, layout and draw for one frame and releases the resulting bitmap. */
fun ImageComposeScene.renderFrame(nanoTime: Long = 0L) {
    render(nanoTime).close()
}
