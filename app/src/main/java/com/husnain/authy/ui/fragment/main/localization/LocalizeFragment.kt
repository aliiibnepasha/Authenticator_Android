package com.husnain.authy.ui.fragment.main.localization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.husnain.authy.data.models.ModelLanguage
import com.husnain.authy.databinding.FragmentLocalizeBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.activities.MainActivity
import com.husnain.authy.utls.popBack
import com.husnain.authy.utls.startActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocalizeFragment : Fragment() {
    private var _binding: FragmentLocalizeBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocalizeBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
        setUpAdapter()
    }


    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
    }

    private fun setUpAdapter() {
        val languagesList = listOf(
            ModelLanguage("English", "en"),
            ModelLanguage("Arabic", "ar"),
            ModelLanguage("Spanish", "es"),
            ModelLanguage("French", "fr"),
            ModelLanguage("Urdu", "ur")

        )

        val adapter = AdapterLanguages(languagesList){
            changeLanguage(it)
        }
        binding.rvLocalizationLanugages.adapter = adapter
        adapter.updateSelectedLang(preferenceManager.getLang())
        binding.rvLocalizationLanugages.hasFixedSize()
    }

    private fun changeLanguage(modelLanguage: ModelLanguage) {
        preferenceManager.saveLang(modelLanguage.langShortType)
        (requireActivity() as MainActivity).changeLanguage(modelLanguage.langShortType)
        startActivity(MainActivity::class.java)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}