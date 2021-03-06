package com.jobinlawrance.pics.ui.home

import android.accounts.NetworkErrorException
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView
import com.jobinlawrance.pics.R
import com.jobinlawrance.pics.application.MyApplication
import com.jobinlawrance.pics.data.retrofit.model.PhotoResponse
import com.jobinlawrance.pics.ui.detail.DetailActivity
import com.jobinlawrance.pics.ui.home.dagger.HomeComponent
import com.jobinlawrance.pics.ui.home.dagger.HomeModule
import com.jobinlawrance.pics.utils.getActionBarSize
import com.jobinlawrance.pics.utils.inflate
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.timeout.*
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


/**
 * Created by jobinlawrance on 7/9/17.
 * ref - https://github.com/nickbutcher/plaid/blob/e6716fb4553ce74e99009904bbcaa02c5d5598b5/app/src/main/java/io/plaidapp/ui/HomeActivity.java
 */
class HomeFragment : MviFragment<HomeContract.View, HomeContract.Presenter>(), HomeContract.View {

    lateinit var homeAdapter: HomeAdapter
    lateinit var gridLayoutManager: GridLayoutManager

    val loadFirstPageSubject = PublishSubject.create<Boolean>()
    val networkStateSubject = PublishSubject.create<Boolean>()

    var monitoringNetwork = false

    var timeOutLayout: View? = null
    var networkErrorLayout: ImageView? = null

    private var networkReconnected: AtomicBoolean

    init {
        networkReconnected = AtomicBoolean(false)
    }

    @Inject
    lateinit var presenter: HomeContract.Presenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeAdapter = HomeAdapter(context, this::openDetails)
        gridLayoutManager = GridLayoutManager(activity, resources.getInteger(R.integer.grid_span_size))
        return container?.inflate(R.layout.fragment_home)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        windowInsetFrameLayout.setOnApplyWindowInsetsListener { _, windowInsets ->

            // inset the toolbar down by the status bar height
            val lpToolbar = toolbar
                    .layoutParams as ViewGroup.MarginLayoutParams
            lpToolbar.topMargin += windowInsets.systemWindowInsetTop
            lpToolbar.leftMargin += windowInsets.systemWindowInsetLeft
            lpToolbar.rightMargin += windowInsets.systemWindowInsetRight
            toolbar.layoutParams = lpToolbar

            // inset the recyclerView top by statusbar+toolbar & the bottom by the navbar (don't clip)
            recyclerView.setPadding(
                    recyclerView.paddingLeft + windowInsets.systemWindowInsetLeft, // landscape
                    windowInsets.systemWindowInsetTop + getActionBarSize(activity),
                    recyclerView.paddingRight + windowInsets.systemWindowInsetRight, // landscape
                    recyclerView.paddingBottom + windowInsets.systemWindowInsetBottom)

            // clear this listener so insets aren't re-applied
            windowInsetFrameLayout.setOnApplyWindowInsetsListener(null)

            windowInsets.consumeStableInsets()
        }

        recyclerView.adapter = homeAdapter
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.addOnScrollListener(toolbarElevation)
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
    }

    override fun onPause() {
        super.onPause()
        if (monitoringNetwork) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
            monitoringNetwork = false
        }
    }

    override fun createPresenter(): HomeContract.Presenter {
        (MyApplication.getInstance().getFragmentComponentBuilder(HomeFragment::class.java) as HomeComponent.Builder)
                .fragmentModule(HomeModule(this)).build().injectMembers(this)
        return presenter
    }

    override fun loadingFirstPageIntent(): Observable<Boolean> =
            loadFirstPageSubject
//                    .startWith(true)
                    .doAfterNext { Timber.d("First Page Loaded") }

    override fun loadNextPageIntent(): Observable<Boolean> {
        return RxRecyclerView.scrollStateChanges(recyclerView)
                .filter { !homeAdapter.isloadingNextPage }
//                .filter { it == RecyclerView.SCROLL_STATE_IDLE }
                .filter { gridLayoutManager.findLastCompletelyVisibleItemPosition() == homeAdapter.itemCount - 1 }
                .map { true }
    }

    override fun networkStateIntent(): Observable<Boolean> = networkStateSubject.doOnNext { Timber.d("Network state - $it") }

    override fun render(viewState: HomeViewState) {
        Timber.v("render : %s", viewState)

        val loadMoreChanged = homeAdapter.setNextPageLoading(viewState.loadingNextPage)
        Timber.v("loadMoreChanged - $loadMoreChanged")

        if (viewState.loadingFirstPage) {
            renderProgress()
        } else if (viewState.firstPageError != null) {
            renderFirstPageError(viewState.firstPageError)
        } else {
            renderData(viewState)
        }

        if (loadMoreChanged && homeAdapter.isloadingNextPage) {
            recyclerView.scrollToPosition(homeAdapter.itemCount - 1)
            Timber.d("Scroll to position = ${homeAdapter.itemCount}")
        }
    }

    override fun openDetails(photoResponse: PhotoResponse, sharedElementsPair: Pair<View, String>) {
        val intent = Intent(activity, DetailActivity::class.java)
        intent.putExtra(DetailActivity.PHOTO_REPSONSE, photoResponse)
        val options = ActivityOptions.makeSceneTransitionAnimation(activity, sharedElementsPair)

        context.startActivity(intent, options.toBundle())
    }

    private fun renderProgress() {
        progressBar.visibility = View.VISIBLE
        networkErrorLayout?.visibility = View.GONE
    }

    private fun renderFirstPageError(throwable: Throwable) {
        progressBar.visibility = View.GONE
        Timber.e(throwable)

        when (throwable) {
            is NetworkErrorException -> {
                if (networkErrorLayout == null) {
                    networkErrorLayout = networkErrorStub.inflate() as ImageView
                }

                networkErrorLayout?.let {
                    it.visibility = View.VISIBLE
                    val networkAvd = context.getDrawable(R.drawable.avd_network) as AnimatedVectorDrawable
                    it.setImageDrawable(networkAvd)
                    networkAvd.start()
                }
            }
            is SocketTimeoutException -> {
                if (timeOutLayout == null) {
                    timeOutLayout = timeoutViewStub.inflate()
                }

                timeOutLayout?.let {

                    it.visibility = View.VISIBLE

                    val timeoutAvd = context.getDrawable(R.drawable.avd_timeout) as AnimatedVectorDrawable
                    timeoutImageView.setImageDrawable(timeoutAvd)
                    timeoutAvd.start()

                    timeOutLayout?.setOnClickListener {
                        Timber.d("Clicked")
                        it.visibility = View.GONE
                        loadFirstPageSubject.onNext(true)
                    }
                }
            }
        }

    }

    private fun renderData(viewState: HomeViewState) {
        progressBar.visibility = View.GONE
        networkErrorLayout?.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        homeAdapter.setItems(viewState.data)
    }

    private fun checkConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo

        val connected = activeNetworkInfo != null && activeNetworkInfo.isConnected
        networkStateSubject.onNext(connected)

        if (!connected) {
            connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), connectivityCallback)

            monitoringNetwork = true
        }
    }

    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            networkStateSubject.onNext(true)
            networkReconnected.set(true)
        }

        override fun onLost(network: Network?) {
            super.onLost(network)
            networkStateSubject.onNext(false)
        }
    }

    private val toolbarElevation = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            // we want the grid to scroll over the top of the toolbar but for the toolbar items
            // to be clickable when visible. To achieve this we play games with elevation. The
            // toolbar is laid out in front of the grid but when we scroll, we lower it's elevation
            // to allow the content to pass in front (and reset when scrolled to top of the grid)
            if (newState == RecyclerView.SCROLL_STATE_IDLE
                    && gridLayoutManager.findFirstVisibleItemPosition() == 0
                    && gridLayoutManager.findViewByPosition(0).top == recyclerView?.paddingTop
                    && toolbar.translationZ != 0f) {
                // at top, reset elevation
                toolbar.translationZ = 0f
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING && toolbar.translationZ != -1f) {
                // grid scrolled, lower toolbar to allow content to pass in front
                toolbar.translationZ = -1f
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun isNetworkReconnected(): AtomicBoolean {
        Timber.d("isNetworkReconnected = ${networkReconnected.get()}")
        return networkReconnected
    }
}