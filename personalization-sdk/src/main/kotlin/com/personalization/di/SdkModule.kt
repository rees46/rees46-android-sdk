package com.personalization.di

import android.content.Context
import com.personalization.RegisterManager
import com.personalization.push.PushTokenManager
import com.personalization.api.managers.CartManager
import com.personalization.api.managers.CategoryManager
import com.personalization.api.managers.CollectionManager
import com.personalization.api.managers.InAppNotificationManager
import com.personalization.api.managers.LoyaltyManager
import com.personalization.api.managers.OrdersManager
import com.personalization.api.managers.ProductsManager
import com.personalization.api.managers.ProfileManager
import com.personalization.api.managers.RecommendationManager
import com.personalization.api.managers.PredictManager
import com.personalization.api.managers.SearchManager
import com.personalization.api.managers.TrackEventManager
import com.personalization.api.managers.TrackingApi
import com.personalization.features.cart.CartManagerImpl
import com.personalization.features.category.impl.CategoryManagerImpl
import com.personalization.features.collection.impl.CollectionManagerImpl
import com.personalization.features.inAppNotification.impl.InAppNotificationManagerImpl
import com.personalization.features.notification.domain.data.NotificationDataExtractor
import com.personalization.features.loyalty.impl.LoyaltyManagerImpl
import com.personalization.features.orders.impl.OrdersManagerImpl
import com.personalization.features.predict.impl.PredictManagerImpl
import com.personalization.features.products.impl.ProductsManagerImpl
import com.personalization.features.profile.impl.ProfileManagerImpl
import com.personalization.features.recommendation.impl.RecommendationManagerImpl
import com.personalization.features.search.impl.SearchManagerImpl
import com.personalization.features.trackEvent.impl.TrackEventManagerImpl
import com.personalization.features.tracking.impl.TrackingApiImpl
import com.personalization.sdk.domain.usecases.network.ExecuteQueueTasksUseCase
import com.personalization.sdk.domain.usecases.network.SendNetworkMethodUseCase
import com.personalization.sdk.domain.usecases.recommendation.GetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.recommendation.SetRecommendedByUseCase
import com.personalization.sdk.domain.usecases.userSettings.GetUserSettingsValueUseCase
import com.personalization.sdk.domain.usecases.userSettings.UpdateUserSettingsValueUseCase
import com.personalization.stories.StoriesManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SdkModule {

    @Singleton
    @Provides
    fun provideRegisterManager(
        updateUserSettingsValueUseCase: UpdateUserSettingsValueUseCase,
        getUserSettingsValueUseCase: GetUserSettingsValueUseCase,
        sendNetworkMethodUseCase: SendNetworkMethodUseCase,
        executeQueueTasksUseCase: ExecuteQueueTasksUseCase,
        inAppNotificationManager: InAppNotificationManager,
        pushTokenManager: PushTokenManager
    ): RegisterManager = RegisterManager(
        updateUserSettingsValueUseCase = updateUserSettingsValueUseCase,
        getUserSettingsValueUseCase = getUserSettingsValueUseCase,
        sendNetworkMethodUseCase = sendNetworkMethodUseCase,
        executeQueueTasksUseCase = executeQueueTasksUseCase,
        inAppNotificationManager = inAppNotificationManager,
        pushTokenManager = pushTokenManager
    )

    @Singleton
    @Provides
    fun provideRecommendationManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): RecommendationManager = RecommendationManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideProductsManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): ProductsManager = ProductsManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideCartManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): CartManager = CartManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideTrackEventManager(
        getRecommendedByUseCase: GetRecommendedByUseCase,
        setRecommendedByUseCase: SetRecommendedByUseCase,
        sendNetworkMethodUseCase: SendNetworkMethodUseCase,
        inAppNotificationManager: InAppNotificationManager,
        getUserSettingsValueUseCase: GetUserSettingsValueUseCase
    ): TrackEventManager = TrackEventManagerImpl(
        getRecommendedByUseCase = getRecommendedByUseCase,
        setRecommendedByUseCase = setRecommendedByUseCase,
        sendNetworkMethodUseCase = sendNetworkMethodUseCase,
        inAppNotificationManager = inAppNotificationManager,
        getUserSettingsValueUseCase = getUserSettingsValueUseCase
    )

    @Singleton
    @Provides
    fun provideTrackingApi(
        trackEventManager: TrackEventManager,
        storiesManager: StoriesManager,
        setRecommendedByUseCase: SetRecommendedByUseCase
    ): TrackingApi = TrackingApiImpl(
        trackEventManager = trackEventManager,
        storiesManager = storiesManager,
        setRecommendedByUseCase = setRecommendedByUseCase
    )

    @Singleton
    @Provides
    fun provideStoriesManager(
        setRecommendedByUseCase: SetRecommendedByUseCase,
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): StoriesManager = StoriesManager(
        setRecommendedByUseCase = setRecommendedByUseCase,
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideSearchManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): SearchManager = SearchManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun providePredictManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): PredictManager = PredictManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideOrdersManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): OrdersManager = OrdersManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideLoyaltyManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): LoyaltyManager = LoyaltyManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideProfileManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): ProfileManager = ProfileManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideCategoryManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): CategoryManager = CategoryManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideCollectionManager(
        sendNetworkMethodUseCase: SendNetworkMethodUseCase
    ): CollectionManager = CollectionManagerImpl(
        sendNetworkMethodUseCase = sendNetworkMethodUseCase
    )

    @Singleton
    @Provides
    fun provideInAppNotificationManager(
        context: Context,
        getUserSettingsValueUseCase: GetUserSettingsValueUseCase,
        trackEventManagerProvider: javax.inject.Provider<TrackEventManager>
    ): InAppNotificationManager {
        return InAppNotificationManagerImpl(
            context = context,
            getUserSettingsValueUseCase = getUserSettingsValueUseCase,
            trackEventManager = dagger.Lazy { trackEventManagerProvider.get() }
        )
    }

    @Singleton
    @Provides
    fun provideNotificationDataExtractor(): NotificationDataExtractor {
        return NotificationDataExtractor()
    }

}
