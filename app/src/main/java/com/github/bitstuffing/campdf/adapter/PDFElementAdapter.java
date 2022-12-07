package com.github.bitstuffing.campdf.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.bitstuffing.campdf.ISignals;
import com.github.bitstuffing.campdf.R;
import com.github.bitstuffing.campdf.Utils;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDFElementAdapter extends ArrayAdapter<File> implements Filterable {

    private List<File> list;
    private Activity activity;
    private Map<String,Bitmap> cachedHashMap;

    protected static Handler messageHandler;

    int WIDTH = 150;
    int HEIGHT = 150;

    public PDFElementAdapter(List<File> listFiles, Activity activity){
        super(activity,R.layout.custom,listFiles);
        this.list = listFiles;
        this.activity = activity;
        this.cachedHashMap = new HashMap<String,Bitmap>();

        messageHandler = new Handler() {
            @Override
            public void handleMessage (Message msg){
                Bundle bundle = msg.getData();
                int code = bundle.getInt("code",0);
                String message = bundle.getString("message","");
                switch(code) {
                    case ISignals.REFRESH:
//                        new Runnable(){
//                            @Override
//                            public void run() {
                                int size = list.size();
                                List<File> tempList = Utils.fillFileList(activity);
                                if (size!=tempList.size()){
                                    list = tempList;
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyDataSetChanged();
                                        }
                                    });
                                }else{
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    PDFElementAdapter.sendMessage(ISignals.REFRESH,"");
                                }
//                            }
//                        };
                        break;
                }
            }
        };
    }

    public static void sendMessage(int code,String message) {
        Message messageToBeSent = new Message();
        Bundle bundle = new Bundle();
        bundle.putInt("code",code);
        bundle.putString("message",message);
        messageToBeSent.setData(bundle);
        messageHandler.sendMessage(messageToBeSent);
    }

    @Override
    public int getCount() {
        return list !=null ? list.size(): 0;
    }

    @Override
    public File getItem(int i) {
        return list != null ? list.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if(view !=null && cachedHashMap.containsKey(list.get(i).getName())){ //search in cached
            ((ViewGroup)view.getParent()).removeView(view); //needs this tip to avoid invalid view identifier
            //View row = cachedHashMap.get(list.get(i).getName());
            //return row;
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.custom, parent,false);
        TextView title, detail;
        ImageView i1;
        title = (TextView) row.findViewById(R.id.title);
        detail = (TextView) row.findViewById(R.id.detail);
        title.setText(list.get(i).getName());
        detail.setText(list.get(i).getName());
        i1=(ImageView)row.findViewById(R.id.img);
        Bitmap thumbnail = null;
        if(cachedHashMap.containsKey(list.get(i).getName())){
            thumbnail = cachedHashMap.get(list.get(i).getName());
        }else{
            try{
                thumbnail = getThumbnailFromPDF(list.get(i).getName()); //get thumbnail
            }catch(IOException e){
                e.printStackTrace(); //TODO log
            }
        }
        if(thumbnail!=null){
            i1.setImageBitmap(thumbnail);
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //File file = new File(((TextView)view.findViewById(R.id.detail)).getText().toString());
                try {
                    Utils.openPDFFile(((TextView)view.findViewById(R.id.detail)).getText().toString(),activity);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        /*
        row.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
                registerForContextMenu(view);
                //OpenContextMenu
                return false;
            }
        });*/
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                return false;
            }
        });
        //cached but is not a valid solution, needs more control (like renamed or deleted)
        cachedHashMap.put(list.get(i).getName(),thumbnail);
        return row;
    }

    private Bitmap getThumbnailFromPDF(String s) throws IOException {
        Bitmap bitmap = null;
        try{
            PDDocument pdf = PDDocument.load(new File(activity.getFilesDir().getAbsolutePath()+File.separatorChar+s));
            PDFRenderer renderer = new PDFRenderer(pdf);
            bitmap = Bitmap.createScaledBitmap(renderer.renderImage(0), WIDTH, HEIGHT, true);
        }catch(Exception e){
            bitmap = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.pdf)).getBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap,WIDTH/3, HEIGHT/3 ,true);
        }
        if(bitmap!=null){
            cachedHashMap.put(s,bitmap);
        }
        return bitmap;
    }

    @Override
    public Filter getFilter(){
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<File> list = Utils.fillFileList(activity);

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();

                if (constraint != null && constraint.toString().length() > 0) {
                    List<File> founded = new ArrayList<File>();
                    for (File item : list) {
                        if (item.getName().toLowerCase().contains(constraint)) {
                            founded.add(item);
                        }
                    }

                    result.values = founded;
                    result.count = founded.size();
                } else {
                    result.values = list;
                    result.count = list.size();
                }
                return result;
            }


            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
                list.clear();
                for (File item : (List<File>) results.values) {
                    list.add(item);
                }
                notifyDataSetInvalidated();
            }
        };
    }


    @Override
    public int getViewTypeCount() {
        if(getCount() > 0){
            return getCount();
        }else{
            return super.getViewTypeCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}
