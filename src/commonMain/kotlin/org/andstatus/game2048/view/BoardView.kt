package org.andstatus.game2048.view

import com.soywiz.korev.Key
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centerXBetween
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.position
import com.soywiz.korge.view.positionY
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.vector.roundRect
import org.andstatus.game2048.defaultTextSize

class BoardView(val gameView: GameView): Container() {
    private val controlsArea: SolidRect

    init {
        with(gameView) {
            position(boardLeft, boardTop)

            roundRect(boardWidth, boardWidth, buttonRadius, fill = gameColors.buttonBackground)
            graphics {
                fill(gameColors.cellBackground) {
                    for (x in 0 until settings.boardWidth) {
                        for (y in 0 until settings.boardHeight) {
                            roundRect(
                                cellMargin + (cellMargin + cellSize) * x, cellMargin + (cellMargin + cellSize) * y,
                                cellSize, cellSize, buttonRadius
                            )
                        }
                    }
                }
            }

            controlsArea = solidRect(boardWidth, boardWidth + buttonSize + buttonPadding,
                gameColors.transparent)

            controlsArea.onSwipe(20.0) {
                duplicateKeyPressFilter.onSwipeOrOver {
                    presenter.onSwipe(it.direction)
                }
            }

            controlsArea.addUpdater {
                val ifKey = { key: Key, action: () -> Unit ->
                    if (gameStage.views.input.keys[key]) {
                        duplicateKeyPressFilter.onPress(key, action)
                    }
                }
                ifKey(Key.LEFT) { presenter.onSwipe(SwipeDirection.LEFT) }
                ifKey(Key.RIGHT) { presenter.onSwipe(SwipeDirection.RIGHT) }
                ifKey(Key.UP) { presenter.onSwipe(SwipeDirection.TOP) }
                ifKey(Key.DOWN) { presenter.onSwipe(SwipeDirection.BOTTOM) }
                ifKey(Key.SPACE) { presenter.onPauseClick() }
                ifKey(Key.M) { presenter.onGameMenuClick() }
                ifKey(Key.BACKSPACE) {
                    presenter.onCloseGameWindowClick()
                }
            }
        }
    }

    /** Ensure the view is on the top to receive onSwipe events */
    fun setOnTop() {
        controlsArea.addTo(this)
        this.addTo(gameView.gameStage)
    }

    fun showGameOver(): Container = container {
        val window = this
        with(gameView) {
            val gameColors = gameColors
            val format = TextFormat(gameColors.labelText, defaultTextSize.toInt(), font)
            val skin = TextSkin(
                normal = format,
                over = format.copy(gameColors.labelTextOver),
                down = format.copy(gameColors.labelTextDown)
            )

            graphics {
                fill(gameColors.gameOverBackground) {
                    roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
                }
            }
            text(stringResources.text("game_over"),
                defaultTextSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                position(boardWidth / 2, (boardWidth - textSize) / 2)
            }
            uiText(stringResources.text("try_again"), 120.0, 35.0, skin) {
                centerXBetween(0.0, boardWidth)
                positionY((boardWidth + textSize) / 2)
                window.customOnClick {
                    window.removeFromParent()
                    presenter.restart()
                }
            }

            addUpdater {
                duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                    window.removeFromParent()
                    presenter.restart()
                }
            }
        }
    }

}