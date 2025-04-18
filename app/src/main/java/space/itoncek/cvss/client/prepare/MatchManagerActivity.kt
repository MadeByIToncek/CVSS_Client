package space.itoncek.cvss.client.prepare

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.GenerateNavigation
import space.itoncek.cvss.client.SourceActivity
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.EventStreamWebsocketHandler
import space.itoncek.cvss.client.api.objects.Match
import space.itoncek.cvss.client.api.objects.Team
import space.itoncek.cvss.client.determineRightScreen
import space.itoncek.cvss.client.runOnUiThread
import space.itoncek.cvss.client.switchToGameView
import space.itoncek.cvss.client.switchToPrepareView
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

private var eventStream: EventStreamWebsocketHandler? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current;
    val api = CVSSAPI(ctx.filesDir);
    val matches = remember { mutableStateListOf<Match>() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showMatchEdit by remember { mutableStateOf(false) }
    var showMatchCreate by remember { mutableStateOf(false) }
    var selectedMatchId by remember { mutableIntStateOf(-1) }

    ModalNavigationDrawer(
        drawerContent = {
            GenerateNavigation(SourceActivity.MatchManager, scope, drawerState, ctx)
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
                        showMatchCreate = true
                    },
                ) {
                    Icon(Icons.Filled.Add, "Floating action button.")
                }
            }
        ) { innerPadding ->
            Log.w(TeamManagerActivity::class.qualifiedName, "Init'd")
            val editState = rememberModalBottomSheetState()
            val createState = rememberModalBottomSheetState()
            var showDeletionDialog by remember { mutableStateOf(false) }
            var showStartDialog by remember { mutableStateOf(false) }
            if (showDeletionDialog) {
                DeleteMatchDialog(
                    onClose = { showDeletionDialog = false },
                    onDelete = {
                        showDeletionDialog = false
                        thread {
                            if (!api.deleteMatch(selectedMatchId)) {
                                runOnUiThread {
                                    Toast.makeText(
                                        ctx,
                                        "Unable to delete that match!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            selectedMatchId = -1
                        }
                    },
                    selectedMatchId,
                    api
                )
            }
            if (showStartDialog) {
                StartMatchDialog(
                    onClose = { showStartDialog = false },
                    onDelete = {
                        showStartDialog = false
                        thread {
                            if (!api.armMatch(selectedMatchId)) {
                                runOnUiThread {
                                    Toast.makeText(
                                        ctx,
                                        "Unable to arm that match!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            selectedMatchId = -1
                        }.join()
                    },
                    selectedMatchId,
                    api
                )
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(matches.count()) { match ->
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
                        val g = horizontalGradient(
                            colors = listOf(
                                getColorFromString("#" + matches[match].left.colorDark),
                                getColorFromString("#" + matches[match].right.colorDark)
                            )
                        )
                        Box(modifier = Modifier.background(brush = g)) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "%s x %s (%d)".format(
                                        matches[match].left.name,
                                        matches[match].right.name,
                                        matches[match].id
                                    )
                                )

                                Spacer(modifier = Modifier.weight(1f))
                                FilledIconButton(
                                    onClick = {
                                        selectedMatchId = matches[match].id;
                                        showStartDialog = true;
                                    }
                                ) {
                                    Icon(Icons.Filled.PlayArrow, "")
                                }
                                FilledIconButton(
                                    onClick = {
                                        selectedMatchId = matches[match].id;
                                        showDeletionDialog = true;
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, "")
                                }
                                FilledIconButton(onClick = {
                                    selectedMatchId = matches[match].id;
                                    showMatchEdit = true
                                }) {
                                    Icon(Icons.Filled.Edit, "")
                                }
                            }
                        }
                    }
                }
            }

            if (showMatchCreate) {
                showMatchEdit = false

                ModalBottomSheet(
                    onDismissRequest = {
                        showMatchCreate = false
                    },
                    sheetState = createState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var leftTeamId by remember { mutableIntStateOf(-1) }
                        var rightTeamId by remember { mutableIntStateOf(-1) }
                        var ready by remember { mutableStateOf(false) }
                        val teams = remember { mutableStateListOf<Team>() }

                        LaunchedEffect(Unit) {
                            if (showMatchCreate) {
                                thread {
                                    val teamss = api.listTeams();
                                    if (teamss == null) {
                                        Toast.makeText(
                                            ctx,
                                            "Unable to load the teams",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@thread
                                    } else teamss.forEach {
                                        teams.add(it);
                                    }
                                    runOnUiThread {
                                        ready = true
                                    }
                                }.join()
                            }
                        }

                        if (!ready) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                "Loading", modifier = Modifier
                                    .padding(8.dp)
                                    .padding(bottom = 16.dp)
                            )
                        } else {
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
                                            value = if (rightTeamId < 0) "" else teams.first {
                                                it.id == rightTeamId
                                            }.name,
                                            onValueChange = { value ->
                                                if (value != "") {
                                                    rightTeamId = teams.first { team ->
                                                        value == team.name
                                                    }.id
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coordinates ->
                                                    // This value is used to assign to
                                                    // the DropDown the same width
                                                    mTextFieldSize = coordinates.size.toSize()
                                                },
                                            label = { Text("Left team") },
                                            trailingIcon = {
                                                Icon(
                                                    icon, "contentDescription",
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


                                // Right team dropdown
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

                                            value = if (leftTeamId < 0) "" else teams.first {
                                                it.id == leftTeamId
                                            }.name,
                                            onValueChange = { value ->
                                                if (value != "") {
                                                    leftTeamId = teams.first { team ->
                                                        value == team.name
                                                    }.id
                                                }
                                            },
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
                                        scope.launch { editState.hide() }.invokeOnCompletion {
                                            if (!editState.isVisible) {
                                                showMatchCreate = false
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
                                            if (rightTeamId < 0 || leftTeamId < 0) {
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        ctx,
                                                        "One of the teams is not selected!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                return@thread
                                            }
                                            if (api.createMatch(
                                                    leftTeamId,
                                                    rightTeamId
                                                )
                                            ) {
                                                scope.launch {
                                                    createState.hide()
                                                    updateMatches(api, matches)
                                                }.invokeOnCompletion {
                                                    if (!createState.isVisible) {
                                                        showMatchCreate = false
                                                    }
                                                }
                                            } else runOnUiThread {
                                                Toast.makeText(
                                                    ctx,
                                                    "Unable to save!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }.join()
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

            var requestFailed by remember { mutableStateOf(false) }

            if (showMatchEdit) {
                showMatchCreate = false
                ModalBottomSheet(
                    onDismissRequest = {
                        showMatchEdit = false
                    },
                    sheetState = editState,
                    containerColor = if (requestFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
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
                        var loading by remember { mutableStateOf(true) }
                        val teams = remember { mutableStateListOf<Team>() }

                        LaunchedEffect(showMatchEdit) {
                            if (showMatchEdit) {
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
                        if (loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                "Loading", modifier = Modifier
                                    .padding(8.dp)
                                    .padding(bottom = 16.dp)
                            )
                        } else if (requestFailed) {
                            Text(
                                "Request failed!",
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text("Please check your connectivity & server logs for more info.")
                        } else {
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
                                        label = { Text("Match state") },
                                        trailingIcon = {
                                            Icon(
                                                icon, "contentDescription",
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
                                        label = { Text("Match result") },
                                        trailingIcon = {
                                            Icon(
                                                icon, "contentDescription",
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
                                            label = { Text("Left team") },
                                            trailingIcon = {
                                                Icon(
                                                    icon, "contentDescription",
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


                                // Right team dropdown
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
                                            label = { Text("Right team") },
                                            trailingIcon = {
                                                Icon(
                                                    icon, "contentDescription",
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

                            }

                            // Button row
                            Row {
                                Button(
                                    onClick = {
                                        scope.launch { editState.hide() }.invokeOnCompletion {
                                            if (!editState.isVisible) {
                                                showMatchEdit = false
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

                                        scope.launch { editState.hide() }.invokeOnCompletion {
                                            if (!editState.isVisible) {
                                                showMatchEdit = false
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
    }

    // Screen content
    val dev = LocalInspectionMode.current;
    DisposableEffect(LocalContext.current) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !dev) {
                thread {
                    if (determineRightScreen(api).contains(SourceActivity.MatchManager)) {
                        updateMatches(api, matches)

                        eventStream =
                            api.createEventHandler(
                                { e ->
                                    if (e == null) return@createEventHandler
                                    when (e) {
                                        EventStreamWebsocketHandler.Event.TEAM_UPDATE_EVENT -> {}
                                        EventStreamWebsocketHandler.Event.MATCH_UPDATE_EVENT -> {
                                            updateMatches(api, matches)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_ARM -> {
                                            switchToGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_RESET -> {}
                                        EventStreamWebsocketHandler.Event.MATCH_START -> {
                                            switchToGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_RECYCLE -> {
                                            switchToGameView(ctx)
                                        }

                                        EventStreamWebsocketHandler.Event.MATCH_END -> {}
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
                                    eventStream?.close()
                                })
                    } else {
                        switchToGameView(ctx)
                    }
                }
            } else if (dev) {
                matches.clear()
                matches.add(
                    Match(
                        -1,
                        Match.State.UPCOMING,
                        Match.Result.NOT_FINISHED,
                        Team(1, "Test", "000000","000000", listOf("")),
                        Team(2, "Test", "000000","000000", listOf(""))
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
                        Team(1, "Test", "000000","000000", listOf("")),
                        Team(2, "Test", "000000","000000", listOf(""))
                    )
                )
            }
        }
    }.join()
}

@Composable
fun DeleteMatchDialog(onClose: () -> Unit, onDelete: () -> Unit, editingTeam: Int, api: CVSSAPI) {
    var match by remember { mutableStateOf<Match?>(null) }

    LaunchedEffect(Unit) {
        thread {
            val m = api.getMatch(editingTeam)
            if (m != null) {
                match = m
            }
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Deleting match") },
        text = {
            Text(
                text = if (match == null) {
                    "Loading"
                } else {
                    "Are you sure you want to delete match #${match!!.id} between ${match!!.left.name} and ${match!!.right.name}?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Dismiss") }
        }
    )
}

@Composable
fun StartMatchDialog(onClose: () -> Unit, onDelete: () -> Unit, editingTeam: Int, api: CVSSAPI) {
    var match by remember { mutableStateOf<Match?>(null) }

    LaunchedEffect(Unit) {
        thread {
            val m = api.getMatch(editingTeam)
            if (m != null) {
                match = m
            }
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Arming match") },
        text = {
            Text(
                text = if (match == null) {
                    "Loading"
                } else {
                    "Are you sure you want to ARM match #${match!!.id} between ${match!!.left.name} and ${match!!.right.name}?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete) { Text("ARM") }
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
fun MatchManagerActitiyPreview() {
    CVSSClientTheme {
        Greeting()
    }
}