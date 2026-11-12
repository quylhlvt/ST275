package com.anime.oc.characters.av

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anime.oc.characters.av.ui.onboarding.splash.SplashActivity

/** Retains the installed launcher component; screen content belongs to SplashActivity. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Existing installations may restore the old NavHost and removed Fragment classes.
        super.onCreate(null)
        if (isTaskRoot) startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }
}
