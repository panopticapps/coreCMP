package com.corecmp.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime


enum class DateRestrictionType {
    NONE,
    PAST_ONLY,
    FUTURE_ONLY,
    CUSTOM_RANGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericDatePicker(
    show: Boolean,
    isRangePicker: Boolean = false,
    restrictionType: DateRestrictionType = DateRestrictionType.NONE,
    pastLimitDays: Int? = null,
    futureLimitDays: Int? = null,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    selectedDateRange: String? = null,
    initialSelectedDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onDateSelected: (startDate: Long?, endDate: Long?) -> Unit
) {

    if (!show) return

    val now = Clock.System.now().toEpochMilliseconds()
    val todayMillis = remember { todayStartOfDayUtcMillis() }

    val pastMin = pastLimitDays?.let { now - (it * 24 * 60 * 60 * 1000L) }
    val futureMax = futureLimitDays?.let { now + (it * 24 * 60 * 60 * 1000L) }

    val selectableDates = remember(
        restrictionType,
        pastLimitDays,
        futureLimitDays,
        minDateMillis,
        maxDateMillis
    ) {

        object : SelectableDates {

            override fun isSelectableDate(utcTimeMillis: Long): Boolean {

                val afterMin = when {
                    minDateMillis != null -> utcTimeMillis >= minDateMillis
                    pastMin != null -> utcTimeMillis >= pastMin
                    else -> true
                }

                val beforeMax = when {
                    maxDateMillis != null -> utcTimeMillis <= maxDateMillis
                    futureMax != null -> utcTimeMillis <= futureMax
                    else -> true
                }

                val restriction = when (restrictionType) {

                    DateRestrictionType.NONE -> true

                    DateRestrictionType.PAST_ONLY ->
                        utcTimeMillis <= now

                    DateRestrictionType.FUTURE_ONLY ->
                        utcTimeMillis >= now

                    DateRestrictionType.CUSTOM_RANGE ->
                        afterMin && beforeMax
                }

                return afterMin && beforeMax && restriction
            }
        }
    }

    if (isRangePicker) {

        val (parsedStartMillis, parsedEndMillis) = remember(selectedDateRange) {
            parseDateRangeToMillis(selectedDateRange)
        }

        val initialStartMillis = remember(
            parsedStartMillis,
            parsedEndMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveRangeInitialStartMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                parsedStartMillis = parsedStartMillis,
                parsedEndMillis = parsedEndMillis
            )
        }

        val initialEndMillis = remember(
            parsedStartMillis,
            parsedEndMillis,
            selectableDates
        ) {
            if (parsedStartMillis != null &&
                parsedEndMillis != null &&
                selectableDates.isSelectableDate(parsedStartMillis) &&
                selectableDates.isSelectableDate(parsedEndMillis)
            ) {
                parsedEndMillis
            } else {
                null
            }
        }

        val initialDisplayedMonthMillis = remember(
            initialStartMillis,
            parsedStartMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveDisplayedMonthMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = initialStartMillis ?: parsedStartMillis
            )
        }

        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            selectableDates = selectableDates
        )

        RelaxCalendarDialog(
            onDismiss = onDismiss,
            confirmText = "Apply range",
            onConfirm = {
                onDateSelected(
                    state.selectedStartDateMillis,
                    state.selectedEndDateMillis
                )
                onDismiss()
            }
        ) {
            CalendarRangePicker(state = state)
        }

    } else {

        val initialSelectedDate = remember(
            initialSelectedDateMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveSingleInitialDateMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = initialSelectedDateMillis
            )
        }

        val initialDisplayedMonthMillis = remember(
            initialSelectedDate,
            initialSelectedDateMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveDisplayedMonthMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = initialSelectedDateMillis ?: initialSelectedDate
            )
        }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDate,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            selectableDates = selectableDates
        )

        RelaxCalendarDialog(
            onDismiss = onDismiss,
            onConfirm = {
                onDateSelected(
                    state.selectedDateMillis,
                    null
                )
                onDismiss()
            }
        ) {
            CalendarDatePicker(state = state)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyDatePicker(
    show: Boolean,
    isRangePicker: Boolean = false,
    restrictionType: DateRestrictionType = DateRestrictionType.NONE,
    minDaysFromToday: Int? = null,
    maxDaysFromToday: Int? = null,
    selectedDateString: String? = null,
    useCurrentDateAsDefault: Boolean = false,
    onDismiss: () -> Unit,
    onDateSelected: (startDate: Long?, endDate: Long?) -> Unit
) {

    if (!show) return

    val now = Clock.System.now().toEpochMilliseconds()
    val todayMillis = remember { todayStartOfDayUtcMillis() }

    val minDateMillis = remember(minDaysFromToday) {
        minDaysFromToday?.let {
            now + (it * 24 * 60 * 60 * 1000L)
        }
    }

    val maxDateMillis = remember(maxDaysFromToday) {
        maxDaysFromToday?.let {
            now + (it * 24 * 60 * 60 * 1000L)
        }
    }

    val selectableDates = remember(
        restrictionType,
        minDateMillis,
        maxDateMillis
    ) {
        object : SelectableDates {

            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {

                val afterMin = minDateMillis?.let {
                    utcTimeMillis >= it
                } ?: true

                val beforeMax = maxDateMillis?.let {
                    utcTimeMillis <= it
                } ?: true

                val restrictionCheck = when (restrictionType) {

                    DateRestrictionType.NONE -> true

                    DateRestrictionType.PAST_ONLY ->
                        utcTimeMillis <= now

                    DateRestrictionType.FUTURE_ONLY ->
                        utcTimeMillis >= now

                    DateRestrictionType.CUSTOM_RANGE ->
                        afterMin && beforeMax
                }

                return afterMin && beforeMax && restrictionCheck
            }
        }
    }

    if (isRangePicker) {

        val (parsedStartMillis, parsedEndMillis) = remember(selectedDateString) {
            parseDateRangeToMillis(selectedDateString)
        }

        val initialStartMillis = remember(
            parsedStartMillis,
            parsedEndMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveRangeInitialStartMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                parsedStartMillis = parsedStartMillis,
                parsedEndMillis = parsedEndMillis
            )
        }

        val initialEndMillis = remember(
            parsedStartMillis,
            parsedEndMillis,
            selectableDates
        ) {
            if (parsedStartMillis != null &&
                parsedEndMillis != null &&
                selectableDates.isSelectableDate(parsedStartMillis) &&
                selectableDates.isSelectableDate(parsedEndMillis)
            ) {
                parsedEndMillis
            } else {
                null
            }
        }

        val initialDisplayedMonthMillis = remember(
            initialStartMillis,
            parsedStartMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveDisplayedMonthMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = initialStartMillis ?: parsedStartMillis
            )
        }

        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            selectableDates = selectableDates
        )

        RelaxCalendarDialog(
            onDismiss = onDismiss,
            confirmText = "Apply range",
            onConfirm = {
                onDateSelected(
                    state.selectedStartDateMillis,
                    state.selectedEndDateMillis
                )
                onDismiss()
            }
        ) {
            CalendarRangePicker(state = state)
        }

    } else {

        val parsedMillis = remember(selectedDateString) {
            parseDateStringToMillis(selectedDateString)
        }

        val initialSelectedDate = remember(
            parsedMillis,
            useCurrentDateAsDefault,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            val preferredMillis = if (useCurrentDateAsDefault && parsedMillis == null) {
                todayMillis
            } else {
                parsedMillis
            }

            resolveSingleInitialDateMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = preferredMillis
            )
        }

        val initialDisplayedMonthMillis = remember(
            initialSelectedDate,
            parsedMillis,
            todayMillis,
            minDateMillis,
            maxDateMillis,
            selectableDates
        ) {
            resolveDisplayedMonthMillis(
                selectableDates = selectableDates,
                todayMillis = todayMillis,
                minDateMillis = minDateMillis,
                maxDateMillis = maxDateMillis,
                preferredMillis = parsedMillis ?: initialSelectedDate
            )
        }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDate,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            selectableDates = selectableDates
        )

        RelaxCalendarDialog(
            onDismiss = onDismiss,
            onConfirm = {
                onDateSelected(
                    state.selectedDateMillis,
                    null
                )
                onDismiss()
            }
        ) {
            CalendarDatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelaxCalendarDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "OK",
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val designWidth = 360.dp
            val scale = (maxWidth / designWidth).coerceAtMost(1f)
            val dialogHeight = maxHeight
            Surface(
                modifier = Modifier
                    .width(designWidth * scale)
                    .heightIn(max = dialogHeight),
                shape = DatePickerDefaults.shape,
                color = DatePickerDefaults.colors().containerColor,
            ) {
                Column {
                    Box(
                        Modifier
                            .weight(1f, fill = false)
                            .calendarScale(scale),
                    ) {
                        content()
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = confirmText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePicker(state: DatePickerState) {
    val date = state.selectedDateMillis?.let { formatDateMillis(it) } ?: "Select date"
    DatePicker(
        state = state,
        title = null,
        headline = {
            CalendarHeadline(label = "Select date", value = date)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarRangePicker(state: DateRangePickerState) {
    val scheme = MaterialTheme.colorScheme
    val displayed = Instant.fromEpochMilliseconds(state.displayedMonthMillis)
        .toLocalDateTime(TimeZone.UTC)
        .date
    val firstOfMonth = LocalDate(displayed.year, displayed.month, 1)
    val gridStart = firstOfMonth.minus(
        firstOfMonth.dayOfWeek.isoDayNumber % 7,
        DateTimeUnit.DAY,
    )
    val days = List(42) { index -> gridStart.plus(index, DateTimeUnit.DAY) }
    val previousMonth = firstOfMonth.minus(1, DateTimeUnit.MONTH)
    val nextMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    val canGoPrevious = previousMonth.year >= state.yearRange.first
    val canGoNext = nextMonth.year <= state.yearRange.last
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis
    val today = Instant.fromEpochMilliseconds(todayStartOfDayUtcMillis())
        .toLocalDateTime(TimeZone.UTC)
        .date
    val monthTitle = "${displayed.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayed.year}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Select dates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Text(
            text = "Choose a start date, then an end date.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RangeEndpointBox(
                label = "Start",
                value = startMillis?.let(::formatEndpointDate) ?: "Start date",
                selected = startMillis != null,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
            RangeEndpointBox(
                label = "End",
                value = endMillis?.let(::formatEndpointDate) ?: "End date",
                selected = endMillis != null,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = monthTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            IconButton(
                onClick = { state.displayedMonthMillis = monthStartMillis(previousMonth) },
                enabled = canGoPrevious,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = if (canGoPrevious) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f),
                )
            }
            IconButton(
                onClick = { state.displayedMonthMillis = monthStartMillis(nextMonth) },
                enabled = canGoNext,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = if (canGoNext) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        days.chunked(7)
            .filter { week ->
                week.any { it.month == firstOfMonth.month && it.year == firstOfMonth.year }
            }
            .forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val inDisplayedMonth = date.month == firstOfMonth.month && date.year == firstOfMonth.year
                        if (!inDisplayedMonth) {
                            Spacer(Modifier.weight(1f).height(44.dp))
                            return@forEach
                        }
                        val millis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                        val enabled = state.selectableDates.isSelectableDate(millis)
                        val isStart = millis == startMillis
                        val isEnd = millis == endMillis
                        val inRange = startMillis != null && endMillis != null &&
                            millis in startMillis..endMillis
                        RangeDayCell(
                            day = date.dayOfMonth.toString(),
                            enabled = enabled,
                            isStart = isStart,
                            isEnd = isEnd,
                            inRange = inRange,
                            isToday = date == today,
                            onClick = {
                                if (startMillis == null || endMillis != null) {
                                    state.setSelection(millis, null)
                                } else if (millis >= startMillis) {
                                    state.setSelection(startMillis, millis)
                                } else {
                                    state.setSelection(millis, null)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
    }
}

@Composable
private fun RangeEndpointBox(
    label: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(primary.copy(alpha = if (selected) 0.14f else 0.06f))
            .border(
                width = 1.dp,
                color = if (selected) primary else scheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RangeDayCell(
    day: String,
    enabled: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    inRange: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val endpoint = isStart || isEnd
    Box(
        modifier = modifier
            .height(44.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (inRange) {
            val shape = when {
                isStart && isEnd -> RoundedCornerShape(20.dp)
                isStart -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                isEnd -> RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(primary.copy(alpha = 0.16f), shape),
            )
        }
        if (endpoint) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(primary, CircleShape),
            )
        } else if (isToday && enabled) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, primary, CircleShape),
            )
        }
        Text(
            text = day,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !enabled -> scheme.onSurface.copy(alpha = 0.38f)
                endpoint -> scheme.onPrimary
                else -> scheme.onSurface
            },
        )
    }
}

private fun monthStartMillis(date: kotlinx.datetime.LocalDate): Long {
    return LocalDate(date.year, date.month, 1)
        .atStartOfDayIn(TimeZone.UTC)
        .toEpochMilliseconds()
}

private fun formatEndpointDate(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.UTC)
        .date
    val weekday = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday, ${date.dayOfMonth} $month"
}

@Composable
private fun CalendarHeadline(label: String, value: String) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.calendarScale(scale: Float): Modifier {
    if (scale >= 0.999f) return this
    return layout { measurable, constraints ->
        val childConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = if (constraints.hasBoundedWidth) {
                (constraints.maxWidth / scale).roundToInt()
            } else {
                constraints.maxWidth
            },
            maxHeight = if (constraints.hasBoundedHeight) {
                (constraints.maxHeight / scale).roundToInt()
            } else {
                constraints.maxHeight
            },
        )
        val placeable = measurable.measure(childConstraints)
        val scaledHeight = (placeable.height * scale).roundToInt().coerceAtLeast(0)
        layout(
            width = (placeable.width * scale).roundToInt().coerceAtLeast(0),
            height = if (constraints.hasBoundedHeight) {
                scaledHeight.coerceAtMost(constraints.maxHeight)
            } else {
                scaledHeight
            },
        ) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

private fun parseDdMmYyyyToMillis(date: String): Long? {
    return try {
        val parts = date.trim().split("-")
        if (parts.size != 3) return null

        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()

        LocalDate(year, Month(month), day)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

private fun parseDateRangeToMillis(range: String?): Pair<Long?, Long?> {
    if (range.isNullOrBlank()) return null to null

    val parts = range.split("to", ignoreCase = true, limit = 2)
    if (parts.size != 2) return null to null

    return parseDdMmYyyyToMillis(parts[0]) to parseDdMmYyyyToMillis(parts[1])
}

private fun todayStartOfDayUtcMillis(): Long {
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    return today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

private fun resolveClosestToTodayMillis(
    selectableDates: SelectableDates,
    todayMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
): Long {
    if (selectableDates.isSelectableDate(todayMillis)) return todayMillis

    val candidates = listOfNotNull(todayMillis, minDateMillis, maxDateMillis)
        .distinct()
        .filter { selectableDates.isSelectableDate(it) }

    return candidates.minByOrNull { abs(it - todayMillis) } ?: todayMillis
}

private fun resolveDisplayedMonthMillis(
    selectableDates: SelectableDates,
    todayMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    preferredMillis: Long?,
): Long {
    if (selectableDates.isSelectableDate(todayMillis)) return todayMillis

    if (preferredMillis != null && selectableDates.isSelectableDate(preferredMillis)) {
        return preferredMillis
    }

    return resolveClosestToTodayMillis(
        selectableDates = selectableDates,
        todayMillis = todayMillis,
        minDateMillis = minDateMillis,
        maxDateMillis = maxDateMillis
    )
}

private fun resolveSingleInitialDateMillis(
    selectableDates: SelectableDates,
    todayMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    preferredMillis: Long?,
): Long? {
    if (preferredMillis != null && selectableDates.isSelectableDate(preferredMillis)) {
        return preferredMillis
    }

    return resolveClosestToTodayMillis(
        selectableDates = selectableDates,
        todayMillis = todayMillis,
        minDateMillis = minDateMillis,
        maxDateMillis = maxDateMillis
    )
}

private fun resolveRangeInitialStartMillis(
    selectableDates: SelectableDates,
    todayMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    parsedStartMillis: Long?,
    parsedEndMillis: Long?,
): Long? {
    if (parsedStartMillis != null &&
        parsedEndMillis != null &&
        selectableDates.isSelectableDate(parsedStartMillis) &&
        selectableDates.isSelectableDate(parsedEndMillis)
    ) {
        return parsedStartMillis
    }

    return resolveClosestToTodayMillis(
        selectableDates = selectableDates,
        todayMillis = todayMillis,
        minDateMillis = minDateMillis,
        maxDateMillis = maxDateMillis
    )
}

fun parseDateStringToMillis(
    date: String?,
): Long? {
    return try {
        if (date.isNullOrEmpty()) return null

        val parts = date.split(" ")
        val day = parts[0].toInt()

        val month = Month.entries.first {
            it.name.take(3).equals(parts[1], ignoreCase = true)
        }

        val year = parts[2].toInt()

        // ✅ ALWAYS UTC
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

    } catch (e: Exception) {
        null
    }
}

fun resolveInitialDateMillis(
    parsedMillis: Long?,
    useCurrentDateAsDefault: Boolean,
    now: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?
): Long? {

    fun coerceInRange(value: Long): Long {
        var v = value
        if (minDateMillis != null && v < minDateMillis) v = minDateMillis
        if (maxDateMillis != null && v > maxDateMillis) v = maxDateMillis
        return v
    }

    return when {
        // ✅ valid parsed date
        parsedMillis != null -> coerceInRange(parsedMillis)

        // ✅ fallback to current date
        useCurrentDateAsDefault -> coerceInRange(now)

        // ❌ no default
        else -> null
    }
}