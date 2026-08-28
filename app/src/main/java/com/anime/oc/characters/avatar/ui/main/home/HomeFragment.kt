package com.anime.oc.characters.avatar.ui.main.home

import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.ViewModelActivity
import com.anime.oc.characters.avatar.core.base.BackPressHandler
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.InternetExtension
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.safeNavigate
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.core.extention.toSettingFromHome
import com.anime.oc.characters.avatar.core.helper.RateHelper
import com.anime.oc.characters.avatar.utils.state.RateState
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.loadNativeCollabAds
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.databinding.FragmentHomeBinding
import com.anime.oc.characters.avatar.utils.BlockableFrameLayout
import com.lvt.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate, HomeViewModel::class.java
), BackPressHandler {

    private val mainViewModel: ViewModelActivity by activityViewModels()
    private var countRate = 0

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun initView() {
        Admob.getInstance().loadNativeAll(requireContext(), getString(R.string.native_all))
        initNativeCollab()
        binding.rcvMain.adapter = HomeMenuAdapter(::onHomeMenuClick)
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_home), binding.flNativeCollab
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PERF2", "HomeFragment onViewCreated: ${System.currentTimeMillis()}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("PERF2", "HomeFragment onResume: ${System.currentTimeMillis()}")
    }

    override fun onFragmentStart() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = false
        initNativeCollab()
    }

    override fun onFragmentStop() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = true
//        binding.flNativeCollab.removeAllViews()
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarRight.onClick { toSettingFromHome() }
    }

    private fun onHomeMenuClick(action: HomeMenuAction) {
        when (action) {
            HomeMenuAction.CREATE -> navigateWithCheck(R.id.action_home_to_createPony)

            HomeMenuAction.COSPLAY -> navigateWithCheck(R.id.action_home_to_cosplay)
            HomeMenuAction.RANDOM -> navigateWithCheck(R.id.action_home_to_random)
            HomeMenuAction.MY_ALBUM -> {
                showInter {
                    findNavController().safeNavigate(R.id.action_home_to_myPony)
                }
            }
        }
    }

    private fun navigateWithCheck(destination: Int) {
        val hasNetwork = InternetExtension.isInternetAvailable(requireContext()) &&
                InternetExtension.isNetworkConnected(requireContext())
        val hasData = viewModelActivity.templates.value.isNotEmpty()

        when {
            !hasNetwork -> showUnstableNetworkDialog()
            !hasData -> {
                mainViewModel.fetchOnlineTemplates()
                showLoadingDataDialog()
            }

            else -> findNavController().safeNavigate(destination)
        }
    }

    override fun observeData() {
        binding.root.post {
            Log.d("PERF2", "HomeFragment first frame: ${System.currentTimeMillis()}")
            if (!isAdded || isDetached) return@post
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mainViewModel.error.collect { error ->
                        error?.let { Log.e("HomeFragment", "❌ $it") }
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}

    override fun onBackPressed(): Boolean {
        countRate = sharedPreferences.isBackRequest() + 1
        sharedPreferences.setBackRequest(countRate)
        if (!sharedPreferences.isRateRequest() && countRate % 2 == 0) {
            RateHelper.showRateDialog(requireActivity(), sharedPreferences) { state ->
                if (state != RateState.CANCEL) showToast(R.string.have_rated)
                requireActivity().finishAffinity()
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
        } else {
            requireActivity().finishAffinity()
            exitProcess(0)
        }
        return true
    }
}
