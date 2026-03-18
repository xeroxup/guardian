package com.guardian.app.ui.screens

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guardian.app.R
import com.guardian.app.data.BlacklistedApp
import com.guardian.app.databinding.FragmentBlacklistBinding
import com.guardian.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BlacklistFragment : Fragment() {
    
    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GuardianViewModel by activityViewModels()
    private lateinit var adapter: BlacklistAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeData()
    }
    
    private fun setupRecyclerView() {
        adapter = BlacklistAdapter { app ->
            viewModel.removeFromBlacklist(app.id)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }
    
    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_app, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.appNameInput)
        val packageInput = dialogView.findViewById<EditText>(R.id.packageInput)
        
        AlertDialog.Builder(requireContext(), R.style.Theme_Guardian)
            .setTitle(getString(R.string.add_app))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = nameInput.text.toString().trim()
                val packageName = packageInput.text.toString().trim()
                if (name.isNotEmpty() && packageName.isNotEmpty()) {
                    viewModel.addToBlacklist(name, packageName)
                } else {
                    Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.blacklist.collectLatest { apps ->
                adapter.submitList(apps)
                binding.emptyView.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
