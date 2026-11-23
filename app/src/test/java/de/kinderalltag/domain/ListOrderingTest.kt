package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test

class ListOrderingTest {
    private val items =
        listOf(
            FamilyListItem("milk", FamilyListKind.SHOPPING, "Milch"),
            FamilyListItem("apple", FamilyListKind.SHOPPING, "Äpfel"),
            FamilyListItem("bread", FamilyListKind.SHOPPING, "Brot"),
            FamilyListItem(
                "gift",
                FamilyListKind.GIFTS,
                "Bauklötze",
                recipient = AppointmentRecipient.JOHANN,
            ),
        )

    @Test
    fun existingListsStayAlphabeticalUntilSortedAndNewItemsAppendWithoutMixingKinds() {
        assertEquals(
            listOf("apple", "bread", "milk"),
            orderedListIds(items, FamilyListKind.SHOPPING, emptyList()),
        )
        val order =
            orderedListIds(
                items,
                FamilyListKind.SHOPPING,
                listOf("milk", "apple", "missing", "gift", "milk"),
            )
        assertEquals(listOf("milk", "apple", "bread"), order)
        assertEquals(
            order,
            listItems(items, emptyList(), FamilyListKind.SHOPPING, false, order).map { it.id },
        )
        assertEquals(listOf("gift"), orderedListIds(items, FamilyListKind.GIFTS, order))
    }

    @Test
    fun movingAcrossFilteredItemsPreservesHiddenItemsAndDoesNotDuplicateOrLoseIds() {
        val order = listOf("a", "hidden", "b", "c")
        assertEquals(listOf("hidden", "b", "c", "a"), moveListItem(order, "a", "c", true))
        assertEquals(listOf("c", "a", "hidden", "b"), moveListItem(order, "c", "a", false))
        assertEquals(order, moveListItem(order, "a", "missing", true))
        assertEquals(order, moveListItem(order, "a", "a", false))
        val once = moveListItem(order, "a", "b", true)
        assertEquals(order.toSet(), once.toSet())
        assertEquals(order.size, once.size)
        assertEquals(order, moveListItem(once, "a", "hidden", false))
    }

    @Test
    fun buyingReopeningDeletingAndRestoringKeepTheManualPositionAndAuditSnapshot() {
        val order = listOf("milk", "apple", "bread")
        val milk = items.first()
        val purchase =
            ListActivity(
                "bought",
                milk.id,
                ListAction.BOUGHT,
                milk,
                emptyList(),
                "parent",
                "Papa",
                1,
            )
        assertEquals(
            listOf("apple", "bread"),
            listItems(items, listOf(purchase), FamilyListKind.SHOPPING, false, order).map { it.id },
        )
        val reopen =
            purchase.copy(id = "reopen", action = ListAction.REOPENED, targetId = purchase.id)
        assertEquals(
            order,
            listItems(items, listOf(purchase, reopen), FamilyListKind.SHOPPING, false, order).map {
                it.id
            },
        )
        val deleted = items.map { if (it.id == milk.id) it.copy(deleted = true) else it }
        assertEquals(order, orderedListIds(deleted, FamilyListKind.SHOPPING, order))
        assertEquals(
            listOf("apple", "bread"),
            listItems(deleted, emptyList(), FamilyListKind.SHOPPING, false, order).map { it.id },
        )
        assertEquals(milk, purchase.snapshot)
        assertEquals(
            order,
            listItems(items, emptyList(), FamilyListKind.SHOPPING, false, order).map { it.id },
        )
    }

    @Test
    fun orderValidationRejectsDuplicatesMalformedIdsAndOversizedDocuments() {
        FamilyListOrder(FamilyListKind.SHOPPING).validate()
        FamilyListOrder(FamilyListKind.GIFTS, (1..1000).map { "id-$it" }, 1).validate()
        listOf(
                listOf("same", "same"),
                listOf(""),
                listOf("a,b"),
                listOf("a/b"),
                listOf("x".repeat(101)),
                (1..1001).map { "id-$it" },
            )
            .forEach { ids ->
                assertThrows(IllegalArgumentException::class.java) {
                    FamilyListOrder(FamilyListKind.SHOPPING, ids).validate()
                }
            }
        assertThrows(IllegalArgumentException::class.java) {
            FamilyListOrder(FamilyListKind.SHOPPING, version = -1).validate()
        }
    }
}
