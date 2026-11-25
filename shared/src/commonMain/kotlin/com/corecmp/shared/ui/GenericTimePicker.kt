package com.corecmp.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun GenericTimePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedTime: String?, // Format: "HH:mm"
    onTimeSelected: (String) -> Unit
) {
    if (!show) return

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    var selectedHour by remember(show) {
        mutableStateOf(
            if (selectedTime.isNullOrBlank()) {
                val h = now.hour
                if (h == 0) 12 else if (h > 12) h - 12 else h
            } else {
                try {
                    val cleanTime = selectedTime.trim()
                    val parts = cleanTime.split(" ")
                    val timePart = parts[0]
                    val hRaw = timePart.split(":")[0].toIntOrNull() ?: now.hour
                    if (parts.size >= 2) {
                        // "06:05 PM" format
                        if (hRaw == 0) 12 else if (hRaw > 12) hRaw - 12 else hRaw
                    } else {
                        // "18:05" 24-hour format
                        if (hRaw == 0) 12 else if (hRaw > 12) hRaw - 12 else hRaw
                    }
                } catch (e: Exception) {
                    val h = now.hour
                    if (h == 0) 12 else if (h > 12) h - 12 else h
                }
            }
        )
    }

    var selectedMinute by remember(show) {
        mutableStateOf(
            if (selectedTime.isNullOrBlank()) now.minute
            else {
                try {
                    val cleanTime = selectedTime.trim()
                    val timePart = cleanTime.split(" ")[0]
                    timePart.split(":")[1].toIntOrNull() ?: now.minute
                } catch (e: Exception) {
                    now.minute
                }
            }
        )
    }

    var isAm by remember(show) {
        mutableStateOf(
            if (selectedTime.isNullOrBlank()) now.hour < 12
            else {
                try {
                    val cleanTime = selectedTime.trim()
                    val parts = cleanTime.split(" ")
                    if (parts.size >= 2) {
                        parts[1].equals("AM", ignoreCase = true)
                    } else {
                        val hRaw = parts[0].split(":")[0].toIntOrNull() ?: now.hour
                        hRaw < 12
                    }
                } catch (e: Exception) {
                    now.hour < 12
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        confirmButton = {
            TextButton(
                onClick = {
                    val hour12 = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
                    val amPm = if (isAm) "AM" else "PM"
                    val formattedTime = "${hour12.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')} $amPm"
                    onTimeSelected(formattedTime)
                    onDismiss()
                }
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        title = { Text("Select Time", style = MaterialTheme.typography.titleMedium) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PickerColumn(
                    range = (1..12).toList(),
                    selected = selectedHour,
                    onSelectedChange = { selectedHour = it },
                    label = "Hour"
                )

                PickerColumn(
                    range = (0..59).toList(),
                    selected = selectedMinute,
                    onSelectedChange = { selectedMinute = it },
                    label = "Min"
                )

                PickerColumn(
                    range = listOf(0, 1),
                    selected = if (isAm) 0 else 1,
                    onSelectedChange = { isAm = it == 0 },
                    label = "AM/PM",
                    displayMapper = { if (it == 0) "AM" else "PM" }
                )
            }
        }
    )
}

@Composable
fun PickerColumn(
    range: List<Int>,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    label: String,
    displayMapper: (Int) -> String = { it.toString().padStart(2, '0') }
) {
    val listState = rememberLazyListState()


    LaunchedEffect(Unit) {
        val index = range.indexOf(selected)
        val itemHeight = 56
        val listHeight = 140
        val centerOffset = (listHeight / 2) - (itemHeight / 2)
        if (index >= 0) {
            listState.scrollToItem(index = index, scrollOffset = -centerOffset)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.height(240.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(range) { item ->
                val isSelected = item == selected
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable {
                            // Yahan sirf value change hogi, LaunchedEffect trigger nahi hoga
                            onSelectedChange(item)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = displayMapper(item),
                        style = if (isSelected)
                            MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                        else
                            MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }
            }
        }
    }
}
