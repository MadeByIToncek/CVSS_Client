package space.itoncek.cvss.client

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import space.itoncek.cvss.client.api.CVSSAPI
import space.itoncek.cvss.client.prepare.TeamManagerActivity
import space.itoncek.cvss.client.prepare.runOnUiThread
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import java.io.File
import java.io.FileWriter
import java.util.Scanner
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            NarrowCasterMainMenu()
        }
    }
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
/*
@Preview(
    name = "dev preview",
    group = "dev",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)*/
@Composable
fun NarrowCasterMainMenu() {
    var url by remember { mutableStateOf("") }
    var pingEstablished by remember { mutableStateOf(false) }
    var ping by remember { mutableLongStateOf(-1) }
    var disable by remember { mutableStateOf(false) }
    var serverVer by remember { mutableIntStateOf(-1) }
    var ver by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val api = CVSSAPI(ctx.filesDir)

    LaunchedEffect(Unit) {
        while (true) {
            thread {
                val p = api.ping
                runOnUiThread {
                    pingEstablished = p >= 0
                    ping = p
                    if (pingEstablished) thread {
                        ver = api.version
                        serverVer = when (ver) {
                            serverVersion -> 2
                            devVersion -> 1
                            else -> 0
                        }
                    }
                    else serverVer = -1
                }
            }
            if (!disable) {
                delay(1.seconds)
            }
        }
    }

    CVSSClientTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pingEstablished) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onError)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (pingEstablished) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (pingEstablished) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                elevation = CardDefaults.elevatedCardElevation(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(12.dp),
                ) {
                    AutoSizeText(
                        "CVSS_Client",
                        34.sp,
                        if (pingEstablished) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Absolute.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Text(
                            text = "by ",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Image(
                            imageVector = ImageVector.vectorResource(R.drawable.by_centrumdeti)
                                .apply {},
                            contentDescription = "centrumdeti.cz",
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (pingEstablished) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (pingEstablished) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                elevation = CardDefaults.elevatedCardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = url,
                        onValueChange = {
                            url = it;
                            val fw = FileWriter(File(ctx.filesDir.toString() + "/config.cfg"))
                            fw.write(it)
                            fw.close()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        placeholder = {
                            Text(
                                "Server URL",
                                color = if (pingEstablished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = if (pingEstablished) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer,
                            focusedContainerColor = if (pingEstablished) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer,
                        )
                    )

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            "Server version: %s".format(
                                when (serverVer) {
                                    2 -> ver
                                    1 -> "👩‍💻🔨🔧💻"
                                    0 -> "🛑 $ver"
                                    else -> "📵/❌"
                                }
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            "Server connection: %s".format(
                                if (pingEstablished) "%dms".format(ping) else "No connection"
                            )
                        )
                    }

                    Button(
                        onClick = {
                            ctx.startActivity(Intent(ctx, TeamManagerActivity::class.java))
                        }, enabled = pingEstablished && serverVer > 0
                    ) {
                        Text("Login")
                    }
                }
            }
            val dev = LocalInspectionMode.current;
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(LocalContext.current) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START && !dev) {
                        thread {
                            val p = api.ping
                            runOnUiThread {
                                pingEstablished = p >= 0
                                ping = p
                                if (pingEstablished) thread {
                                    ver = api.version
                                    serverVer = when (ver) {
                                        serverVersion -> 2
                                        devVersion -> 1
                                        else -> 0
                                    }
                                }.join()
                                else serverVer = -1
                            }
                        }.join()

                        if (ctx.filesDir == null) {
                            url = "http://192.168.99.64:4444"
                        }
                        val cfgfile = File(ctx.filesDir.toString() + "/config.cfg")
                        if (cfgfile.exists()) {
                            Scanner(cfgfile).use { sc ->
                                url = ""
                                while (sc.hasNextLine()) {
                                    url += sc.nextLine()
                                }
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    disable = true
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }
}

@Composable
fun AutoSizeText(
    text: String, fontSize: TextUnit, textColor: Color, modifier: Modifier = Modifier
) {
    var fontSizeState by remember { mutableStateOf(fontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        fontSize = fontSizeState,
        softWrap = false,
        color = textColor,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                fontSizeState *= .95
            } else {
                readyToDraw = true
            }
        })
}