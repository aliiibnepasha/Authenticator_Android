package com.husnain.authy.ui.fragment.main.backup

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.husnain.authy.data.room.daos.DaoTotp
import com.husnain.authy.databinding.FragmentBackUpBinding
import com.husnain.authy.preferences.PreferenceManager
import com.husnain.authy.utls.popBack
import com.husnain.authy.workers.SyncJobService
import dagger.hilt.android.AndroidEntryPoint
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
        binding.tvLastSyncTime.text = preferenceManager.getLastSyncTime()
    }

    private fun setOnClickListener() {
        binding.imgBack.setOnClickListener {
            popBack()
        }
        binding.btnSyncNow.setOnClickListener {
            val userId = auth.currentUser?.uid!!
            startSyncJob(userId)
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
            Log.d("JobScheduler", "Job scheduled successfully")
        } else {
            Log.e("JobScheduler", "Job scheduling failed")
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}