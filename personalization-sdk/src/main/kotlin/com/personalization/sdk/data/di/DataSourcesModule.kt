package com.personalization.sdk.data.di

import com.personalization.sdk.data.repositories.notification.NotificationDataSource
import com.personalization.sdk.data.repositories.notification.NotificationDataSourceImpl
import com.personalization.sdk.data.repositories.preferences.PreferencesDataSource
import com.personalization.sdk.data.repositories.preferences.PreferencesDataSourceImpl
import com.personalization.sdk.data.repositories.recommendation.RecommendationDataSource
import com.personalization.sdk.data.repositories.recommendation.RecommendationDataSourceImpl
import com.personalization.sdk.data.repositories.trackingSource.TrackingSourceDataSource
import com.personalization.sdk.data.repositories.trackingSource.TrackingSourceDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface DataSourcesModule {

    @Binds
    @Singleton
    fun bindPreferencesDataSource(impl: PreferencesDataSourceImpl): PreferencesDataSource

    @Binds
    @Singleton
    fun bindRecommendationDataSource(impl: RecommendationDataSourceImpl): RecommendationDataSource

    @Binds
    @Singleton
    fun bindTrackingSourceDataSource(impl: TrackingSourceDataSourceImpl): TrackingSourceDataSource

    companion object {

        @Provides
        fun provideNotificationDataSource(
            preferencesDataSource: PreferencesDataSource
        ): NotificationDataSource = NotificationDataSourceImpl(
            preferencesDataSource = preferencesDataSource
        )
    }
}
