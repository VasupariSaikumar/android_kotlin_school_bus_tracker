package com.example.android_kotlin_school_bus_tracker.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.android_kotlin_school_bus_tracker.data.AuthRepository
import com.example.android_kotlin_school_bus_tracker.data.BusRepository
import com.example.android_kotlin_school_bus_tracker.data.LocationRepository
import com.example.android_kotlin_school_bus_tracker.data.StopRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository = AuthRepository(auth)

    @Provides
    @Singleton
    fun provideBusRepository(firestore: FirebaseFirestore): BusRepository = BusRepository(firestore)

    @Provides
    @Singleton
    fun provideStopRepository(firestore: FirebaseFirestore): StopRepository = StopRepository(firestore)

    @Provides
    @Singleton
    fun provideLocationRepository(firestore: FirebaseFirestore): LocationRepository = LocationRepository(firestore)
}
