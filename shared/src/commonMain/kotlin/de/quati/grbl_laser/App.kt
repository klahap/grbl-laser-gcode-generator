package de.quati.grbl_laser

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import java.awt.FileDialog
import java.awt.Frame
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.io.path.writeText

data class AppUiState(
    val fontSize: String = "13.0",
    val laserPower: String = "400",
    val laserSpeed: String = "400",
    val inputData: String = "",
    val fontFile: File? = Path("./").walk()
        .firstOrNull { it.isRegularFile() && it.name.endsWith(".ttf") }
        ?.toFile(),
    val hAlign: Align = Align.CENTER,
    val vAlign: Align = Align.CENTER,
    val successMessage: String? = null,
) {
    val fontSizeValue = fontSize.toFloatOrNull()?.takeIf { it > 0f }
    val laserPowerValue = laserPower.toUIntOrNull()?.takeIf { it in 0u..1000u }
    val laserSpeedValue = laserSpeed.toUIntOrNull()?.takeIf { it > 0u }
    val inputDataValue = inputData.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        .takeIf { it.isNotEmpty() }
    val generateGCodeData: GenerateGCodeData? = run {
        GenerateGCodeData(
            fontFile = fontFile ?: return@run null,
            fontSize = fontSizeValue ?: return@run null,
            inputData = inputDataValue ?: return@run null,
            laserPower = laserPowerValue ?: return@run null,
            laserSpeed = laserSpeedValue ?: return@run null,
            hAlign = hAlign,
            vAlign = vAlign,
        )
    }
    val canGenerate = generateGCodeData != null
}

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF1F5E5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F0ED),
    onPrimaryContainer = Color(0xFF0B2422),
    secondary = Color(0xFF526461),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E7E3),
    onSecondaryContainer = Color(0xFF10201E),
    tertiary = Color(0xFF556179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE5FF),
    onTertiaryContainer = Color(0xFF111B33),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDDE5E2),
    onSurfaceVariant = Color(0xFF414947),
    outline = Color(0xFF6F7976),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
)

sealed interface AppIntent {
    data class FontSizeChanged(val value: String) : AppIntent
    data class LaserPowerChanged(val value: String) : AppIntent
    data class LaserSpeedChanged(val value: String) : AppIntent
    data class InputDataChanged(val value: String) : AppIntent
    data class FontFileChanged(val value: File?) : AppIntent
    data class HAlignChanged(val value: Align) : AppIntent
    data class VAlignChanged(val value: Align) : AppIntent
    data class GenerateClicked(val outputDir: File) : AppIntent
    data object SuccessMessageDismissed : AppIntent
}

class AppViewModel : ViewModel() {
    var uiState by mutableStateOf(AppUiState())
        private set

    fun onIntent(intent: AppIntent) {
        uiState = when (intent) {
            is AppIntent.FontSizeChanged -> uiState.copy(fontSize = intent.value)
            is AppIntent.LaserPowerChanged -> uiState.copy(laserPower = intent.value)
            is AppIntent.LaserSpeedChanged -> uiState.copy(laserSpeed = intent.value)
            is AppIntent.InputDataChanged -> uiState.copy(inputData = intent.value)
            is AppIntent.FontFileChanged -> uiState.copy(fontFile = intent.value)
            is AppIntent.HAlignChanged -> uiState.copy(hAlign = intent.value)
            is AppIntent.VAlignChanged -> uiState.copy(vAlign = intent.value)
            AppIntent.SuccessMessageDismissed -> uiState.copy(successMessage = null)
            is AppIntent.GenerateClicked -> {
                uiState.generateGCodeData?.let { data ->
                    println("Generating G-Code files...")
                    val nofDigits = (data.inputData.size + 1).toString().length.coerceAtLeast(2)
                    val outputDir = intent.outputDir.toPath().also { it.createDirectories() }
                    generate(data).forEachIndexed { index, (name, gcode) ->
                        val prefix = (index + 1).toString().padStart(nofDigits, '0')
                        val filename = "${prefix}_${name.toPathSafe()}.nc"
                        outputDir.resolve(filename).writeText(gcode.content)
                    }
                    println("Generated all files in $outputDir")
                    uiState.copy(successMessage = "Generated ${data.inputData.size} G-Code file(s) in $outputDir")
                } ?: uiState
            }
        }
    }
}

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App(appViewModel: AppViewModel = viewModel { AppViewModel() }) {
    val uiState = appViewModel.uiState

    MaterialTheme(colorScheme = AppColorScheme) {
        val textFieldColors = appTextFieldColors()

        uiState.successMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { appViewModel.onIntent(AppIntent.SuccessMessageDismissed) },
                confirmButton = {
                    Button(onClick = { appViewModel.onIntent(AppIntent.SuccessMessageDismissed) }) {
                        Text("OK")
                    }
                },
                title = { Text("Generation complete") },
                text = { Text(message) },
            )
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.fontSize,
                    onValueChange = { appViewModel.onIntent(AppIntent.FontSizeChanged(it)) },
                    label = { Text("Font Size") },
                    supportingText = {
                        if (uiState.fontSizeValue == null) Text("Enter a value greater than 0")
                    },
                    isError = uiState.fontSizeValue == null,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.padding(4.dp))
                val fontFilePath = uiState.fontFile?.absolutePath ?: ""
                var isFileDialogVisible by remember { mutableStateOf(false) }
                val fontFileInteractionSource = remember { MutableInteractionSource() }
                LaunchedEffect(fontFileInteractionSource) {
                    fontFileInteractionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            isFileDialogVisible = true
                        }
                    }
                }
                OutlinedTextField(
                    value = TextFieldValue(
                        text = fontFilePath,
                        selection = TextRange(fontFilePath.length),
                    ),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Font File") },
                    supportingText = {
                        if (uiState.fontFile == null) Text("Select a TTF font file")
                    },
                    isError = uiState.fontFile == null,
                    singleLine = true,
                    colors = textFieldColors,
                    interactionSource = fontFileInteractionSource,
                    modifier = Modifier.weight(1f),
                )
                if (isFileDialogVisible) {
                    FileDialog(
                        onCloseRequest = {
                            isFileDialogVisible = false
                            if (it != null) appViewModel.onIntent(AppIntent.FontFileChanged(File(it)))
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.laserPower,
                    onValueChange = { appViewModel.onIntent(AppIntent.LaserPowerChanged(it)) },
                    label = { Text("Laser Power") },
                    supportingText = {
                        if (uiState.laserPowerValue == null) Text("Enter a value from 0 to 1000")
                    },
                    isError = uiState.laserPowerValue == null,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = uiState.laserSpeed,
                    onValueChange = { appViewModel.onIntent(AppIntent.LaserSpeedChanged(it)) },
                    label = { Text("Laser Speed") },
                    supportingText = {
                        if (uiState.laserSpeedValue == null) Text("Enter a value greater than 0")
                    },
                    isError = uiState.laserSpeedValue == null,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AlignSelectField(
                    label = "Horizontal Alignment",
                    value = uiState.hAlign,
                    onValueChange = { appViewModel.onIntent(AppIntent.HAlignChanged(it)) },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.padding(4.dp))
                AlignSelectField(
                    label = "Vertical Alignment",
                    value = uiState.vAlign,
                    onValueChange = { appViewModel.onIntent(AppIntent.VAlignChanged(it)) },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.inputData,
                onValueChange = { appViewModel.onIntent(AppIntent.InputDataChanged(it)) },
                label = { Text("Input Data") },
                supportingText = {
                    if (uiState.inputDataValue == null) Text("Enter input data")
                },
                isError = uiState.inputDataValue == null,
                colors = textFieldColors,
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 80.dp),
            )
            Spacer(Modifier.height(16.dp))
            var isOutputDirDialogVisible by remember { mutableStateOf(false) }
            Button(
                onClick = { isOutputDirDialogVisible = true },
                enabled = uiState.canGenerate,
            ) {
                Text("Generate")
            }
            if (isOutputDirDialogVisible) {
                DirectoryDialog(
                    onCloseRequest = {
                        isOutputDirDialogVisible = false
                        if (it != null) appViewModel.onIntent(AppIntent.GenerateClicked(File(it)))
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AlignSelectField(
    label: String,
    value: Align,
    onValueChange: (Align) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = colors,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Align.entries.forEach { align ->
                DropdownMenuItem(
                    text = { Text(align.name) },
                    onClick = {
                        onValueChange(align)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FileDialog(
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    visible = true,
    create = {
        object : FileDialog(null as Frame?, "Select Font", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    if (directory != null && file != null) {
                        onCloseRequest(File(directory, file).absolutePath)
                    } else {
                        onCloseRequest(null)
                    }
                }
            }
        }
    },
    update = {},
    dispose = FileDialog::dispose
)

@Composable
private fun DirectoryDialog(
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    visible = true,
    create = {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        object : FileDialog(null as Frame?, "Select Output Directory", SAVE) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    if (directory != null && file != null) {
                        onCloseRequest(File(directory, file).absolutePath)
                    } else {
                        onCloseRequest(null)
                    }
                }
            }
        }
    },
    update = {},
    dispose = FileDialog::dispose
)