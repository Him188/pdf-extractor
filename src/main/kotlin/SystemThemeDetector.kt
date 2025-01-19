import androidx.compose.ui.SystemTheme
import com.jthemedetecor.OsThemeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemThemeDetector {
    private val detector = OsThemeDetector.getDetector()

    private val _current = MutableStateFlow(isDarkToTheme(detector.isDark))
    val current: StateFlow<SystemTheme> = _current.asStateFlow()

    init {
        detector.registerListener {
            _current.value = isDarkToTheme(it)
        }
    }

    private fun isDarkToTheme(isDark: Boolean): SystemTheme {
        return if (isDark) {
            SystemTheme.Dark
        } else {
            SystemTheme.Light
        }
    }
}