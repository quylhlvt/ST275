package com.anime.oc.characters.avatar.core.base

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.anime.oc.characters.avatar.AppSession
import com.anime.oc.characters.avatar.LoadingController
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.*
import com.anime.oc.characters.avatar.core.helper.SharedPreferencesManager
import com.anime.oc.characters.avatar.databinding.DialogbaseBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** Each screen owns its content, ViewModel, dialogs and back callback. */
abstract class BaseActivity<VB : ViewBinding, VM : ViewModel>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    private val viewModelClass: Class<VM>
) : AppCompatActivity(), LoadingController {
    protected lateinit var binding: VB
        private set
    protected val contentView: View? get() = if (::binding.isInitialized) binding.root else null
    val isScreenAvailable: Boolean get() = !isFinishing && !isDestroyed
    protected val isScreenResumed: Boolean get() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    var isNavigating: Boolean = false
        private set
    private var firstStart = true
    private var screenStopped = false
    protected val viewModel: VM by lazy { ViewModelProvider(this)[viewModelClass] }
    @Inject lateinit var appSession: AppSession
    @Inject internal lateinit var sharedPreferences: SharedPreferencesManager
    private var toast: Toast? = null

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("DEFAULT", MODE_PRIVATE)
            .getString("language_key", "").orEmpty().ifEmpty { "en" }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale(language))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("DEFAULT", MODE_PRIVATE)
        SharedPreferencesManager.sharedPreferences = prefs
        SharedPreferencesManager.editor = prefs.edit()
        binding = bindingInflater(layoutInflater, null, false)
        setContentView(binding.root)
        hideNavigation(true)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if ((this@BaseActivity as? BackPressHandler)?.onScreenBackPressed() == true) return
                popBack()
            }
        })
        setupPreViews()
        onContentReady(binding.root, savedInstanceState)

    }

    protected open fun onContentReady(view: View, savedInstanceState: Bundle?) {
        initView()
        initText()
        view.post {
            if (!isScreenAvailable) return@post
            viewListener()
            bindViewModel()
            observeData()
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    appSession.networkOnline.filter { it }.collect {
                        if (appSession.templates.value.none { it.id.startsWith("online_") }) {
                            appSession.fetchOnlineTemplates()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        screenStopped = false
        if (firstStart) firstStart = false else onScreenRestart()
    }
    override fun onStop() {
        screenStopped = true
        onScreenStop()
        // Dialogs belong to this Activity and cannot cover the next screen.
        hideGlobalLoading()
        hideGlobalDialog()
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        isNavigating = false
        if (hasWindowFocus()) hideNavigation(true)
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigation(true)
    }
    override fun onDestroy() {
        globalLoadingDialog?.dismiss()
        globalConfirmDialog?.dismiss()
        globalLoadingDialog = null
        globalConfirmDialog = null
        toast?.cancel()
        super.onDestroy()
    }
    fun markNavigating() { isNavigating = true }
    open fun setupPreViews() {}
    open fun initView() {}
    open fun initText() {}
    open fun observeData() {}
    open fun onScreenRestart() {}
    open fun onScreenStop() {}
    abstract fun viewListener()
    abstract fun bindViewModel()

    fun showSnackbar(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    fun showToast(content: Any) {
        toast?.cancel()
        val message = when (content) { is String -> content; is Int -> getString(content); else -> "" }
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).also { it.show() }
    }
    fun showLoadingSafe() { if (isScreenAvailable) showGlobalLoading() }
    fun hideLoadingSafe() = hideGlobalLoading()
    fun hideGlobalDialogSafe() = hideGlobalDialog()
    fun hideLoading() = hideGlobalLoading()
    fun showConfirmDialog(message: String, title: String? = null, onYes: () -> Unit,
        onNo: (() -> Unit)? = null, yesText: String? = null, noText: String? = null) {
        showGlobalConfirmDialog(message, title, onYes, onNo, yesText, noText)
    }
    fun showOkDialog(message: String, title: String? = null, onOk: (() -> Unit)? = null) {
        showGlobalOkDialog(message, title, onOk)
    }
    fun showNoInternetDialog(onOk: (() -> Unit)? = null) = showOkDialog(
        getString(R.string.please_connect_to_the_internet_to_download_more_data), getString(R.string.no_internet), onOk)
    fun showLoadingDataDialog(onOk: (() -> Unit)? = null) = showOkDialog(
        getString(R.string.please_wait_a_few_seconds_for_data_to_load), getString(R.string.internet), onOk)
    fun showUnstableNetworkDialog(onOk: (() -> Unit)? = null) = showOkDialog(
        getString(R.string.unstable_connection_please_check_your_network_connection), getString(R.string.internet), onOk)
    fun checkNetworkAndShowDialog() {
        when {
            !InternetExtension.isInternetAvailable(this) -> showNoInternetDialog()
            !InternetExtension.isNetworkConnected(this) -> showUnstableNetworkDialog()
        }
    }
    private var globalLoadingDialog: Dialog? = null
    private var globalConfirmDialog: Dialog? = null

    private fun canShowWindow(): Boolean =
        isScreenAvailable && !isNavigating && !screenStopped

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
            if (!canShowWindow()) return@runOnUiThread
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
            runCatching { globalConfirmDialog?.show() }
                .onFailure { globalConfirmDialog = null }
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

}
