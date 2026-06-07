package de.quati.grbl_laser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle as ComposeFontStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App(appViewModel: AppViewModel = viewModel { AppViewModel() }) {
    val uiState by appViewModel.uiState.collectAsState()

    MaterialTheme(colorScheme = AppColorScheme) {
        val textFieldColors = appTextFieldColors()

        when (val status = uiState.generateStatus) {
            GenerateStatus.InProgress, GenerateStatus.NotStarted -> Unit
            is GenerateStatus.Error ->
                AlertDialog(
                    icon = {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onDismissRequest = { appViewModel.onIntent(AppIntent.GeneratorStatusMessageDismissed) },
                    confirmButton = {
                        Button(onClick = { appViewModel.onIntent(AppIntent.GeneratorStatusMessageDismissed) }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Generation failed") },
                    text = { Text(status.message) },
                )

            is GenerateStatus.Success ->
                AlertDialog(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onDismissRequest = { appViewModel.onIntent(AppIntent.GeneratorStatusMessageDismissed) },
                    confirmButton = {
                        Button(onClick = { appViewModel.onIntent(AppIntent.GeneratorStatusMessageDismissed) }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Generation complete") },
                    text = { Text(status.message) },
                )
        }
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        modifier = Modifier.weight(0.6f),
                    )
                    Spacer(Modifier.padding(4.dp))
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(top = 8.dp, bottom = 22.dp)
                    ) {
                        for ((index, style) in FontStyle.entries.withIndex()) {
                            val shape = when {
                                FontStyle.entries.size == 1 -> MaterialTheme.shapes.small
                                index == 0 -> MaterialTheme.shapes.small.copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                )

                                index == FontStyle.entries.size - 1 -> MaterialTheme.shapes.small.copy(
                                    topStart = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp)
                                )

                                else -> RectangleShape
                            }
                            FilterChip(
                                selected = style in uiState.fontStyles,
                                onClick = { appViewModel.onIntent(AppIntent.FontStyleToggled(style)) },
                                shape = shape,
                                label = {
                                    Text(
                                        text = when (style) {
                                            FontStyle.BOLD -> "B"
                                            FontStyle.ITALIC -> "I"
                                        },
                                        style = when (style) {
                                            FontStyle.BOLD -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                            FontStyle.ITALIC -> MaterialTheme.typography.bodyLarge.copy(fontStyle = ComposeFontStyle.Italic)
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                            )
                        }
                    }
                    Spacer(Modifier.padding(4.dp))
                    FontSelectField(
                        value = uiState.fontName,
                        onValueChange = { appViewModel.onIntent(AppIntent.FontNameChanged(it)) },
                        fonts = remember { getFonts() },
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                    )
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
                Button(
                    onClick = { appViewModel.onIntent(AppIntent.GenerateClicked) },
                    enabled = uiState.canGenerate && uiState.generateStatus is GenerateStatus.NotStarted,
                ) {
                    Text("Generate")
                }
            }

            //Spacer(Modifier.padding(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FontSelectField(
    label: String = "Font",
    value: String?,
    onValueChange: (String) -> Unit,
    fonts: List<String>,
    colors: TextFieldColors,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = {
                if (value == null) Text("Select a font")
            },
            isError = value == null,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = colors,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            fonts.forEach { font ->
                DropdownMenuItem(
                    text = { Text(font) },
                    onClick = {
                        onValueChange(font)
                        expanded = false
                    },
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
    colors: TextFieldColors,
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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
