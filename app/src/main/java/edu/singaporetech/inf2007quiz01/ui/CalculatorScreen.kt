package edu.singaporetech.inf2007quiz01.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.singaporetech.inf2007quiz01.CalculatorButton
import edu.singaporetech.inf2007quiz01.CalculatorPadRow
import edu.singaporetech.inf2007quiz01.ui.theme.ACColor
import edu.singaporetech.inf2007quiz01.ui.theme.ApiToggleBg
import edu.singaporetech.inf2007quiz01.ui.theme.ApiToggleBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.DELColor
import edu.singaporetech.inf2007quiz01.ui.theme.DigitButtonColor
import edu.singaporetech.inf2007quiz01.ui.theme.DigitButtonColorLight
import edu.singaporetech.inf2007quiz01.ui.theme.DisplayBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.DisplayBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.EqualsColor
import edu.singaporetech.inf2007quiz01.ui.theme.FIBColor
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryItemBgDark
import edu.singaporetech.inf2007quiz01.ui.theme.HistoryItemBgLight
import edu.singaporetech.inf2007quiz01.ui.theme.NodeAsm
import edu.singaporetech.inf2007quiz01.ui.theme.NodeCpp
import edu.singaporetech.inf2007quiz01.ui.theme.NodeKotlin
import edu.singaporetech.inf2007quiz01.ui.theme.NodeRust
import edu.singaporetech.inf2007quiz01.ui.theme.NodeWasm
import edu.singaporetech.inf2007quiz01.ui.theme.OperatorColor
import edu.singaporetech.inf2007quiz01.ui.theme.OperatorColorLight
import edu.singaporetech.inf2007quiz01.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    calBotName: String,
    onBack: (() -> Unit)? = null
) {
    val displayText by viewModel.displayText.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val isApiToggled by viewModel.isApiToggled.collectAsState()
    val catchPhrase by viewModel.catchPhrase.collectAsState()
    val lambdaInfo by viewModel.lambdaInfo.collectAsState()
    val blockchainRecords by viewModel.blockchainRecords.collectAsState()
    val raytracedImage by viewModel.raytracedImage.collectAsState()
    val chainVerificationState by viewModel.chainVerificationState.collectAsState()
    val geneticResult by viewModel.geneticResult.collectAsState()
    val monteCarloResult by viewModel.monteCarloResult.collectAsState()
    val gpuVerification by viewModel.gpuVerification.collectAsState()
    val sonificationInfo by viewModel.sonificationInfo.collectAsState()
    val consensusViz by viewModel.consensusViz.collectAsState()
    val configuration = LocalConfiguration.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showChain by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        calBotName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    TextButton(onClick = { showChain = true }) {
                        Text(
                            "Chain",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HistoryPanel(
                        historyList = historyList,
                        catchPhrase = catchPhrase,
                        lambdaInfo = lambdaInfo,
                        geneticResult = geneticResult,
                        monteCarloResult = monteCarloResult,
                        gpuVerification = gpuVerification,
                        sonificationInfo = sonificationInfo,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        DisplayField(displayText, isDark)
                        RaytracedResultView(raytracedImage)
                        CalculatorPad(
                            viewModel = viewModel,
                            isApiToggled = isApiToggled,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize()
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DisplayField(displayText, isDark)
                    Column(modifier = Modifier.weight(1f)) {
                        RaytracedResultView(raytracedImage)
                        AnimatedVisibility(visible = consensusViz != null) {
                            consensusViz?.let { ConsensusPanel(viz = it, isDark = isDark) }
                        }
                        HistoryPanel(
                            historyList = historyList,
                            catchPhrase = catchPhrase,
                            lambdaInfo = lambdaInfo,
                            geneticResult = geneticResult,
                            monteCarloResult = monteCarloResult,
                            gpuVerification = gpuVerification,
                            sonificationInfo = sonificationInfo,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorPad(
                            viewModel = viewModel,
                            isApiToggled = isApiToggled,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize()
                        )
                    }
                }
            }
        }
    }

    if (showChain) {
        ModalBottomSheet(
            onDismissRequest = {
                showChain = false
                viewModel.resetVerification()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BlockchainSheet(
                records = blockchainRecords,
                isDark = isDark,
                verificationState = chainVerificationState,
                onVerify = { viewModel.startChainVerification() },
                onResetVerification = { viewModel.resetVerification() }
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
fun DisplayField(displayText: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isDark) DisplayBgDark else DisplayBgLight,
                        if (isDark) DisplayBgDark.copy(alpha = 0.8f) else DisplayBgLight.copy(alpha = 0.8f),
                    )
                )
            )
    ) {
        TextField(
            value = displayText,
            onValueChange = {},
            textStyle = TextStyle(
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Black,
                lineHeight = 640.sp,
                fontSize = 60.sp,
                color = if (isDark) Color.White else Color(0xFF1C1B1F)
            ),
            maxLines = 2,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("display")
        )
    }
}

@Composable
fun RaytracedResultView(pixels: ByteArray?) {
    if (pixels == null) return
    val bitmap = remember(pixels) {
        val intPixels = IntArray(160 * 120) { i ->
            val r = pixels[i * 4].toInt() and 0xFF
            val g = pixels[i * 4 + 1].toInt() and 0xFF
            val b = pixels[i * 4 + 2].toInt() and 0xFF
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        Bitmap.createBitmap(intPixels, 160, 120, Bitmap.Config.ARGB_8888)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Raytraced Scene",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun HistoryPanel(
    historyList: List<String>,
    catchPhrase: String,
    lambdaInfo: String,
    geneticResult: CalculatorViewModel.GeneticResult?,
    monteCarloResult: CalculatorViewModel.MonteCarloResult?,
    gpuVerification: CalculatorViewModel.GpuVerificationResult?,
    sonificationInfo: CalculatorViewModel.SonificationInfo?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) HistoryBgDark else HistoryBgLight)
    ) {
        if (historyList.isEmpty()) {
            Text(
                text = "No history yet",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("history"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (catchPhrase.isNotEmpty()) {
                item {
                    Text(
                        text = "\"$catchPhrase\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
            items(historyList) { expression ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) HistoryItemBgDark else HistoryItemBgLight)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = expression,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF4A3780)
                    )
                }
            }
            // ── Verification telemetry (below history to avoid test interference) ──
            if (lambdaInfo.isNotEmpty()) {
                item {
                    Text(
                        text = lambdaInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800).copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (geneticResult != null) {
                item {
                    val gr = geneticResult!!
                    Text(
                        text = if (gr.converged) "GA: Evolved in ${gr.generations} gen (pop=${gr.populationSize})"
                               else "GA: FAILED after ${gr.generations} generations",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (gr.converged) Color(0xFF4CAF50).copy(alpha = 0.85f)
                               else Color(0xFFF44336).copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (monteCarloResult != null) {
                item {
                    val mc = monteCarloResult!!
                    Text(
                        text = "MC: n=${mc.samples}, ${String.format("%.2f", mc.errorPct)}% error, ${mc.method}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2196F3).copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (gpuVerification != null) {
                item {
                    val gpu = gpuVerification!!
                    Text(
                        text = if (gpu.verified) "GPU: Vulkan verified in ${gpu.dispatchUs}\u00B5s (${gpu.device})"
                               else "GPU: Verification unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (gpu.verified) Color(0xFF4CAF50).copy(alpha = 0.85f)
                               else Color(0xFFF44336).copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (sonificationInfo != null) {
                item {
                    val si = sonificationInfo!!
                    Text(
                        text = "\u266B ${si.scaleType}, ${si.noteCount} notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (si.scaleType) {
                            "MAJOR_CHORD" -> Color(0xFFE91E63)
                            "PENTATONIC" -> Color(0xFF00BCD4)
                            else -> Color(0xFF9C27B0)
                        }.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorPad(
    viewModel: CalculatorViewModel,
    isApiToggled: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonSpacing = 4.dp
    Column(
        modifier = modifier
            .then(Modifier.padding(4.dp))
            .testTag("calculatorPad"),
        verticalArrangement = Arrangement.spacedBy(buttonSpacing)
    ) {
        for (row in 0..3) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                for (btn in CalculatorPadRow[row])
                    ExpressiveButton(btn, viewModel, isDark, Modifier.weight(1f))
            }
        }

        // 5th row with API toggle
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) ApiToggleBg else ApiToggleBgLight
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "API",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.9f)
                        else Color(0xFF4A3780),
                    )
                    Switch(
                        checked = isApiToggled,
                        modifier = Modifier.testTag("toggleAPI"),
                        onCheckedChange = { viewModel.toggleAPI() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) Color(0xFF555555) else Color(0xFFBBBBBB),
                        )
                    )
                }
            }

            for (btn in CalculatorPadRow[4])
                ExpressiveButton(btn, viewModel, isDark, Modifier.weight(1f))
        }
    }
}

@Composable
fun ExpressiveButton(
    button: CalculatorButton,
    viewModel: CalculatorViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        scale.animateTo(
            if (isPressed) 0.86f else 1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = if (isPressed) Spring.StiffnessMedium else Spring.StiffnessLow
            )
        )
    }

    val (bgColor, textColor) = buttonStyle(button, isDark)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(bgColor, bgColor.copy(alpha = 0.82f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                when (button.text) {
                    "AC" -> viewModel.onAC()
                    "DEL" -> viewModel.onDEL()
                    "FIB" -> viewModel.onFIB()
                    "=" -> viewModel.onEquals()
                    "+", "-", "*", "/" -> viewModel.onOperatorPress(button.text)
                    else -> viewModel.onDigitPress(button.text)
                }
            }
            .testTag("button${button.text}")
            .then(modifier)
    ) {
        Text(
            text = button.text,
            fontSize = if (button.text.length > 2) 22.sp else 30.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun BlockchainSheet(
    records: List<CalculatorViewModel.BlockRecord>,
    isDark: Boolean,
    verificationState: CalculatorViewModel.ChainVerificationState,
    onVerify: () -> Unit,
    onResetVerification: () -> Unit
) {
    // Build a lookup map from verification results
    val verificationMap: Map<Int, CalculatorViewModel.BlockVerification> = when (verificationState) {
        is CalculatorViewModel.ChainVerificationState.InProgress ->
            verificationState.results.associateBy { it.index }
        is CalculatorViewModel.ChainVerificationState.Complete ->
            verificationState.results.associateBy { it.index }
        else -> emptyMap()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header row with title and Verify button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blockchain — ${records.size} blocks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onVerify,
                enabled = verificationState !is CalculatorViewModel.ChainVerificationState.InProgress
            ) {
                Text(
                    text = "Verify Chain",
                    fontWeight = FontWeight.Bold,
                    color = if (verificationState is CalculatorViewModel.ChainVerificationState.InProgress)
                        Color.Gray
                    else
                        Color(0xFF4CAF50)
                )
            }
        }

        // Verification progress / result section
        when (verificationState) {
            is CalculatorViewModel.ChainVerificationState.InProgress -> {
                val progress = if (verificationState.totalBlocks > 0)
                    verificationState.currentIndex.toFloat() / verificationState.totalBlocks
                else 0f
                val scanColor by animateColorAsState(
                    targetValue = if (verificationState.currentIndex % 2 == 0) Color(0xFF4CAF50) else Color(0xFF00BCD4),
                    animationSpec = tween(300),
                    label = "scanColor"
                )
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = scanColor,
                    )
                    Text(
                        text = "SCANNING BLOCK ${verificationState.currentIndex + 1}/${verificationState.totalBlocks}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = scanColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            is CalculatorViewModel.ChainVerificationState.Complete -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (verificationState.allPassed) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else Color(0xFFF44336).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (verificationState.allPassed)
                                    "CHAIN INTEGRITY VERIFIED"
                                else
                                    "CHAIN COMPROMISED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (verificationState.allPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            TextButton(onClick = onResetVerification) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(records.reversed()) { block ->
                BlockRow(
                    block = block,
                    isDark = isDark,
                    verification = verificationMap[block.index]
                )
            }
        }
    }
}

@Composable
private fun BlockRow(
    block: CalculatorViewModel.BlockRecord,
    isDark: Boolean,
    verification: CalculatorViewModel.BlockVerification? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) HistoryItemBgDark else HistoryItemBgLight)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            if (block.expression == "GENESIS") {
                Text(
                    text = "#0 — GENESIS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "#${block.index}: ${block.expression} = ${block.result}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFD0BCFF) else Color(0xFF4A3780)
                )
            }
            Text(
                text = block.hash.take(16) + "\u2026",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "PQ: ${block.pqSeal}",
                fontSize = 10.sp,
                color = Color(0xFF4CAF50).copy(alpha = 0.8f)
            )
            if (block.zkpVerified) {
                Text(
                    text = "ZK-Verified \u2713",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3).copy(alpha = 0.9f)
                )
            }
            // Verification chips
            AnimatedVisibility(visible = verification != null) {
                if (verification != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        VerificationChip("SHA", verification.hashValid)
                        VerificationChip("LINK", verification.chainLinkValid)
                        VerificationChip("PQ", verification.pqSealValid)
                        VerificationChip("ZKP", verification.zkpValid)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationChip(label: String, passed: Boolean) {
    val chipColor by animateColorAsState(
        targetValue = if (passed) Color(0xFF4CAF50) else Color(0xFFF44336),
        animationSpec = tween(300),
        label = "chipColor"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(chipColor.copy(alpha = 0.2f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label ${if (passed) "\u2713" else "\u2717"}",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = chipColor
        )
    }
}

@Composable
private fun ConsensusPanel(
    viz: CalculatorViewModel.ConsensusVisualization,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val nodeColors = mapOf(
        "Kotlin" to NodeKotlin, "C++" to NodeCpp, "Rust" to NodeRust,
        "ARM-ASM" to NodeAsm, "WASM" to NodeWasm
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1A1A2E) else Color(0xFFF0E8FF))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONSENSUS: ${viz.consensusValue}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isDark) Color.White else Color(0xFF1C1B1F)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (viz.unanimous) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFF44336).copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (viz.unanimous) "UNANIMOUS" else "SPLIT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (viz.unanimous) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            viz.nodes.forEach { node ->
                val nodeColor = nodeColors[node.name] ?: Color.Gray
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (node.isByzantine) Color(0xFFF44336).copy(alpha = 0.15f)
                            else nodeColor.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = node.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = nodeColor,
                            maxLines = 1
                        )
                        Text(
                            text = node.result.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF1C1B1F)
                        )
                        Text(
                            text = if (node.isByzantine) "\u2717" else "\u2713",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (node.isByzantine) Color(0xFFF44336) else Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

private fun buttonStyle(button: CalculatorButton, isDark: Boolean): Pair<Color, Color> {
    return when (button.text) {
        "AC" -> ACColor to Color.White
        "DEL" -> DELColor to Color.White
        "FIB" -> FIBColor to Color.White
        "=" -> EqualsColor to Color.White
        "+", "-", "*", "/" -> Pair(
            if (isDark) OperatorColor else OperatorColorLight,
            Color.White
        )
        else -> Pair(
            if (isDark) DigitButtonColor else DigitButtonColorLight,
            if (isDark) Color.White else Color(0xFF1C1B1F)
        )
    }
}
