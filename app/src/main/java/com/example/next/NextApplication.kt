package com.example.next

import android.app.Application
import com.example.next.di.AppContainer

class NextApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}