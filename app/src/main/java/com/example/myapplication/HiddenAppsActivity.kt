package com.example.myapplication

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.ActivityHiddenAppsBinding
import kotlinx.coroutines.launch

class HiddenAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiddenAppsBinding
    private lateinit var adapter: AppGridAdapter
    private val repository by lazy { AppRepository.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHiddenAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        refreshList()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AppGridAdapter(
            activity = this,
            onAppLongClick = { app, iconView ->
                iconView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                AppMenuHelper.showMenu(this, iconView, app) {
                    refreshList()
                }
            }
        )
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@HiddenAppsActivity, 4)
            this.adapter = this@HiddenAppsActivity.adapter
        }
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val hiddenApps = repository.getHiddenApps()
            adapter.submitList(hiddenApps)
            binding.emptyState.visibility = if (hiddenApps.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
