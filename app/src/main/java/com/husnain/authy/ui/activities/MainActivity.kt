package com.husnain.authy.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.husnain.authy.R
import com.husnain.authy.databinding.ActivityMainBinding
import com.husnain.authy.ui.fragment.main.HomeFragment
import com.husnain.authy.utls.BackPressedExtensions.goBackPressed
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.afterAuthActivityNavHostFragment)!!
        inIt()
    }

    private fun inIt() {
        setUpBottomBar()
        handleBackPressed()
        setOnClickListener()
    }

    private fun setOnClickListener() {

    }

    private fun setUpBottomBar() {
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.homeFragment || destination.id == R.id.newToolsFragment || destination.id == R.id.settingFragment) {
                binding.bottomNavigationView.visible()
            } else {
                binding.bottomNavigationView.gone()
            }
        }
    }

    private fun handleBackPressed() {
        goBackPressed {
            if (navHostFragment.childFragmentManager.fragments.first() is HomeFragment) {
                finishAffinity()
            } else {
                navHostFragment.findNavController().popBackStack()
            }
        }
    }
}