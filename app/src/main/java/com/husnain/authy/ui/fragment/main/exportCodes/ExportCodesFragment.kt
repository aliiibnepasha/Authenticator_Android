package com.husnain.authy.ui.fragment.main.exportCodes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.husnain.authy.R
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.data.room.tables.EntityTotp
import com.husnain.authy.databinding.FragmentExportCodesBinding
import com.husnain.authy.utls.CustomToast.showCustomToast
import com.husnain.authy.utls.gone
import com.husnain.authy.utls.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import com.husnain.authy.utls.popBack
import javax.inject.Inject

@AndroidEntryPoint
class ExportCodesFragment : Fragment() {
    private var _binding: FragmentExportCodesBinding? = null
    private val binding get() = _binding!!
    private var isWithEncryption = false
    @Inject lateinit var daoTotp: DaoTotp
    private val salt = "some_fixed_salt".toByteArray() // Fixed salt for simplicity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportCodesBinding.inflate(inflater, container, false)
        init()
        return binding.root
    }

    private fun init() {
        binding.lyDropDown.setOnClickListener { showCustomDropdownMenu(it) }
        binding.btnExportFile.setOnClickListener {
            if (isWithEncryption) {
                if (binding.edtPass.text.isNullOrEmpty()) {
                    showCustomToast("Password is required")
                } else {
                    openFilePicker()
                }
            } else {
                openFilePicker()
            }
        }

        binding.imgBack.setOnClickListener {
            popBack()
        }
    }

    private fun showCustomDropdownMenu(view: View) {
        val data = listOf("No Encryption", "Encrypt Everything")
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dropdown_menu, null)
        val popupWindow = PopupWindow(popupView, view.width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val listView = popupView.findViewById<ListView>(R.id.listView)
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, data)
        listView.setOnItemClickListener { _, _, position, _ ->
            isWithEncryption = position != 0
            binding.lyPassword.visibility = if (isWithEncryption) View.VISIBLE else View.GONE
            binding.tvTitle.text = data[position]
            popupWindow.dismiss()
        }
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
        if (requestCode == REQUEST_CODE_CREATE_FILE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let { exportTotpDetails(it) }
        }
    }

    private fun exportTotpDetails(uri: Uri) {
        lifecycleScope.launch {
            val totpDetails = getTotpDetailsFromDb()
            val json = createJsonFromTotpDetails(totpDetails)
            val dataToSave = if (isWithEncryption) encrypt(json, binding.edtPass.text.toString()) else json
            Log.d("data to save",dataToSave);
            writeToFile(uri, dataToSave)
        }
    }

    private suspend fun getTotpDetailsFromDb(): List<EntityTotp> = withContext(Dispatchers.IO) {
        daoTotp.getAllTotpData()
    }

    private fun createJsonFromTotpDetails(totpDetails: List<EntityTotp>): String {
        val jsonArray = JSONArray()
        for (detail in totpDetails) {
            val jsonObject = JSONObject()
            jsonObject.put("serviceName", detail.serviceName)
            jsonObject.put("secretKey", detail.secretKey)
            jsonArray.put(jsonObject)
        }
        return JSONObject().put("totpDetails", jsonArray).toString()
    }

    private suspend fun writeToFile(uri: Uri, data: String) = withContext(Dispatchers.IO) {
        requireContext().contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
        withContext(Dispatchers.Main){
            showCustomToast("Exported Successfully")
        }
    }

    private fun encrypt(data: String, password: String): String {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        }
        val encryptedData = cipher.doFinal(data.toByteArray(UTF_8))
        return "ENCRYPTED" + Base64.encodeToString(iv + encryptedData, Base64.DEFAULT)
    }


    companion object {
        const val REQUEST_CODE_CREATE_FILE = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
