@file:Suppress("AssignedValueIsNeverRead")

package tech.chiji.fuellog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import tech.chiji.fuellog.data.DriveEntity
import tech.chiji.fuellog.data.FillUpEntity
import tech.chiji.fuellog.ui.theme.FuelLogTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
private sealed interface FuelLogDestination : NavKey {
    @Serializable
    data object FillUps : FuelLogDestination

    @Serializable
    data object Drives : FuelLogDestination
}

private enum class FuelLogDialog {
    FillUp,
    FillUpDetail,
    DriveStart,
    DriveComplete,
    DriveDetail,
}

@Composable
fun FuelLogRoute(
    viewModel: FuelLogViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FuelLogApp(
        uiState = uiState,
        onAddFillUp = viewModel::addFillUp,
        onStartDrive = viewModel::startDrive,
        onCompleteDrive = viewModel::completeDrive,
        onDeleteFillUp = viewModel::deleteFillUp,
        onDeleteDrive = viewModel::deleteDrive,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogApp(
    uiState: FuelLogUiState,
    onAddFillUp: (Double, Double, Int) -> Unit,
    onStartDrive: (Double) -> Unit,
    onCompleteDrive: (Long, Double) -> Unit,
    onDeleteFillUp: (Long) -> Unit,
    onDeleteDrive: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(FuelLogDestination.FillUps)
    val currentDestination = backStack.lastOrNull() ?: FuelLogDestination.FillUps
    val canEstimateDriveCost = uiState.latestFuelEconomyKmPerLiter != null && uiState.latestYenPerLiter != null
    var dialog by rememberSaveable { mutableStateOf<FuelLogDialog?>(null) }
    var selectedFillUpId by rememberSaveable { mutableStateOf<Long?>(null) }
    var completingDriveId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDriveId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("FuelLog") },
            )
        },
        floatingActionButton = {
            if (currentDestination != FuelLogDestination.Drives || canEstimateDriveCost) {
                ExtendedFloatingActionButton(
                    onClick = {
                        dialog = when (currentDestination) {
                            FuelLogDestination.FillUps -> FuelLogDialog.FillUp
                            FuelLogDestination.Drives -> FuelLogDialog.DriveStart
                            else -> FuelLogDialog.FillUp
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            when (currentDestination) {
                                FuelLogDestination.FillUps -> "給油を記録"
                                FuelLogDestination.Drives -> "ドライブ開始"
                                else -> "記録"
                            },
                        )
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == FuelLogDestination.FillUps,
                    onClick = { backStack.replaceWith(FuelLogDestination.FillUps) },
                    icon = {
                        Icon(
                            imageVector = if (currentDestination == FuelLogDestination.FillUps) {
                                Icons.Filled.LocalGasStation
                            } else {
                                Icons.Outlined.LocalGasStation
                            },
                            contentDescription = null,
                        )
                    },
                    label = { Text("給油") },
                )
                NavigationBarItem(
                    selected = currentDestination == FuelLogDestination.Drives,
                    onClick = { backStack.replaceWith(FuelLogDestination.Drives) },
                    icon = {
                        Icon(
                            imageVector = if (currentDestination == FuelLogDestination.Drives) {
                                Icons.Filled.DirectionsCar
                            } else {
                                Icons.Outlined.DirectionsCar
                            },
                            contentDescription = null,
                        )
                    },
                    label = { Text("ドライブ") },
                )
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {
                entry(FuelLogDestination.FillUps) {
                    FillUpsScreen(
                        uiState = uiState,
                        onFillUpClick = { fillUpId ->
                            selectedFillUpId = fillUpId
                            dialog = FuelLogDialog.FillUpDetail
                        },
                    )
                }
                entry(FuelLogDestination.Drives) {
                    DrivesScreen(
                        uiState = uiState,
                        canEstimateDriveCost = canEstimateDriveCost,
                        onCompleteDriveClick = { driveId ->
                            completingDriveId = driveId
                            dialog = FuelLogDialog.DriveComplete
                        },
                        onDriveClick = { driveId ->
                            selectedDriveId = driveId
                            dialog = FuelLogDialog.DriveDetail
                        },
                    )
                }
            },
        )
    }

    when (dialog) {
        FuelLogDialog.FillUp -> FillUpDialog(
            onDismiss = { dialog = null },
            onSave = { odometerKm, liters, totalYen ->
                onAddFillUp(odometerKm, liters, totalYen)
                dialog = null
            },
        )

        FuelLogDialog.FillUpDetail -> FillUpDetailDialog(
            fillUp = selectedFillUpId?.let { id -> uiState.fillUps.firstOrNull { it.id == id } },
            onDismiss = {
                selectedFillUpId = null
                dialog = null
            },
            onDelete = { fillUpId ->
                onDeleteFillUp(fillUpId)
                selectedFillUpId = null
                dialog = null
            },
        )

        FuelLogDialog.DriveStart -> DriveStartDialog(
            onDismiss = { dialog = null },
            onSave = { startOdometerKm ->
                onStartDrive(startOdometerKm)
                dialog = null
            },
        )

        FuelLogDialog.DriveComplete -> DriveCompleteDialog(
            drive = completingDriveId?.let { id -> uiState.drives.firstOrNull { it.id == id } },
            onDismiss = {
                completingDriveId = null
                dialog = null
            },
            onSave = { driveId, endOdometerKm ->
                onCompleteDrive(driveId, endOdometerKm)
                completingDriveId = null
                dialog = null
            },
            onDelete = { driveId ->
                onDeleteDrive(driveId)
                completingDriveId = null
                dialog = null
            },
        )

        FuelLogDialog.DriveDetail -> DriveDetailDialog(
            drive = selectedDriveId?.let { id -> uiState.drives.firstOrNull { it.id == id } },
            onDismiss = {
                selectedDriveId = null
                dialog = null
            },
            onDelete = { driveId ->
                onDeleteDrive(driveId)
                selectedDriveId = null
                dialog = null
            },
        )

        null -> Unit
    }
}

@Composable
private fun FillUpsScreen(
    uiState: FuelLogUiState,
    onFillUpClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            FuelEconomySummary(uiState = uiState)
        }
        items(uiState.fillUps, key = { it.id }) { fillUp ->
            ListItem(
                modifier = Modifier.clickable { onFillUpClick(fillUp.id) },
                headlineContent = {
                    Text("${fillUp.liters.format(2)} L / ${fillUp.totalYen.formatYen()}")
                },
                supportingContent = {
                    Text("${fillUp.odometerKm.format(1)} km")
                },
                trailingContent = {
                    Text("${(fillUp.totalYen / fillUp.liters).format(1)} 円/L")
                },
            )
            HorizontalDivider(thickness = DividerDefaults.Thickness)
        }
    }
}

@Composable
private fun FuelEconomySummary(uiState: FuelLogUiState) {
    ListItem(
        headlineContent = {
            Text(
                text = uiState.latestFuelEconomyKmPerLiter?.let {
                    "実燃費 ${it.format(2)} km/L"
                } ?: "2回給油すると実燃費を計算できます",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = uiState.latestFuelEconomyKmPerLiter?.let {
                    "直近2回の給油記録から計算しています"
                } ?: "給油量、金額、給油時の総走行距離を記録してください",
            )
        },
        leadingContent = {
            Icon(Icons.Default.LocalGasStation, contentDescription = null)
        },
    )
    HorizontalDivider()
}

@Composable
private fun DrivesScreen(
    uiState: FuelLogUiState,
    canEstimateDriveCost: Boolean,
    onCompleteDriveClick: (Long) -> Unit,
    onDriveClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = if (canEstimateDriveCost) {
                            "ドライブのガソリン代を計算できます"
                        } else {
                            "給油記録が2回必要です"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(
                        if (canEstimateDriveCost) {
                            "実燃費 ${uiState.latestFuelEconomyKmPerLiter?.format(2)} km/L を使って、走行距離から概算します"
                        } else {
                            "実燃費が出ると、ドライブのガソリン代を計算できます"
                        },
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                },
            )
            HorizontalDivider()
        }
        if (canEstimateDriveCost) {
            items(uiState.drives, key = { it.id }) { drive ->
            val endOdometerKm = drive.endOdometerKm
            val distanceKm = endOdometerKm?.let { it - drive.startOdometerKm }
            ListItem(
                modifier = if (endOdometerKm == null) {
                    Modifier.clickable { onCompleteDriveClick(drive.id) }
                } else {
                    Modifier.clickable { onDriveClick(drive.id) }
                },
                headlineContent = {
                    Text(
                        distanceKm?.let {
                            "${it.format(1)} km"
                        } ?: "ドライブ中",
                    )
                },
                supportingContent = {
                    Text(
                        endOdometerKm?.let {
                            "${drive.startOdometerKm.format(1)} km → ${it.format(1)} km"
                        } ?: "出発 ${drive.startOdometerKm.format(1)} km",
                    )
                },
                trailingContent = {
                    Text(
                        when {
                            endOdometerKm == null -> "未完了"
                            drive.estimatedFuelCostYen == null -> "未計算"
                            else -> drive.estimatedFuelCostYen.formatYen()
                        },
                    )
                },
            )
            HorizontalDivider(thickness = DividerDefaults.Thickness)
            }
        }
    }
}

@Composable
private fun FillUpDialog(
    onDismiss: () -> Unit,
    onSave: (Double, Double, Int) -> Unit,
) {
    var odometer by rememberSaveable { mutableStateOf("") }
    var liters by rememberSaveable { mutableStateOf("") }
    var totalYen by rememberSaveable { mutableStateOf("") }
    val parsedOdometer = odometer.toDoubleOrNull()
    val parsedLiters = liters.toDoubleOrNull()
    val parsedTotalYen = totalYen.toIntOrNull()
    val canSave = parsedOdometer != null && parsedLiters != null && parsedLiters > 0.0 && parsedTotalYen != null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(parsedOdometer!!, parsedLiters!!, parsedTotalYen!!) },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
        title = { Text("給油を記録") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = "給油量 L",
                )
                NumberTextField(
                    value = totalYen,
                    onValueChange = { totalYen = it },
                    label = "金額 円",
                    keyboardType = KeyboardType.Number,
                )
                NumberTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = "給油時の総走行距離 km",
                )
            }
        },
    )
}

@Composable
private fun FillUpDetailDialog(
    fillUp: FillUpEntity?,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        dismissButton = {
            if (fillUp != null) {
                DeleteTextButton(onClick = { onDelete(fillUp.id) })
            }
        },
        title = { Text("給油記録") },
        text = {
            if (fillUp == null) {
                Text("対象の給油記録が見つかりません。")
            } else {
                DetailColumn {
                    DetailLine("総走行距離", "${fillUp.odometerKm.format(1)} km")
                    DetailLine("給油量", "${fillUp.liters.format(2)} L")
                    DetailLine("金額", fillUp.totalYen.formatYen())
                    DetailLine("単価", "${(fillUp.totalYen / fillUp.liters).format(1)} 円/L")
                    DetailLine("記録日時", fillUp.recordedAtMillis.formatRecordedAt())
                }
            }
        },
    )
}

@Composable
private fun DriveStartDialog(
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf("") }
    val parsedStart = start.toDoubleOrNull()
    val canSave = parsedStart != null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(parsedStart!!) },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
        title = { Text("ドライブ開始") },
        text = {
            NumberTextField(
                value = start,
                onValueChange = { start = it },
                label = "出発前の総走行距離 km",
            )
        },
    )
}

@Composable
private fun DriveCompleteDialog(
    drive: DriveEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, Double) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var end by rememberSaveable { mutableStateOf("") }
    val parsedEnd = end.toDoubleOrNull()
    val canSave = drive != null && parsedEnd != null && parsedEnd > drive.startOdometerKm

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(drive!!.id, parsedEnd!!) },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            if (drive != null) {
                DeleteTextButton(onClick = { onDelete(drive.id) })
            } else {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        },
        title = { Text("ドライブ完了") },
        text = {
            when {
                drive == null -> Text("対象のドライブ記録が見つかりません。")
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("出発 ${drive.startOdometerKm.format(1)} km")
                        NumberTextField(
                            value = end,
                            onValueChange = { end = it },
                            label = "ドライブ後の総走行距離 km",
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DriveDetailDialog(
    drive: DriveEntity?,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    val endOdometerKm = drive?.endOdometerKm
    val distanceKm = if (drive != null && endOdometerKm != null) {
        endOdometerKm - drive.startOdometerKm
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        dismissButton = {
            if (drive != null) {
                DeleteTextButton(onClick = { onDelete(drive.id) })
            }
        },
        title = { Text("ドライブ記録") },
        text = {
            if (drive == null) {
                Text("対象のドライブ記録が見つかりません。")
            } else {
                DetailColumn {
                    DetailLine("出発距離", "${drive.startOdometerKm.format(1)} km")
                    DetailLine("到着距離", endOdometerKm?.let { "${it.format(1)} km" } ?: "未完了")
                    DetailLine("走行距離", distanceKm?.let { "${it.format(1)} km" } ?: "未完了")
                    DetailLine("概算ガソリン代", drive.estimatedFuelCostYen?.formatYen() ?: "未計算")
                    if (endOdometerKm != null && drive.estimatedFuelCostYen == null) {
                        DetailLine("計算基準", "給油記録が2回そろうと計算できます")
                    }
                    DetailLine("記録日時", drive.recordedAtMillis.formatRecordedAt())
                    drive.completedAtMillis?.let {
                        DetailLine("完了日時", it.formatRecordedAt())
                    }
                }
            }
        },
    )
}

@Composable
private fun DetailColumn(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DeleteTextButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text("削除")
    }
}

@Composable
private fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

private fun <T> MutableList<T>.replaceWith(value: T) {
    clear()
    add(value)
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)

private fun Int.formatYen(): String = "%,d円".format(this)

private fun Long.formatRecordedAt(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(recordedAtFormatter)

private val recordedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

@Preview(showBackground = true)
@Composable
private fun FuelLogAppPreview() {
    FuelLogTheme {
        FuelLogApp(
            uiState = FuelLogUiState(
                fillUps = listOf(
                    FillUpEntity(id = 2, odometerKm = 12_420.0, liters = 32.4, totalYen = 5_508),
                    FillUpEntity(id = 1, odometerKm = 12_050.0, liters = 30.8, totalYen = 5_236),
                ),
                drives = listOf(
                    DriveEntity(
                        id = 1,
                        startOdometerKm = 12_421.0,
                        endOdometerKm = 12_598.0,
                        estimatedFuelCostYen = 2_633,
                        fuelEconomyKmPerLiter = 11.42,
                        yenPerLiter = 170.0,
                        completedAtMillis = 1_777_777_777_000,
                    ),
                    DriveEntity(
                        id = 2,
                        startOdometerKm = 12_650.0,
                        endOdometerKm = null,
                        estimatedFuelCostYen = null,
                        fuelEconomyKmPerLiter = null,
                        yenPerLiter = null,
                    ),
                ),
                latestFuelEconomyKmPerLiter = 11.42,
                latestYenPerLiter = 170.0,
            ),
            onAddFillUp = { _, _, _ -> },
            onStartDrive = {},
            onCompleteDrive = { _, _ -> },
            onDeleteFillUp = {},
            onDeleteDrive = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyFuelLogAppPreview() {
    FuelLogTheme {
        FuelLogApp(
            uiState = FuelLogUiState(),
            onAddFillUp = { _, _, _ -> },
            onStartDrive = {},
            onCompleteDrive = { _, _ -> },
            onDeleteFillUp = {},
            onDeleteDrive = {},
        )
    }
}
