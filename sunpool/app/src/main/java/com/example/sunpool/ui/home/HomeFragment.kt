package com.example.sunpool.ui.home

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.sunpool.databinding.FragmentHomeBinding
import com.google.gson.JsonParser.parseString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.lang.StringBuilder
import java.net.URL
import java.text.DecimalFormat
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // All our TextViews that we'll need to fill in
    private var networkHashrate: TextView? = null
    private var poolHashrate: TextView? = null

    private var minerPublicKey: EditText? = null
    private var refreshButton: Button? = null

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
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        networkHashrate = binding.networkHashrate
        poolHashrate = binding.poolHashrate

        minerPublicKey = binding.publicKey
        loadSharedPrefs()

        refreshButton = binding.refreshBtn
        refreshButton!!.setOnClickListener {
            refreshPressed()
        }

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

        fillNetworkData()
        doSimpleGet(
            "https://beam.sunpool.top/pool-info.php?pool-hash-rate-current",
            poolHashrate!!, " Sol/s"
        )

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSharedPrefs() {
        // TODO: Get shared preferences and check for miner public key
        //  If the entry exists, fill it in and run the fetch automatically
        val prefs = requireContext().getSharedPreferences("SUNPOOL_PREFS", MODE_PRIVATE)
        val loadedKey = prefs.getString("PUBLIC_KEY", null)

        minerPublicKey!!.setText(loadedKey)
        refreshPressed()
    }

    private fun refreshPressed() {
        fetchData()
        val view = this.activity?.currentFocus
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)

        // Save the public key for later so user doesn't have to enter it every time
        val editor = requireContext().getSharedPreferences("SUNPOOL_PREFS", MODE_PRIVATE).edit()
        editor.putString("PUBLIC_KEY", minerPublicKey!!.text.toString())
        editor.apply()
    }

    private fun fetchData() {
        fillNetworkData()
        doSimpleGet(
            "https://beam.sunpool.top/pool-info.php?pool-hash-rate-current",
            poolHashrate!!, " Sol/s"
        )

        // Use the miner public key value to fetch data
        val minerInfoUrl =
            "https://beam.sunpool.top/api.php?query=miner-workers&miner=" + minerPublicKey?.text
        val balanceInfoUrl =
            "https://beam.sunpool.top/api.php?query=miner-balances&miner=" + minerPublicKey?.text

        // Get data for workers
        doComplexGet(
            minerInfoUrl,
            "workers"
        )

        // Get data for earnings
        fillEarningsData()

        // Get data for balance
        doComplexGet(
            balanceInfoUrl,
            "balance"
        )
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

    private fun fillNetworkData() {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL("https://beam.sunpool.top")
            var reader: BufferedReader? = null
            val builder = StringBuilder()
            try {
                reader = BufferedReader(InputStreamReader(url.openStream(), "UTF-8"))
                for (line in reader.readLines()) {
                    builder.append(line.trim { it <= ' ' })
                }
            } finally {
                if (reader != null) try {
                    reader.close()
                } catch (logOrIgnore: IOException) {
                }
            }

//            var start = "Network:"
            var start = "Network: <br class=\"d-lg-none\" /><strongclass=\"text-nowrap\">"
            var end = " KS<span class=\"d-lg-none\">.</span>"
            var part = builder.substring(builder.indexOf(start) + start.length)
            var line = part.substring(0, part.indexOf(end))
            networkHashrate!!.text = line + " KSol/s"
        }
    }

    private fun fillWorkersData(responseData: String) {
        if (responseData == "") {
            Toast.makeText(context, "Missing miner public key", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = parseString(responseData).asJsonObject
        val workersArray = jsonObject.get("data").asJsonArray

        // FIXME: Currently counts workers that are down... Add a check
        //  that takes into account last seen time. >11 minutes and do not count worker
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
        twentyFourHourAvgHashrate!!.text =
            df.format(totalTwentyFourHourHashrate).toString() + " Sol/s"
        invalidShares!!.text = totalInvalidShares.toString()
    }

    private fun fillEarningsData() {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL("https://beam.sunpool.top/miner-stats.php?miner=" + minerPublicKey!!.text)
            var reader: BufferedReader? = null
            val builder = StringBuilder()
            try {
                reader = BufferedReader(InputStreamReader(url.openStream(), "UTF-8"))
                for (line in reader.readLines()) {
                    builder.append(line.trim { it <= ' ' })
                }
            } finally {
                if (reader != null) try {
                    reader.close()
                } catch (logOrIgnore: IOException) {
                }
            }

            val df = DecimalFormat("######.######")
            var start = "Last Hour Earnings:"
            var end = " Beam</span></td>"
            var part = builder.substring(builder.indexOf(start) + start.length)
            var line = part.substring(0, part.indexOf(end))
            var result = line.substring(67 until line.length)
            lastHour!!.text = df.format(result.toDouble()) + " Beam"

            // Last day earnings
            start = "in the last day\">"
            end = " Beam</abbr></span></td>"
            part = builder.substring(builder.indexOf(start) + start.length)
            line = part.substring(0, part.indexOf(end))
            lastDay!!.text = df.format(line.toDouble()) + " Beam"

            // Last 7 days earnings
            start = "in the last 7 days\">"
            end = " Beam</abbr></span></td>"
            part = builder.substring(builder.indexOf(start) + start.length)
            line = part.substring(0, part.indexOf(end))
            lastSevenDays!!.text = df.format(line.toDouble()) + " Beam"

            // Last 30 days earnings
            start = "in the last 30 days\">"
            end = " Beam</abbr></span></td>"
            part = builder.substring(builder.indexOf(start) + start.length)
            line = part.substring(0, part.indexOf(end))
            lastThirtyDays!!.text = df.format(line.toDouble()).toString() + " Beam"
        }
    }

    private fun fillBalanceData(responseData: String) {
        if (responseData == "") {
            // Output to alert user of problem handled in fillWorkersData since it is called first
            return
        }

        val jsonObject = parseString(responseData).asJsonObject
        val jsonData = jsonObject.get("data").asJsonObject

        val df = DecimalFormat("######.######")
        availableBalance!!.text =
            df.format(jsonData!!.get("availableBalance").asDouble).toString() + " Beam"
        unconfirmedBalance!!.text =
            df.format(jsonData!!.get("unconfirmedBalance").asDouble).toString() + " Beam"
        totalPaidBalance!!.text =
            df.format(jsonData!!.get("totalPaid").asDouble).toString() + " Beam"
    }
}