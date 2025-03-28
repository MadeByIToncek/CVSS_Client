package space.itoncek.cvss.client

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
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

var matchHandler: EventStreamWebsocketHandler? = null

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
                .background(MaterialTheme.colorScheme.background),
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Match manager")
                    }
                )
            }
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
                            Text(
                                "%s x %s (%d)".format(
                                    matches[team].left.name,
                                    matches[team].right.name,
                                    matches[team].id
                                )
                            )

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

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var matchState by remember { mutableStateOf(Match.State.NULL) }
                        var result by remember { mutableStateOf(Match.Result.NULL) }
                        var leftTeamId by remember { mutableIntStateOf(-1) }
                        var rightTeamId by remember { mutableIntStateOf(-1) }
                        var requestFailed by remember { mutableStateOf(false) }
                        var loading by remember { mutableStateOf(true) }
                        val teams = remember { mutableStateListOf<Team>() }

                        LaunchedEffect(showBottomSheet) {
                            if (showBottomSheet) {
                                thread {
                                    val teamss = api.listTeams();
                                    if (teamss == null) {
                                        requestFailed = true
                                        Toast.makeText(
                                            ctx,
                                            "Unable to load the teams",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@thread
                                    } else teamss.forEach {
                                        teams.add(it);
                                    }

                                    val match = api.getMatch(selectedMatchId);
                                    if (match != null) {
                                        requestFailed = false;
                                        matchState = match.state;
                                        result = match.result;
                                        leftTeamId = match.left.id;
                                        rightTeamId = match.right.id;
                                        loading = false
                                        return@thread
                                    } else {
                                        requestFailed = true
                                        Toast.makeText(
                                            ctx,
                                            "Unable to load the match",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }.join()
                            }
                        }
                        if (!loading) {
                            // State dropdown
                            Box(
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 0.dp,
                                    bottom = 16.dp
                                )
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
                                        value = matchState.name,
                                        onValueChange = { matchState = Match.State.valueOf(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                // This value is used to assign to
                                                // the DropDown the same width
                                                mTextFieldSize = coordinates.size.toSize()
                                            },
                                        label = { Text("Label") },
                                        trailingIcon = {
                                            Icon(icon, "contentDescription",
                                                Modifier.clickable { mExpanded = !mExpanded })
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
                                        Match.State.entries.forEach { label ->
                                            DropdownMenuItem(onClick = {
                                                matchState = label
                                                mExpanded = false
                                            }, text = {
                                                Text(text = label.name)
                                            })
                                        }
                                    }
                                }
                            }

                            // Result dropdown
                            Box(
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 0.dp,
                                    bottom = 16.dp
                                )
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
                                        value = result.name,
                                        onValueChange = { result = Match.Result.valueOf(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                // This value is used to assign to
                                                // the DropDown the same width
                                                mTextFieldSize = coordinates.size.toSize()
                                            },
                                        label = { Text("Label") },
                                        trailingIcon = {
                                            Icon(icon, "contentDescription",
                                                Modifier.clickable { mExpanded = !mExpanded })
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
                                        Match.Result.entries.forEach { label ->
                                            DropdownMenuItem(onClick = {
                                                result = label
                                                mExpanded = false
                                            }, text = {
                                                Text(text = label.name)
                                            })
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(
                                        start = 8.dp,
                                        end = 8.dp,
                                        bottom = 16.dp
                                    )
                                    .fillMaxWidth()
                            ) {

                                // Left team dropdown
                                Box(
                                    modifier = Modifier
                                        .padding(
                                            end = 16.dp
                                        )
                                        .fillMaxWidth(.5f)
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
                                            value = teams.first {
                                                it.id == rightTeamId
                                            }.name,
                                            onValueChange = { value ->
                                                rightTeamId = teams.first { team ->
                                                    value == team.name
                                                }.id
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coordinates ->
                                                    // This value is used to assign to
                                                    // the DropDown the same width
                                                    mTextFieldSize = coordinates.size.toSize()
                                                },
                                            label = { Text("Label") },
                                            trailingIcon = {
                                                Icon(icon, "contentDescription",
                                                    Modifier.clickable { mExpanded = !mExpanded })
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
                                            teams.forEach { team ->
                                                DropdownMenuItem(onClick = {
                                                    rightTeamId = team.id
                                                    mExpanded = false
                                                }, text = {
                                                    Text(text = team.name)
                                                })
                                            }
                                        }
                                    }
                                }


                                // Left team dropdown
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                            value = teams.first {
                                                it.id == leftTeamId
                                            }.name,
                                            onValueChange = { value ->
                                                leftTeamId = teams.first { team ->
                                                    value == team.name
                                                }.id
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coordinates ->
                                                    // This value is used to assign to
                                                    // the DropDown the same width
                                                    mTextFieldSize = coordinates.size.toSize()
                                                },
                                            label = { Text("Label") },
                                            trailingIcon = {
                                                Icon(icon, "contentDescription",
                                                    Modifier.clickable { mExpanded = !mExpanded })
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
                                            teams.forEach { team ->
                                                DropdownMenuItem(onClick = {
                                                    leftTeamId = team.id
                                                    mExpanded = false
                                                }, text = {
                                                    Text(text = team.name)
                                                })
                                            }
                                        }
                                    }
                                }

                            }

                            // Button row
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
                                            api.updateMatch(
                                                selectedMatchId,
                                                matchState,
                                                result,
                                                leftTeamId,
                                                rightTeamId
                                            )
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
                }
            }
        }

        // Screen content
        val dev = LocalInspectionMode.current;
        DisposableEffect(LocalContext.current) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !dev) {
                    updateMatches(api, matches)

                    matchHandler = api.createEventHandler({}, {
                        updateMatches(api, matches)
                    });
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
    Log.i("updating()", "Updating matches")
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