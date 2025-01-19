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

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PDF Extractor") {
        MaterialTheme {
            PDFDropperScreen()
        }
    }
}

@Composable
fun PDFDropperScreen() {
    // The dropped PDF file
    var lastFile by remember { mutableStateOf<File?>(null) }

    // Total pages in the PDF
    var totalPages by remember { mutableStateOf(0) }

    // User-selected page range
    var startPage by remember { mutableStateOf(1) }
    var endPage by remember { mutableStateOf(1) }

    // We only show the "Copied!" message after a successful extraction
    var copyStatus by remember { mutableStateOf("") }

    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Our drag-and-drop target
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true }, // accept any file drop, you can refine this
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        when (event.action) {
                            DragAndDropTransferAction.Link,
                            DragAndDropTransferAction.Move,
                                -> {
                                val awtTransferable: Transferable = event.awtTransferable

                                // Get the list of dropped files
                                val fileList =
                                    awtTransferable.getTransferData(DataFlavor.javaFileListFlavor)
                                            as? List<File>

                                val file = fileList?.firstOrNull() ?: return false

                                // Update state with new PDF file
                                lastFile = file
                                copyStatus = ""

                                // Open PDF just to get total pages
                                PDDocument.load(file).use { doc ->
                                    totalPages = doc.numberOfPages
                                }

                                // Reset default page range to full doc
                                startPage = 1
                                endPage = totalPages

                                return true
                            }

                            else -> {}
                        }
                        return false
                    }
                }
            )
    ) {
        // Main UI for showing file info, page range, etc.
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("PDF File: ${lastFile?.name ?: "<Drop a PDF here>"}")
            Spacer(modifier = Modifier.height(8.dp))

            if (lastFile != null && totalPages > 0) {
                Text("Total Pages: $totalPages")
                Spacer(modifier = Modifier.height(8.dp))

                // Let the user pick a start and end page
                Row {
                    OutlinedTextField(
                        value = startPage.toString(),
                        onValueChange = {
                            startPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("Start Page") },
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = endPage.toString(),
                        onValueChange = {
                            endPage = it.toIntOrNull() ?: 1
                        },
                        label = { Text("End Page") },
                        modifier = Modifier.width(120.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Button to extract/copy the selected page range
                Button(
                    onClick = {
                        // Validate page range
                        val actualStart = startPage.coerceAtLeast(1).coerceAtMost(totalPages)
                        val actualEnd = endPage.coerceAtLeast(1).coerceAtMost(totalPages)
                        if (actualStart > actualEnd) {
                            copyStatus = "Invalid page range."
                            return@Button
                        }

                        // Extract text from the selected pages
                        val file = lastFile!!
                        val extractedText = extractTextFromPDF(file, actualStart, actualEnd)

                        // Copy to clipboard
                        clipboard.setText(AnnotatedString(extractedText))

                        copyStatus = "Copied pages $actualStart to $actualEnd!"
                    }
                ) {
                    Text("Extract & Copy Pages")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(copyStatus)
            }
        }
    }
}

/**
 * Extracts text from the given PDF file using Apache PDFBox, for the given page range.
 */
fun extractTextFromPDF(file: File, start: Int, end: Int): String {
    return PDDocument.load(file).use { document ->
        val stripper = PDFTextStripper().apply {
            startPage = start
            endPage = end
        }
        stripper.getText(document)
    }
}
