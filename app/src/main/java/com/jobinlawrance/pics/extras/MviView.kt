package com.jobinlawrance.pics.extras

import com.hannesdorfmann.mosby3.mvp.MvpView

/**
 * Created by jobinlawrance on 7/9/17.
 */
interface MviView<VS> : MvpView {
    fun render(viewState: VS)
}