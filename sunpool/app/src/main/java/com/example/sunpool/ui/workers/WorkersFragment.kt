package com.example.sunpool.ui.workers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.sunpool.R
import com.example.sunpool.Utils
import com.example.sunpool.databinding.FragmentWorkersBinding
import com.google.gson.JsonParser
import java.text.DecimalFormat

class WorkersFragment : Fragment() {

    private lateinit var workersViewModel: WorkersViewModel
    private var _binding: FragmentWorkersBinding? = null

    private var minerPublicKey: String? = null
    private var linearLayout: LinearLayout? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        workersViewModel =
            ViewModelProvider(this).get(WorkersViewModel::class.java)

        _binding = FragmentWorkersBinding.inflate(inflater, container, false)
        val root: View = binding.root
        linearLayout = binding.workersLinearLayout

        minerPublicKey = Utils().loadSavedKey(requireContext().applicationContext)
        if (minerPublicKey != "" && minerPublicKey != null) {
            getMinerInfo()
        }

        return root
    }

    private fun getMinerInfo() {
        val minerInfoUrl =
            "https://beam.sunpool.top/api.php?query=miner-workers&miner=$minerPublicKey"
        doComplexGet(minerInfoUrl)
    }

    private fun doComplexGet(urlString: String) {
        val queue = Volley.newRequestQueue(this.activity)
        val stringRequest = StringRequest(
            Request.Method.GET, urlString,
            { response ->
                println("Response is: $response")
                fillWorkersData(response)
            },
            {
                println("Error")
            })
        queue.add(stringRequest)
    }

    private fun fillWorkersData(responseData: String) {
        // TODO: Add an input for miner public key on this page too?
        if (responseData == "") {
            Toast.makeText(context, "Missing miner public key", Toast.LENGTH_SHORT).show()
            return
        }

        val df = DecimalFormat("######.######")
        val jsonObject = JsonParser.parseString(responseData).asJsonObject
        val workersArray = jsonObject.get("data").asJsonArray

        var name = ""
        var time = 0
        var lastSeen = 0
        var elapsedTime = 0
        var currentHashrate = 0.0
        var oneHourHashrate = 0.0
        var sixHourHashrate = 0.0
        var twentyFourHourHashrate = 0.0
        var invalidShares = 0

        (0 until workersArray.size()).forEach {
            // Loop through the array of workers
            name = workersArray[it].asJsonObject.get("worker").asString
            time = workersArray[it].asJsonObject.get("time").asInt
            lastSeen = workersArray[it].asJsonObject.get("lastSeen").asInt
            currentHashrate = workersArray[it].asJsonObject.get("currentHashrate").asDouble
            oneHourHashrate = workersArray[it].asJsonObject.get("1hAvgHashrate").asDouble
            sixHourHashrate = workersArray[it].asJsonObject.get("6hAvgHashrate").asDouble
            twentyFourHourHashrate = workersArray[it].asJsonObject.get("24hAvgHashrate").asDouble
            invalidShares = workersArray[it].asJsonObject.get("invalidShares24h").asInt

            // Get the minutes since last seen
            elapsedTime = (time - lastSeen) / 60

            requireActivity().runOnUiThread {
                val view = layoutInflater.inflate(R.layout.worker_layout, null)
                view.findViewById<TextView>(R.id.worker_name).text = name

                val minuteStr = if (elapsedTime > 1) "minutes ago" else "minute ago"
                view.findViewById<TextView>(R.id.last_seen).text = "$elapsedTime $minuteStr"
                view.findViewById<TextView>(R.id.hashrate_current).text =
                    "${df.format(currentHashrate)} Sol/s"
                view.findViewById<TextView>(R.id.hashrate_1h).text =
                    "${df.format(oneHourHashrate)} Sol/s"
                view.findViewById<TextView>(R.id.hashrate_6h).text =
                    "${df.format(sixHourHashrate)} Sol/s"
                view.findViewById<TextView>(R.id.hashrate_24h).text =
                    "${df.format(twentyFourHourHashrate)} Sol/s"
                view.findViewById<TextView>(R.id.invalid_shares).text = invalidShares.toString()
                binding.workersLinearLayout.addView(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}