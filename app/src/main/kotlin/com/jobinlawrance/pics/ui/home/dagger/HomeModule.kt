package com.jobinlawrance.pics.ui.home.dagger

import com.jobinlawrance.pics.data.feed.PhotoFeeder
import com.jobinlawrance.pics.data.feed.PhotoFeederImpl
import com.jobinlawrance.pics.data.retrofit.services.PhotoService
import com.jobinlawrance.pics.di.fragment.FragmentModule
import com.jobinlawrance.pics.ui.home.HomeContract
import com.jobinlawrance.pics.ui.home.HomeFragment
import com.jobinlawrance.pics.ui.home.HomeInteractorImpl
import com.jobinlawrance.pics.ui.home.HomePresenterImpl
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

/**
 * Created by jobinlawrance on 7/9/17.
 */

@Module
class HomeModule(homeFragment: HomeFragment) : FragmentModule<HomeFragment>(homeFragment) {

    @Provides
    fun providePhotoFeeder(retrofit: Retrofit): PhotoFeeder =
            PhotoFeederImpl(retrofit.create(PhotoService::class.java))

    @Provides
    fun provideInteractor(photoFeeder: PhotoFeeder): HomeContract.Interactor
            = HomeInteractorImpl(photoFeeder)

    @Provides
    fun providePresenter(interactor: HomeContract.Interactor): HomeContract.Presenter
            = HomePresenterImpl(interactor)
}