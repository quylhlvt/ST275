package com.anime.oc.characters.avatar

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.hideNavigation
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.DialogbaseBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

interface LoadingController {
    fun showGlobalLoading()
    fun hideGlobalLoading()
    fun hideGlobalDialog()
    fun showGlobalConfirmDialog(
        message: String,
        title: String? = null,
        onYes: () -> Unit,
        onNo: (() -> Unit)? = null,
        yesText: String? = null,
        noText: String? = null
    )
    fun showGlobalOkDialog(
        message: String,
        title: String? = null,
        onOk: (() -> Unit)? = null
    )
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() , LoadingController{
    private var globalLoadingDialog: Dialog? = null
    private var globalConfirmDialog: Dialog? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private fun canShowWindow(): Boolean =
        !isFinishing && !isDestroyed

    override fun showGlobalLoading() {
        if (globalLoadingDialog?.isShowing == true) return
        runOnUiThread {
            if (!canShowWindow()) return@runOnUiThread
            if (globalLoadingDialog == null) {
                globalLoadingDialog = buildDialog(
                    message = getString(R.string.loading),
                    showButtons = false,
                    cancelable = false
                )
            }
            runCatching { globalLoadingDialog?.show() }
                .onFailure { globalLoadingDialog = null }
            hideNavigation(true)
        }
    }
    override fun showGlobalOkDialog(message: String, title: String?, onOk: (() -> Unit)?) {
        runOnUiThread {
            if (!canShowWindow()) return@runOnUiThread
            globalConfirmDialog?.dismiss()
            globalConfirmDialog = buildDialog(
                message = message,
                title = title,
                showButtons = true,
                cancelable = true,
                onOk = {
                    globalConfirmDialog?.dismiss()
                    globalConfirmDialog = null
                    onOk?.invoke()
                }
            )
            runCatching { globalConfirmDialog?.show() }
                .onFailure { globalConfirmDialog = null }
            hideNavigation(true)
        }
    }
    override fun hideGlobalLoading() {
        Log.d("LOADING", "hideGlobalLoading")

        runOnUiThread {
            globalLoadingDialog?.dismiss()
            globalLoadingDialog = null
            if (hasWindowFocus()) hideNavigation(true)
        }
    }

    override fun hideGlobalDialog() {
        runOnUiThread {
            globalConfirmDialog?.dismiss()
            globalConfirmDialog = null
            if (hasWindowFocus()) hideNavigation(true)
        }
    }

    override fun showGlobalConfirmDialog(
        message: String,
        title: String?,
        onYes: () -> Unit,
        onNo: (() -> Unit)?,
        yesText: String?,
        noText: String?
    ) {
        runOnUiThread {
            globalConfirmDialog?.dismiss()
            globalConfirmDialog = buildDialog(
                message = message,
                title = title,
                showButtons = true,
                cancelable = true,
                yesText = yesText,
                noText = noText,
                onYes = {
                    globalConfirmDialog?.dismiss()
                    globalConfirmDialog = null
                    onYes()
                },
                onNo = {
                    globalConfirmDialog?.dismiss()
                    globalConfirmDialog = null
                    onNo?.invoke()
                }
            )
            globalConfirmDialog?.show()
            hideNavigation(true)
        }
    }

    // Builder dùng chung
    private fun buildDialog(
        message: String,
        title: String? = null,
        showButtons: Boolean = false,
        cancelable: Boolean = false,
        onYes: (() -> Unit)? = null,
        onNo: (() -> Unit)? = null,
        onOk: (() -> Unit)? = null,
        yesText: String? = null,
        noText: String? = null
    ): Dialog {
        return Dialog(this, R.style.BaseDialog).apply {
            val binding = DialogbaseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.txtYes.isSelected = true
            binding.tvTitle.isSelected = true
            binding.txtNo.isSelected = true
            title?.let { binding.tvTitle.text = it }
            binding.txtContent.text = message
            yesText?.let { binding.txtYes.text = it }
            noText?.let { binding.txtNo.text = it }

            if (showButtons) {
                binding.txtContent.visible()
                binding.progressBar.gone()
                binding.txtPlease.gone()

                if (onOk != null) {
                    // ← chế độ OK only
                    binding.btnYes.gone()
                    binding.btnNo.gone()
                    binding.btnOk.visible()
                    binding.btnOk.setOnClickListener { onOk.invoke() }
                } else {
                    // ← chế độ Yes/No
                    binding.btnYes.visible()
                    binding.btnNo.visible()
                    binding.btnOk.gone()
                    binding.btnYes.setOnClickListener { onYes?.invoke() }
                    binding.btnNo.setOnClickListener { onNo?.invoke() }
                }
            } else {
                binding.btnYes.gone()
                binding.btnNo.gone()
                binding.btnOk.gone()
                binding.txtContent.gone()
                binding.progressBar.visible()
                binding.txtPlease.visible()
            }

            setCancelable(cancelable)
            window?.apply {
                setBackgroundDrawableResource(R.color.transparent)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                setGravity(Gravity.CENTER)
            }
        }
    }

    private fun preloadHomeDrawables(onDone: () -> Unit) {
        // This work only warms Glide's caches and must not be tied to the Activity.
        // A decorView callback may run after the Activity has already been destroyed.
        val requestManager = Glide.with(applicationContext)
        val flagResIds = listOf(
            R.drawable.ic_flag_hindi,
            R.drawable.ic_flag_spanish,
            R.drawable.ic_flag_french,
            R.drawable.ic_flag_english,
            R.drawable.ic_flag_portugeese,
            R.drawable.ic_flag_indo,
            R.drawable.ic_flag_germani,
            R.drawable.ic_select_lang,
            R.drawable.ic_un_select_lang,
            R.drawable.select_language,
            R.drawable.back_app,
        )

        val total = flagResIds.size
        val doneCount = AtomicInteger(0)
        val checkDone = { if (doneCount.incrementAndGet() == total) onDone() }

        // ✅ Flag icons — nhỏ, dùng override nhỏ + memory cache
        flagResIds.forEach { resId ->
            requestManager
                .load(resId)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .override(64, 64)           // ← giảm xuống 64 cho flag
                .preload()                  // ← preload vào memory cache
            checkDone()                     // ← không cần đợi callback
        }
    }
    private lateinit var navController: NavController
    private val mainViewModel: ViewModelActivity by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("PERF2", "onCreate START: ${System.currentTimeMillis()}")
        super.onCreate(savedInstanceState)
        Log.d("PERF2", "super.onCreate done: ${System.currentTimeMillis()}")

        setContentView(R.layout.activity_main)
        Log.d("PERF2", "setContentView done: ${System.currentTimeMillis()}")

        hideNavigation(true)        // ← sau setContentView, window đã sẵn sàng
        globalLoadingDialog = buildDialog(
            message = getString(R.string.loading),
            showButtons = false,
            cancelable = false
        )
        initSharedPreferences()
//        applyLanguage()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        navController.addOnDestinationChangedListener { _, destination,_  ->
            val screenName = destination.label?.toString() ?: resources.getResourceEntryName(destination.id)
            val screenClass = if (destination is FragmentNavigator.Destination) {
                destination.className.substringAfterLast(".")
            } else {
                screenName
            }
            Log.d("SCREEN", "$screenName - $screenClass")
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                if (currentFragment is BackPressHandler && currentFragment.onBackPressed()) return
                if (!navController.popBackStack()) finish()
            }
        })
//        Admob.getInstance().setTimeLimitShowAds(30000)
//        Admob.getInstance().setTimeCountdownNativeCollab(18000)
//        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))

        window.decorView.post {
            preloadHomeDrawables {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        mainViewModel.notifyImagesReady()
                    }
                }
            }
        }

        Log.d("PERF2", "onCreate END: ${System.currentTimeMillis()}")
    }
    private fun initSharedPreferences() {
        val sharedPrefs = getSharedPreferences("DEFAULT", MODE_PRIVATE)
        SharedPreferencesManager.sharedPreferences = sharedPrefs
        SharedPreferencesManager.editor = sharedPrefs.edit()
    }
    override fun attachBaseContext(newBase: Context) {
        val sharedPrefs = newBase.getSharedPreferences("DEFAULT", MODE_PRIVATE)
        val lang = sharedPrefs.getString("language_key", "") ?: ""

        val locale = Locale(lang.ifEmpty { "en" })
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
//    private fun applyLanguage() {
//        val savedLanguage = SharedPreferencesManager.isLanguageKey()
//        if (savedLanguage.isNotEmpty()) {
//            val locale = Locale(savedLanguage)
//            Locale.setDefault(locale)
//
//            val config = Configuration(resources.configuration)
//            config.setLocale(locale)
//
//            // ✅ QUAN TRỌNG: Update configuration
//            resources.updateConfiguration(config, resources.displayMetrics)
//        }
//    }
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//
//        // ✅ Re-apply language khi xoay màn hình
//        applyLanguage()
//    }

//    // QUAN TRỌNG: xử lý nút Back đúng cách
//    override fun onBackPressed() {
//        if (!navController.popBackStack()) {
//            // Không còn gì trong back stack → thoát app
//            super.onBackPressed()
//        }
//    }

    // Nếu bạn dùng Toolbar + NavigationIcon (mũi tên back)
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        // onResume co the den truoc khi window lay lai focus tu Settings/share.
        // Chi an system bars khi focus da tro ve de tranh tranh chap transition.
        if (hasWindowFocus()) hideNavigation(true)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigation(true)
    }

}
