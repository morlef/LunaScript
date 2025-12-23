package si.f5.luna3419.script.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimeWithEasingField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, label: String = "Time(ms)"
) {
    val parts = value.split("@")
    val time = parts.getOrNull(0) ?: ""
    val easing = parts.getOrNull(1) ?: ""

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = time, onValueChange = {
                val newE = if (easing.isNotEmpty()) "@$easing" else ""
                onValueChange("$it$newE")
            }, label = { Text(label) }, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            OutlinedTextField(value = easing, onValueChange = {
                val newE = if (it.isNotEmpty()) "@$it" else ""
                onValueChange("$time$newE")
            }, label = { Text("Easing") }, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.ArrowDropDown, "Select Easing")
                }
            })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("linear", "ease-in", "ease-out", "ease-in-out").forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = {
                        val newE = "@$type"
                        onValueChange("$time$newE")
                        expanded = false
                    })
                }
            }
        }
    }
}
