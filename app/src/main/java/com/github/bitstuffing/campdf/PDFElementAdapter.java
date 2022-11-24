package com.github.bitstuffing.campdf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PDFElementAdapter extends BaseAdapter {

    List<String> list;
    Activity activity;

    int WIDTH = 150;
    int HEIGHT = 150;

    public PDFElementAdapter(List<String> list, Activity activity){
        super();
        this.list = list;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return list !=null ? list.size(): 0;
    }

    @Override
    public Object getItem(int i) {
        return list != null ? list.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.custom,null);
        TextView title, detail;
        ImageView i1;
        title = (TextView) row.findViewById(R.id.title);
        detail = (TextView) row.findViewById(R.id.detail);
        title.setText(list.get(i));
        detail.setText(list.get(i));
        i1=(ImageView)row.findViewById(R.id.img);
        try{
            i1.setImageBitmap(getThumbnailFromPDF(list.get(i))); //get thumbnail
        }catch(IOException e){
            e.printStackTrace(); //TODO log
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
                //TODO OpenContextMenu
                return false;
            }
        });*/
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                return false;
            }
        });
        return row;
    }

    private Bitmap getThumbnailFromPDF(String s) throws IOException {
        PDDocument pdf = PDDocument.load(new File(activity.getFilesDir().getAbsolutePath()+File.separatorChar+s));
        //PDPage pdpage = (PDPage) pdf.getDocumentCatalog().getPages().get(0);
        PDFRenderer renderer = new PDFRenderer(pdf);
        return  Bitmap.createScaledBitmap(renderer.renderImage(0), WIDTH, HEIGHT, true);
    }
}
