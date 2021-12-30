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
    private var sharesAccepted: TextView? = null
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
        sharesAccepted = binding.sharesAccepted
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

        // FIXME: Get this dynamically from a list/array
        currentPoolText?.text = "Beam"

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun doSimpleGet(urlString: String, textView: TextView, appendString: String) {
        val url = urlString

        val queue = Volley.newRequestQueue(this.activity)
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                println("Response is: $response")
                textView.text = response + appendString
            },
            {
                println("Error")
            })
        queue.add(stringRequest)
    }

    private fun doComplexGet(urlString: String) {
        val url = urlString

        val queue = Volley.newRequestQueue(this.activity)
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                println("Response is: $response")
                // TODO: Figure out a way to return the response...
                //  We don't want to have a bunch of redundant code for just GET requests
            },
            {
                println("Error")
            })
        queue.add(stringRequest)
    }
}