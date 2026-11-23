# Localization

Kinderalltag supports German and English. In **Settings → Appearance → Language**, choose System language, Deutsch, or English. The setting belongs to the phone, not the household. Android 13+ also exposes it in system app-language settings. Unsupported languages fall back to German; the first supported locale in the preferred list wins. English without a region uses UK presentation, German without a region uses Germany.

AppCompat persists the override on Android 8–12 and uses the platform language preferences on newer devices. MainActivity remains the explicit IDE target and uses an AppCompat light theme around the existing Compose themes. Language changes recreate the activity: saveable navigation/filters and editor state are retained. Temporary error notices are cleared; synchronization summaries are rebuilt from their original error codes without restarting the database listeners.

## Text ownership

App text lives in German `res/values/messages.xml` and English `res/values-en/messages.xml`. Counts use the corresponding `plurals.xml` files. TextKey and QuantityKey are platform-independent typed message identifiers with German defaults, allowing domain unit tests without Android. AndroidTextResources binds each key to a compile-time resource ID. AppStrings supplies locale-aware rendering, including background contexts; domain code imports no Android resources.

Validation uses LocalizedArgumentException/LocalizedStateException and AppMessage identifiers with arguments. Render through the resource adapter at the presentation boundary. Do not compare translated text to make scheduling, authorization, filtering or identity decisions.

To add a message:

1. Add a descriptive TextKey, its German default and both translated resource entries.
2. Add its exhaustive AndroidTextResources binding. Preserve indexed placeholders in both languages.
3. For counts, use QuantityKey and Android plurals with one/other forms.
4. Run formatting, unit tests and lint. Missing/extra translations and invalid/mismatched format arguments are errors. LocalizationTest checks resource/catalog parity and default text too.

Do not translate saved task titles, notes, places, personal names, legal contact details or existing labels. New starter labels use translated names with unchanged preset IDs. Shared manual order and German alphabetical fallback remain unchanged.

## Dates, speech and notifications

Date/month/weekday presentation follows the app locale; time fields retain the phone's 12/24-hour choice. Scheduling, Monday-first calendars, ISO values and Europe/Berlin remain unchanged. Speech uses the app language; shopping import understands German or English separators and always presents editable review text.

Notifications resolve text in the current app language. New local reminder records retain raw titles and recipient identifiers. Legacy records are reconstructed from available cached data, with a localized generic title if unavailable. Channel IDs, delivery records and reminder fingerprints stay stable; language changes do not grant notification permission or replay events.

No Firestore schema migration or rule deployment is required. Google sign-in and external calendar/speech apps can show their own language choices.
