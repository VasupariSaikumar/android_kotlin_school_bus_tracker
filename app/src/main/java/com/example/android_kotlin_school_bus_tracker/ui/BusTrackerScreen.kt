package com.example.android_kotlin_school_bus_tracker.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Colour palette ──────────────────────────────────────────────────────
private val BusYellow = Color(0xFFFFC107)
private val BusOrange = Color(0xFFFF9800)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF16213E)
private val AccentGreen = Color(0xFF00E676)
private val AccentRed = Color(0xFFFF5252)
private val TextWhite = Color(0xFFEEEEEE)
private val TextGrey = Color(0xFF9E9E9E)

// Day labels for the chips (Calendar constants)
private val DAYS = listOf(
    Calendar.MONDAY to "Mon",
    Calendar.TUESDAY to "Tue",
    Calendar.WEDNESDAY to "Wed",
    Calendar.THURSDAY to "Thu",
    Calendar.FRIDAY to "Fri",
    Calendar.SATURDAY to "Sat"
)

// ═════════════════════════════════════════════════════════════════════════
// Main Screen
// ═════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackerApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }
    var showAddStopDialog by remember { mutableStateOf(false) }

    // ── Permission launcher ─────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled implicitly */ }

    // Show snackbar on message changes
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "School Bus Tracker",
                        fontWeight = FontWeight.Bold,
                        color = BusYellow
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Tracking Status Card ────────────────────────────────────
            TrackingStatusCard(
                isTracking = state.isTracking,
                onStart = {
                    // Request permissions if needed
                    val perms = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val missing = perms.filter {
                        ContextCompat.checkSelfPermission(context, it) !=
                                PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        viewModel.startTracking()
                    }
                },
                onStop = { viewModel.stopTracking() }
            )

            Spacer(Modifier.height(20.dp))

            // ── Mode info card ──────────────────────────────────────────
            ModeInfoCard(
                stopsCount = state.stops.size,
                frequencyMinutes = state.frequencyMinutes
            )

            Spacer(Modifier.height(20.dp))

            // ── Stops section ───────────────────────────────────────────
            StopsSection(
                stops = state.stops,
                onAddStop = { showAddStopDialog = true },
                onDeleteStop = { viewModel.deleteStop(it) }
            )

            Spacer(Modifier.height(24.dp))

            // ── Schedule summary ────────────────────────────────────────
            ScheduleSummaryCard(
                hour = state.notifyHour,
                minute = state.notifyMinute,
                activeDays = state.activeDays
            )

            Spacer(Modifier.height(32.dp))
        }

        // ── Settings bottom sheet ───────────────────────────────────────
        if (showSettings) {
            SettingsBottomSheet(
                state = state,
                onDismiss = { showSettings = false },
                onTimeChanged = { h, m -> viewModel.updateNotifyTime(h, m) },
                onDayToggled = { viewModel.toggleDay(it) },
                onFrequencyChanged = { viewModel.updateFrequency(it) },
                onFirebaseSaved = { key, proj, app, db ->
                    viewModel.updateFirebaseConfig(key, proj, app, db)
                }
            )
        }

        // ── Add-stop dialog ─────────────────────────────────────────────
        if (showAddStopDialog) {
            AddStopDialog(
                onDismiss = { showAddStopDialog = false },
                onConfirm = { name ->
                    showAddStopDialog = false
                    // Get current location
                    val fusedClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                            if (loc != null) {
                                viewModel.addCurrentLocationAsStop(
                                    loc.latitude, loc.longitude, name
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Location unavailable. Make sure GPS is enabled."
                                    )
                                }
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Location permission required")
                        }
                    }
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// Tracking Status Card
// ═════════════════════════════════════════════════════════════════════════

@Composable
fun TrackingStatusCard(
    isTracking: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val bgBrush = if (isTracking) {
        Brush.linearGradient(listOf(AccentGreen.copy(alpha = 0.2f), DarkCard))
    } else {
        Brush.linearGradient(listOf(DarkCard, DarkCard))
    }

    val borderColor by animateColorAsState(
        if (isTracking) AccentGreen else Color.Transparent,
        label = "border"
    )

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(if (isTracking) pulse else 1f)
                        .clip(CircleShape)
                        .background(if (isTracking) AccentGreen else AccentRed)
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (isTracking) "GPS TRACKING ACTIVE" else "GPS TRACKING OFF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isTracking) AccentGreen else TextWhite,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isTracking)
                        "Your location is being shared"
                    else
                        "Tap below to start sharing your location",
                    color = TextGrey,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { if (isTracking) onStop() else onStart() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) AccentRed else AccentGreen
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        if (isTracking) Icons.Filled.LocationOn else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isTracking) "Stop Tracking" else "Start Tracking",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// Mode Info Card
// ═════════════════════════════════════════════════════════════════════════

@Composable
fun ModeInfoCard(stopsCount: Int, frequencyMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BusYellow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚌", fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    if (stopsCount > 0) "Stop Proximity Mode" else "Periodic Mode",
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    if (stopsCount > 0)
                        "$stopsCount stop(s) configured – sends location near each stop once/day"
                    else
                        "No stops – sending location every $frequencyMinutes min",
                    color = TextGrey,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// Stops Section
// ═════════════════════════════════════════════════════════════════════════

@Composable
fun StopsSection(
    stops: List<com.example.android_kotlin_school_bus_tracker.domain.Stop>,
    onAddStop: () -> Unit,
    onDeleteStop: (com.example.android_kotlin_school_bus_tracker.domain.Stop) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bus Stops",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Button(
                    onClick = onAddStop,
                    colors = ButtonDefaults.buttonColors(containerColor = BusOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add stop", tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Stop", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (stops.isEmpty()) {
                Text(
                    "No stops saved yet.\nAdd a stop at your current location to enable proximity tracking.",
                    color = TextGrey,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                stops.forEachIndexed { idx, stop ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkSurface.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BusYellow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        color = DarkSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stop.name.ifBlank { "Stop ${idx + 1}" },
                                        color = TextWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "%.5f, %.5f".format(stop.latitude, stop.longitude),
                                        color = TextGrey,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = { onDeleteStop(stop) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = AccentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// Schedule Summary Card
// ═════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleSummaryCard(hour: Int, minute: Int, activeDays: Set<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Schedule", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Daily reminder at %02d:%02d".format(hour, minute),
                color = BusYellow,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAYS.forEach { (dayConst, label) ->
                    val active = activeDays.contains(dayConst)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) BusYellow else DarkSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            color = if (active) DarkSurface else TextGrey,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// Settings Bottom Sheet
// ═════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    state: MainUiState,
    onDismiss: () -> Unit,
    onTimeChanged: (Int, Int) -> Unit,
    onDayToggled: (Int) -> Unit,
    onFrequencyChanged: (Int) -> Unit,
    onFirebaseSaved: (String, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var freqText by remember { mutableStateOf(state.frequencyMinutes.toString()) }
    var apiKey by remember { mutableStateOf(state.firebaseApiKey) }
    var projId by remember { mutableStateOf(state.firebaseProjectId) }
    var appIdField by remember { mutableStateOf(state.firebaseAppId) }
    var dbUrl by remember { mutableStateOf(state.firebaseDatabaseUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Settings",
                color = BusYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ── Notification Time ───────────────────────────────────────
            SectionLabel("Notification Time")
            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimeChanged(h, m) },
                        state.notifyHour,
                        state.notifyMinute,
                        true
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "%02d:%02d".format(state.notifyHour, state.notifyMinute),
                    color = TextWhite,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Active Days ─────────────────────────────────────────────
            SectionLabel("Active Days")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAYS.forEach { (dayConst, label) ->
                    val selected = state.activeDays.contains(dayConst)
                    FilterChip(
                        selected = selected,
                        onClick = { onDayToggled(dayConst) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BusYellow,
                            selectedLabelColor = DarkSurface,
                            containerColor = DarkSurface,
                            labelColor = TextGrey
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Frequency ───────────────────────────────────────────────
            SectionLabel("Location Frequency (minutes)")
            OutlinedTextField(
                value = freqText,
                onValueChange = {
                    freqText = it
                    it.toIntOrNull()?.let { v -> onFrequencyChanged(v) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ── Firebase Config ─────────────────────────────────────────
            SectionLabel("Custom Firebase (optional)")
            Text(
                "Leave blank to use the built-in Firebase project.",
                color = TextGrey,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = projId,
                onValueChange = { projId = it },
                label = { Text("Project ID", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = appIdField,
                onValueChange = { appIdField = it },
                label = { Text("App ID", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dbUrl,
                onValueChange = { dbUrl = it },
                label = { Text("Database URL (optional)", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onFirebaseSaved(apiKey, projId, appIdField, dbUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = BusOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Firebase Config", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextWhite,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// ═════════════════════════════════════════════════════════════════════════
// Add Stop Dialog
// ═════════════════════════════════════════════════════════════════════════

@Composable
fun AddStopDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text("Save Current Location as Stop", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Your current GPS coordinates will be saved.",
                    color = TextGrey,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stop name (optional)", color = TextGrey) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGrey)
            }
        }
    )
}
