package space.itoncek.cvss.client

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.objects.Team
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainUI() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val teams = remember { mutableStateListOf<Team>() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            GlobalNavigation(ScreenView.TeamManager, scope, drawerState, ctx)
        },
        drawerState = drawerState
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            Log.w(TeamManagerActivity::class.qualifiedName, "Init'd")
            var showBottomSheet by remember { mutableStateOf(false) }
            var editingTeam by remember { mutableIntStateOf(-1) }
            var editingTeamName by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState()
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
                            .padding(16.dp)
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
                            Button(onClick = {
                                showBottomSheet = true
                                editingTeam = teams[team].id;
                                editingTeamName = teams[team].name;
                            }) {
                                Text("Edit")
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
                        TextField(teamName, onValueChange = {
                            teamName = it
                        }, modifier = Modifier.fillMaxWidth())
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
                                        api.updateTeam(editingTeam, teamName)
                                    }.join()
                                    updateTeams(api, teams)

                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
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
                    Button(onClick = {

                    }) {
                        Text("Hide bottom sheet")
                    }
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
            } else if (dev) {
                teams.clear()
                teams.add(Team(-1, "Dev mode!"))
                teams.add(Team(-2, "Dev mode!"))
                teams.add(Team(-3, "Dev mode!"))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

fun updateTeams(api: CVSSAPI, teams: SnapshotStateList<Team>) {
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
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
fun GreetingPreview() {
    CVSSClientTheme {
        MainUI()
    }
}