@file:OptIn(ExperimentalMaterial3Api::class)

package space.itoncek.cvss.client.prepare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.GenerateNavigation
import space.itoncek.cvss.client.SourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.api.objects.GraphicsInstance
import space.itoncek.cvss.client.api.objects.GraphicsInstance.GraphicsMode
import space.itoncek.cvss.client.determineRightScreen
import space.itoncek.cvss.client.play.OverlayControlActivity
import space.itoncek.cvss.client.runOnUiThread
import space.itoncek.cvss.client.switchToGameView
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread

class OverlaySetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                OverlaySetup()
            }
        }
    }
}

private var eventStream: EventStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySetup() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val instances = remember { mutableStateListOf<GraphicsInstance>() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedInstance by remember { mutableStateOf("") }
    var showDeletionDialog by remember { mutableStateOf(false) }
    var allowEdits by remember { mutableStateOf(false) }
    val probe = remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            GenerateNavigation(SourceActivity.TeamManager, scope, drawerState, ctx)
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
                        Text("Overlay setup")
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
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                ) {
                    Switch(
                        checked = allowEdits,
                        onCheckedChange = {
                            allowEdits = it
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Allow edits")
                    Switch(
                        checked = probe.value,
                        onCheckedChange = {
                            probe.value = it
                            thread {
                                api.probe = probe.value
                                updateProbeStatus(api,probe)
                            }
                        },
                        modifier = Modifier.padding(start = 24.dp,end = 8.dp),
                        enabled = allowEdits
                    )
                    Text("Probe")
                }
                LazyColumn {
                    items(instances.count()) { instance ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp
                                )
                                .wrapContentHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(instances[instance].id)

                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.width(200.dp).padding(end = 8.dp)
                                ) {
                                    // Declaring a boolean value to store
                                    // the expanded state of the Text Field
                                    var mExpanded by remember { mutableStateOf(false) }

                                    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

                                    // Up Icon when expanded and down icon when collapsed
                                    val icon = if (mExpanded)
                                        Icons.Filled.KeyboardArrowUp
                                    else
                                        Icons.Filled.KeyboardArrowDown

                                    Column {
                                        // Create an Outlined Text Field
                                        // with icon and not expanded
                                        OutlinedTextField(
                                            value = instances[instance].mode.name,
                                            onValueChange = { value ->
                                                thread {
                                                    api.updateGraphicsInstance(
                                                        instances[instance].id,
                                                        GraphicsInstance.GraphicsMode.valueOf(value)
                                                    )
                                                    updateGraphicsInstances(api, instances)
                                                }
                                            },
                                            enabled = allowEdits,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coordinates ->
                                                    // This value is used to assign to
                                                    // the DropDown the same width
                                                    mTextFieldSize = coordinates.size.toSize()
                                                },
                                            label = { Text("Right team") },
                                            trailingIcon = {
                                                Icon(
                                                    icon, "contentDescription",
                                                    Modifier.clickable {
                                                        if (!allowEdits) {
                                                            mExpanded = false
                                                        } else {
                                                            mExpanded = !mExpanded
                                                        }
                                                    })
                                            }
                                        )

                                        // Create a drop-down menu with list of cities,
                                        // when clicked, set the Text Field text as the city selected
                                        DropdownMenu(
                                            expanded = mExpanded,
                                            onDismissRequest = { mExpanded = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
                                        ) {
                                            GraphicsMode.entries.forEach { team ->
                                                DropdownMenuItem(onClick = {
                                                    thread {
                                                        api.updateGraphicsInstance(
                                                            instances[instance].id,
                                                            team
                                                        );
                                                        updateGraphicsInstances(api, instances)
                                                        mExpanded = false
                                                    }
                                                }, text = {
                                                    Text(text = team.name)
                                                })
                                            }
                                        }
                                    }
                                }
                                FilledIconButton(
                                    onClick = {
                                        selectedInstance = instances[instance].id;
                                        showDeletionDialog = true;
                                    },
                                    enabled = allowEdits,
                                ) {
                                    Icon(Icons.Filled.Delete, "")
                                }
                            }
                        }
                    }
                }
            }
        }

        val dev = LocalInspectionMode.current;
        DisposableEffect(LocalContext.current) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !dev) {
                    thread {
                        if (determineRightScreen(api).contains(SourceActivity.TeamManager)) {
                            updateGraphicsInstances(api, instances)
                            updateProbeStatus(api, probe)
                            eventStream = api.createEventHandler(
                                { e ->
                                    if (e == null) return@createEventHandler
                                    when (e) {
                                        EventStreamWebsocketHandler.Event.TEAM_UPDATE_EVENT -> {}
                                        EventStreamWebsocketHandler.Event.MATCH_UPDATE_EVENT -> {}
                                        EventStreamWebsocketHandler.Event.MATCH_ARM -> {
                                            switchToOverlayGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_RESET -> {}
                                        EventStreamWebsocketHandler.Event.MATCH_START -> {
                                            switchToOverlayGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_RECYCLE -> {
                                            switchToOverlayGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_END -> {}
                                        EventStreamWebsocketHandler.Event.SCORE_CHANGED -> {}
                                        EventStreamWebsocketHandler.Event.GRAPHICS_UPDATE_EVENT -> {
                                            updateGraphicsInstances(api, instances)
                                        }
                                    }
                                },
                                { s ->
                                    Toast.makeText(
                                        ctx,
                                        "Event stream failed! $s",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    eventStream?.close()
                                })
                        } else {
                            switchToGameView(ctx)
                        }
                    }
                } else if (dev) {
                    instances.clear();
                    instances.add(
                        GraphicsInstance(
                            "tv1",
                            GraphicsInstance.GraphicsMode.TV_TWO_LEFT
                        )
                    )
                    instances.add(
                        GraphicsInstance(
                            "tv2",
                            GraphicsInstance.GraphicsMode.TV_TWO_RIGHT
                        )
                    )
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

fun updateProbeStatus(api: CVSSAPI, probe: MutableState<Boolean>) {
    thread {
        probe.value = api.probe;
    }
}

fun switchToOverlayGameView(ctx: Context) {
    ctx.startActivity(Intent(ctx, OverlayControlActivity::class.java))
    (ctx as? Activity)?.finish()
}

fun updateGraphicsInstances(api: CVSSAPI, instances: SnapshotStateList<GraphicsInstance>) {
    Log.i("updating()", "Updating teams")
    thread {
        val t = api.listGraphicsInstances()

        runOnUiThread {
            if (t != null) {
                instances.clear()
                instances.addAll(t)
            } else {
                instances.clear()
                instances.add(GraphicsInstance("ERROR",GraphicsInstance.GraphicsMode.TV_TWO_LEFT))
                instances.add(GraphicsInstance("(check connectivity!)",GraphicsInstance.GraphicsMode.TV_TWO_RIGHT))
            }
        }
    }.join()
}

@Preview(
    name = "Dynamic Blue Dark",
    group = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun OverlaySetupPreview() {
    CVSSClientTheme {
        OverlaySetup()
    }
}