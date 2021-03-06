package com.jobinlawrance.pics.di.application

import android.support.annotation.VisibleForTesting
import com.jobinlawrance.pics.application.MyApplication
import com.jobinlawrance.pics.businesslogic.download.DownloadServiceImpl
import com.jobinlawrance.pics.di.fragment.FragmentBindingModule
import com.jobinlawrance.pics.ui.widget.PicsRemoteViewFactory
import dagger.Component
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Created by jobinlawrance on 5/9/17.
 */
@Singleton
@Component(modules = arrayOf(NetModule::class, FragmentBindingModule::class))
interface AppComponent {

    fun inject(myApplication: MyApplication)
    fun inject(downloadServiceImpl: DownloadServiceImpl)
    fun inject(picsRemoteViewFactory: PicsRemoteViewFactory)

    @VisibleForTesting
    fun getRetrofit(): Retrofit
}