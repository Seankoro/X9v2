package dk.itu.moapd.x9.s25134

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.google.firebase.Firebase
import com.google.firebase.database.database
import io.github.cdimascio.dotenv.dotenv

/**
 * Application subclass that initializes Firebase and Coil before any Activity is created.
 */
class X9Application : Application() {

    companion object {
        const val PREFS_NAME = "x9_prefs"
        lateinit var DATABASE_URL: String
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Load environment variables
        val env = dotenv {
            directory = "/assets"
            filename = "env"
        }
        DATABASE_URL = env["DATABASE_URL"]

        // Enable offline caching
        Firebase.database(DATABASE_URL).setPersistenceEnabled(true)

        // Configure Coil image loader
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components {
                    add(OkHttpNetworkFetcherFactory())
                }
                .build()
        }
    }
}
