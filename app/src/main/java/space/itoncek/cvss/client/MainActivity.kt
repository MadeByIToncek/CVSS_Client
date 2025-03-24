package space.itoncek.cvss.client

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import de.markusfisch.android.barcodescannerview.widget.BarcodeScannerView
import de.markusfisch.android.zxingcpp.ZxingCpp
import space.itoncek.cvss.client.ui.theme.CVSSClientTheme
import space.itoncek.cvss.client.ui.theme.Primary

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
    showSystemUi = true,
    device = "id:Galaxy A54 5G"
)
@Composable
fun NarrowCasterMainMenu() {
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission Accepted: Do something
            Log.d(MainActivity::class.qualifiedName, "PERMISSION GRANTED")
            hasPermission = true;
        } else {
            // Permission Denied: Do something
            Log.d(MainActivity::class.qualifiedName, "PERMISSION DENIED")
            hasPermission = false;
        }
    }
    val context = LocalContext.current

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
                if (LocalInspectionMode.current) {
                    GeneratePermissionCard(launcher, true)
                } else {
                    if (hasPermission) {
                        GenerateQRReader()
                    } else {
                        when (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )) {
                            PERMISSION_GRANTED -> {
                                GenerateQRReader()
                            }

                            else -> {
                                GeneratePermissionCard(launcher, false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenerateQRReader() {
    AndroidView(modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            // Creates view
            BarcodeScannerView(context).apply {
                cropRatio = 0.75f;
                formats.clear()
                formats.add(ZxingCpp.BarcodeFormat.PDF_417)

                setOnBarcodeListener { result ->
                    Log.i(MainActivity::class.qualifiedName, result.text)
                    return@setOnBarcodeListener true
                }

                openAsync()
            }
        }, update = { view ->
            view.close();
            view.openAsync();
        })
}

@Composable
fun GeneratePermissionCard(
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    isdev: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(12.dp)
    ) {
        Text(
            text = "To use this app, you must give us Camera permission to scan the PDF code from the console.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = {
            launcher.launch(Manifest.permission.CAMERA)
        }) {
            Text("Grant Camera Permission")
        }
        if (isdev) Text(text = "This is dev mode!", color = MaterialTheme.colorScheme.onError)
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