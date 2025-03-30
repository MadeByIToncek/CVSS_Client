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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.itoncek.cvss.client.play.MatchMasterControlActivity
import space.itoncek.cvss.client.prepare.MatchManagerActivity
import space.itoncek.cvss.client.prepare.TeamManagerActivity

const val serverVersion = "v0.0.0.1"
const val devVersion = "vDEVELOPMENT"

@Composable
fun PrepareNavigation(i: PrepareSourceActivity, scope: CoroutineScope, drawerState: DrawerState, ctx: Context) {
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
                style = MaterialTheme.typography.titleMedium
            )
            NavigationDrawerItem(
                label = { Text("Teams") },
                selected = i == PrepareSourceActivity.TeamManager,
                onClick = {
                    if (i != PrepareSourceActivity.TeamManager) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, TeamManagerActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )
            NavigationDrawerItem(
                label = { Text("Matches") },
                selected = i == PrepareSourceActivity.MatchManager,
                onClick = {
                    if (i != PrepareSourceActivity.MatchManager) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, MatchManagerActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )
        }
    }
}
@Composable
fun PlayNavigation(i: PlaySourceActivity, scope: CoroutineScope, drawerState: DrawerState, ctx: Context) {
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
                style = MaterialTheme.typography.titleMedium
            )
            NavigationDrawerItem(
                label = { Text("Master control") },
                selected = i == PlaySourceActivity.MatchMasterControl,
                onClick = {
                    if (i != PlaySourceActivity.MatchMasterControl) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, MatchMasterControlActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )
            NavigationDrawerItem(
                label = { Text("Scoring") },
                selected = i == PlaySourceActivity.ScoringControl,
                onClick = {
                    if (i != PlaySourceActivity.ScoringControl) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, MatchManagerActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )
        }
    }
}

fun switchToGameView(ctx: Context) {
    ctx.startActivity(Intent(ctx,MatchMasterControlActivity::class.java))
    (ctx as? Activity)?.finish()
}

fun switchToPrepareView(ctx: Context) {
    ctx.startActivity(Intent(ctx,TeamManagerActivity::class.java))
    (ctx as? Activity)?.finish()
}

enum class PrepareSourceActivity {
    TeamManager,
    MatchManager
}
enum class PlaySourceActivity {
    MatchMasterControl,
    ScoringControl
}
