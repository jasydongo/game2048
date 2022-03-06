import com.soywiz.klock.Stopwatch
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.lang.Thread_sleep
import kotlinx.coroutines.delay
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePlies
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.model.PlyAndPosition.Companion.allowedRandomPly
import org.andstatus.game2048.model.ShortRecord
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.ViewData
import org.andstatus.game2048.view.viewData
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


// TODO: Make some separate class for this...
private val isViewDataInitialized = korAtomic(false)
private val viewDataRef: KorAtomicRef<ViewData?> = korAtomic(null)
private val viewData: ViewData
    get() = if (isViewDataInitialized.value) viewDataRef.value
        ?: throw IllegalStateException("Value is not initialized yet")
    else throw IllegalStateException("Value is not initialized yet")

fun ViewsForTesting.myViewsTest(testObject: Any, handler: suspend ViewData.() -> Unit = {}) {
    val testWasExecuted = korAtomic(false)
    myLog("Test $testObject started")
    viewsTest {
        if (isViewDataInitialized.value) {
            viewData.handler()
            testWasExecuted.value = true
        } else {
            viewData(stage, animateViews = false) {
                myLog("Initialized in test")
                viewDataRef.value = this
                isViewDataInitialized.value = true
                sWaitFor("Main view shown 1") { viewData.presenter.mainViewShown.value }
                viewData.handler()
                testWasExecuted.value = true
            }
            myLog("initializeViewDataInTest after 'viewData' function ended")
        }
    }
    waitFor("Test $testObject was executed") { testWasExecuted.value }
}

fun ViewData.presentedPieces() = presenter.boardViews.blocksOnBoard.map { it.firstOrNull()?.piece }

fun ViewData.blocksAt(square: Square) = presenter.boardViews.getAll(square).map { it.piece }

fun ViewData.modelAndViews() =
    "Model:     " + presenter.model.gamePosition.pieces.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece?.text ?: "-")
    } +
            (if (presenter.model.history.currentGame.shortRecord.bookmarks.isNotEmpty())
                "  bookmarks: " + presenter.model.history.currentGame.shortRecord.bookmarks.size
            else "") +
            "\n" +
            "BoardViews:" + presenter.boardViews.blocksOnBoard.mapIndexed { ind, list ->
        ind.toString() + ":" + (if (list.isEmpty()) "-" else list.joinToString(transform = { it.piece.text }))
    }

fun ViewData.currentGameString(): String = "CurrentGame " + presenter.model.history.currentGame.toLongString()

fun ViewData.historyString(): String = with(presenter.model.history) {
    "History: index:$redoPlyPointer, moves:${currentGame.gamePlies.size}"
}

fun ViewData.waitForMainViewShown(action: () -> Any? = { null }) {
    presenter.mainViewShown.value = false
    action()
    waitFor("Main view shown") { presenter.mainViewShown.value }
}

fun waitFor(message: String = "???", condition: () -> Boolean) {
    val stopWatch = Stopwatch().start()
    while (stopWatch.elapsed.seconds < 300) {
        if (condition()) {
            myLog("Success after ${stopWatch.elapsed.seconds} sec waiting for: $message")
            return
        }
        Thread_sleep(50)
    }
    throw AssertionError("Condition wasn't met after timeout ${stopWatch.elapsed.seconds} sec: $message")
}

suspend fun sWaitFor(message: String = "???", condition: () -> Boolean) {
    val stopWatch = Stopwatch().start()
    while (stopWatch.elapsed.seconds < 300) {
        if (condition()) {
            myLog("Success waiting for: $message")
            return
        }
        delay(50)
    }
    throw AssertionError("Condition wasn't met after timeout: $message")
}

fun newGameRecord(
    settings: Settings, position: GamePosition, id: Int, bookmarks: List<GamePosition>,
    plies: List<Ply>
) = ShortRecord(settings, position.board, "", id, position.startingDateTime, position, bookmarks)
    .let {
        GameRecord(it, GamePlies.fromPlies(it, plies))
    }

fun ViewData.generateGame(expectedPliesCount: Int, bookmarkOnPly: Int? = null): GameRecord {
    waitForMainViewShown {
        presenter.onRestartClick()
    }

    var iteration = 0
    while (presenter.model.gamePosition.plyNumber < expectedPliesCount &&
        iteration < expectedPliesCount
    ) {
        bookmarkOnPly?.let { plyNumber ->
            if (plyNumber == presenter.model.gamePosition.plyNumber) {
                waitForMainViewShown {
                    presenter.onBookmarkClick()
                }
            }
        }
        allowedRandomPly(presenter.model.gamePosition).ply.plyEnum.swipeDirection?.let {
            waitForMainViewShown {
                presenter.onSwipe(it)
            }
        }
        iteration++
    }
    val message = "Failed to generate game with $expectedPliesCount plies. ${currentGameString()}"
    assertEquals(expectedPliesCount, presenter.model.gamePosition.plyNumber, message)
    assertEquals(expectedPliesCount, presenter.model.history.currentGame.gamePlies.size, message)
    assertNotNull(presenter.model.history.currentGame.gamePlies.get(expectedPliesCount), message)

    waitForMainViewShown {
        presenter.onPauseClick()
    }

    val game = presenter.model.history.currentGame
    waitFor("Recent games reloaded with gameId:${game.id}") {
        presenter.model.history.recentGames.any { it.id == game.id }
    }
    return game
}

fun Sequence<String>.toTextLines(): String = fold(StringBuilder()) { acc, str ->
    acc.append(str).append("\n")
}.toString()
