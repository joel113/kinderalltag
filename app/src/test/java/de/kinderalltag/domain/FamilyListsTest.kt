package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FamilyListsTest {
    private val day = LocalDate.of(2026, 11, 18)
    private val shopping = FamilyListItem("milk", FamilyListKind.SHOPPING, "2 × Milch")
    private val gift = FamilyListItem("gift", FamilyListKind.GIFTS, "Bauklötze", recipient = AppointmentRecipient.BOTH_CHILDREN)
    private fun event(id: String, item: FamilyListItem = shopping, action: ListAction = ListAction.BOUGHT,
        target: String? = null, author: String = "a", hour: Int = 10, labels: List<FamilyLabel> = emptyList()) =
        ListActivity(id, item.id, action, item, labels, author, author, day.atTime(hour, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli(), targetId = target)

    @Test fun shoppingAndGiftValidationRejectsDatesByModelAndInvalidRecipients() {
        shopping.validate(); gift.validate()
        assertThrows(IllegalArgumentException::class.java) { shopping.copy(recipient = AppointmentRecipient.JOHANN).validate() }
        assertThrows(IllegalArgumentException::class.java) { gift.copy(recipient = null).validate() }
        assertThrows(IllegalArgumentException::class.java) { gift.copy(recipient = AppointmentRecipient.HOUSEHOLD).validate() }
        assertThrows(IllegalArgumentException::class.java) { shopping.copy(title = " ").validate() }
        assertThrows(IllegalArgumentException::class.java) { shopping.copy(notes = "x".repeat(2001)).validate() }
        assertThrows(IllegalArgumentException::class.java) { shopping.copy(labelIds = (1..7).map { "$it" }.toSet()).validate() }
    }
    @Test fun simultaneousPurchasesRemainUntilTheirOwnRetractions() {
        val a = event("a")
        val b = event("b", author = "b")
        val undoA = event("undo-a", action = ListAction.REOPENED, target = a.id)
        assertEquals(listOf("b"), activePurchases(listOf(a, b, undoA), shopping.id).map { it.id })
        val undoB = event("undo-b", action = ListAction.REOPENED, target = b.id)
        assertTrue(activePurchases(listOf(a, b, undoA, undoB), shopping.id).isEmpty())
    }
    @Test fun retractionCannotAffectAnotherItemAndUnseenPurchasesRemainActive() {
        val known = event("known")
        val late = event("late", author = "b")
        val correction = event("correction", action = ListAction.REOPENED, target = known.id)
        val unrelated = event("other", gift, ListAction.REOPENED, target = late.id)
        assertEquals(listOf(late), activePurchases(listOf(known, correction, late, unrelated), shopping.id))
    }
    @Test fun deletedItemsStayHiddenDespiteLateOfflinePurchasesAndRestoreTheirStatus() {
        val events = listOf(event("offline", hour = 11).copy(pending = true))
        assertTrue(listItems(listOf(shopping.copy(deleted = true)), events, FamilyListKind.SHOPPING, true).isEmpty())
        assertEquals(listOf(shopping), listItems(listOf(shopping), events, FamilyListKind.SHOPPING, true))
        assertTrue(listItems(listOf(shopping), events, FamilyListKind.SHOPPING, false).isEmpty())
    }
    @Test fun openItemsSortAlphabeticallyAndBoughtItemsByConfirmationTime() {
        val apple = shopping.copy(id = "apple", title = "Äpfel")
        val bread = shopping.copy(id = "bread", title = "Brot")
        assertEquals(listOf(shopping, apple, bread), listItems(listOf(apple, shopping, bread), emptyList(), FamilyListKind.SHOPPING, false))
        val events = listOf(event("old", shopping), event("new", bread, hour = 11))
        assertEquals(listOf(bread, shopping), listItems(listOf(shopping, bread), events, FamilyListKind.SHOPPING, true))
    }
    @Test fun labelFiltersUseOrWithinGroupsAndAndBetweenGroupsIncludingArchivedReferences() {
        val xmas = FamilyLabel("xmas", LabelGroup.OCCASION, "Weihnachten")
        val easter = FamilyLabel("easter", LabelGroup.OCCASION, "Ostern")
        val grandpa = FamilyLabel("grandpa", LabelGroup.GIVER, "Opa", active = false)
        val labels = listOf(xmas, easter, grandpa)
        val item = gift.copy(labelIds = setOf(xmas.id, grandpa.id))
        assertTrue(matchesListLabels(item, setOf(xmas.id, easter.id, grandpa.id), labels))
        assertFalse(matchesListLabels(item, setOf(easter.id, grandpa.id), labels))
        assertTrue(matchesListLabels(item, emptySet(), labels))
        assertFalse(matchesListLabels(gift, setOf(xmas.id), labels))
    }
    @Test fun presetsHaveStableDistinctIdsAndSeparateGroups() {
        val all = LabelGroup.entries.flatMap(::starterLabels)
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertEquals(starterLabels(LabelGroup.GIVER), starterLabels(LabelGroup.GIVER))
        assertEquals(setOf(LabelGroup.GIVER), starterLabels(LabelGroup.GIVER).map { it.group }.toSet())
    }
    @Test fun historyUsesActionTimeAndSnapshotsEvenAfterCurrentItemOrLabelChanges() {
        val old = FamilyLabel("label", LabelGroup.SHOPPING, "Supermarkt")
        val snapshot = shopping.copy(labelIds = setOf(old.id))
        val added = event("add", snapshot, ListAction.ADDED, hour = 9, labels = listOf(old))
        val events = listOf(added, event("buy", snapshot, labels = listOf(old)), event("delete", snapshot.copy(deleted = true), ListAction.DELETED))
        val entries = history(events)
        assertEquals(3, entries.size)
        assertTrue(entries.all { it.subjectDate == null && it.title == "2 × Milch" })
        assertEquals(listOf("Supermarkt"), entries.first { it.id == "list-event:add" }.labels)
        assertEquals(setOf(HistoryKind.SHOPPING_ADDED, HistoryKind.SHOPPING_BOUGHT, HistoryKind.SHOPPING_DELETED), entries.map { it.kind }.toSet())
    }
    private fun history(events: List<ListActivity>, period: HistoryPeriod = HistoryPeriod(day, day)) =
        activityHistory(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), listOf(Parent("a", "Mama"), Parent("b", "Papa")), period, listActivity = events)
    @Test fun statisticsDeduplicatePurchaseCreditAndCountBothChildGiftsOnce() {
        val events = listOf(event("a"), event("b", author = "b"), event("gift-a", gift), event("gift-b", gift, author = "b"))
        val stats = eventStatistics(history(events), emptyList(), listOf(Parent("a", "Mama"), Parent("b", "Papa")), emptyList(), events)
        assertEquals(4, stats.total.events)
        assertEquals(1, stats.total.boughtShopping)
        assertEquals(1, stats.total.boughtGifts)
        assertEquals(1, stats.subjects.single { it.id == "both" }.counts.boughtGifts)
        assertEquals(1, stats.subjects.single { it.id == "shopping" }.counts.boughtShopping)
        assertEquals(0, stats.parents.single { it.id == "b" }.counts.boughtShopping)
        assertEquals(stats.total.events, stats.subjects.sumOf { it.counts.events })
        assertEquals(stats.total.events, stats.weekdays.sumOf { it.counts.events })
    }
    @Test fun reopeningOutsideThePeriodCorrectsEarlierNetPurchaseStatistics() {
        val purchase = event("bought")
        val undo = event("undo", action = ListAction.REOPENED, target = purchase.id).copy(clientMillis = purchase.clientMillis + 2 * 86400000L)
        val events = listOf(purchase, undo)
        val stats = eventStatistics(history(events), emptyList(), emptyList(), emptyList(), events)
        assertEquals(1, stats.total.events)
        assertEquals(0, stats.total.boughtShopping)
    }
    @Test fun dictationPreservesExistingTextAndCancellationDoesNotChangeIt() {
        assertEquals("Milch", appendDictation("", " Milch "))
        assertEquals("2 Liter Milch", appendDictation("2 Liter", "Milch"))
        assertEquals("Brot\nMilch", appendDictation("Brot", "Milch", true))
        assertEquals("Original ", appendDictation("Original ", " "))
    }
}
