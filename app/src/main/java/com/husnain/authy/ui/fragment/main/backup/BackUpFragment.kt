package com.husnain.authy.ui.fragment.main.backup

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.databinding.FragmentBackUpBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.ui.fragment.main.backup.workers.SyncJobService
import com.husnain.authy.utls.popBack
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BackUpFragment : Fragment() {
    private var _binding: FragmentBackUpBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var jobScheduler: JobScheduler
    @Inject lateinit var daoTotp: DaoTotp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBackUpBinding.inflate(inflater, container, false)
        jobScheduler = requireContext().getSystemService(JobScheduler::class.java)
        inIt()
        return binding.root
    }

    private fun inIt() {
        inItUi()
        setOnClickListener()
    }

    private fun inItUi() {
        inItLastSyncTime()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnSyncNow.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null){
                startSyncJob(userId)
            }
        }
    }

    private fun startSyncJob(userId: String) {

        val extras = PersistableBundle().apply {
            putString("userId", userId)
        }

        val jobInfo = JobInfo.Builder(123, ComponentName(requireContext(), SyncJobService::class.java))
            .setExtras(extras)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .build()

        val result = jobScheduler.schedule(jobInfo)

        if (result == JobScheduler.RESULT_SUCCESS) {
            setCurrentTime()
        }
    }

    private fun setCurrentTime(){
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("d MMM yyyy - hh:mm a", Locale.ENGLISH)
        val formattedTime = formatter.format(Date(currentTime))
        binding.tvLastSyncTime.text = "Last sync: $formattedTime"
    }

    private fun inItLastSyncTime(){
        var lastSyncDateTime = preferenceManager.getLastSyncTime()
        if (lastSyncDateTime?.isEmpty() == true){
            binding.tvLastSyncTime.text = "Last sync: --"
        }else{
            binding.tvLastSyncTime.text = "Last sync: $lastSyncDateTime"
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}