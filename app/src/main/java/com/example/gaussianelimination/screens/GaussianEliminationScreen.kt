package com.example.gaussianelimination.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gaussianelimination.viewmodel.GaussianEliminationViewModel
import com.example.gaussianelimination.viewmodel.MatrixStep
import com.example.gaussianelimination.viewmodel.MAX_SIZE
import com.example.gaussianelimination.viewmodel.MIN_SIZE
import com.example.gaussianelimination.viewmodel.formatSmart
import kotlin.math.abs

private const val MATRIX_COMPARE_EPS = 1e-9
private const val FINAL_MATRIX_MARKER = "Final matrix" // detect final-matrix step by this prefix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaussianEliminationScreen(viewModel: GaussianEliminationViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gaussian Elimination Solver") }) },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.activeCell != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NumericKeypad(onKeyPress = viewModel::onKeyPress)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Number of Equations: ${viewModel.equations}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = viewModel.equations.toFloat(),
                        onValueChange = viewModel::onEquationsChange,
                        valueRange = MIN_SIZE.toFloat()..MAX_SIZE.toFloat(),
                        steps = (MAX_SIZE - MIN_SIZE - 1),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Number of Unknowns: ${viewModel.unknowns}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = viewModel.unknowns.toFloat(),
                        onValueChange = viewModel::onUnknownsChange,
                        valueRange = MIN_SIZE.toFloat()..MAX_SIZE.toFloat(),
                        steps = (MAX_SIZE - MIN_SIZE - 1),
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Button(
                        onClick = viewModel::initializeMatrix,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Initialize Matrix (${viewModel.equations} x ${viewModel.unknowns})")
                    }
                }
            }

            item {
                AnimatedVisibility(visible = viewModel.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(text = viewModel.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = viewModel.isMatrixInputVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "Augmented Matrix [A | b]",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::fillRandomMatrix,
                                    modifier = Modifier.widthIn(min = 130.dp)
                                ) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("Random", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = viewModel::initializeMatrix,
                                    modifier = Modifier.widthIn(min = 130.dp)
                                ) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clear", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            itemsIndexed(viewModel.matrixInputStrings) { rowIndex, row ->
                AnimatedVisibility(visible = viewModel.isMatrixInputVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEachIndexed { colIndex, value ->
                            val isBCol = colIndex == row.lastIndex
                            val isActive = viewModel.activeCell == Pair(rowIndex, colIndex)
                            MatrixCell(
                                value = value,
                                isActive = isActive,
                                isBColumn = isBCol,
                                onClick = { viewModel.onCellClick(rowIndex, colIndex) },
                                modifier = Modifier.weight(1f)
                            )
                            if (colIndex == row.size - 2) {
                                Spacer(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = viewModel.isMatrixInputVisible) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::solve,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.isMatrixReady && !viewModel.isLoading
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Solve", modifier = Modifier.padding(end = 8.dp))
                                Text("Solve")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = viewModel.resultText.isNotEmpty() && !viewModel.isLoading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val isError = viewModel.errorMessage != null
                    val isUnique = viewModel.resultText.contains("Unique solution")
                    val isInfinite = viewModel.resultText.contains("Infinite solutions")
                    val icon = when {
                        isError -> Icons.Default.Error
                        isUnique -> Icons.Default.CheckCircle
                        isInfinite -> Icons.Default.AllInclusive
                        else -> Icons.Default.Info
                    }
                    val containerColor = when {
                        isError -> MaterialTheme.colorScheme.errorContainer
                        isUnique -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        isInfinite -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = "Result type", modifier = Modifier.padding(end = 8.dp))
                                Text("Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Text(viewModel.resultText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Steps list: use itemsIndexed so we can compare each step with previous
            if (viewModel.stepLog.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = viewModel.stepLog.isNotEmpty() && !viewModel.isLoading,
                        enter = fadeIn(animationSpec = tween(delayMillis = 100))
                    ) {
                        Column {
                            Text("Steps:", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                itemsIndexed(viewModel.stepLog) { index, step ->
                    AnimatedVisibility(
                        visible = viewModel.stepLog.isNotEmpty() && !viewModel.isLoading,
                        enter = fadeIn(animationSpec = tween(delayMillis = 200)),
                    ) {
                        val prevMatrix = if (index > 0) viewModel.stepLog[index - 1].matrixState else null
                        StepDisplay(step = step, prevMatrix = prevMatrix)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = viewModel.stepLog.isNotEmpty() && !viewModel.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.resetUiState()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("New Matrix")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StepDisplay(step: MatrixStep, prevMatrix: Array<DoubleArray>?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {

        // If this step is the block of back-substitution equations, render them prominently
        val backMarker = "Back-substitution equations:"
        if (step.operation.startsWith(backMarker)) {
            val block = step.operation.removePrefix(backMarker).trimStart('\n')
            val lines = if (block.isEmpty()) listOf() else block.split("\n").map { it.trim() }

            Text(
                text = "Back-substitution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // show matrix only if it changed compared to previous snapshot (to avoid duplication)
            if (!matricesEqual(step.matrixState, prevMatrix)) {
                MatrixDisplay(matrix = step.matrixState)
            }
            return
        }

        // Default rendering for other steps
        Text(step.operation, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))

        // If this is the final matrix step (detect by prefix), ALWAYS show matrix regardless of prevMatrix
        if (step.operation.startsWith(FINAL_MATRIX_MARKER)) {
            MatrixDisplay(matrix = step.matrixState)
            return
        }

        // Otherwise: show matrix ONLY if different from previous step's matrixState (or if no previous)
        if (!matricesEqual(step.matrixState, prevMatrix)) {
            MatrixDisplay(matrix = step.matrixState)
        }
    }
}

/**
 * Compare two matrix snapshots. Return true if equal (within EPS), false if different.
 * If prev is null -> considered different (so we will show current).
 */
private fun matricesEqual(a: Array<DoubleArray>?, b: Array<DoubleArray>?): Boolean {
    if (a === b) return true
    if (a == null || b == null) return false
    if (a.size != b.size) return false
    if (a.isEmpty()) return b.isEmpty()
    if (a[0].size != b[0].size) return false
    val rows = a.size
    val cols = a[0].size
    for (i in 0 until rows) {
        for (j in 0 until cols) {
            if (abs(a[i][j] - b[i][j]) > MATRIX_COMPARE_EPS) return false
        }
    }
    return true
}

@Composable
fun MatrixCell(
    value: String,
    isActive: Boolean,
    isBColumn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val containerColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isBColumn -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    // If the value is a valid Double, display using formatSmart for consistent formatting.
    val displayText = value.toDoubleOrNull()?.let { formatSmart(it) } ?: value

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(containerColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            fontWeight = if (isBColumn) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NumericKeypad(onKeyPress: (String) -> Unit) {
    val keys = listOf(
        listOf("7", "8", "9", "DEL"),
        listOf("4", "5", "6", "Clear"),
        listOf("1", "2", "3", "-"),
        listOf("DONE", "0", ".", "->")
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    row.forEach { key ->
                        if (key.isBlank()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            Button(
                                onClick = { onKeyPress(key) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (key == "DEL" || key == "Clear") {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                } else if (key == "DONE") {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixDisplay(matrix: Array<DoubleArray>) {
    val rows = matrix.size
    val cols = if (rows > 0) matrix[0].size else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until cols) {
                        val isBCol = c == cols - 1
                        Text(
                            text = formatSmart(matrix[r][c]),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            textAlign = TextAlign.End,
                            fontWeight = if (isBCol) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isBCol) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (c % 2 == 1 && c != cols - 1) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
                if (r % 2 == 1 && r != rows - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
