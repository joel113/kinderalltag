package de.kinderalltag.text

import java.util.Locale
import kotlin.contracts.*

/** Resource-backed presentation, with German defaults for platform-independent callers/tests. */
data class AppMessage(val key: TextKey, val arguments: List<Any?> = emptyList()) {
    fun render(): String = TextCatalog.render(key, arguments.toTypedArray())
}

object TextCatalog {
    @PublishedApi internal val scopedRenderer = ThreadLocal<(TextKey, Array<out Any?>) -> String>()
    @Volatile var renderer: ((TextKey, Array<out Any?>) -> String)? = null

    inline fun <T> withRenderer(
        noinline renderer: (TextKey, Array<out Any?>) -> String,
        block: () -> T,
    ): T {
        val previous = scopedRenderer.get()
        scopedRenderer.set(renderer)
        return try {
            block()
        } finally {
            scopedRenderer.set(previous)
        }
    }

    fun render(key: TextKey, arguments: Array<out Any?>): String =
        (scopedRenderer.get() ?: renderer)?.invoke(key, arguments)
            ?: if (arguments.isEmpty()) key.german
            else String.format(Locale.GERMAN, key.german, *arguments)
}

fun text(key: TextKey, vararg arguments: Any?): String = TextCatalog.render(key, arguments)

fun message(key: TextKey, vararg arguments: Any?) = AppMessage(key, arguments.toList())

class LocalizedArgumentException(val displayMessage: AppMessage) : IllegalArgumentException() {
    override val message: String
        get() = displayMessage.render()
}

class LocalizedStateException(val displayMessage: AppMessage) : IllegalStateException() {
    override val message: String
        get() = displayMessage.render()
}

@OptIn(ExperimentalContracts::class)
inline fun requireLocalized(value: Boolean, lazyMessage: () -> AppMessage) {
    contract { returns() implies value }
    if (!value) throw LocalizedArgumentException(lazyMessage())
}

@OptIn(ExperimentalContracts::class)
inline fun checkLocalized(value: Boolean, lazyMessage: () -> AppMessage) {
    contract { returns() implies value }
    if (!value) throw LocalizedStateException(lazyMessage())
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> requireNotNullLocalized(value: T?, lazyMessage: () -> AppMessage): T {
    contract { returns() implies (value != null) }
    return value ?: throw LocalizedArgumentException(lazyMessage())
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> checkNotNullLocalized(value: T?, lazyMessage: () -> AppMessage): T {
    contract { returns() implies (value != null) }
    return value ?: throw LocalizedStateException(lazyMessage())
}
