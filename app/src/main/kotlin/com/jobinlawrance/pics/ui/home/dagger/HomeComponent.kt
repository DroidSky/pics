package com.jobinlawrance.pics.ui.home.dagger

import com.jobinlawrance.pics.di.fragment.FragmentComponent
import com.jobinlawrance.pics.di.fragment.FragmentComponentBuilder
import com.jobinlawrance.pics.di.fragment.FragmentScope
import com.jobinlawrance.pics.ui.home.HomeFragment
import dagger.Subcomponent

/**
 * Created by jobinlawrance on 7/9/17.
 */

@FragmentScope
@Subcomponent(modules = arrayOf(HomeModule::class))
interface HomeComponent : FragmentComponent<HomeFragment> {

    @Subcomponent.Builder
    abstract class Builder : FragmentComponentBuilder<HomeModule, HomeComponent>

}