package com.husnain.authy.ui.fragment.main.exportCodes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentExportCodesBinding
import com.husnain.authy.ui.fragment.main.settings.SettingFragment
import com.husnain.authy.ui.fragment.main.settings.SettingFragment.Companion
import com.husnain.authy.ui.fragment.main.settings.SettingFragment.Companion.REQUEST_CODE_CREATE_FILE
import com.husnain.authy.ui.fragment.main.settings.SettingFragment.Companion.REQUEST_CODE_OPEN_FILE
import com.husnain.authy.utls.Flags
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class ExportCodesFragment : Fragment(){
    private var _binding: FragmentExportCodesBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var daoTotp: DaoTotp

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExportCodesBinding.inflate(inflater, container, false)
        inIt()
        return binding.root
    }

    private fun inIt() {
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.lyDropDown.setOnClickListener {
            showCustomDropdownMenu(it)
        }
        binding.btnExportFile.setOnClickListener {
            openFilePicker()
        }
    }

    private fun showCustomDropdownMenu(view: View) {
        val data = listOf("No Encryption", "Encrypt Everything")

        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dropdown_menu, null)
        val popupWindow = PopupWindow(popupView, view.width, WindowManager.LayoutParams.WRAP_CONTENT, true)

        val listView = popupView.findViewById<ListView>(R.id.listView)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, data)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            binding.tvTitle.text = data[position]
            popupWindow.dismiss()
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(view, 0, 20)
    }


    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "totp_details.json")
        }
        startActivityForResult(intent, REQUEST_CODE_CREATE_FILE)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SettingFragment.REQUEST_CODE_CREATE_FILE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let { exportTotpDetails(it) }
        }
    }

    private fun exportTotpDetails(uri: Uri) {
        lifecycleScope.launch {
            val totpDetails = getTotpDetailsFromDb()
            val json = createJsonFromTotpDetails(totpDetails)
            writeToFile(uri, json)
        }
    }

    private suspend fun getTotpDetailsFromDb(): List<EntityTotp> {
        return withContext(Dispatchers.IO) {
            daoTotp.getAllTotpData()
        }
    }

    private fun createJsonFromTotpDetails(totpDetails: List<EntityTotp>): String {
        val jsonArray = JSONArray()
        for (detail in totpDetails) {
            val jsonObject = JSONObject()
            jsonObject.put("serviceName", detail.serviceName)
            jsonObject.put("secretKey", detail.secretKey)
            jsonArray.put(jsonObject)
        }
        val jsonObject = JSONObject()
        jsonObject.put("totpDetails", jsonArray)
        return jsonObject.toString()
    }

    private suspend fun writeToFile(uri: Uri, json: String) {
        withContext(Dispatchers.IO) {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                outputStream.flush()
            }
        }
    }
    companion object {
        const val REQUEST_CODE_CREATE_FILE = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}