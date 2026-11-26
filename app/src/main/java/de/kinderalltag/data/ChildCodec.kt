package de.kinderalltag.data

import de.kinderalltag.domain.*

/** Missing fields belong to the original profile schema; never invent replacement IDs. */
internal fun decodeChild(id: String, data: Map<String, Any?>): Child {
    require(data["name"] is String && (data["name"] as String).isNotBlank())
    require(!data.containsKey("archived") || data["archived"] is Boolean)
    require(
        !data.containsKey("version") ||
            data["version"] is Number && (data["version"] as Number).toLong() >= 0
    )
    return Child(
        id = id,
        name = data["name"] as String,
        bearColor = BearColor.fromStored(data["bearColor"] as? String, id),
        icon = (data["icon"] as? String)?.let(ChildIconId::valueOf) ?: ChildIconId.BEAR,
        archived = data["archived"] as? Boolean ?: false,
        version = (data["version"] as? Number)?.toLong() ?: 0,
    )
}
