import com.soywiz.korge.view.Stage
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.ViewData
import org.andstatus.game2048.view.viewData

// TODO: Make some separate class for this...
private val uninitializedLazy = lazy { throw IllegalStateException("Value is not initialized yet") }
private var lazyViewData: Lazy<ViewData> = uninitializedLazy
private var viewData: ViewData
    get() = lazyViewData.value
    set(value) {
        lazyViewData = lazyOf(value)
    }

fun unsetGameView() {
    if (lazyViewData.isInitialized()) lazyViewData = uninitializedLazy
}

suspend fun Stage.initializeViewDataInTest(handler: suspend ViewData.() -> Unit = {}) {
    if (lazyViewData.isInitialized()) {
        viewData.handler()
    } else {
        viewData(stage, animateViews = false) {
            myLog("Initialized in test")
            viewData = this
            viewData.handler()
        }
        myLog("initializeViewDataInTest after 'viewData' function ended")
    }
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

fun ViewData.currentGameString(): String = "CurrentGame " + presenter.model.history.currentGame.plies.toLongString()

fun ViewData.historyString(): String = with(presenter.model.history) {
    "History: index:$historyIndex, moves:${currentGame.plies.size}"
}
