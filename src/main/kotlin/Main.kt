@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
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
    // We'll store extracted text from the PDF here
    var extractedText by remember { mutableStateOf("Drag and drop a PDF onto the window.") }

    // We put a "SwingPanel" so we can attach a DropTarget to it.
    // This panel can basically be invisible or you can style it as you like.
    var lastFileLink by remember { mutableStateOf<File?>(null) }
    val clipboard = LocalClipboardManager.current
    Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize().dragAndDropTarget(
            { true },
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    when (event.action) {
                        DragAndDropTransferAction.Link,
                        DragAndDropTransferAction.Move,
                            -> {
                            val awtTransferable: Transferable = event.awtTransferable
                            val file =
                                (awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>)
                                    .first()
                            lastFileLink = file
                            extractedText = extractTextFromPDF(file)
                            clipboard.setText(AnnotatedString(extractedText))
                            return true
                        }

                        else -> {}
                    }

                    return false
                }
            })
    ) {
        // Show extracted text in a simple scrolling area
        Column(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp)) {
            Text("File: ${lastFileLink?.name ?: "<Drop a file here>"}")

            if (lastFileLink != null) {
                Text(
                    "Copied!",
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

/**
 * Extracts text from the given PDF file using Apache PDFBox.
 */
fun extractTextFromPDF(file: File): String {
    return PDDocument.load(file).use { document ->
        PDFTextStripper().getText(document)
    }
}
