package com.example.gaussianelimination.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gaussianelimination.viewmodel.GaussianEliminationViewModel
import com.example.gaussianelimination.viewmodel.MatrixStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaussianEliminationScreen(viewModel: GaussianEliminationViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gaussian Elimination Solver") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Matrix Size Input
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.sizeInput,
                        onValueChange = viewModel::onSizeInputChange,
                        label = { Text("Matrix Size N") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = viewModel::initializeMatrix,
                        enabled = viewModel.sizeInput.toIntOrNull() ?: 0 in 1..8
                    ) {
                        Text("Initialize Matrix")
                    }
                }
            }

            // Error Display
            viewModel.errorMessage?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(text = errorMsg, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (viewModel.isMatrixInputVisible) {
                // Matrix Title and Random Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Augmented Matrix [A|b]", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = viewModel::fillRandomMatrix) {
                            Text("Random Values")
                        }
                    }
                }

                // Matrix Input Grid
                itemsIndexed(viewModel.matrixInputStrings) { rowIndex, row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEachIndexed { colIndex, value ->
                            val isBColumn = colIndex == row.lastIndex

                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { viewModel.onMatrixElementChange(rowIndex, colIndex, it) },
                                    label = { Text(if (isBColumn) "b${rowIndex + 1}" else "A${rowIndex + 1}${colIndex + 1}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )
                            }
                            // Add a visual separator for the augmented matrix
                            if (colIndex == row.size - 2) {
                                Spacer(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(56.dp)
                                        .padding(vertical = 8.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Solve Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::solve,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Solve Equations")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Solution Result
                if (viewModel.resultText.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Final Solution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = viewModel.resultText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Step-by-Step Log
                if (viewModel.stepLog.isNotEmpty()) {
                    item {
                        Text("Detailed Gaussian Elimination Steps:", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary), modifier = Modifier.padding(top = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(viewModel.stepLog) { step ->
                        StepDisplay(step = step)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StepDisplay(step: MatrixStep) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {

        Text(
            text = step.operation,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (step.operation.contains("Matrix") || step.operation.contains("Swap") || step.operation.contains("Eliminate")) {
            MatrixDisplay(matrix = step.matrixState)
        }
    }
}

@Composable
fun MatrixDisplay(matrix: Array<DoubleArray>) {
    val m = if (matrix.isNotEmpty()) matrix[0].size else 0
    val formatDigits = 4

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            matrix.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { colIndex, value ->
                        val isBColumn = colIndex == m - 1

                        Text(
                            text = value.format(formatDigits),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            textAlign = TextAlign.End,
                            fontWeight = if (isBColumn) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isBColumn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)