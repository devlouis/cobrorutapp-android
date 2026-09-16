package com.pe.cobrorutapp.probe

import android.content.Context
import com.pe.cobrorutapp.core.data.probe.LocalNotificationProbeRepository
import com.pe.cobrorutapp.core.domain.probe.NotificationProbeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object ProbeModule {
    @Provides
    @Singleton
    fun provideNotificationProbeRepository(@ApplicationContext context: Context): NotificationProbeRepository =
        LocalNotificationProbeRepository(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
}
