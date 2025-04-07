package space.itoncek.cvss.client.play

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import space.itoncek.cvss.client.AutoSizeText
import space.itoncek.cvss.client.GenerateNavigation
import space.itoncek.cvss.client.SourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.api.TimeStreamWebsocketHandler
import space.itoncek.cvss.client.determineRightScreen
import space.itoncek.cvss.client.switchToPrepareView
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MatchMasterControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                MatchMasterControl()
            }
        }
    }
}

private var eventStream: EventStreamWebsocketHandler? = null
private var timeStream: TimeStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchMasterControl() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var time by remember { mutableIntStateOf(0) }
    var leftScore by remember { mutableIntStateOf(0) }
    var rightScore by remember { mutableIntStateOf(0) }
    var leftName by remember { mutableStateOf("Filmáci") }
    var rightName by remember { mutableStateOf("Roboťáci") }
    ModalNavigationDrawer(
        drawerContent = {
            GenerateNavigation(SourceActivity.MatchMasterControl, scope, drawerState, ctx)
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
                        Text("Match master control")
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
            Column (modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .wrapContentHeight()) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        thread {
                            api.startMatch();
                        }
                    }, modifier = Modifier.fillMaxWidth(.485f)) {
                        Text("Start game")
                    }
                    Button(onClick = {
                        thread {
                            api.resetMatch();
                        }}, modifier = Modifier.fillMaxWidth()) {
                        Text("Abort game")
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        AutoSizeText(
                            toTime(time),
                            100.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(.455f)) {
                        AutoSizeText(
                            "$leftScore",
                            64.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                        AutoSizeText(
                            leftName,
                            20.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(.1f)) {
                        AutoSizeText(
                            ":",
                            64.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                        AutoSizeText(
                            "x",
                            20.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        AutoSizeText(
                            "$rightScore",
                            64.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                        AutoSizeText(
                            rightName,
                            20.sp,
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                            ).contains(SourceActivity.MatchMasterControl)
                        ) {
                            eventStream =
                                api.createEventHandler(
                                    { e->
                                        if(e == null) return@createEventHandler
                                        when (e) {
                                            EventStreamWebsocketHandler.Event.TEAM_UPDATE_EVENT -> {
                                                val sides = api.currentMatchSides
                                                leftName = sides.first.name
                                                rightName = sides.second.name
                                            }
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
                                            EventStreamWebsocketHandler.Event.SCORE_CHANGED -> {
                                                val points = api.getCurrentMatchScore()
                                                leftScore = points.first
                                                rightScore = points.second
                                            }
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
                            timeStream =
                                api.createTimeHandler({ t ->
                                    time = t
                                }, { s ->
                                    Toast.makeText(ctx, "Time stream failed! $s", Toast.LENGTH_LONG)
                                        .show()
                                    eventStream?.close()
                                });
                            time = api.getMatchLength()
                            Log.i("debdeb", time.toString())
                            val points = api.currentMatchScore
                            leftScore = points.first
                            rightScore = points.second

                            val sides = api.currentMatchSides
                            leftName = sides.first.name
                            rightName = sides.second.name
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

fun toTime(time: Int): String {
    time.seconds.toComponents({ minutes, seconds, _ ->
        return "%02d:%02d".format(minutes,seconds)
    })
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
fun GreetingPreview2() {
    CVSSClientTheme {
        MatchMasterControl()
    }
}