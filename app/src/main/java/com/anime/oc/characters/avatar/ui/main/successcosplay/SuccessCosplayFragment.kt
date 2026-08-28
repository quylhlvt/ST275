package com.anime.oc.characters.avatar.ui.main.successcosplay

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.anime.oc.characters.avatar.core.base.BaseFragment
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.popBack
import com.anime.oc.characters.avatar.core.extention.safeNavigate
import com.anime.oc.characters.avatar.core.extention.select
import com.anime.oc.characters.avatar.core.extention.setImageActionBar
import com.anime.oc.characters.avatar.ui.main.cosplay.CosplayViewModel
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.extention.InternetExtension.isInternetAvailable
import com.anime.oc.characters.avatar.core.extention.loadNativeCollabAds
import com.anime.oc.characters.avatar.core.extention.setTextActionBar
import com.anime.oc.characters.avatar.core.extention.showInter
import com.anime.oc.characters.avatar.databinding.FragmentSuccessCosplayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class SuccessCosplayFragment : BaseFragment<FragmentSuccessCosplayBinding, SuccessCosplayViewModel>( FragmentSuccessCosplayBinding::inflate, SuccessCosplayViewModel::class.java) {
    private var starAnimator: ValueAnimator? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                }
            }
        )
    }
    private fun initNativeCollab() {
        loadNativeCollabAds(
            getString(R.string.native_cl_cosplaySuccess), binding.flNativeCollab
        )
    }
    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSuccessCosplayBinding = FragmentSuccessCosplayBinding.inflate(inflater, container, false)

    override fun initView() {
        initNativeCollab()
        binding.apply {

            txtShow.isSelected = true
            setupActionBar()
            val userBitmap = viewModelActivity.userResultBitmap
            if (userBitmap != null && !userBitmap.isRecycled) {
                imvImage2.setImageBitmap(userBitmap)
            }

            // imvImage3 = ảnh cosplay gốc
            val cosplayBitmap = viewModelActivity.cosplayBitmap
            if (cosplayBitmap != null && !cosplayBitmap.isRecycled) {
                imvImage3.setImageBitmap(cosplayBitmap)
            }

            val percent = viewModelActivity.cosplayPercent
            val starCount = when (percent) {
                0 -> 0
                in 1..20 -> 1
                in 21..40 -> 2
                in 41..70 -> 3
                in 71..98 -> 4
                in 99..100 -> 5
                else -> 0
            }
            binding.ll1.rating = starCount.toFloat()
            updateProgressBar(percent)
        }
    }
    private fun updateProgressBar(percent: Int) {
        val safePercent = percent.coerceIn(0, 100)
        val targetBias = safePercent / 100f

        binding.layoutProgress.post {
            if (!isAdded || isDetached) return@post

            val currentBias =
                (binding.imgStar.layoutParams as ConstraintLayout.LayoutParams)
                    .horizontalBias
                    .coerceIn(0f, 1f)

            starAnimator?.cancel()
            starAnimator = ValueAnimator.ofFloat(currentBias, targetBias).apply {
                duration = 400L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val animatedBias = animator.animatedValue as Float

                    binding.imgStar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        horizontalBias = animatedBias
                    }
                    binding.tvMatchPercent.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        horizontalBias = animatedBias
                    }
                    val animatedPercent = (animatedBias * 100)
                        .roundToInt()
                        .coerceIn(0, safePercent)
                    binding.tvMatchPercent.text = "$animatedPercent/100"
                }
                start()
            }
        }
    }

    override fun onDestroyView() {
        starAnimator?.cancel()
        starAnimator = null
        super.onDestroyView()
    }
    private fun FragmentSuccessCosplayBinding.setupActionBar() {
        actionBar.apply {
            tvCenter.select()
            setImageActionBar(
                btnActionBarRight,
                R.drawable.ic_home
            )
            setTextActionBar(tvCenter, getString(R.string.successfully))
        }
    }

    override fun viewListener() {
        binding.apply {
            setupActionBarListeners()
            setupNavigationListeners()
        }
    }

    private fun FragmentSuccessCosplayBinding.setupActionBarListeners() {
        actionBar.btnActionBarRight.onClick {
            showInter {
                findNavController().safeNavigate(R.id.action_successCosplay_to_home)
            }
        }
    }

    private fun FragmentSuccessCosplayBinding.setupNavigationListeners() {
        btnTryAgain.onClick {
            if (!isInternetAvailable(requireContext())) {
                showUnstableNetworkDialog(); return@onClick
            }
            showInter {
                val cosplayEntry = runCatching {
                    findNavController().getBackStackEntry(R.id.cosplay)
                }.getOrNull()

                cosplayEntry?.let {
                    val factory = HiltViewModelFactory(requireContext(), it)
                    val cosplayViewModel = ViewModelProvider(it, factory)[CosplayViewModel::class.java]
                    cosplayViewModel.randomize()
                }

                viewModelActivity.shouldRestartShow = true  // ← báo ShowFragment reset
                popBack()
            }
        }
    }


    override fun observeData() {}

    override fun bindViewModel() {}
}
