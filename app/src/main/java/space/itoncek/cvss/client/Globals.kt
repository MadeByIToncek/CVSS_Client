package space.itoncek.cvss.client

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.play.MatchMasterControlActivity
import space.itoncek.cvss.client.play.OverlayControlActivity
import space.itoncek.cvss.client.play.ScoreControlActivity
import space.itoncek.cvss.client.prepare.MatchManagerActivity
import space.itoncek.cvss.client.prepare.TeamManagerActivity

const val serverVersion = "v0.0.0.1"
const val devVersion = "vDEVELOPMENT"

@Composable
fun GenerateNavigation(i: SourceActivity, scope: CoroutineScope, drawerState: DrawerState, ctx: Context) {
    if(listOf(SourceActivity.TeamManager, SourceActivity.MatchManager).contains(i)) {
        ModalDrawerSheet {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "CVSS Client",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                Text(
                    "Setup",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                )
                NavigationDrawerItem(
                    label = { Text("Teams") },
                    selected = i == SourceActivity.TeamManager,
                    onClick = {
                        if (i != SourceActivity.TeamManager) {
                            scope.launch { drawerState.close() }
                            ctx.startActivity(Intent(ctx, TeamManagerActivity::class.java))
                            (ctx as Activity).finish()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Matches") },
                    selected = i == SourceActivity.MatchManager,
                    onClick = {
                        if (i != SourceActivity.MatchManager) {
                            scope.launch { drawerState.close() }
                            ctx.startActivity(Intent(ctx, MatchManagerActivity::class.java))
                            (ctx as Activity).finish()
                        }
                    }
                )
            }
        }
    } else if(listOf(SourceActivity.ScoringControl, SourceActivity.MatchMasterControl, SourceActivity.OverlayControl).contains(i)) {
        ModalDrawerSheet {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "CVSS Client",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                Text(
                    "In Game",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                )
                NavigationDrawerItem(
                    label = { Text("Master control") },
                    selected = i == SourceActivity.MatchMasterControl,
                    onClick = {
                        if (i != SourceActivity.MatchMasterControl) {
                            scope.launch { drawerState.close() }
                            ctx.startActivity(Intent(ctx, MatchMasterControlActivity::class.java))
                            (ctx as Activity).finish()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Scoring") },
                    selected = i == SourceActivity.ScoringControl,
                    onClick = {
                        if (i != SourceActivity.ScoringControl) {
                            scope.launch { drawerState.close() }
                            ctx.startActivity(Intent(ctx, ScoreControlActivity::class.java))
                            (ctx as Activity).finish()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Overlay") },
                    selected = i == SourceActivity.OverlayControl,
                    onClick = {
                        if (i != SourceActivity.OverlayControl) {
                            scope.launch { drawerState.close() }
                            ctx.startActivity(Intent(ctx, OverlayControlActivity::class.java))
                            (ctx as Activity).finish()
                        }
                    }
                )
            }
        }
    }
}

fun runOnUiThread(block: suspend () -> Unit) = CoroutineScope(Dispatchers.Main).launch { block() }

fun determineRightScreen(api: CVSSAPI): List<SourceActivity> {
    val inGame = api.isInGame();
    val armed = api.isArmed();
    return if(inGame || armed) {
        listOf(SourceActivity.MatchMasterControl, SourceActivity.ScoringControl, SourceActivity.OverlayControl)
    } else {
        listOf(SourceActivity.TeamManager,SourceActivity.MatchManager)
    }
}

fun switchToGameView(ctx: Context) {
    ctx.startActivity(Intent(ctx,MatchMasterControlActivity::class.java))
    (ctx as? Activity)?.finish()
}

fun switchToPrepareView(ctx: Context) {
    ctx.startActivity(Intent(ctx,MatchManagerActivity::class.java))
    (ctx as? Activity)?.finish()
}

enum class SourceActivity {
    TeamManager,
    MatchManager,
    MatchMasterControl,
    ScoringControl,
    OverlayControl
}
