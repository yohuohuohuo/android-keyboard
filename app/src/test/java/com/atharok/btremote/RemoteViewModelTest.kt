package com.atharok.btremote

import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUSVirtualKeyboardLayout
import com.atharok.btremote.presentation.viewmodel.TextSendStatus
import com.atharok.btremote.ui.views.keyboard.KeyboardReportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val layout = EnglishUSVirtualKeyboardLayout()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun busySubmissionIsRejectedAndDraftClearsOnlyAfterReportsFinish() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("ab")
        assertTrue(vm.sendDraft(layout, true))
        assertFalse(vm.sendDraft(layout, true))
        assertEquals("ab", vm.draft.value)
        runCurrent()
        assertEquals(TextSendStatus.SENDING, vm.textStatus.value)
        advanceUntilIdle()
        assertEquals(TextSendStatus.SUBMITTED, vm.textStatus.value)
        assertEquals("", vm.draft.value)
        assertEquals(listOf(listOf<Byte>(0,4), listOf<Byte>(0,0), listOf<Byte>(0,5), listOf<Byte>(0,0)), f.reports.map { it.second })
    }

    @Test fun editingDuringSubmissionDoesNotClearNewDraft() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("a"); vm.sendDraft(layout, true); runCurrent()
        vm.updateDraft("new draft")
        advanceUntilIdle()
        assertEquals("new draft", vm.draft.value)
    }

    @Test fun unsupportedCharactersStayInDraftAndGenerateNoReports() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("a你好")
        assertFalse(vm.sendDraft(layout, true))
        assertEquals(TextSendStatus.UNSUPPORTED, vm.textStatus.value)
        assertEquals("a你好", vm.draft.value)
        assertTrue(f.reports.isEmpty())
    }

    @Test fun rejectedReportKeepsOriginalForManualCheck() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel; f.reject = true
        vm.updateDraft("ab"); vm.sendDraft(layout, true); advanceUntilIdle()
        assertEquals(TextSendStatus.FAILED, vm.textStatus.value)
        assertEquals("ab", vm.draft.value)
        assertFalse(f.reports.any { it.second == listOf<Byte>(0,5) })
    }

    @Test fun cancellationReleasesThreeReportFamiliesWithoutLateReleaseOrReplay() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("ab"); vm.sendDraft(layout, true); runCurrent()
        vm.releaseAllInputs()
        assertEquals(listOf(1 to listOf<Byte>(0,0), 2 to listOf<Byte>(0,0,0,0), 3 to listOf<Byte>(0,0)), f.reports.takeLast(3))
        vm.sendKeyboardReport(byteArrayOf(8,6))
        val count = f.reports.size
        advanceUntilIdle()
        assertEquals(count, f.reports.size)
        assertEquals(TextSendStatus.CANCELLED, vm.textStatus.value)
        assertEquals("ab", vm.draft.value)
    }

    @Test fun backgroundStopsInputsAndResumeNeverReplaysThem() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("ab"); vm.sendDraft(layout, true); runCurrent()
        vm.setInputsActive(false)
        val count = f.reports.size
        vm.sendKeyboardReport(byteArrayOf(0,4)); assertFalse(vm.sendDraft(layout, true))
        advanceUntilIdle(); assertEquals(count, f.reports.size)
        vm.setInputsActive(true); advanceUntilIdle(); assertEquals(count, f.reports.size)
    }

    @Test fun modifiersUseBitOperationsAndF7RemainsAnOrdinaryKey() {
        val reports = mutableListOf<List<Byte>>()
        val keys = KeyboardReportState { reports += it.toList() }
        keys.press(8, true); keys.press(8, true); keys.press(4, true); keys.press(0x40, false)
        assertEquals(listOf<Byte>(12,0x40), reports.last())
        keys.release(8, true)
        assertEquals(listOf<Byte>(4,0x40), reports.last())
        keys.releaseAll(); assertEquals(listOf<Byte>(0,0), reports.last())
    }

    @Test fun rightMetaSignBitAndShiftCanCombineAndClearIndependently() {
        val reports = mutableListOf<List<Byte>>()
        val keys = KeyboardReportState { reports += it.toList() }
        keys.press(0x80.toByte(), true); keys.press(2, true); keys.press(4, false)
        assertEquals(listOf<Byte>(0x82.toByte(),4), reports.last())
        keys.release(2, true); assertEquals(listOf<Byte>(0x80.toByte(),4), reports.last())
        keys.release(4, false); keys.release(0x80.toByte(), true)
        assertEquals(listOf<Byte>(0,0), reports.last())
    }
    @Test fun editingAfterSubmissionResetsTheOldSuccessMessage() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("a"); vm.sendDraft(layout, true); advanceUntilIdle()
        vm.updateDraft("b")
        assertEquals(TextSendStatus.IDLE, vm.textStatus.value)
    }

    @Test fun interruptedSendRetainsOriginalAlongsideEditedDraft() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("ab"); vm.sendDraft(layout, true); runCurrent()
        vm.updateDraft("new draft"); vm.releaseAllInputs(); advanceUntilIdle()
        assertEquals("new draft", vm.draft.value)
        assertEquals("ab", vm.lastAttempt.value)
    }

    @Test fun failedSendRetainsOriginalAlongsideEditedDraft() = runTest {
        val f = RemoteFixture(); val vm = f.viewModel
        vm.updateDraft("ab"); vm.sendDraft(layout, true); runCurrent()
        vm.updateDraft("new draft"); f.reject = true; advanceUntilIdle()
        assertEquals(TextSendStatus.FAILED, vm.textStatus.value)
        assertEquals("new draft", vm.draft.value)
        assertEquals("ab", vm.lastAttempt.value)
    }

}
