package com.example.sunpool.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.sunpool.databinding.FragmentHomeBinding
import com.google.gson.JsonParser
import com.google.gson.JsonParser.parseString
import java.text.DecimalFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Use this to store our response from any GET requests done in this fragment
    private var responseString: String? = null

    // All our TextViews that we'll need to fill in
    private var currentPoolText: TextView? = null
    private var networkHashrate: TextView? = null
    private var poolHashrate: TextView? = null

    private var minerPublicKey: TextView? = null

    private var numWorkers: TextView? = null
    private var currentHashrate: TextView? = null
    private var oneHourAvgHashrate: TextView? = null
    private var sixHourAvgHashrate: TextView? = null
    private var twentyFourHourAvgHashrate: TextView? = null
    private var invalidShares: TextView? = null

    private var lastHour: TextView? = null
    private var lastDay: TextView? = null
    private var lastSevenDays: TextView? = null
    private var lastThirtyDays: TextView? = null

    private var availableBalance: TextView? = null
    private var unconfirmedBalance: TextView? = null
    private var totalPaidBalance: TextView? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // TODO: Get icon dynamically?
        //  Can grab from https://beam.sunpool.top/assets/img/beam-logo-small.png

        currentPoolText = binding.currentPool
        networkHashrate = binding.networkHashrate
        poolHashrate = binding.poolHashrate

        minerPublicKey = binding.publicKey

        numWorkers = binding.numWorkers
        currentHashrate = binding.hashrateCurrent
        oneHourAvgHashrate = binding.hashrate1h
        sixHourAvgHashrate = binding.hashrate6h
        twentyFourHourAvgHashrate = binding.hashrate24h
        invalidShares = binding.invalidShares

        lastHour = binding.lastHour
        lastDay = binding.lastDay
        lastSevenDays = binding.last7Days
        lastThirtyDays = binding.last30Days

        availableBalance = binding.balanceAvailable
        unconfirmedBalance = binding.balanceUnconfirmed
        totalPaidBalance = binding.balanceTotalPaid

        // Get the pool hashrate and fill the textview
        doSimpleGet(
            "https://beam.sunpool.top/pool-info.php?pool-hash-rate-current",
            poolHashrate!!, " Sol/s"
        )

        // Get data for workers
        doComplexGet(
            // FIXME: Replace miner address with input from textview
            "https://beam.sunpool.top/api.php?query=miner-workers&miner=23671e9ac0c60d6c7411aa705ba10c8a2f206ab0814cecc456e2546c60baf606",
            "workers"
            )

        // TODO: Get data for earnings

        // Get data for balance
        doComplexGet(
            "https://beam.sunpool.top/api.php?query=miner-balances&miner=23671e9ac0c60d6c7411aa705ba10c8a2f206ab0814cecc456e2546c60baf606",
            "balance"
        )

        // FIXME: Get this dynamically from a somewhere
        // TODO: Allow switching between Grin and Beam pools
        currentPoolText?.text = "Beam"

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun doSimpleGet(urlString: String, textView: TextView, appendString: String) {
        val queue = Volley.newRequestQueue(this.activity)
        val stringRequest = StringRequest(Request.Method.GET, urlString,
            { response ->
                println("Response is: $response")
                textView.text = response + appendString
            },
            {
                println("Error")
            })
        queue.add(stringRequest)
    }

    private fun doComplexGet(urlString: String, flag: String) {
        val queue = Volley.newRequestQueue(this.activity)
        val stringRequest = StringRequest(Request.Method.GET, urlString,
            { response ->
                println("Response is: $response")

                when (flag) {
                    "workers" -> {
                        fillWorkersData(response)
                    }
                    "earnings" -> {

                    }
                    "balance" -> {
                        fillBalanceData(response)
                    }
                }
            },
            {
                println("Error")
            })
        queue.add(stringRequest)
    }

    private fun fillDataFromJson(textView: TextView, responseData: String, appendString: String?) {
        val jsonObject = parseString(responseData).asJsonObject

        if(textView == numWorkers) {
            // Do something specific...
        } else if(textView == oneHourAvgHashrate) {
            // Do something else...
        }
        // ... continue as needed
    }

    private fun fillWorkersData(responseData: String) {
        val jsonObject = parseString(responseData).asJsonObject
        val workersArray = jsonObject.get("data").asJsonArray

        // FIXME: Currently counts workers that are down... Add a check
        //  that takes into account last seen time. >120 seconds and do not count worker
        //  Or check workers down alert link and subtract num of names from array size
        numWorkers!!.text = workersArray.size().toString()

        var totalCurrentHashrate = 0.0
        var totalOneHourHashrate = 0.0
        var totalSixHourHashrate = 0.0
        var totalTwentyFourHourHashrate = 0.0
        var totalInvalidShares = 0

        (0 until workersArray.size()).forEach {
            totalCurrentHashrate += workersArray[it].asJsonObject.get("currentHashrate").asDouble
            totalOneHourHashrate += workersArray[it].asJsonObject.get("1hAvgHashrate").asDouble
            totalSixHourHashrate += workersArray[it].asJsonObject.get("6hAvgHashrate").asDouble
            totalTwentyFourHourHashrate += workersArray[it].asJsonObject.get("24hAvgHashrate").asDouble
            totalInvalidShares += workersArray[it].asJsonObject.get("invalidShares24h").asInt
        }

        val df = DecimalFormat("#####.##")
        currentHashrate!!.text = df.format(totalCurrentHashrate).toString() + " Sol/s"
        oneHourAvgHashrate!!.text = df.format(totalOneHourHashrate).toString() + " Sol/s"
        sixHourAvgHashrate!!.text = df.format(totalSixHourHashrate).toString() + " Sol/s"
        twentyFourHourAvgHashrate!!.text = df.format(totalTwentyFourHourHashrate).toString() + " Sol/s"
        invalidShares!!.text = totalInvalidShares.toString()
    }

    private fun fillEarningsData(responseData: String) {
        val jsonObject = parseString(responseData).asJsonObject
        val workersArray = jsonObject.get("data").asJsonArray


    }

    private fun fillBalanceData(responseData: String) {
        val jsonObject = parseString(responseData).asJsonObject
        val jsonData = jsonObject.get("data").asJsonObject

        availableBalance!!.text = jsonData.get("availableBalance").toString()
        unconfirmedBalance!!.text = jsonData.get("unconfirmedBalance").toString()
        totalPaidBalance!!.text = jsonData.get("totalPaid").toString()
    }
}