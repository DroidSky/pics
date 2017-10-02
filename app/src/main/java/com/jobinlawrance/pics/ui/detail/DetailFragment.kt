package com.jobinlawrance.pics.ui.detail

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.jobinlawrance.pics.R
import com.jobinlawrance.pics.application.GlideApp
import com.jobinlawrance.pics.data.retrofit.model.PhotoResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.android.synthetic.main.fragment_detail.*

/**
 * A simple [Fragment] subclass.
 * Use the [DetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DetailFragment : MviFragment<DetailContract.View, DetailContract.Presenter>(), DetailContract.View {

    lateinit var photoResponse: PhotoResponse

    /**
     * Using [ReplaySubject] here is important since we are emitting the [photoResponse] in [onCreate]
     * The presenter is not yet created at this point, so it isn't subscribed to [loadDetailsSubject],
     * hence it'll miss out on [photoResponse] if we use [PublishSubject]
     */
    val loadDetailsSubject = ReplaySubject.create<PhotoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            photoResponse = arguments.getParcelable(ARG_PARAM1)
            loadDetailsSubject.onNext(photoResponse)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_detail, container, false)
    }

    //TODO - use dagger
    override fun createPresenter(): DetailContract.Presenter = DetailPresenterImpl()

    override fun render(viewState: PhotoResponse) {
        GlideApp.with(image_view.context)
                .load(viewState.urls?.regular)
                .into(image_view)

        GlideApp.with(user_avatar.context)
                .load(viewState.user?.profileImage?.medium)
                .circleCrop()
                .placeholder(R.drawable.avatar_placeholder)
                .transition(withCrossFade())
                .into(user_avatar)

        user_name.text = viewState.user?.name
    }

    override fun loadDetailsIntent(): Observable<PhotoResponse> = loadDetailsSubject

    companion object {
        private val ARG_PARAM1 = "param1"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param photoResponse Parameter.
         * @return A new instance of fragment DetailFragment.
         */

        fun newInstance(photoResponse: PhotoResponse): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, photoResponse)
            fragment.arguments = args
            return fragment
        }
    }
}