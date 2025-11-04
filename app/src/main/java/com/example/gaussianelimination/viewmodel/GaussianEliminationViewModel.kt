package com.example.gaussianelimination.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.abs

data class MatrixStep(
    val operation: String,
    val matrixState: Array<DoubleArray>
)

class GaussianEliminationViewModel : ViewModel() {
    var sizeInput by mutableStateOf("3")
        private set
    var matrixInputStrings by mutableStateOf(Array(3) { Array(4) { "0" } })
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

    fun onSizeInputChange(newSize: String) {
        sizeInput = newSize.filter { it.isDigit() }
        isMatrixInputVisible = false
        resultText = ""
        stepLog = listOf()
        errorMessage = null
    }

    fun initializeMatrix() {
        val size = sizeInput.toIntOrNull()
        errorMessage = null
        if (size == null || size <= 0 || size > 8) {
            errorMessage = "Please enter a valid matrix size (1-8)."
            return
        }
        matrixInputStrings = Array(size) { Array(size + 1) { "0" } }
        isMatrixInputVisible = true
        resultText = ""
        stepLog = listOf()
    }

    fun onMatrixElementChange(row: Int, col: Int, value: String) {
        var cleanedValue = value.trim()
        val oldValue = matrixInputStrings[row][col]

        cleanedValue = cleanedValue.filter { it.isDigit() || it == '.' || it == '-' }

        if (oldValue == "0" && cleanedValue.isNotEmpty() && cleanedValue != "0" && cleanedValue != "-") {
            cleanedValue = cleanedValue.removePrefix("0")
        }

        if (cleanedValue.isEmpty() || cleanedValue == "-") {
            // Allow "-" for negative start
        } else if (cleanedValue == ".") {
            cleanedValue = "0."
        } else {
            if (cleanedValue.count { it == '.' } > 1) {
                cleanedValue = cleanedValue.substringBeforeLast('.')
            }
            if (cleanedValue.contains('-') && !cleanedValue.startsWith('-')) {
                cleanedValue = cleanedValue.replace("-", "")
            }
        }

        val newMatrix = matrixInputStrings.map { it.clone() }.toTypedArray()
        newMatrix[row][col] = if (cleanedValue.isEmpty()) "0" else cleanedValue
        matrixInputStrings = newMatrix
        errorMessage = null
    }

    fun fillRandomMatrix() {
        val n = matrixInputStrings.size
        if (n == 0) return

        val newMatrix = Array(n) {
            Array(n + 1) {
                val randomValue = Random.nextDouble(-10.0, 10.0)
                randomValue.format(2)
            }
        }
        matrixInputStrings = newMatrix
        resultText = ""
        stepLog = listOf()
        errorMessage = null
    }

    fun solve() {
        errorMessage = null
        isLoading = true
        resultText = ""
        stepLog = listOf()

        viewModelScope.launch(Dispatchers.Default) {
            val n = matrixInputStrings.size
            if (n == 0) {
                isLoading = false
                return@launch
            }

            val a: Array<DoubleArray>
            try {
                a = Array(n) { r ->
                    DoubleArray(n + 1) { c ->
                        val value = matrixInputStrings[r][c]
                        if (value == "-" || value.isEmpty()) 0.0 else value.toDoubleOrNull() ?: throw IllegalArgumentException("Row ${r + 1}, Column ${c + 1} has an invalid number: '${value}'.")
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Input Error: ${e.message}"
                isLoading = false
                return@launch
            }

            gaussianElimination(n, a)
            isLoading = false
        }
    }

    private fun gaussianElimination(n: Int, a: Array<DoubleArray>) {
        val newStepLog = mutableListOf<MatrixStep>()
        val x = DoubleArray(n)

        newStepLog.add(MatrixStep("1. Initial Augmented Matrix:", deepCopy(a)))

        try {
            // Forward Elimination
            for (k in 0 until n) {
                // Find pivot and swap
                var iMax = k
                for (i in k + 1 until n) {
                    if (abs(a[i][k]) > abs(a[iMax][k])) {
                        iMax = i
                    }
                }

                if (iMax != k) {
                    val temp = a[k]
                    a[k] = a[iMax]
                    a[iMax] = temp
                    newStepLog.add(MatrixStep("Step ${k + 1}.1: Row Swap R${k + 1} <-> R${iMax + 1} (Maximizing Pivot)", deepCopy(a)))
                }

                if (abs(a[k][k]) < 1e-9) {
                    throw IllegalStateException("The system is singular or has infinite solutions (Zero pivot at row ${k + 1}).")
                }

                // Eliminate other rows
                for (i in k + 1 until n) {
                    val factor = a[i][k] / a[k][k]
                    val operationString = "Step ${k + 1}.2.${i + 1}: Eliminate x${k + 1}. Row Operation:\n **R${i + 1} = R${i + 1} - ${factor.format(4)} * R${k + 1}**"

                    for (j in k until n + 1) {
                        a[i][j] -= factor * a[k][j]
                    }
                    newStepLog.add(MatrixStep(operationString, deepCopy(a)))
                }
            }

            newStepLog.add(MatrixStep("2. Forward Elimination Completed: Matrix is in Row Echelon Form.", deepCopy(a)))

            // Check for inconsistent system (0 = non-zero)
            for (i in n - 1 until n) {
                if (abs(a[i][i]) < 1e-9 && abs(a[i][n]) > 1e-9) {
                    throw IllegalStateException("The system is inconsistent (No solution).")
                }
            }

            // Back Substitution
            for (i in n - 1 downTo 0) {
                var sum = 0.0
                val sumTerms = mutableListOf<String>()

                for (j in i + 1 until n) {
                    sum += a[i][j] * x[j]
                    sumTerms.add("${a[i][j].format(4)} * x${j + 1} (${x[j].format(4)})")
                }

                val numerator = a[i][n] - sum
                val denominator = a[i][i]

                x[i] = numerator / denominator

                val formulaString = "Formula: **x${i + 1} = (b'${i + 1} - Σ(A'${i + 1}j * x_j)) / A'${i + 1}${i + 1}**"
                val calculationString = "Substitution: x${i + 1} = (${a[i][n].format(4)} - (${if (sumTerms.isEmpty()) "0" else sumTerms.joinToString(" + ")})) / ${denominator.format(4)}"
                val resultString = "Final Result: x${i + 1} = ${x[i].format(4)}"

                val fullStepString = "3. Back Substitution for **x${i + 1}**:\n${formulaString}\n${calculationString}\n${resultString}"
                newStepLog.add(MatrixStep(fullStepString, deepCopy(a)))
            }

            // Format results: Using "\n" to make solutions appear line-by-line
            val solutionText = x.mapIndexed { index, value -> "x${index + 1} = ${value.format(4)}" }.joinToString("\n")
            resultText = "Solutions Found:\n$solutionText"

        } catch (e: Exception) {
            errorMessage = e.message
        }

        stepLog = newStepLog
    }

    private fun deepCopy(matrix: Array<DoubleArray>): Array<DoubleArray> {
        return Array(matrix.size) { i -> matrix[i].clone() }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}