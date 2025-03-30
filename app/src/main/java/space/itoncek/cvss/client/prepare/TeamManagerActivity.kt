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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.PrepareNavigation
import space.itoncek.cvss.client.PrepareSourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.api.objects.Team
import space.itoncek.cvss.client.switchToGameView
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread

class TeamManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                MainUI()
            }
        }
    }
}

fun runOnUiThread(block: suspend () -> Unit) = CoroutineScope(Dispatchers.Main).launch { block() }


var teamHandler: EventStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainUI() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val teams = remember { mutableStateListOf<Team>() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var createInsteadOfEdit by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableIntStateOf(-1) }
    var editingTeamName by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    var showDeletionDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            PrepareNavigation(PrepareSourceActivity.TeamManager, scope, drawerState, ctx)
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
                        Text("Team manager")
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        createInsteadOfEdit = true
                        editingTeamName = ""
                        showBottomSheet = true
                    },
                ) {
                    Icon(Icons.Filled.Add, "Floating action button.")
                }
            }
        ) { innerPadding ->
            if (showDeletionDialog) {
                DeleteTeamDialog(
                    onClose = { showDeletionDialog = false },
                    onDelete = {
                        showDeletionDialog = false
                        thread {
                            if (!api.deleteTeam(editingTeam)) {
                                runOnUiThread {
                                    Toast.makeText(
                                        ctx,
                                        "Unable to delete that team, check if there are some matches, they are associated with!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            editingTeam = -1
                        }
                    },
                    editingTeam,
                    api
                )
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(teams.count()) { team ->
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
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(teams[team].name)

                            Spacer(modifier = Modifier.weight(1f))
                            FilledIconButton(
                                onClick = {
                                    editingTeam = teams[team].id;
                                    showDeletionDialog = true;
                                }
                            ) {
                                Icon(Icons.Filled.Delete, "")
                            }
                            FilledIconButton(onClick = {
                                createInsteadOfEdit = false
                                showBottomSheet = true
                                editingTeam = teams[team].id;
                                editingTeamName = teams[team].name;
                            }) {
                                Icon(Icons.Filled.Edit, "")
                            }
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    }, sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var teamName by remember { mutableStateOf(editingTeamName) }
                        TextField(teamName,
                            onValueChange = {
                                teamName = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Team name", color = Color.Gray)
                            })
                        Row {
                            Button(
                                onClick = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(.4f)
                                    .padding(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    thread {
                                        if (createInsteadOfEdit) {
                                            api.createTeam(teamName)
                                        } else {
                                            api.updateTeam(editingTeam, teamName)
                                        }
                                    }.join()
                                    updateTeams(api, teams)

                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            createInsteadOfEdit = false
                                            showBottomSheet = false
                                            editingTeam = -1
                                            editingTeamName = ""
                                        }
                                    }
                                }, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                    // Sheet content
                }
            }
        }

        // Screen content
    }
    val dev = LocalInspectionMode.current;
    DisposableEffect(LocalContext.current) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !dev) {
                updateTeams(api, teams)
                teamHandler = api.createEventHandler({
                    updateTeams(api, teams)
                }, {},{
                    switchToGameView(ctx);
                },{
                    switchToGameView(ctx);
                },{});
            } else if (dev) {
                teams.clear()
                teams.add(Team(-1, "Dev mode!"))
                teams.add(Team(-2, "Dev mode!"))
                teams.add(Team(-3, "Dev mode!"))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            teamHandler?.close();
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

fun updateTeams(api: CVSSAPI, teams: SnapshotStateList<Team>) {
    Log.i("updating()", "Updating teams")
    thread {
        val t = api.listTeams()

        runOnUiThread {
            if (t != null) {
                teams.clear()
                teams.addAll(t)
            } else {
                teams.clear()
                teams.add(Team(-1, "CONNECTION ERROR!"))
            }
        }
    }.join()
}

@Composable
fun DeleteTeamDialog(onClose: () -> Unit, onDelete: () -> Unit, editingTeam: Int, api: CVSSAPI) {
    var teamName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        thread {
            val tn = api.getTeam(editingTeam)?.name
            if (tn != null) {
                teamName = tn
            }
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Deleting team") },
        text = { Text(text = "Are you sure, you want to delete team $teamName (#$editingTeam)?") },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Dismiss") }
        }
    )
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
fun GreetingPreview() {
    CVSSClientTheme {
        MainUI()
    }
}