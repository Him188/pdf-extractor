@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

/**
 * Entry point of the application.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PDF Extractor") {
        MaterialTheme {
            PDFDropperScreen()
        }
    }
}

/**
 * High-level composable that sets up and displays our PDF-dropper UI.
 */
@Composable
fun PDFDropperScreen() {
    val pdfDropperState = rememberPDFDropperState()
    PDFDropperUI(pdfDropperState)
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

        // Validate page range.
        val actualStart = startPage.coerceIn(1, totalPages)
        val actualEnd = endPage.coerceIn(1, totalPages)

        if (actualStart > actualEnd) {
            statusMessage = "Invalid page range."
            return
        }

        // Extract text from the PDF in the selected range.
        val extractedText = extractTextFromPDF(file, actualStart, actualEnd)

        // Copy the result to clipboard.
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
 * The main UI for dropping a PDF file, selecting the page range,
 * and copying the text to the clipboard.
 */
@Composable
fun PDFDropperUI(state: PDFDropperState) {
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
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("PDF File: ${state.pdfFile?.name ?: "<Drop a PDF here>"}")
            Spacer(modifier = Modifier.height(8.dp))

            if (state.pdfFile != null && state.totalPages > 0) {
                Text("Total Pages: ${state.totalPages}")
                Spacer(modifier = Modifier.height(8.dp))

                // Page range inputs
                Row {
                    OutlinedTextField(
                        value = state.startPage.toString(),
                        onValueChange = {
                            state.startPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("Start Page") },
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = state.endPage.toString(),
                        onValueChange = {
                            state.endPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("End Page") },
                        modifier = Modifier.width(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        state.extractAndCopy { text ->
                            // Provide a way to set the clipboard text
                            clipboard.setText(AnnotatedString(text))
                        }
                    }
                ) {
                    Text("Extract & Copy Pages")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(state.statusMessage)
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
