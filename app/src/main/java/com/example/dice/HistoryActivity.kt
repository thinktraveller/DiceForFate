package com.example.dice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dice.databinding.ActivityHistoryBinding
import com.example.dice.ui.ResultAdapter

class HistoryActivity : ComponentActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ResultAdapter(emptyList()) { }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        HistoryStore.results.observe(this) { adapter.submitList(it) }

        binding.btnClearHistory.setOnClickListener { HistoryStore.clear() }
    }
}
