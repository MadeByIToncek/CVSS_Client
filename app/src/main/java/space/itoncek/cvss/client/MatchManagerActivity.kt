package space.itoncek.cvss.client

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.objects.Match
import space.itoncek.cvss.client.api.objects.Team
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import kotlin.concurrent.thread

class MatchManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVSSClientTheme {
                Greeting()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val matches = remember { mutableStateListOf<Match>() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMatchId by remember { mutableIntStateOf(-1) }

    ModalNavigationDrawer(
        drawerContent = {
            GlobalNavigation(ScreenView.MatchManager, scope, drawerState, ctx)
        },
        drawerState = drawerState
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            Log.w(TeamManagerActivity::class.qualifiedName, "Init'd")
            val sheetState = rememberModalBottomSheetState()
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(matches.count()) { team ->
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
                            Text("%d".format(matches[team].id))

                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = {
                                selectedMatchId = matches[team].id;
                                showBottomSheet = true
                            }) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }


            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var matchState by remember { mutableStateOf(Match.State.NULL) }
                    var result by remember { mutableStateOf(Match.Result.NULL) }
                    var leftTeamId by remember { mutableStateOf(-1) }
                    var rightTeamId by remember { mutableStateOf(-1) }
                    var requestFailed by remember { mutableStateOf(false) }

                    LaunchedEffect(showBottomSheet) {
                        if (showBottomSheet) {
                            thread {
                                val match = api.getMatch(selectedMatchId);
                                if (match != null) {
                                    requestFailed = false;
                                    matchState = match.state;
                                    result = match.result;
                                    leftTeamId = match.left.id;
                                    rightTeamId = match.right.id;
                                } else {
                                    requestFailed = true
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 0.dp,
                            bottom = 8.dp
                        )
                    ) {
                        Column {
                            val matchstateOpen = remember { mutableStateOf(false) } // initial value
                            val openCloseOfDropDownList: (Boolean) -> Unit = {
                                matchstateOpen.value = it
                            }
                            val userSelectedString: (String) -> Unit = {
                                matchState = Match.State.valueOf(it);
                            }

                            OutlinedTextField(
                                value = matchState.name,
                                onValueChange = { matchState = Match.State.valueOf(it) },
                                label = { Text(text = "Match state") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropDownList(
                                requestToOpen = matchstateOpen.value,
                                list = Match.State.entries.map { it.name },
                                openCloseOfDropDownList,
                                userSelectedString
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 0.dp,
                            bottom = 8.dp
                        )
                    ) {
                        Column {
                            val resultOpen = remember { mutableStateOf(false) } // initial value
                            val resultopenCloseOfDropDownList: (Boolean) -> Unit = {
                                resultOpen.value = it
                            }
                            val resultuserSelectedString: (String) -> Unit = {
                                result = Match.Result.valueOf(it);
                            }

                            OutlinedTextField(
                                value = result.name,
                                onValueChange = { result = Match.Result.valueOf(it) },
                                label = { Text(text = "Match result") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropDownList(
                                requestToOpen = resultOpen.value,
                                list = Match.Result.entries.map { it.name },
                                resultopenCloseOfDropDownList,
                                resultuserSelectedString
                            )
                        }
                    }
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
                                    //api.updateMatch(editingTeam, teamName)
                                }.join()
                                updateMatches(api, matches)

                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                            }, modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {

                }
            }
        }

        // Screen content
        val dev = LocalInspectionMode.current;
        DisposableEffect(LocalContext.current) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !dev) {
                    updateMatches(api, matches)
                } else if (dev) {
                    matches.clear()
                    matches.add(
                        Match(
                            -1,
                            Match.State.UPCOMING,
                            Match.Result.NOT_FINISHED,
                            Team(1, "Test"),
                            Team(2, "Test")
                        )
                    )
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}

fun updateMatches(api: CVSSAPI, matches: SnapshotStateList<Match>) {
    thread {
        val m = api.listMatches()

        runOnUiThread {
            if (m != null) {
                matches.clear()
                matches.addAll(m)
            } else {
                matches.clear()
                matches.add(
                    Match(
                        -1,
                        Match.State.UPCOMING,
                        Match.Result.NOT_FINISHED,
                        Team(1, "Test"),
                        Team(2, "Test")
                    )
                )
            }
        }
    }.join()
}

@Composable
fun DropDownList(
    requestToOpen: Boolean = false,
    list: List<String>,
    request: (Boolean) -> Unit,
    selectedString: (String) -> Unit
) {
    DropdownMenu(
        modifier = Modifier.fillMaxWidth(.9f),
        expanded = requestToOpen,
        onDismissRequest = { request(false) },
    ) {
        list.forEach {
            DropdownMenuItem(modifier = Modifier.fillMaxWidth(), text = {
                Text(
                    it, modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.Start)
                )
            }, onClick = {
                request(false)
                selectedString(it)
            })
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun GreetingPreview2() {
    CVSSClientTheme {
        Greeting()
    }
}