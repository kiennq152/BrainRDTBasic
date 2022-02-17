package com.example.brainrdtbasic

import android.annotation.SuppressLint
import org.json.JSONArray
import org.json.JSONObject
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.pm.ActivityInfo
import android.os.Environment
import org.json.JSONException
import android.widget.AdapterView.OnItemClickListener
import android.content.Intent
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.example.brainrdtbasic.databinding.FragmentMainBinding
import com.example.brainrdtbasic.network.StackOverflowAPI
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import com.shawnlin.numberpicker.NumberPicker;

//import com.example.presentation.VideoViewActivity;
class MainFragment : Fragment() {
    private var binding: FragmentMainBinding? = null
    var picker: NumberPicker? = null
    var pickerVals = arrayOf("0", "1", "2", "-1", "-2")
    var bttstart: Button? = null
    var videolist: ListView? = null
    var adapter: ArrayAdapter<String?>? = null
    var link: String? = null
    var iotd: String? = null
    lateinit var listItem: Array<String?>
    var isexit:Int? = null
    var jsonArray: JSONArray? = null
    var jobj: JSONObject? = null
    var jsonbody: String? = null
    var ytlink: String? = null
    var videodetail: TextView? = null
    var reallink:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Bundle bundle = getArguments();
//        if (bundle != null) {
//            jsonbody = bundle.getString("videos","");
//            //Json mess reading
//            try {
//                jobj = new JSONObject(jsonbody);
//                jsonArray = jobj.getJSONArray("videos");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            for (int i=0;i< jsonArray.length();i++) {
//                try {
//                    JSONObject subobj = jsonArray.getJSONObject(i);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }


        // Khởi tạo Retrofit để gán API ENDPOINT (Domain URL) cho Retrofit 2.0
        val retrofit = Retrofit.Builder()
            .baseUrl("https://iotd.terasoftvn.com") // Sử dụng GSON cho việc parse và maps JSON data tới Object
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Khởi tạo các cuộc gọi cho Retrofit 2.0

        // Khởi tạo các cuộc gọi cho Retrofit 2.0
        val stackOverflowAPI: StackOverflowAPI = retrofit.create(
            StackOverflowAPI::class.java)
        val call1: Call<ResponseBody> =
            stackOverflowAPI.getVideolist("user0", "0", "10")
        call1.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                Log.i("Response code:", response.toString())
                if (response.isSuccessful) {
                    try {
                        val sbody =
                            response.body()!!
                                .string() // keep this line before working with JSON body
                        Log.i("Response body:", sbody)

                        //Json mess forwarding
                        val bundle = Bundle()
                        bundle.putString("videos", sbody)
                        val mainFragment = MainFragment()
                        mainFragment.arguments = bundle
                        (activity as MainActivity?)!!.videolist = sbody

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("Response body:", e.toString())
                    }
                } else {
                }
                //Json mess reading
                if ((activity as MainActivity?)!!.videolist != null) {
                    jsonbody = (activity as MainActivity?)!!.videolist
                    try {
                        jobj = JSONObject(jsonbody)
                        jsonArray = jobj!!.getJSONArray("videos")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                if (jsonArray != null) {
                    listItem = arrayOfNulls(jsonArray!!.length())
                    for (i in 0 until jsonArray!!.length()) {
                        try {
                            listItem[i] = jsonArray!!.getJSONObject(i)["name"].toString()
                        } catch (jsonException: JSONException) {
                            jsonException.printStackTrace()
                        }
                    }
                    adapter = ArrayAdapter(
                        requireActivity(), android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        listItem
                    )
                } else Log.e("Files", "Folder not found:")
                videolist!!.adapter = adapter
                videolist!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                    link = parent.getItemAtPosition(position) as String
                    try {
//                videodetail!!.text =
//                    "Time:" + jsonArray!!.getJSONObject(position)["current_time"].toString() + "/n" +
//                            "Duration:" + jsonArray!!.getJSONObject(position)["time"].toString() + "/n" +
//                            "Feedback:"
                        ytlink = jsonArray!!.getJSONObject(position)["url"].toString()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    if (ytlink!=null) {
                        loadYoutube(ytlink!!)
//                reallink = "https://iotd.terasoftvn.com/protectedVideos/video001.mp4"
//                reallink  = "https://rr4---sn-4g5e6nz7.googlevideo.com/videoplayback?expire=1645089925&ei=JcANYprGI9a28gPjp7GIAg&ip=216.131.114.126&id=o-APUsTUogZGKU-9YAd3d6qJxA_xp0hBqba05Pn4JnUzrv&itag=22&source=youtube&requiressl=yes&mh=UE&mm=31%2C26&mn=sn-4g5e6nz7%2Csn-2gb7sn7k&ms=au%2Conr&mv=m&mvi=4&pl=24&initcwndbps=327500&vprv=1&mime=video%2Fmp4&ns=FDPA36KduhlSAF20AK6k9g0G&cnr=14&ratebypass=yes&dur=242.903&lmt=1645030044736074&mt=1645067847&fvip=4&fexp=24001373%2C24007246&beids=23886216&c=WEB&txp=5432434&n=yzRemk_9Ic21uP_lAFD&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cns%2Ccnr%2Cratebypass%2Cdur%2Clmt&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRQIhALaPmvnmdLyYGFJD5rAIJZ70eOSG9jx963QATcGbq-p_AiBlJUpuXoOvHRGz-2xV5Syz9gNvyRcYaC6lLltPAtBkQw%3D%3D&sig=AOq0QJ8wRgIhALMtwgsMfTn1sAkkKSmRydfE51136jnU0kDBg5X5Ru2bAiEA0bgOH4IwmXJ6VfH1ebnauxPHHo-iBKxj9FviZbZsM7k%3D&title=Chelsea%20unleash%20exciting%20trio%20in%20Premier%20League%20after%2030%20minute%20Club%20World%20Cup%20final%20trial%20run"
//                reallink = path +"/video001.mp4"


                    }else {
                        Toast.makeText(activity, "Please select video!", Toast.LENGTH_LONG).show()
                    }

                    Log.i("Select:", link!! +" "+ ytlink+" "+reallink )
                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    activity!!.applicationContext,
                    "Loading Video Fail",
                    Toast.LENGTH_SHORT
                )
            }
        })
//
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = FragmentMainBinding.inflate(inflater, container, false)

        videolist = binding!!.videoList
        videodetail = binding!!.videoDetail

        picker = binding!!.iotdsel
        picker!!.setMaxValue(pickerVals.size-1);
        picker!!.setMinValue(0);
        picker!!.setDisplayedValues(pickerVals);



        //getting file from storage
//        try {
//            File directory = new File(path);
//            File[] files = directory.listFiles();
//            FragmentManager fm = new FragmentManager() {
//                @Nullable
//                @Override
//                public Fragment findFragmentById(int id) {
//                    return super.findFragmentById(id);
//                }
//            };
//
//            if (files.length !=0)
//            {
//                listItem = new String[files.length];
//
//                for (int i = 0; i < jsonArray.length(); i++)
//                {
//                    Log.d("Files", "FileName:" + files[i].getName());
////                    listItem[i] = files[i].getName();
//                    listItem[i] = jsonArray.getJSONObject(i).toString();
//                }
//                adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,
//                        android.R.id.text1,
//                        listItem);
//
//            }else Log.e("Files", "Folder not found:");
//        }
//        catch (Exception e){
//            Log.e("Files", "Folder not found:");
//        }
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath

        binding!!.btStartview.setOnClickListener {
            val intent = Intent(activity, ViewActivity::class.java)
            if (isexit!=null){
                intent.putExtra("videolist",(activity as MainActivity?)!!.videolist )
                intent.putExtra("link", reallink)
                intent.putExtra("iotd", pickerVals.get(picker!!.value))
                startActivity(intent)

                Log.i("Mainview:", "startvideo")
            }
            else{
                Toast.makeText(activity, "No video found!", Toast.LENGTH_LONG).show()
            }

        }

    }

    private fun loadYoutube(link: String): String? {
        var downloadUrl: String? = null
        val mediaurl = arrayOf<String?>(null)
        object : YouTubeExtractor(activity as MainActivity) {
            @SuppressLint("StaticFieldLeak")
            public override fun onExtractionComplete(
                ytFiles: SparseArray<YtFile>?,
                vMeta: VideoMeta?
            ) {
                if (ytFiles != null) {
                    isexit = 1
                    val itag = 18
                    downloadUrl = ytFiles[itag].url
                    val i = Log.i("youtubeplayer:", "Opening$downloadUrl")
                    mediaurl[0] = downloadUrl
                    reallink = ytFiles[itag].url
                }else{
                    return;
                }

            }
        }.extract(link, true, true)
        return downloadUrl
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}