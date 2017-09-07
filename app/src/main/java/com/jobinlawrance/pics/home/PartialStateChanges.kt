package com.jobinlawrance.pics.home

import com.jobinlawrance.pics.retrofit.data.PhotoResponse

/**
 * Created by jobinlawrance on 7/9/17.
 */
sealed class PartialStateChanges {
    object FirstPageLoading : PartialStateChanges()
    class FirstPageError(val throwable: Throwable) : PartialStateChanges()
    class FirstPageLoaded(val data: List<PhotoResponse>) : PartialStateChanges()
}