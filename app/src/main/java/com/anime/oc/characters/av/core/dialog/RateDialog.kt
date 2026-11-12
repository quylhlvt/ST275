// RateDialog.kt (now extends DialogFragment)
package com.anime.oc.characters.av.core.dialog

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.anime.oc.characters.av.core.extention.strings
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.DialogRateBinding

class RateDialog(private val activity: Activity) : DialogFragment() {

    private var _binding: DialogRateBinding? = null
    private val binding get() = _binding!!

    var onRateGreater3: (() -> Unit)? = null
    var onRateLess3: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    private var rating: Int = 0
    private var ratingToast: Toast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        initView()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        initAction()
    }

    private fun initView() {
        binding.apply {
            tv1.isSelected = true
            tv2.isSelected = true
            btnVote.isSelected = true

        }
    }

    private fun initAction() {
        binding.btnExit.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }

        binding.btnVote.setOnClickListener {
            if (rating == 0) {
                ratingToast?.cancel()
                val toast = Toast.makeText(
                    requireContext(),
                    getString(R.string.rate_us_0),
                    Toast.LENGTH_SHORT
                )
                ratingToast = toast
                toast.show()
                binding.root.postDelayed({
                    toast.cancel()
                    if (ratingToast === toast) ratingToast = null
                }, 700L)
                return@setOnClickListener
            }

            if (rating <= 3) {
                onRateLess3?.invoke()
            } else {
                onRateGreater3?.invoke()
            }
            dismiss()
        }

        binding.ll1.setOnRatingChangeListener() { _, r, _ ->
            rating = r.toInt()
            when (rating) {
                0 -> setView(R.string.zero_star_title, R.string.zero_star, R.drawable.ic_rate_rero)
                1 -> setView(R.string.one_star_title, R.string.three_star, R.drawable.ic_rate_one)
                2 -> setView(R.string.two_star_title, R.string.three_star, R.drawable.ic_rate_two)
                3 -> setView(R.string.three_star_title, R.string.three_star, R.drawable.ic_rate_three)
                4 -> setView(R.string.four_star_title, R.string.four_star, R.drawable.ic_rate_four)
                5 -> setView(R.string.five_star_title, R.string.five_star, R.drawable.ic_rate_five)
            }
        }
    }

    private fun setView(titleRes: Int, descRes: Int,imgRes:Int) {
        binding.tv1.text = requireContext().strings(titleRes)
        binding.tv2.text = requireContext().strings(descRes)
        binding.imvAvtRate.setImageResource(imgRes)
    }

    override fun onDestroyView() {
        ratingToast?.cancel()
        ratingToast = null
        super.onDestroyView()
        _binding = null
    }
}
