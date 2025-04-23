package space.itoncek.cvss.client.play

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Battery1Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChargingStation
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.twotone.Battery2Bar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.GenerateNavigation
import space.itoncek.cvss.client.SourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.determineRightScreen
import space.itoncek.cvss.client.switchToPrepareView
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread

class ScoreControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                ScoreControl()
            }
        }
    }
}

private var eventStream: EventStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreControl() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var field by remember { mutableStateOf(false)}

    ModalNavigationDrawer(
        drawerContent = {
            GenerateNavigation(SourceActivity.ScoringControl, scope, drawerState, ctx)
        },
        drawerState = drawerState
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Match score control")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Menu, "menu")
                        }
                    },
                    actions = {
                        Row(Modifier.padding(8.dp)) {
                            IconButton(onClick = {
                                field = false
                            }, enabled = field) {
                                Icon(Icons.Rounded.KeyboardDoubleArrowLeft, "Switch to left field")
                            }
                            IconButton(onClick = {
                                field = true
                            }, enabled = !field) {
                                Icon(Icons.Rounded.KeyboardDoubleArrowRight, "Switch to right field")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (!field) {
                    GenerateLeftField();
                } else {
                    GenerateRightField();
                }
            }
        }
        val dev = LocalInspectionMode.current;
        DisposableEffect(LocalContext.current) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !dev) {

                    thread {
                        if (determineRightScreen(
                                api
                            ).contains(SourceActivity.ScoringControl)
                        ) {
                            eventStream =
                                api.createEventHandler(
                                    { e->
                                        if(e == null) return@createEventHandler
                                        when (e) {
                                            EventStreamWebsocketHandler.Event.TEAM_UPDATE_EVENT -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_UPDATE_EVENT -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_ARM -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_RESET -> {
                                                switchToPrepareView(ctx);
                                            }
                                            EventStreamWebsocketHandler.Event.MATCH_START -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_RECYCLE -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_END -> {
                                                switchToPrepareView(ctx);
                                            }
                                            EventStreamWebsocketHandler.Event.SCORE_CHANGED -> {}
                                            EventStreamWebsocketHandler.Event.GRAPHICS_UPDATE_EVENT -> {}
                                        }
                                    },
                                    { s ->
                                        Toast.makeText(
                                            ctx,
                                            "Event stream failed! $s",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        eventStream?.close();
                                    });
                        } else {
                            switchToPrepareView(ctx)
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                eventStream?.close();
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
fun GenerateRightField() {

}

@Composable
fun GenerateLeftField() {
    Column {
        GenerateScoringBox(Icons.Rounded.BatteryChargingFull, {}, {})
        GenerateScoringBox(Icons.Rounded.BatteryAlert, {}, {})
    }
}

@Composable
fun GenerateScoringBox(icon: ImageVector, add: () -> Unit, remove: () -> Unit) {
    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.wrapContentSize()
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(icon, contentDescription = icon.name, modifier = Modifier.padding(bottom = 8.dp).size(48.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(onClick = remove) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Remove")
                    }
                    FilledIconButton(onClick = add) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
            }
        }
    }
}

//
//@Preview(
//    name = "Dynamic Red Dark",
//    group = "dark",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
//)
//@Preview(
//    name = "Dynamic Green Dark",
//    group = "dark",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
//)
//@Preview(
//    name = "Dynamic Yellow Dark",
//    group = "dark",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE
//)
@Preview(
    name = "Dynamic Blue Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
//@Preview(
//    name = "Dynamic Red Light",
//    group = "light",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
//)
//@Preview(
//    name = "Dynamic Green Light",
//    group = "light",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
//)
//@Preview(
//    name = "Dynamic Yellow Light",
//    group = "light",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE
//)
//@Preview(
//    name = "Dynamic Blue Light",
//    group = "light",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
//    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
//)
@Composable
fun GreetingPreview3() {
    CVSSClientTheme {
        ScoreControl()
    }
}