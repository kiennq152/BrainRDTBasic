package com.example.brainrdtbasic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import android.os.Bundle as Bundle1
import android.view.*
import androidx.annotation.RequiresApi
import com.example.brainrdtbasic.opengl.IGetDeviceRotation
import com.example.brainrdtbasic.opengl.IStereoVideoView
import com.example.brainrdtbasic.opengl.StereoIotdCameraView
import com.example.brainrdtbasic.opengl.StereoIotdVideoView
import android.media.MediaPlayer.OnCompletionListener
import android.os.SystemClock
import android.widget.*
import android.view.ViewConfiguration
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import java.util.*
import android.content.IntentFilter
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.support.v4.media.session.PlaybackStateCompat


lateinit var staticMediaPlayer: MediaPlayer

@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ViewActivity : AppCompatActivity(), IGetDeviceRotation, TextureView.SurfaceTextureListener {
    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    //inter pupil distance
    val ipd : Float? = 63F
    val w : Float?  = 50F
    var curposition: Int = 0
    var isvideo: Int = 0
    var iscamera: Int = 0
    var videoSource = "https://iotd.terasoftvn.com/protectedVideos/video001.mp4"
    var dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
    var chro: Chronometer? = null
    var cnt:Long? = 0
    var savedList:Array<String?>? = null
    var linkList:Array<String?>? = null

    private var speechRecognizer: SpeechRecognizer? = null
    var speechIntent: Intent? = null
    var txtview: TextView? = null
    var layout:FrameLayout? = null
    var videoUri = Uri.parse(videoSource)
    var iotd:String? = null
    var videolist:String? = null
    val rootView:ViewGroup by lazy { findViewById(android.R.id.content) }
    var mediaPlayer: MediaPlayer? = null
    var idx:String? = null
    var isonline:Int? =null
    var isask:Int? =null


    var t1: TextToSpeech? = null

    //    val mediaPlayer: MediaPlayer by lazy {
//        MediaPlayer.create(applicationContext, videoUri).apply {
//            //isLooping = true
//            setOnErrorListener { mp, what, extra ->
//                Log.e("Green", "Green mediaplayer error: $what | $extra")
//                false
//            }
//            setOnCompletionListener {
//                Log.e("Green", "Green mediaplayer complete")
//            }
//        }
//    }
    var stereoVideoView: IStereoVideoView? = null

    var videoWidthMm = 10f
    var videoDistanceMm = 10f

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle1?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_view)

        //Add media button listener

        val mediaSession = MediaSession(this,TAG) // Debugging tag, any string
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        val extras = intent.extras
        if (extras!=null) {
            idx = extras.getString("position").toString()
            var url:String = extras.getString("link").toString()
            iotd = extras.getString("iotd").toString()
            videolist = extras.getString("videolist").toString()
            isonline = extras.getString("isonline")?.toInt()
            videoSource = url;
            var sList: Array<String?>? = (extras.getString("savedlist")?.split(",")?.toTypedArray())
            savedList = sList?.filterNotNull()?.toTypedArray()
            sList = (extras.getString("linklist")?.split(",")?.toTypedArray())
            linkList =sList?.filterNotNull()?.toTypedArray()

                if (videoSource!=null) {
                videoUri = Uri.parse(videoSource)
            }
            else{
                Log.e(this.toString(),"No URL found")
                Toast.makeText(this, "No video found!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
//
        ActivityCompat.requestPermissions(
            this,PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE
        )
        mediaPlayer = MediaPlayer.create(applicationContext, videoUri)
        if (mediaPlayer != null) {
            staticMediaPlayer = mediaPlayer as MediaPlayer
        }

        Log.d("Green", "Green external storage file: " + videoUri)

        txtview = findViewById<TextView>(R.id.txtview)
        layout = findViewById<FrameLayout>(R.id.frm_video)!!
        chro = findViewById<Chronometer>(R.id.chroma)
        chro!!.setBase(SystemClock.elapsedRealtime());

        chro?.setOnChronometerTickListener {
            if (cnt!! >1800){
                if (iscamera == 0 ){
                    mediaPlayer?.stop()
                    speechRecognizer!!.stopListening()
                    speechRecognizer!!.destroy()
                    val i = Intent(this@ViewActivity, MainActivity::class.java)
                    i.putExtra("fragment","6");
                    i.putExtra("videolist",videolist);
                    startActivity(i)
                }
            }else
            {
                if (iscamera == 0 ){
                    if (mediaPlayer!=null && mediaPlayer!!.isPlaying){
                        cnt = cnt!!+1
                        txtview!!.setText(cnt.toString())
                    }
                }
            }
            val elapsedMillis: Long = SystemClock.elapsedRealtime() - chro!!.getBase()
            if(elapsedMillis!!>=mediaPlayer!!.duration){
                var name = videoSource.substring(videoSource.lastIndexOf("/") + 1)
                 var pos = idx?.toInt()
                if (pos != null) {
                    pos = pos + 1
                }
                if (isonline ==0){
                    var video = savedList?.get(pos!!)
//                var video = idx?.plus(1)?.let { it1 -> savedList?.get(it1) }
                    if (video?.trimStart() == "null"){
                        pos = 0
                        videoUri = Uri.parse(dir + "/" + savedList?.get(pos!!))
                        mediaPlayer =
                            MediaPlayer.create(this, videoUri);
                    }else {
                        videoUri = Uri.parse(dir + "/" + video?.trimStart())
                        mediaPlayer =MediaPlayer.create(this, videoUri);
                    }
                }else{
                    var video = linkList?.get(pos!!)
//                var video = idx?.plus(1)?.let { it1 -> savedList?.get(it1) }
                    if (video?.trimStart() == null){
                        pos = 0
                        videoUri = Uri.parse(dir + "/" + linkList?.get(pos!!))
                        mediaPlayer = MediaPlayer.create(this, videoUri);
                    }else {
                        videoUri = Uri.parse(dir + "/" + video?.trimStart())
                        mediaPlayer =MediaPlayer.create(this, videoUri);
                    }
                }

                idx = pos.toString()
                changetoVideo()
                chro!!.setBase(SystemClock.elapsedRealtime());
                t1?.speak("계속하고십습니까?",TextToSpeech.QUEUE_FLUSH, null);
                isask=1

                speechRecognizer?.startListening(speechIntent)
            }
        }
        chro!!.start()

        videoWidthMm = w!!
        videoDistanceMm = ipd!!
        updateVideoAttributes()

        // Check permission and start the camera
        if (allPermissionsGranted()) {
            start()
        }
        else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION
            )
        }

        //TTS setup
        t1 = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) {
                t1?.setLanguage(Locale.KOREA)
            }
        }

        speechRecognizer= SpeechRecognizer.createSpeechRecognizer(this@ViewActivity)
        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle1) {
                Log.i("Speech to text", "Ready")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEndOfSpeech() {
            }
            override fun onError(error: Int) {

            }
            override fun onResults(results: Bundle1) {
                val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.i("Speech to text", data!![0].toString())
                txtview!!.setText(data!![0].toString())
                when (data.get(0).toString()){
                    "video" -> {
                        changetoVideo()
                        if (mediaPlayer!=null){
                            mediaPlayer!!.seekTo(curposition)
//                            mediaPlayer.start()
                        }
                    }
                    "비디오" -> {
                        changetoVideo()
                        if (mediaPlayer!=null){
                            mediaPlayer!!.seekTo(curposition)
//                            mediaPlayer.start()
                        }
                    }
                    "camera" -> {
                        changetoCam()
                        if (mediaPlayer!=null){
                            mediaPlayer!!.pause()
                            curposition =mediaPlayer!!.currentPosition
                        }
                    }
                    "카메라" -> {
                        changetoCam()
                        if (mediaPlayer!=null){
                            mediaPlayer!!.pause()
                            curposition =mediaPlayer!!.currentPosition
                        }
                    }
                    "stop" -> {
                        if (mediaPlayer!=null){
                            mediaPlayer!!.stop()
                        }
                        speechRecognizer!!.stopListening()
                        speechRecognizer!!.destroy()
                        val i = Intent(this@ViewActivity, MainActivity::class.java)
                        i.putExtra("fragment","6");
                        i.putExtra("videolist",videolist);
                        startActivity(i)
                    }
                    "스탑" -> {
                        if (mediaPlayer!=null){
                            mediaPlayer!!.stop()
                        }
                        speechRecognizer!!.stopListening()
                        speechRecognizer!!.destroy()
                        val i = Intent(this@ViewActivity, MainActivity::class.java)
                        i.putExtra("fragment","6");
                        i.putExtra("videolist",videolist);
                        startActivity(i)
                    }
                    "네" ->{
                        if(isask==1){
                            if (mediaPlayer!=null){
                                mediaPlayer!!.stop()
                            }
                            speechRecognizer!!.stopListening()
                            speechRecognizer!!.destroy()
                            val i = Intent(this@ViewActivity, MainActivity::class.java)
                            isask=0
                            i.putExtra("fragment","6");
                            i.putExtra("videolist",videolist);
                            startActivity(i)
                        }
                    }

                    else -> {
                        t1?.speak("이해하지 못합니다!",TextToSpeech.QUEUE_FLUSH, null);
                        Log.i("Speech to text", data!![0].toString())
                    }
                }

            }

            override fun onPartialResults(partialResults: Bundle1) {}
            override fun onEvent(eventType: Int, params: Bundle1) {}
        })
        // setup speechrecognizer intent
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechIntent!!.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech to text")
        // reduce the delay of Internet quality
        // reduce the delay of Internet quality
        speechIntent!!.putExtra(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
            RecognizerIntent.EXTRA_PREFER_OFFLINE
        )
        mediaPlayer!!.setOnCompletionListener {
//            if ( cnt!!<1800)
//                {
//
//                    var name = videoSource.substring(videoSource.lastIndexOf("/") + 1)
//                    var idx = savedList?.indexOf(name)
//                    var video = idx?.plus(1)?.let { it1 -> savedList?.get(it1) }
//                    if (video?.trimStart() == "null"){
//
//                        idx = 0
//                        videoUri = Uri.parse(dir + "/" + savedList?.get(idx))
////                            mediaPlayer.setDataSource(dir+ "/" + savedList?.get(idx))
//                        mediaPlayer =
//                            MediaPlayer.create(this, Uri.parse(dir + "/" + savedList?.get(idx)));
//
//
//                    }else {
//
//                        videoUri = Uri.parse(dir + "/" + video)
////                        mediaPlayer.setDataSource(dir + "/" + video)
//                        mediaPlayer =MediaPlayer.create(this, Uri.parse(dir+ "/" + video));
//                    }
//
//                    changetoVideo()
//
//                }
//            else
//            {
//                mediaPlayer!!.release()
//                Toast.makeText(this, "30 minutes treatment time is finished!", Toast.LENGTH_LONG).show()
//                this.finish()
//            }
        }

    }

    private fun updateVideoAttributes() {
        val fmt = DecimalFormat("###,###,##0.00")

        stereoVideoView?.setVideoAttributes(videoWidthMm, videoDistanceMm)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                start()
            }
            else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    lateinit var textureView : TextureView

    private fun start() {
        iscamera = 0
        stereoVideoView = StereoIotdVideoView(this, mediaPlayer!!).apply {
//        stereoVideoView = com.example.brainrdtbasic.opengl.StereoIotdCameraView(this, this, this).apply{
            layoutParams =  ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            iotd?.let { setIotdValue(it.toInt()) }
        }
        iotd?.let { Log.i("IOTD:", it) }

//        }

        updateVideoAttributes()
        layout?.addView(stereoVideoView as View)

    }

    class SeekBarChangedListener(val cb: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            cb(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    private fun changetoCam(){
        layout?.removeAllViews()
        stereoVideoView = StereoIotdCameraView(this, this, this).apply{
            layoutParams =  ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            iotd?.toInt()?.let { setIotdValue(it) }
        }
        iotd?.let { Log.i("Cam IOTD:", it) }

        updateVideoAttributes()
        layout?.addView(stereoVideoView as View)
//        speechRecognizer?.startListening(speechIntent)
        iscamera = 1
    }

    private fun changetoVideo(){

        layout?.removeAllViews()
        chro!!.start()
        stereoVideoView = StereoIotdVideoView(this, mediaPlayer!!).apply {
            layoutParams =  ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            iotd?.let { setIotdValue(it.toInt()) }
            iotd?.let { Log.i("Video IOTD:", it) }

        }
        updateVideoAttributes()
        layout?.addView(stereoVideoView as View)
//        speechRecognizer?.startListening(speechIntent)
        iscamera = 0
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

//    private fun getOutDirrectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
//        }
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun getRotation(): Int {
        var rotation: Int = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                rotation = display!!.rotation
            }
        } catch (exc: NoSuchMethodError) {
            rotation = windowManager.defaultDisplay.rotation
        }
        return rotation
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val s = Surface(surface)
        mediaPlayer!!.setSurface(s)
        mediaPlayer!!.start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onPause() {
        super.onPause()
//        if (mediaPlayer != null) {
//            mediaPlayer.pause();
//        }
        speechRecognizer!!.stopListening()

    }

    override fun onDestroy() {
        super.onDestroy()
//        if (mediaPlayer != null) {
//            mediaPlayer.pause();
//        }
        speechRecognizer!!.stopListening()

    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // This is the center button for headphones
        if (event != null) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                t1?.speak("말씀하세요!",TextToSpeech.QUEUE_FLUSH, null);
                isask=1
                speechRecognizer?.startListening(speechIntent)
                return true

//                speechRecognizer?.startListening(speechIntent)
                Toast.makeText(this, "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
                //// switch screen between camera and video
                if (iscamera ==1)
                {
                    changetoVideo()
                    if (mediaPlayer!=null){
                        mediaPlayer!!.seekTo(curposition)
//                            mediaPlayer.start()
                    }
                    return true

                }

                if (iscamera == 0 ){
                    changetoCam()
                    if (mediaPlayer!=null){
                        mediaPlayer!!.pause()
                        curposition =mediaPlayer!!.currentPosition
                    }
                    return true
                }

            }
            if (event.getKeyCode() == KeyEvent.ACTION_DOWN) {
                speechRecognizer?.startListening(speechIntent)
                Toast.makeText(this, "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
                return true;
            }

            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT-> {
                    speechRecognizer?.startListening(speechIntent)
                    Toast.makeText(this, "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
                    return true
                }
            }

        }
//        if (event != null) {
//            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
//                changetoCam()
//                if (mediaPlayer!=null){
//                    mediaPlayer!!.pause()
//                    curposition = mediaPlayer!!.currentPosition
//                }
//
//                Toast.makeText(this, "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        }
//        if (event != null) {
//            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
//                changetoVideo()
//                if (mediaPlayer!=null){
//                    mediaPlayer!!.seekTo(curposition)
//                    mediaPlayer!!.start()
//                }
//                Toast.makeText(this, "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        }
        return super.onKeyDown(keyCode, event)
    }
    var callback: MediaSession.Callback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : MediaSession.Callback() {
        override fun onPlay() {
            // Handle the play button
        }
    }
    override fun onBackPressed() {
        mediaPlayer!!.stop()
        super.onBackPressed()

    }

}