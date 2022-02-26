package com.example.brainrdtbasic


import android.widget.ArrayAdapter

import android.widget.TextView

import android.view.ViewGroup

import android.app.Activity
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi


class VideoListAdapter<T>     // TODO Auto-generated constructor stub
    (
    private var context: Activity,
    private var maintitle: Array<String>,
    private var subtitle: Array<String?>,
    private var imgid: Array<String?>,

    ) :
    ArrayAdapter<String?>(context, R.layout.list_view_item,maintitle) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView: View = inflater.inflate(R.layout.list_view_item, null, true)

        val titleText = rowView.findViewById(R.id.title) as TextView
        val imageView: ImageView = rowView.findViewById(R.id.icon) as ImageView
        val subtitleText = rowView.findViewById(R.id.subtitle) as TextView
        val downloadView: ImageView = rowView.findViewById(R.id.img_download) as ImageView

        titleText?.text = maintitle[position]
        imageView?.setImageResource(R.drawable.mainbrain)
        subtitleText?.text = subtitle[position]
        downloadView?.setImageResource(R.drawable.download_icon)

        downloadView.setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    downloadView.setImageResource(R.drawable.waiting_icon)
                }
            })

        return rowView
        }
    }

