package net.calvuz.qreport.export.data.text

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility per formattazione testo nei report ASCII
 * Gestisce allineamento, wrapping, tabelle e layout
 */
@Singleton
class TextFormatter @Inject constructor() {

    companion object {
        private const val DEFAULT_LINE_WIDTH = 80
        private const val TAB_SIZE = 4
    }

    /**
     * Centra testo in una linea di larghezza specificata
     */
    fun centerText(text: String, width: Int = DEFAULT_LINE_WIDTH): String {
        if (text.length >= width) return text
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text + " ".repeat(width - text.length - padding)
    }

    /**
     * Allinea testo a destra
     */
    fun rightAlignText(text: String, width: Int = DEFAULT_LINE_WIDTH): String {
        if (text.length >= width) return text
        val padding = width - text.length
        return " ".repeat(padding) + text
    }

    /**
     * Allinea testo a sinistra con padding
     */
    fun leftAlignText(text: String, width: Int = DEFAULT_LINE_WIDTH): String {
        if (text.length >= width) return text.take(width)
        return text + " ".repeat(width - text.length)
    }

    /**
     * Wrappa testo lungo su più linee con indentazione
     */
    fun wrapText(
        text: String,
        width: Int = DEFAULT_LINE_WIDTH,
        indent: String = ""
    ): String {
        if (text.length <= width) return indent + text

        val words = text.split(" ")
        val result = StringBuilder()
        var currentLine = StringBuilder()

        for (word in words) {
            // Se aggiungendo questa parola supereremmo la larghezza
            if (currentLine.length + word.length + 1 > width - indent.length) {
                // Aggiungi la linea corrente al risultato
                if (currentLine.isNotEmpty()) {
                    result.appendLine(indent + currentLine.toString().trim())
                    currentLine.clear()
                }
            }

            // Aggiungi la parola alla linea corrente
            if (currentLine.isNotEmpty()) currentLine.append(" ")
            currentLine.append(word)
        }

        // Aggiungi l'ultima linea se non vuota
        if (currentLine.isNotEmpty()) {
            result.append(indent + currentLine.toString().trim())
        }

        return result.toString()
    }

    /**
     * Crea una tabella ASCII formattata
     */
    fun createTable(
        headers: List<String>,
        rows: List<List<String>>,
        columnWidths: List<Int>? = null
    ): String {
        if (headers.isEmpty() || rows.isEmpty()) return ""

        // Calcola larghezze colonne se non specificate
        val widths = columnWidths ?: calculateColumnWidths(headers, rows)

        val result = StringBuilder()

        // Header
        result.appendLine(createTableRow(headers, widths))
        result.appendLine(createTableSeparator(widths))

        // Rows
        rows.forEach { row ->
            result.appendLine(createTableRow(row, widths))
        }

        return result.toString().trimEnd()
    }

    /**
     * Crea una riga di tabella
     */
    private fun createTableRow(cells: List<String>, widths: List<Int>): String {
        return cells.zip(widths) { cell, width ->
            leftAlignText(cell.take(width), width)
        }.joinToString(" | ", "| ", " |")
    }

    /**
     * Crea separatore tabella
     */
    private fun createTableSeparator(widths: List<Int>): String {
        return widths.joinToString("-+-", "+-", "-+") { "-".repeat(it) }
    }

    /**
     * Calcola larghezze colonne ottimali
     */
    private fun calculateColumnWidths(headers: List<String>, rows: List<List<String>>): List<Int> {
        val numColumns = headers.size
        val maxWidths = IntArray(numColumns)

        // Inizializza con lunghezza headers
        headers.forEachIndexed { index, header ->
            maxWidths[index] = header.length
        }

        // Trova lunghezza massima per ogni colonna
        rows.forEach { row ->
            row.forEachIndexed { index, cell ->
                if (index < numColumns) {
                    maxWidths[index] = maxOf(maxWidths[index], cell.length)
                }
            }
        }

        return maxWidths.toList()
    }

    /**
     * Crea box con bordi per testo importante
     */
    fun createBox(
        content: String,
        width: Int = DEFAULT_LINE_WIDTH,
        title: String? = null,
        style: BoxStyle = BoxStyle.SIMPLE
    ): String {
        val lines = content.split("\n")
        val contentWidth = width - 4 // Account for borders

        val result = StringBuilder()

        // Top border
        result.appendLine(style.topLeft + style.horizontal.repeat(width - 2) + style.topRight)

        // Title if provided
        title?.let {
            val titleLine = centerText(it, contentWidth)
            result.appendLine("${style.vertical} $titleLine ${style.vertical}")
            result.appendLine("${style.vertical} ${"-".repeat(contentWidth)} ${style.vertical}")
        }

        // Content lines
        lines.forEach { line ->
            val formattedLine = leftAlignText(line, contentWidth)
            result.appendLine("${style.vertical} $formattedLine ${style.vertical}")
        }

        // Bottom border
        result.appendLine(style.bottomLeft + style.horizontal.repeat(width - 2) + style.bottomRight)

        return result.toString().trimEnd()
    }

    /**
     * Crea progress bar ASCII
     */
    fun createProgressBar(
        current: Int,
        total: Int,
        width: Int = 50,
        showPercentage: Boolean = true
    ): String {
        val percentage = if (total > 0) (current * 100) / total else 0
        val filledWidth = (current * width) / maxOf(total, 1)

        val bar = "█".repeat(filledWidth) + "░".repeat(width - filledWidth)

        return if (showPercentage) {
            "[$bar] $percentage% ($current/$total)"
        } else {
            "[$bar] ($current/$total)"
        }
    }

    /**
     * Formatta lista con bullet points
     */
    fun createBulletList(
        items: List<String>,
        bullet: String = "•",
        indent: String = "  "
    ): String {
        return items.joinToString("\n") { item ->
            "$indent$bullet $item"
        }
    }

    /**
     * Formatta lista numerata
     */
    fun createNumberedList(
        items: List<String>,
        indent: String = "  "
    ): String {
        return items.mapIndexed { index, item ->
            "$indent${index + 1}. $item"
        }.joinToString("\n")
    }

    /**
     * Crea sezione con header e separatore
     */
    fun createSection(
        title: String,
        content: String,
        separatorChar: String = "-",
        addSpacing: Boolean = true
    ): String {
        val result = StringBuilder()

        if (addSpacing) result.appendLine()

        result.appendLine(title)
        result.appendLine(separatorChar.repeat(title.length))
        result.appendLine(content)

        if (addSpacing) result.appendLine()

        return result.toString()
    }

    /**
     * Formatta dimensioni file in formato leggibile
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }

    /**
     * Formatta durata in formato leggibile
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    /**
     * Tronca testo con ellipsis
     */
    fun truncateText(text: String, maxLength: Int, ellipsis: String = "..."): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.take(maxLength - ellipsis.length) + ellipsis
        }
    }

    /**
     * Crea colonne di testo affiancate
     */
    fun createColumns(
        columns: List<String>,
        columnWidths: List<Int>,
        separator: String = "  "
    ): String {
        val columnLines = columns.map { it.split("\n") }
        val maxLines = columnLines.maxOfOrNull { it.size } ?: 0

        val result = StringBuilder()

        for (lineIndex in 0 until maxLines) {
            val line = columnLines.zip(columnWidths) { lines, width ->
                val text = lines.getOrNull(lineIndex) ?: ""
                leftAlignText(text, width)
            }.joinToString(separator)

            result.appendLine(line.trimEnd())
        }

        return result.toString().trimEnd()
    }
}

/**
 * Stili per box ASCII
 */
enum class BoxStyle(
    val topLeft: String,
    val topRight: String,
    val bottomLeft: String,
    val bottomRight: String,
    val horizontal: String,
    val vertical: String
) {
    SIMPLE("┌", "┐", "└", "┘", "─", "│"),
    DOUBLE("╔", "╗", "╚", "╝", "═", "║"),
    HEAVY("┏", "┓", "┗", "┛", "━", "┃"),
    ROUNDED("╭", "╮", "╰", "╯", "─", "│"),
    ASCII("+", "+", "+", "+", "-", "|")
}

/**
 * Extension functions per StringBuilder
 */
fun StringBuilder.appendSeparator(char: String = "-", length: Int = 80) {
    appendLine(char.repeat(length))
}

fun StringBuilder.appendCentered(text: String, width: Int = 80) {
    val formatter = TextFormatter()
    appendLine(formatter.centerText(text, width))
}

fun StringBuilder.appendSection(title: String, content: String) {
    appendLine(title)
    appendLine("-".repeat(title.length))
    appendLine(content)
    appendLine()
}