package xh.zero.xh_camera

import android.app.Application
import timber.log.Timber

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}