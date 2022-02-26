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
import android.graphics.Color
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBindings
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
import java.io.File
import java.lang.NullPointerException

//import com.example.presentation.VideoViewActivity;
class MainFragment : Fragment() {
    private var binding: FragmentMainBinding? = null
    var picker: NumberPicker? = null
    var pickerVals = arrayOf("0", "1", "2", "-1", "-2")
    var bttstart: Button? = null
    var videolist: ListView? = null
    var adapter: VideoListAdapter<String?>? = null
    var link: String? = null
    var iotd: String? = null
    lateinit var listItem: Array<String?>
    lateinit var listLink: Array<String?>

    var isexit:Int? = null
    var jsonArray: JSONArray? = null
    var jobj: JSONObject? = null
    var jsonbody: String? = null
    var ytlink: String? = null
    var videodetail: TextView? = null
    var reallink:String? = null
    var bt_onlinevideo:Button?= null
    var bt_offlinevideo:Button?= null
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
    var isonline = 1
    val accepted_extention = listOf("avi", "mp4")
    var pos = 0
    var imgdownload:View? = null
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
                    listLink = arrayOfNulls(jsonArray!!.length())
                    for (i in 0 until jsonArray!!.length()) {
                        try {
                            listItem[i] = jsonArray!!.getJSONObject(i)["name"].toString()
                            listLink[i] = jsonArray!!.getJSONObject(i)["url"].toString()
                        } catch (jsonException: JSONException) {
                            jsonException.printStackTrace()
                        }
                    }
                    adapter = VideoListAdapter( requireActivity(), listItem.toList().filterNotNull().toTypedArray(),
                        listLink,listLink )
                } else Log.e("Files", "Folder not found:")
                videolist!!.adapter = adapter

                videolist!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                  if(isonline ==1){
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
                      }else {
                          Toast.makeText(activity, "Please select video!", Toast.LENGTH_LONG).show()
                      }
                      pos = position
                      Log.i("Select:", link!! +" "+ ytlink+" "+reallink )
                  }
                  else{
                      link = parent.getItemAtPosition(position) as String
                      pos = position
                  }
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

        bt_onlinevideo = binding!!.btOnlinevideolist
        bt_offlinevideo = binding!!.btLocalvideolist

        bt_offlinevideo!!.setOnClickListener {
            bt_offlinevideo?.setBackgroundColor(getResources().getColor(R.color.shadow))
            bt_onlinevideo?.setBackgroundColor(getResources().getColor(R.color.white))
            isonline = 0
            val mydir: File = File(path)
            val lister: File = mydir?.getAbsoluteFile()
            var i = 0

            listItem = arrayOfNulls<String>(lister?.length().toInt())
            if (lister != null) {
                try {
                    for (list in lister?.list()) {
                        //
                        if (list != null) {
                            if (list.endsWith(".mp4") || list.endsWith(".avi")) {
                                if (list != null) {
                                    listItem!![i] = list.toString().trimStart()
                                    i++
                                }
                            }
                        }
                    }
                }
            catch (e: NullPointerException) {
                listItem = kotlin.arrayOf("")
                }
            }
            else{
                listItem = kotlin.arrayOf("")
            }
            adapter = VideoListAdapter( requireActivity(), listItem.toList().filterNotNull().toTypedArray().toList().filterNotNull().toTypedArray(),listItem.toList().filterNotNull().toTypedArray().toList().filterNotNull().toTypedArray(),listLink )
                videolist!!.adapter = adapter
        }

        bt_onlinevideo!!.setOnClickListener{
            bt_offlinevideo?.setBackgroundColor(getResources().getColor(R.color.white))
            bt_onlinevideo?.setBackgroundColor(getResources().getColor(R.color.shadow))
            isonline = 1
            if (jsonArray != null) {
                listItem = arrayOfNulls(jsonArray!!.length())
                for (i in 0 until jsonArray!!.length()) {
                    try {
                        listItem[i] = jsonArray!!.getJSONObject(i)["name"].toString()
                    } catch (jsonException: JSONException) {
                        jsonException.printStackTrace()
                    }
                }
                adapter = VideoListAdapter( requireActivity(), listItem.toList().filterNotNull().toTypedArray(),listLink,listLink )
            } else Log.e("Files", "Folder not found:")
            videolist!!.adapter = adapter

        }
        //getting file from storage
//        try {
//            File directory = new File(path);
//            File[] files = directory . listFiles ();
//            FragmentManager fm = new FragmentManager() {
//                @Nullable
//                @Override
//                public Fragment findFragmentById(int id) {
//                    return super.findFragmentById(id);
//                }
//            };
//

//            if (files.length != 0) {
//                listItem = new String [files.length];
//
//                for (int i = 0; i < jsonArray.length(); i++)
//                {
//                    Log.d("Files", "FileName:" + files[i].getName());
////                    listItem[i] = files[i].getName();
//                    listItem[i] = jsonArray.getJSONObject(i).toString();
////                }
//                adapter = new ArrayAdapter < String >(
//                    getActivity(), android.R.layout.simple_list_item_1,
//                    android.R.id.text1,
//                    listItem
//                );
//
//            } else Log.e("Files", "Folder not found:");
//        } catch (Exception e) {
//            Log.e("Files", "Folder not found:");
//        }
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.btStartview.setOnClickListener {
            val intent = Intent(activity, ViewActivity::class.java)
            if (isonline == 0){
                intent.putExtra("position",  pos.toString())
                intent.putExtra("savedlist",listItem.joinToString())
                intent.putExtra("videolist",(activity as MainActivity?)!!.videolist )
                intent.putExtra("linklist",listLink.joinToString() )
                intent.putExtra("link",path + "/"+link )
                intent.putExtra("iotd", pickerVals.get(picker!!.value))
                intent.putExtra("isonline", isonline.toString())
                startActivity(intent)
                Log.i("Mainview:", "startvideo")
            }
            else{
                if (isexit!=null){
                    intent.putExtra("videolist",(activity as MainActivity?)!!.videolist )
                    intent.putExtra("link", reallink)
                    intent.putExtra("linklist",listLink.joinToString() )
                    intent.putExtra("position",  pos.toString())
                    intent.putExtra("iotd", pickerVals.get(picker!!.value))
                    intent.putExtra("isonline", isonline.toString())
                    startActivity(intent)
                    Log.i("Mainview:", "startvideo")
                }
                else{
                    Toast.makeText(activity, "No video found!", Toast.LENGTH_LONG).show()
                }
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
                    Toast.makeText(activity, "Video selected! ", Toast.LENGTH_LONG).show()
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