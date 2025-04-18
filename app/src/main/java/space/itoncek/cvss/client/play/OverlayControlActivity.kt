package space.itoncek.cvss.client.play

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.GenerateNavigation
import space.itoncek.cvss.client.SourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.determineRightScreen
import space.itoncek.cvss.client.prepare.OverlaySetupActivity
import space.itoncek.cvss.client.switchToPrepareView
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread

class OverlayControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                OverlayControl()
            }
        }
    }
}

private var eventStream: EventStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayControl() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


    ModalNavigationDrawer(
        drawerContent = {
            GenerateNavigation(SourceActivity.OverlayControl, scope, drawerState, ctx)
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
                        Text("Overlay")
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
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                Text(
                    "Left team lower third",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    fontSize = 20.sp
                )
                Row {
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Left, true); }
                        },
                        modifier = Modifier
                            .fillMaxWidth(.49f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Enable")
                    }
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Left, false); }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Disable")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Right team lower third",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    fontSize = 20.sp
                )
                Row {
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Right, true); }
                        },
                        modifier = Modifier
                            .fillMaxWidth(.49f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Enable")
                    }
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Right, false); }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Disable")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Timer",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    fontSize = 20.sp
                )
                Row {
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Timer, true); }
                        },
                        modifier = Modifier
                            .fillMaxWidth(.49f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Enable")
                    }
                    Button(
                        {
                            thread { api.toggleOverlay(CVSSAPI.OverlayPart.Timer, false); }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Disable")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
        val dev = LocalInspectionMode.current;
        DisposableEffect(LocalContext.current) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !dev) {

                    thread {
                        if (determineRightScreen(
                                api
                            ).contains(SourceActivity.OverlayControl)
                        ) {
                            eventStream =
                                api.createEventHandler(
                                    { e ->
                                        if (e == null) return@createEventHandler
                                        when (e) {
                                            EventStreamWebsocketHandler.Event.TEAM_UPDATE_EVENT -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_UPDATE_EVENT -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_ARM -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_RESET -> {
                                                switchToOverlayPrepareView(ctx);
                                            }

                                            EventStreamWebsocketHandler.Event.MATCH_START -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_RECYCLE -> {}
                                            EventStreamWebsocketHandler.Event.MATCH_END -> {
                                                switchToOverlayPrepareView(ctx);
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

fun switchToOverlayPrepareView(ctx: Context) {
    ctx.startActivity(Intent(ctx,OverlaySetupActivity::class.java))
    (ctx as? Activity)?.finish()
}

@Preview(
    name = "Dynamic Red Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Green Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Yellow Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Blue Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Red Light",
    group = "light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Green Light",
    group = "light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Yellow Light",
    group = "light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE
)
@Preview(
    name = "Dynamic Blue Light",
    group = "light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun OverlayControlPreview() {
    CVSSClientTheme {
        OverlayControl()
    }
}