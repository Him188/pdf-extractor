@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

private val themeDetector = SystemThemeDetector()

/**
 * Entry point of the application.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PDF Extractor") {
        val systemTheme by themeDetector.current.collectAsState()

        SideEffect {
            // https://www.formdev.com/flatlaf/macos/
            window.rootPane.putClientProperty("apple.awt.application.appearance", "system")
            window.rootPane.putClientProperty("apple.awt.fullscreenable", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        }

        PDFExtractorApp(systemTheme)
    }
}

/**
 * High-level composable that decides on color scheme based on system theme
 * and sets up our Material 3 theme and layout.
 */
@Composable
fun WindowScope.PDFExtractorApp(systemTheme: SystemTheme) {
    PDFExtractorTheme(systemTheme = systemTheme) {
        // Scaffold provides a standard Material 3 layout with a top bar.
        Scaffold(
            topBar = {
                WindowDraggableArea {
                    CenterAlignedTopAppBar(
                        title = { Text("PDF Extractor") },
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            // Main box that also adds a background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                PDFDropperScreen()
            }
        }
    }
}

/**
 * Custom theme that decides dark/light color schemes based on the system theme.
 */
@Composable
fun PDFExtractorTheme(systemTheme: SystemTheme, content: @Composable () -> Unit) {
    // Feel free to customize these color schemes
    val darkColors = darkColorScheme(
    )
    val lightColors = lightColorScheme(
    )

    val colorScheme = if (systemTheme == SystemTheme.Dark) darkColors else lightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * Top-level screen composable that sets up drag-and-drop handling
 * and renders the PDF dropper UI.
 */
@Composable
fun PDFDropperScreen() {
    val pdfDropperState = rememberPDFDropperState()
    PDFDropperContent(pdfDropperState)
}

/**
 * Holds the state needed by the PDF dropper screen.
 */
class PDFDropperState {
    var pdfFile: File? by mutableStateOf(null)
    var totalPages: Int by mutableStateOf(0)

    // Page selectors
    var startPage: Int by mutableStateOf(1)
    var endPage: Int by mutableStateOf(1)

    // User feedback
    var statusMessage: String by mutableStateOf("")

    /**
     * Handles a file drop event.
     */
    fun onFileDropped(file: File) {
        pdfFile = file
        statusMessage = ""

        // Open the PDF just to get the total number of pages.
        PDDocument.load(file).use { doc ->
            totalPages = doc.numberOfPages
        }

        // Reset default page range to the entire document.
        startPage = 1
        endPage = totalPages
    }

    /**
     * Extracts and copies the text from the selected page range.
     */
    fun extractAndCopy(clipboardSetter: (String) -> Unit) {
        val file = pdfFile ?: return

        // Validate page range
        val actualStart = startPage.coerceIn(1, totalPages)
        val actualEnd = endPage.coerceIn(1, totalPages)

        if (actualStart > actualEnd) {
            statusMessage = "Invalid page range."
            return
        }

        // Extract text from the PDF in the selected range.
        val extractedText = extractTextFromPDF(file, actualStart, actualEnd)

        // Copy the result to clipboard
        clipboardSetter(extractedText)

        statusMessage = "Copied pages $actualStart to $actualEnd!"
    }
}

/**
 * Creates and remembers a [PDFDropperState].
 */
@Composable
fun rememberPDFDropperState(): PDFDropperState {
    return remember { PDFDropperState() }
}

/**
 * Main content for dropping a PDF file, selecting a page range,
 * and copying the text to the clipboard.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PDFDropperContent(state: PDFDropperState) {
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        return when (event.action) {
                            DragAndDropTransferAction.Link,
                            DragAndDropTransferAction.Move,
                                -> {
                                val transferable: Transferable = event.awtTransferable
                                val fileList =
                                    transferable.getTransferData(DataFlavor.javaFileListFlavor)
                                            as? List<*>
                                val file = fileList?.firstOrNull() as? File ?: return false

                                state.onFileDropped(file)
                                true
                            }

                            else -> false
                        }
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PDF File: ${state.pdfFile?.name ?: "<Drop a PDF here>"}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.pdfFile != null && state.totalPages > 0) {
                Text(
                    text = "Total Pages: ${state.totalPages}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Page range inputs
                Row {
                    OutlinedTextField(
                        value = state.startPage.toString(),
                        onValueChange = {
                            state.startPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("Start Page") },
                        modifier = Modifier
                            .width(120.dp)
                            .padding(end = 16.dp)
                    )

                    OutlinedTextField(
                        value = state.endPage.toString(),
                        onValueChange = {
                            state.endPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("End Page") },
                        modifier = Modifier.width(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        state.extractAndCopy { text ->
                            clipboard.setText(AnnotatedString(text))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Extract & Copy Pages")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Drag and drop a PDF file anywhere in this window.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Extracts text from the given PDF file using Apache PDFBox for the specified page range.
 */
fun extractTextFromPDF(file: File, startPage: Int, endPage: Int): String {
    return PDDocument.load(file).use { document ->
        val stripper = PDFTextStripper().apply {
            this.startPage = startPage
            this.endPage = endPage
        }
        stripper.getText(document)
    }
}
