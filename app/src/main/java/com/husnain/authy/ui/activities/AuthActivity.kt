package com.husnain.authy.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.husnain.authy.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : LocalizationActivity() {
    private lateinit var binding: ActivityAuthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {

    }
}