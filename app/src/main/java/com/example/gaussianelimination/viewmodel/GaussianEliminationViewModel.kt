package com.example.gaussianelimination.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import java.math.BigDecimal
import java.math.RoundingMode

const val MAX_SIZE = 12
const val MIN_SIZE = 1
private const val EPS = 1e-12
private const val INPUT_EPS = 1e-9
private const val DEFAULT_DISPLAY_DIGITS = 4

data class MatrixStep(
    val operation: String,
    val matrixState: Array<DoubleArray>
)

class GaussianEliminationViewModel : ViewModel() {

    var equations by mutableIntStateOf(3)
        private set
    var unknowns by mutableIntStateOf(3)
        private set
    var matrixInputStrings by mutableStateOf(
        Array(equations) { Array(unknowns + 1) { "0" } }
    )
        private set
    var activeCell by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var isMatrixInputVisible by mutableStateOf(false)
        private set
    var resultText by mutableStateOf("")
        private set
    var stepLog by mutableStateOf(listOf<MatrixStep>())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isMatrixReady by mutableStateOf(false)
        private set

    fun onEquationsChange(newValue: Float) {
        val newInt = newValue.toInt()
        if (newInt != equations) {
            equations = newInt
            resetUiState()
        }
    }

    fun onUnknownsChange(newValue: Float) {
        val newInt = newValue.toInt()
        if (newInt != unknowns) {
            unknowns = newInt
            resetUiState()
        }
    }

    fun resetUiState() {
        isMatrixInputVisible = false
        resultText = ""
        stepLog = listOf()
        errorMessage = null
        isMatrixReady = false
        activeCell = null
    }

    fun initializeMatrix() {
        val rows = equations
        val cols = unknowns
        errorMessage = null
        matrixInputStrings = Array(rows) { Array(cols + 1) { "0" } }
        isMatrixInputVisible = true
        resultText = ""
        stepLog = listOf()
        activeCell = null
        isMatrixReady = validateMatrixReady()
    }

    fun onCellClick(row: Int, col: Int) {
        activeCell = Pair(row, col)
    }

    fun onKeyPress(key: String) {
        val (row, col) = activeCell ?: return
        val currentValue = matrixInputStrings[row][col]
        when (key) {
            "DONE" -> {
                activeCell = null
                return
            }
            "->" -> {
                val totalCols = matrixInputStrings[0].size
                val totalRows = matrixInputStrings.size

                var newR = row
                var newC = col + 1

                if (newC >= totalCols) {
                    newC = 0
                    newR += 1
                }
                if (newR >= totalRows) {
                    activeCell = null
                } else {
                    activeCell = Pair(newR, newC)
                }
                return
            }
            "DEL" -> {
                val newValue =
                    if (currentValue.length > 1) currentValue.dropLast(1) else "0"
                onMatrixElementChange(row, col, newValue)
                return
            }
            "Clear" -> {
                onMatrixElementChange(row, col, "0")
                return
            }
            "-" -> {
                val newValue =
                    if (currentValue == "0") "-"
                    else if (!currentValue.startsWith("-")) "-$currentValue"
                    else currentValue.drop(1)
                onMatrixElementChange(row, col, newValue)
                return
            }
            "." -> {
                val newValue =
                    if (!currentValue.contains(".")) "$currentValue." else currentValue
                onMatrixElementChange(row, col, newValue)
                return
            }
        }
        val newValue =
            if (currentValue == "0" && key != ".") key else "$currentValue$key"
        onMatrixElementChange(row, col, newValue)
    }

    fun onMatrixElementChange(row: Int, col: Int, value: String) {
        val copy = matrixInputStrings.map { it.clone() }.toTypedArray()
        var cleanedValue = value
        if (cleanedValue.length > 1 && cleanedValue.startsWith("0") && !cleanedValue.startsWith("0.")) {
            cleanedValue = cleanedValue.drop(1)
        }
        if (cleanedValue == "-0") {
            cleanedValue = "-"
        }
        copy[row][col] = if (cleanedValue.isEmpty()) "0" else cleanedValue
        matrixInputStrings = copy
        errorMessage = null
        isMatrixReady = validateMatrixReady()
    }

    fun fillRandomMatrix() {
        val rows = matrixInputStrings.size
        if (rows == 0) return
        val cols = matrixInputStrings[0].size
        val newM = Array(rows) {
            Array(cols) {
                Random.nextInt(-10, 11).toString()
            }
        }
        matrixInputStrings = newM
        resultText = ""
        stepLog = listOf()
        errorMessage = null
        activeCell = null
        isMatrixReady = validateMatrixReady()
    }

    fun solve() {
        errorMessage = null
        resultText = ""
        stepLog = listOf()
        isLoading = true
        activeCell = null

        viewModelScope.launch {
            val rows = matrixInputStrings.size
            if (rows == 0) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Matrix is empty."
                    isLoading = false
                }
                return@launch
            }
            val colsAug = matrixInputStrings[0].size
            val unknowns = colsAug - 1
            val a: Array<DoubleArray>
            try {
                a = Array(rows) { r ->
                    DoubleArray(colsAug) { c ->
                        val v = matrixInputStrings[r][c]
                        if (v == "-" || v.isEmpty() || v == "-.") throw IllegalArgumentException("Row ${r + 1}, Col ${c + 1} invalid input: '$v'")
                        val parsed = if (v.contains("/")) {
                            val parts = v.split("/")
                            if (parts.size == 2) {
                                val num = parts[0].toDoubleOrNull()
                                val den = parts[1].toDoubleOrNull()
                                if (num != null && den != null && den != 0.0) num / den else throw IllegalArgumentException("Row ${r + 1}, Col ${c + 1} invalid fraction: '$v'")
                            } else throw IllegalArgumentException("Row ${r + 1}, Col ${c + 1} invalid fraction: '$v'")
                        } else {
                            v.toDoubleOrNull() ?: throw IllegalArgumentException("Row ${r + 1}, Col ${c + 1} invalid number: '$v'")
                        }
                        parsed
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Input Error: ${e.message}"
                    isLoading = false
                }
                return@launch
            }

            try {
                val (log, solutionOrMsg) = withContext(Dispatchers.Default) {
                    forwardThenBackSub_ShowFinalMatrixFirst(rows, unknowns, a)
                }
                withContext(Dispatchers.Main) {
                    stepLog = log
                    resultText = solutionOrMsg
                    errorMessage = null
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message
                    resultText = "Error occurred."
                    isLoading = false
                }
            }
        }
    }

    private fun forwardThenBackSub_ShowFinalMatrixFirst(rows: Int, unknowns: Int, aInput: Array<DoubleArray>): Pair<List<MatrixStep>, String> {
        val a = Array(rows) { r -> aInput[r].clone() }
        val log = mutableListOf<MatrixStep>()
        log.add(MatrixStep("Initial augmented matrix:", deepCopy(a)))
        var pivotRow = 0
        val pivotPositions = mutableListOf<Pair<Int,Int>>()
        for (col in 0 until unknowns) {
            if (pivotRow >= rows) break
            var iMax = pivotRow
            for (r in pivotRow + 1 until rows) {
                if (abs(a[r][col]) > abs(a[iMax][col])) iMax = r
            }
            log.add(MatrixStep("Step 1 (Find pivot) - Column ${col + 1}: candidate row ${iMax + 1} (value = ${formatSmart(a[iMax][col])})", deepCopy(a)))
            if (abs(a[iMax][col]) < EPS) {
                log.add(MatrixStep("Column ${col + 1}: no valid pivot (column ~ 0), skip column", deepCopy(a)))
                continue
            }
            if (iMax != pivotRow) {
                val tmp = a[pivotRow]; a[pivotRow] = a[iMax]; a[iMax] = tmp
                log.add(MatrixStep("Step 2 (Swap) - Swap R${pivotRow + 1} <-> R${iMax + 1}", deepCopy(a)))
            } else {
                log.add(MatrixStep("Step 2 (Swap) - No swap needed (R${pivotRow + 1} is pivot row)", deepCopy(a)))
            }
            val pivotValBefore = a[pivotRow][col]
            if (abs(pivotValBefore - 1.0) > EPS) {
                for (j in col until unknowns + 1) {
                    a[pivotRow][j] /= pivotValBefore
                    if (abs(a[pivotRow][j]) < EPS) a[pivotRow][j] = 0.0
                }
                log.add(MatrixStep("Step 3 (Scale) - Divide R${pivotRow + 1} by ${formatSmart(pivotValBefore)} to make leading 1", deepCopy(a)))
            } else {
                log.add(MatrixStep("Step 3 (Scale) - Pivot already ~1 in R${pivotRow + 1}", deepCopy(a)))
            }
            pivotPositions.add(pivotRow to col)
            for (r in pivotRow + 1 until rows) {
                val factor = a[r][col]
                if (abs(factor) < EPS) continue
                for (j in col until unknowns + 1) {
                    a[r][j] -= factor * a[pivotRow][j]
                    if (abs(a[r][j]) < EPS) a[r][j] = 0.0
                }
                log.add(MatrixStep("Step 4 (Eliminate below) - R${r + 1} -> R${r + 1} - (${formatSmart(factor)}) * R${pivotRow + 1}", deepCopy(a)))
            }
            pivotRow += 1
        }
        log.add(MatrixStep("Final matrix after forward elimination (upper-triangular under pivots):", deepCopy(a)))
        for (r in 0 until rows) {
            var allZero = true
            for (c in 0 until unknowns) {
                if (abs(a[r][c]) > EPS) { allZero = false; break }
            }
            if (allZero && abs(a[r][unknowns]) > INPUT_EPS) {
                throw IllegalStateException("Inconsistent system: row ${r + 1} reduces to 0 = ${formatSmart(a[r][unknowns])}")
            }
        }
        var rank = 0
        for (r in 0 until rows) {
            var rowHasPivot = false
            for (c in 0 until unknowns) {
                if (abs(a[r][c]) > EPS) { rowHasPivot = true; break }
            }
            if (rowHasPivot) rank += 1
        }
        if (rank < unknowns) {
            val msg = StringBuilder()
            msg.append("Rank = $rank, Unknowns = $unknowns → Infinite solutions (free variables exist).\n")
            msg.append("Forward elimination result shown above. Cannot compute unique solution by back-substitution.\n")
            return Pair(log, msg.toString())
        }
        val x = DoubleArray(unknowns) { 0.0 }
        val equationsText = mutableListOf<String>()
        for (idx in pivotPositions.size - 1 downTo 0) {
            val (prow, pcol) = pivotPositions[idx]
            var sumKnown = 0.0
            val terms = mutableListOf<String>()
            for (j in pcol + 1 until unknowns) {
                val coeff = a[prow][j]
                if (abs(coeff) < EPS) continue
                terms.add("${formatSmart(coeff)}·x${j + 1}")
                sumKnown += coeff * x[j]
            }
            val rhs = a[prow][unknowns]
            val leftSide = if (terms.isEmpty()) {
                "x${pcol + 1}"
            } else {
                "x${pcol + 1} + ${terms.joinToString(" + ")}"
            }
            val eq = "$leftSide = ${formatSmart(rhs)}"
            val coeffPivot = a[prow][pcol]
            val value = (rhs - sumKnown) / coeffPivot
            x[pcol] = value
            equationsText.add(eq)
        }
        val equationsBlock = equationsText.reversed().joinToString("\n")
        log.add(MatrixStep("Back-substitution equations:\n$equationsBlock", deepCopy(a)))
        val solutionText = x.mapIndexed { idx, v -> "x${idx + 1} = ${formatSmart(v)}" }.joinToString("\n")
        return Pair(log, "Unique solution found (by back-substitution):\n$solutionText")
    }

    private fun deepCopy(matrix: Array<DoubleArray>): Array<DoubleArray> {
        return Array(matrix.size) { i -> matrix[i].clone() }
    }

    private fun validateMatrixReady(): Boolean {
        val rows = matrixInputStrings.size
        if (rows == 0) return false
        val cols = matrixInputStrings[0].size
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val v = matrixInputStrings[r][c]
                if (v == "-" || v.isEmpty() || v == "-.") return false
                if (v.toDoubleOrNull() == null) {
                    if (!v.contains("/")) return false
                    val parts = v.split("/")
                    if (parts.size != 2) return false
                    val n = parts[0].toDoubleOrNull()
                    val d = parts[1].toDoubleOrNull()
                    if (n == null || d == null || d == 0.0) return false
                }
            }
        }
        return true
    }

    private fun Double.toLongOrNull(): Long? {
        return try {
            if (this.isFinite()) this.toLong() else null
        } catch (e: Exception) {
            null
        }
    }
}

fun formatSmart(value: Double, digits: Int = DEFAULT_DISPLAY_DIGITS): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    return try {
        val bd = BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP).stripTrailingZeros()
        val s = bd.toPlainString()
        if (s.contains('.') && s.substringAfter('.').all { it == '0' }) {
            s.substringBefore('.')
        } else {
            s
        }
    } catch (e: Exception) {
        val s = "%.${digits}f".format(value).trimEnd('0').trimEnd('.')
        if (s.isEmpty()) "0" else s
    }
}
