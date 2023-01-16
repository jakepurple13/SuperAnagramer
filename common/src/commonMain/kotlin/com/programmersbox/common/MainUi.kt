package com.programmersbox.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordUi(
    scope: CoroutineScope = rememberCoroutineScope(),
    vm: WordViewModel = remember { WordViewModel(scope) },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val gridState = rememberLazyGridState()

    SnackbarHandler(vm = vm, snackbarHostState = snackbarHostState)

    WordDialogs(vm)

    ModalNavigationDrawer(
        drawerContent = { },//DismissibleDrawerSheet { /*DefinitionDrawer(vm)*/ } },
        drawerState = drawerState,
        gesturesEnabled = vm.definition != null && drawerState.isOpen
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Guess the Words") },
                    actions = {
                        Text("${vm.wordGuesses.size}/${vm.anagramWords.size}")
                        TextButton(
                            onClick = { vm.finishGame = true },
                            enabled = !vm.finishedGame
                        ) { Text("Finish") }
                        TextButton(onClick = { vm.shouldStartNewGame = true }) { Text("New Game") }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                BottomBar(
                    vm = vm,
                    snackbarHostState = snackbarHostState
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { padding ->
            WordContent(
                padding = padding,
                vm = vm,
                gridState = gridState,
                drawerState = drawerState
            )
        }
    }

    LoadingDialog(showLoadingDialog = vm.isLoading)
}

@Composable
internal fun LoadingDialog(showLoadingDialog: Boolean) {
    if (showLoadingDialog) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.0.dp)
                    )
            ) {
                Column {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(text = "Loading", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

internal val Emerald = Color(0xFF2ecc71)
internal val Sunflower = Color(0xFFf1c40f)
internal val Alizarin = Color(0xFFe74c3c)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomBar(
    vm: WordViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    CustomBottomAppBar {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    vm.wordGuess.forEachIndexed { index, c ->
                        OutlinedButton(
                            onClick = { vm.updateGuess(vm.wordGuess.removeRange(index, index + 1)) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) { Text(c.uppercase()) }
                    }
                }

                OutlinedButton(
                    onClick = { vm.wordGuess = "" }
                ) { Icon(Icons.Default.Clear, null, tint = Alizarin) }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .height(if (vm.useLetters) 40.dp else 500.dp) //TODO: Use an expect for the patterninput height
                    .animateContentSize()
                    .fillMaxWidth()
            ) {
                when (vm.useLetters) {
                    true -> {
                        val cornerSize = 16.dp
                        vm.mainLetters.forEachIndexed { index, it ->
                            OutlinedIconButton(
                                onClick = { vm.updateGuess("${vm.wordGuess}$it") },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = if (vm.useLetters) Modifier.weight(1f) else Modifier,
                                shape = if (vm.useLetters) when (index) {
                                    0 -> RoundedCornerShape(topStart = cornerSize, bottomStart = cornerSize)
                                    vm.mainLetters.lastIndex -> RoundedCornerShape(
                                        topEnd = cornerSize,
                                        bottomEnd = cornerSize
                                    )

                                    else -> RectangleShape
                                } else CircleShape
                            ) { Text(it.uppercase()) }
                        }
                    }

                    false -> {
                        PatternInput(
                            options = vm.mainLetters.toList(),
                            optionToString = { it.uppercase() },
                            modifier = Modifier.size(500.dp), //TODO: Use an expect for the patterninput height
                            colors = PatternInputDefaults.defaultColors(
                                dotsColor = MaterialTheme.colorScheme.primary,
                                linesColor = MaterialTheme.colorScheme.onSurface,
                                letterColor = MaterialTheme.colorScheme.primary,
                            ),
                            sensitivity = 50f,
                            dotsSize = 25.sp.value,
                            dotCircleSize = 25.sp.value * 1.5f,
                            linesStroke = 50f,
                            circleStroke = Stroke(width = 4.dp.value),
                            animationDuration = 200,
                            animationDelay = 100,
                            onStart = {
                                vm.wordGuess = ""
                                vm.updateGuess("${vm.wordGuess}${it.id}")
                            },
                            onDotRemoved = { vm.updateGuess(vm.wordGuess.removeSuffix(it.id.toString())) },
                            onDotConnected = { vm.updateGuess("${vm.wordGuess}${it.id}") },
                            onResult = {
                                if (it.isNotEmpty()) {
                                    scope.launch {
                                        val message = vm.guess {

                                        }
                                        vm.wordGuess = ""
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            message,
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                /*Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .height(48.dp)
                            .animateContentSize()
                            .fillMaxWidth()
                    ) {
                        vm.mainLetters.forEachIndexed { index, it ->
                            OutlinedButton(
                                onClick = { vm.updateGuess("${vm.wordGuess}$it") },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) { Text(it.uppercase()) }
                        }
                    }


                }*/
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateContentSize()
                    .fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = vm::bringBackWord
                ) { Icon(Icons.Default.Undo, null) }

                OutlinedButton(
                    onClick = vm::shuffle,
                ) { Icon(Icons.Default.Shuffle, null) }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val message = vm.guess {}
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    enabled = vm.wordGuess.isNotEmpty()
                ) {
                    Text(
                        "ENTER",
                        color = if (vm.wordGuess.isNotEmpty()) Emerald else LocalContentColor.current
                    )
                }
            }
        }
    }
}

@Composable
internal fun CustomBottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        // TODO(b/209583788): Consider adding a shape parameter if updated design guidance allows
        shape = RectangleShape,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordDialogs(vm: WordViewModel) {
    val isFinished by remember { derivedStateOf { vm.wordGuesses.size == vm.anagramWords.size } }

    /*if (vm.shouldStartNewGame) {
        AlertDialog(
            onDismissRequest = { vm.shouldStartNewGame = false },
            title = { Text("New Game?") },
            text = { Text("Are you sure?${if (!isFinished) " You will lose all your progress." else ""}") },
            confirmButton = { TextButton(onClick = vm::getWord) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { vm.shouldStartNewGame = false }) { Text("No") } }
        )
    }

    if (vm.finishGame) {
        AlertDialog(
            onDismissRequest = { vm.finishGame = false },
            title = { Text("Finish Game?") },
            text = { Text("Are you sure?${if (!isFinished) " You will lose all your progress." else ""}") },
            confirmButton = { TextButton(onClick = vm::endGame) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { vm.finishGame = false }) { Text("No") } }
        )
    }

    if (vm.showScoreInfo) {
        AlertDialog(
            onDismissRequest = { vm.showScoreInfo = false },
            title = { Text("Score Info") },
            text = {
                LazyColumn {
                    items(vm.scoreInfo.entries.toList()) {
                        ListItem(headlineText = { Text("${it.key} = ${it.value} points") })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.showScoreInfo = false }) { Text("Done") } },
        )
    }*/
}

@Composable
internal fun SnackbarHandler(vm: WordViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(vm.error) {
        if (vm.error != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                vm.error!!,
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed -> vm.error = null
                SnackbarResult.ActionPerformed -> vm.error = null
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(vm.gotNewHint) {
        if (vm.gotNewHint) {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                "Got enough words for a new hint!",
                duration = SnackbarDuration.Short
            )
            vm.gotNewHint = when (result) {
                SnackbarResult.Dismissed -> false
                SnackbarResult.ActionPerformed -> false
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordContent(
    padding: PaddingValues,
    vm: WordViewModel,
    gridState: LazyGridState,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.padding(padding)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = vm::useHint,
                enabled = vm.hintCount > 0,
                modifier = Modifier.align(Alignment.CenterStart)
            ) { Text("?" + vm.hintCount.toString()) }

            OutlinedButton(
                onClick = { vm.showScoreInfo = true },
                enabled = vm.score > 0,
                modifier = Modifier.align(Alignment.Center)
            ) { Text("${animateIntAsState(vm.score).value} points") }

            OutlinedButton(
                onClick = { vm.showHighScores = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) { Text("View HighScores") }
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 2.dp)
        ) {
            items(vm.anagramWords.sortedByDescending { it.length }) { anagrams ->
                Crossfade(targetState = vm.wordGuesses.any { it.equals(anagrams, true) }) { state ->
                    if (state) {
                        OutlinedCard(
                            onClick = { vm.getDefinition(anagrams) { scope.launch { drawerState.open() } } },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
                        ) {
                            CustomListItem(
                                containerColor = Color.Transparent
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                ) { Text(anagrams, style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    } else {
                        Card(onClick = {}, enabled = false) {
                            CustomListItem {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        anagrams
                                            .uppercase()
                                            .replace(
                                                if (vm.hintList.isNotEmpty()) {
                                                    Regex("[^${vm.hintList.joinToString("")}]")
                                                } else {
                                                    Regex("\\w")
                                                },
                                                " _"
                                            ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${anagrams.length}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun CustomListItem(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 4.dp,
    shadowElevation: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 8.dp)
                .padding(PaddingValues(vertical = 16.dp, horizontal = 16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}