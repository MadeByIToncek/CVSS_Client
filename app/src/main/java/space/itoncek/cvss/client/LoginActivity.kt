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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import space.itoncek.cvss.client.ui.theme.Primary
import java.io.File
import java.io.FileWriter
import java.util.Scanner

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
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun NarrowCasterMainMenu() {
    var url by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    CVSSClientTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                        .padding(12.dp)
                ) {
                    AutoSizeText(
                        "NarrowCaster client",
                        34.sp,
                        if (LocalInspectionMode.current) Primary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Absolute.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Text(
                            "by ",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (LocalInspectionMode.current) Primary else MaterialTheme.colorScheme.onPrimaryContainer
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
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
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        placeholder = {
                            Text("Server URL", color = Color.Gray)
                        }
                    )

                    Button(onClick = {
                        ctx.startActivity(Intent(ctx, TeamManagerActivity::class.java))
                    }) {
                        Text("Login")
                    }
                }
            }
            val dev = LocalInspectionMode.current;
            val ctx = LocalContext.current;
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(LocalContext.current) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START && !dev) {
                        if (ctx.filesDir == null) {
                            url = "http://localhost:4444"
                        }
                        val cfgfile: File = File(ctx.filesDir.toString() + "/config.cfg")
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