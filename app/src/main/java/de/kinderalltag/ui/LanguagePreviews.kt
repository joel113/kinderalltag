package de.kinderalltag.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.kinderalltag.text.QuantityCatalog
import de.kinderalltag.text.TextCatalog

/** Per-thread scopes keep simultaneous German and English previews independent. */
@Composable
internal fun PreviewKinderTheme(theme: AppThemeId, content: @Composable () -> Unit) {
    val context = LocalContext.current
    TextCatalog.withRenderer({ key, args ->
        if (args.isEmpty()) context.getString(key.resourceId)
        else context.getString(key.resourceId, *args)
    }) {
        QuantityCatalog.withRenderer({ key, count, args ->
            context.resources.getQuantityString(key.resourceId, count, *args)
        }) {
            KinderTheme(theme, content)
        }
    }
}
