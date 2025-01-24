package com.husnain.authy.utls.progress

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.husnain.authy.R
import com.husnain.authy.databinding.BottomSheetDeleteTotpBinding
import com.husnain.authy.databinding.CustomBottomSheetDeleteAccountBinding

fun Fragment.showDeleteAccountConfirmationBottomSheet(
    onYes: () -> Unit,
) {
    val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
    val binding = CustomBottomSheetDeleteAccountBinding.inflate(LayoutInflater.from(requireContext()))
    bottomSheetDialog.setContentView(binding.root)

    binding.lyYes.setOnClickListener {
        onYes.invoke()
        bottomSheetDialog.dismiss()
    }

    binding.lyNo.setOnClickListener {
        bottomSheetDialog.dismiss()
    }

    bottomSheetDialog.show()
}

fun Fragment.showBottomSheetDialog(txtPrimaryButton: String,onPrimaryClick:() -> Unit) {
    val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
    val binding = BottomSheetDeleteTotpBinding.inflate(LayoutInflater.from(requireContext()))
    bottomSheetDialog.setContentView(binding.root)

    binding.tvTop.text = txtPrimaryButton

    binding.lyTopText.setOnClickListener {
        onPrimaryClick.invoke()
        bottomSheetDialog.dismiss()
    }

    binding.lyCancel.setOnClickListener {
        bottomSheetDialog.dismiss()
    }

    bottomSheetDialog.show()
}