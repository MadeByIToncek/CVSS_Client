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

const val serverVersion = "v0.0.0.1"
const val devVersion = "vDEVELOPMENT"

@Composable
fun GlobalNavigation(i: ScreenView, scope: CoroutineScope, drawerState: DrawerState, ctx: Context) {
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
                selected = i == ScreenView.TeamManager,
                onClick = {
                    if (i != ScreenView.TeamManager) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, TeamManagerActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )
            NavigationDrawerItem(
                label = { Text("Matches") },
                selected = i == ScreenView.MatchManager,
                onClick = {
                    if (i != ScreenView.MatchManager) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(Intent(ctx, MatchManagerActivity::class.java))
                        (ctx as Activity).finish()
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Playing",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            NavigationDrawerItem(
                label = { Text("Setup next game") },
                selected = i == ScreenView.GameSetup,
                onClick = {
                    if (i != ScreenView.GameSetup) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(
                            Intent(
                                ctx, /*TODO)) Replace*/
                                SetupNextGameActivity::class.java
                            )
                        )
                        (ctx as Activity).finish()
                    }
                }
            )
            NavigationDrawerItem(
                label = { Text("Game scoring") },
                selected = i == ScreenView.GameScoring,
                onClick = {
                    if (i != ScreenView.GameScoring) {
                        scope.launch { drawerState.close() }
                        ctx.startActivity(
                            Intent(
                                ctx, /*TODO)) Replace*/
                                MatchManagerActivity::class.java
                            )
                        )
                        (ctx as Activity).finish()
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

enum class ScreenView {
    TeamManager,
    MatchManager,
    GameSetup,
    GameScoring
}
