package ca.gbc.comp3074.shaikh.gpslocationlab

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Checks whether the user has granted precise (GPS) location permission
fun hasFineLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // permission is checked before every location call
@Composable
fun LocationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(hasFineLocationPermission(context)) }
    var location by remember { mutableStateOf<Location?>(null) }
    var status by remember { mutableStateOf("Requesting location permission...") }

    // Runtime permission request (required on Android 6.0+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!hasPermission) {
            status = "Permission denied. Precise location is required to use GPS."
        }
    }

    // Ask for permission when the screen first opens
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Start GPS updates once permission is granted; stop them when the screen closes
    DisposableEffect(hasPermission) {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                location = newLocation
                status = "Live location from GPS"
            }

            override fun onProviderEnabled(provider: String) {
                status = "GPS turned on. Waiting for a fix..."
            }

            override fun onProviderDisabled(provider: String) {
                status = "GPS is off. Turn on Location in Settings."
            }
        }

        if (hasPermission && hasFineLocationPermission(context)) {
            status = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                "Waiting for GPS fix..."
            } else {
                "GPS is off. Turn on Location in Settings."
            }

            // Show the last known GPS position immediately, if there is one
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                location = it
                status = "Last known GPS location"
            }

            // Ask for a new GPS reading every 2 seconds
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GPS Location",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Latitude", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = location?.let { "%.6f".format(it.latitude) } ?: "--",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(16.dp))

                Text("Longitude", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = location?.let { "%.6f".format(it.longitude) } ?: "--",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(text = status, textAlign = TextAlign.Center)

        if (!hasPermission) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) {
                Text("Grant Location Permission")
            }
        }
    }
}