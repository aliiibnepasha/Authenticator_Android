package com.husnain.authy.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.husnain.authy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inIt()
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {

    }
}