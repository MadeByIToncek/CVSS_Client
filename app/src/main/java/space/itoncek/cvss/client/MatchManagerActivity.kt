package space.itoncek.cvss.client

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.api.objects.Match
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme

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
                                showBottomSheet = true
                            }) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }

//            if (showBottomSheet) {
//                ModalBottomSheet(
//                    onDismissRequest = {
//                        showBottomSheet = false
//                    }, sheetState = sheetState
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp)
//                            .wrapContentHeight(),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        var teamName by remember { mutableStateOf(editingTeamName) }
//                        TextField(teamName, onValueChange = {
//                            teamName = it
//                        }, modifier = Modifier.fillMaxWidth())
//                        Row {
//                            Button(
//                                onClick = {
//                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
//                                        if (!sheetState.isVisible) {
//                                            showBottomSheet = false
//                                        }
//                                    }
//                                },
//                                modifier = Modifier
//                                    .fillMaxWidth(.4f)
//                                    .padding(8.dp),
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = MaterialTheme.colorScheme.errorContainer,
//                                    contentColor = MaterialTheme.colorScheme.error
//                                )
//                            ) {
//                                Text("Cancel")
//                            }
//                            Button(
//                                onClick = {
//                                    thread {
//                                        api.updateTeam(editingTeam, teamName)
//                                    }.join()
//
//                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
//                                        if (!sheetState.isVisible) {
//                                            showBottomSheet = false
//                                        }
//                                    }
//                                }, modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(8.dp)
//                            ) {
//                                Text("Save")
//                            }
//                        }
//                    }
//                    // Sheet content
//                    Button(onClick = {
//
//                    }) {
//                        Text("Hide bottom sheet")
//                    }
//                }
//            }
        }

        // Screen content
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    CVSSClientTheme {
        Greeting()
    }
}