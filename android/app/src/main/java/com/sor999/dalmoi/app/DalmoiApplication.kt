package com.sor999.dalmoi.app

import android.app.Application

class DalmoiApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
