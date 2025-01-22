package com.husnain.authy.ui.fragment.main.webView

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.husnain.authy.databinding.FragmentWebViewBinding
import com.husnain.authy.utls.popBack

class WebViewFragment : Fragment() {
    private var _binding: FragmentWebViewBinding? = null
    private val binding get() = _binding!!
    val KEY_HEADER_TITLE = "headerTitle"
    val KEY_LINK_TO_LOAD = "linkToLoad"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWebViewBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupHeader()
        setupWebView()
        setupListeners()
    }

    private fun setupHeader() {
        val headerTitle = arguments?.getString(KEY_HEADER_TITLE) ?: ""
        binding.tvTitle.text = headerTitle
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val linkToLoad = arguments?.getString(KEY_LINK_TO_LOAD) ?: ""

        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                builtInZoomControls = true
                displayZoomControls = false // Hides the default zoom controls
                useWideViewPort = true // Enables wide viewport for better scaling
                loadWithOverviewMode = true // Scales content to fit the WebView
            }
            webViewClient = WebViewClient()
            webChromeClient = createWebChromeClient()
            loadUrl(linkToLoad)
        }
    }


    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                updateProgressIndicator(newProgress)
            }
        }
    }

    private fun updateProgressIndicator(progress: Int) {
        binding.linearProgressIndicator.apply {
            visibility = if (progress == 100) View.GONE else View.VISIBLE
            this.progress = progress
        }
    }

    private fun setupListeners() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}