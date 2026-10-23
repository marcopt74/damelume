package com.marcopt74.damelume.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.marcopt74.damelume.data.db.CompletedSessionDao
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.domain.SystemClock
import com.marcopt74.damelume.system.alarm.AlarmScheduler
import com.marcopt74.damelume.system.alarm.AndroidAlarmScheduler
import com.marcopt74.damelume.system.locale.AndroidLocaleApplicator
import com.marcopt74.damelume.system.locale.LocaleApplicator
import com.marcopt74.damelume.system.notify.AndroidDevicePermissions
import com.marcopt74.damelume.system.notify.AndroidTimerNotifier
import com.marcopt74.damelume.system.notify.DevicePermissions
import com.marcopt74.damelume.system.notify.TimerNotifier
import com.marcopt74.damelume.system.sound.AndroidChimePlayer
import com.marcopt74.damelume.system.sound.AppForegroundTracker
import com.marcopt74.damelume.system.sound.ChimePlayer
import com.marcopt74.damelume.system.sound.ForegroundTracker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.lumeDataStore: DataStore<Preferences> by preferencesDataStore(name = "lume_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.lumeDataStore

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LumeDatabase = Room.databaseBuilder(context, LumeDatabase::class.java, "lume.db").build()

    @Provides
    fun provideDao(database: LumeDatabase): CompletedSessionDao = database.completedSessionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {
    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindNotifier(impl: AndroidTimerNotifier): TimerNotifier

    @Binds
    @Singleton
    abstract fun bindChime(impl: AndroidChimePlayer): ChimePlayer

    @Binds
    @Singleton
    abstract fun bindForeground(impl: AppForegroundTracker): ForegroundTracker

    @Binds
    @Singleton
    abstract fun bindLocale(impl: AndroidLocaleApplicator): LocaleApplicator

    @Binds
    @Singleton
    abstract fun bindPermissions(impl: AndroidDevicePermissions): DevicePermissions
}
