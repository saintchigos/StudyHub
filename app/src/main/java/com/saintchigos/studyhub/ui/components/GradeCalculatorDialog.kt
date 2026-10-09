package com.saintchigos.studyhub.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.domain.GradeCalculator
import java.util.Locale

/**
 * Answers "what do I need in the exam to pass?" from three numbers.
 *
 * Nothing is saved. It is a calculator, and the answer is on screen the moment the
 * three boxes are filled, so there is no button to press.
 */
@Composable
fun GradeCalculatorDialog(onDismiss: () -> Unit) {
    var coursework by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("40") }
    var target by remember { mutableStateOf("50") }

    val required = run {
        val c = coursework.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        val t = target.toDoubleOrNull()
        if (c == null || w == null || t == null) null else GradeCalculator.requiredExamMark(c, w, t)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What do I need in the exam?") },
        text = {
            Column {
                NumberField("Coursework mark so far (%)", coursework) { coursework = it }
                Spacer(Modifier.height(8.dp))
                NumberField("Coursework is worth (%)", weight) { weight = it }
                Spacer(Modifier.height(8.dp))
                NumberField("Mark I want overall (%)", target) { target = it }
                Spacer(Modifier.height(16.dp))

                val message = when {
                    required == null -> "Fill in the three boxes with numbers from 0 to 100."
                    GradeCalculator.verdict(required) == GradeCalculator.Verdict.ALREADY_THERE ->
                        "You already have enough. Even 0% in the exam reaches ${target.trim()}%."
                    GradeCalculator.verdict(required) == GradeCalculator.Verdict.OUT_OF_REACH ->
                        "That needs ${format(required)}% in the exam, which is over 100%. " +
                            "Try a lower target."
                    else -> "You need ${format(required)}% in the exam."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() || ch == '.' }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)
