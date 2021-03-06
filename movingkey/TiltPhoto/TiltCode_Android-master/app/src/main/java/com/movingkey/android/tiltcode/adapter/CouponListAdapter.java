package com.movingkey.android.tiltcode.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.foldablelayout.UnfoldableView;
import com.movingkey.android.tiltcode.Model.Coupon;
import com.movingkey.android.tiltcode.Model.LoginResult;
import com.movingkey.android.tiltcode.R;
import com.movingkey.android.tiltcode.Util.Util;
import com.movingkey.android.tiltcode.activitys.CouponReceiveActivity;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Gyul on 2016-06-03.
 */
public class CouponListAdapter extends BaseAdapter{

    //로그에 쓰일 tag
    public static final String TAG = CouponListAdapter.class.getSimpleName();

    public List<Coupon> couponList;
    public Context context;
    public UnfoldableView mUnfoldableView;
    public View mDetailsLayout;

    public CouponListAdapter(List<Coupon> couponList, Context context, UnfoldableView mUnfoldableView, View mDetailsLayout){
        this.couponList = couponList;
        this.context = context;
        this.mUnfoldableView = mUnfoldableView;
        this.mDetailsLayout = mDetailsLayout;
    }

    //http://givenjazz.tistory.com/48
    /*
    public void recycle() {
        for (WeakReference<View> ref : mRecycleList) {
            Util.recursiveRecycle(ref.get());
        }
    }*/

    @Override
    public int getCount() {
        return couponList.size();
    }

    @Override
    public Object getItem(int i) {
        return couponList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View v, ViewGroup viewGroup) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//        if(v==null) {
        v = inflater.inflate(R.layout.item_coupon_row, null);
//        }
        final ImageView imv = (ImageView)v.findViewById(R.id.img_coupon_row);
        TextView tv = (TextView)v.findViewById(R.id.tv_coupon_row);

        if(imv==null) {
            Log.e(TAG,"error : no view");
            CouponReceiveActivity.mListView.setRefreshing();
            return v;
        }

        Log.d(TAG,"i : "+i+" id : "+couponList.get(i).id);

        try {
            Picasso.with(context).load(context.getResources().getText(R.string.API_SERVER) + ":40002/couponGetImage?id="
                    + couponList.get(i).id + "." + couponList.get(i).imageEx).resize(400, 400).centerCrop().into(imv);
//        imageLoader.displayImage(context.getResources().getText(R.string.API_SERVER)+":40002/couponGetImage?id="
//                +couponList.get(i).id+"."+couponList.get(i).imageEx,imv,options);
        } catch (Exception e){
            Toast.makeText(context,"error : "+e.getMessage(),Toast.LENGTH_LONG).show();
            Log.d(TAG,"error : "+e.getMessage());
            e.printStackTrace();
        }
        tv.setText(couponList.get(i).title);


        ((LinearLayout)v.findViewById(R.id.layout_coupon_detail)).setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                mUnfoldableView.unfold(view, mDetailsLayout);

                ImageView couponDetail = ((ImageView) mDetailsLayout.findViewById(R.id.img_coupon_detail));
                couponDetail.setImageDrawable(imv.getDrawable());

                final TextView couponCreate = ((TextView)mDetailsLayout.findViewById(R.id.tv_coupon_detail_create));
                TextView couponTitle = ((TextView)mDetailsLayout.findViewById(R.id.tv_coupon_detail_title));
                TextView couponDesc = ((TextView)mDetailsLayout.findViewById(R.id.tv_coupon_detail_desc));

                couponCreate.setText(Util.decrypt(couponList.get(i).create));
                couponTitle.setText(couponList.get(i).title);
                couponDesc.setText(couponList.get(i).desc);

                Log.d(TAG,"coupon type : "+couponList.get(i).type);
                if(couponList.get(i).type.equals("barcode")){
                    Log.d(TAG,"image url : "+":40002/couponGetFile?id="
                            +couponList.get(i).id+"."+couponList.get(i).fileEx);
                    Picasso.with(context).load(context.getResources().getText(R.string.API_SERVER)+":40002/couponGetFile?id="
                            +couponList.get(i).id+"."+couponList.get(i).fileEx+"&session="+Util.getAccessToken().getToken()).resize(400,400).centerCrop().into(((ImageView)mDetailsLayout.findViewById(R.id.img_coupon_image)));
                    ((ImageView)mDetailsLayout.findViewById(R.id.img_coupon_image)).setVisibility(View.VISIBLE);
                }
                else{
                    ((ImageView)mDetailsLayout.findViewById(R.id.img_coupon_image)).setVisibility(View.GONE);
                }




                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                Address address;
                String result = null;
                List<Address> list = null;
                try {
                    Log.d(TAG, "lat : " + couponList.get(i).lat+" lng : "+couponList.get(i).lng);
                    list = geocoder.getFromLocation(Double.valueOf(couponList.get(i).lat),Double.valueOf(couponList.get(i).lng), 1);
                    address = list.get(0);
                    result = address.getAddressLine(0) + ", " + address.getLocality();

                    ((TextView) mDetailsLayout.findViewById(R.id.tv_coupon_detail_location)).setText(result);
                    Log.d(TAG, "location : " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "locale error : " + e.getMessage());
                }


                File pdfFile = new File(Environment.getExternalStorageDirectory() + "/Download/" + setFilePath(couponList.get(i).title) + "." + couponList.get(i).fileEx);
                if(pdfFile.exists()){
                    ((Button)mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setText("열기");
                    ((TextView)mDetailsLayout.findViewById(R.id.tv_coupon_detail_file)).setText("/Download/"+setFilePath(couponList.get(i).title)+"."+couponList.get(i).fileEx);

                }
                else{
                    if(couponList.get(i).type.equals("file")){
                        ((Button)mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setText("파일 다운로드");
                    }
                    else if(couponList.get(i).type.equals("image")){
                        ((Button)mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setText("쿠폰 다운로드");
                    }
                    else if(couponList.get(i).type.equals("link")){
                        ((Button)mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setText("링크 이동");
                    }
                    ((TextView)mDetailsLayout.findViewById(R.id.tv_coupon_detail_file)).setText("");
                }

                ((ImageButton)mDetailsLayout.findViewById(R.id.btn_couponitem_delete)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("쿠폰삭제");
                        builder.setMessage("쿠폰을 삭제하시겠습니까?");
                        builder.setNegativeButton("취소", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        Log.d(TAG,"delete token : "+Util.getAccessToken().getToken());
                        Log.d(TAG,"delete coupon id : "+couponList.get(i).id);

                        builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int index) {
                                Util.getEndPoint().setPort("40002");
                                Util.getHttpSerivce().couponDelete(Util.getAccessToken().getToken(),
                                        couponList.get(i).id,
                                        new Callback<LoginResult>() {
                                            @Override
                                            public void success(LoginResult loginResult, Response response) {

                                                if (loginResult.code.equals("1")) {
                                                    Toast.makeText(context, context.getResources().getText(R.string.message_success_delete), Toast.LENGTH_LONG).show();
                                                    mUnfoldableView.foldBack();
                                                    CouponReceiveActivity.mListView.setRefreshing();
                                                } else {
                                                    Toast.makeText(context, context.getResources().getText(R.string.message_network_error), Toast.LENGTH_LONG).show();
                                                    Log.e(TAG, "error delete coupon(result code : " + loginResult.code + " ) : " + response.toString());
                                                }
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                Toast.makeText(context, context.getResources().getText(R.string.message_network_error), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, "error delete coupon : " + error.getMessage());
                                            }
                                        });
                            }
                        });
                        builder.show();
                    }
                });
                ((Button)mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG,"couponitemproc click");
                        if (couponList.get(i).type.equals("link")) {
                            String url = couponList.get(i).link;

                            // jhnunu 150930
                            if (url.contains("www.")==false)  url = "www." + url;
                            if (url.contains("http://")==false)    url = "http://" + url;
                            //---------------------------

                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            context.startActivity(i);
                        } else if (((Button) view).getText().toString().equals("열기")) {
                            File pdfFile = new File(Environment.getExternalStorageDirectory() + "/Download/" + setFilePath(couponList.get(i).title) + "." + couponList.get(i).fileEx);

                            try {
/*                                Intent myIntent = new Intent(Intent.ACTION_VIEW);
                                myIntent.setData(Uri.fromFile(pdfFile));
                                Intent j = Intent.createChooser(myIntent, "이 파일을 열 어플리케이션을 선택해주세요.");
                                context.startActivity(j);
                                /**/
                                /*
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(pdfFile));
                                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                                context.startActivity(intent);*/

                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(pdfFile), getMimeType(pdfFile.getAbsolutePath()));
                                context.startActivity(intent);

                            } catch (Exception e) {
                                Toast.makeText(context, "파일을 열수 없습니다. " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.d(TAG,"proc else type : "+couponList.get(i).type);
                            if (couponList.get(i).type.equals("file") || couponList.get(i).type.equals("barcode")) {

                                Log.d(TAG,"download image");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Util.getEndPoint().setPort("40002");
                                        retrofit.client.Response response = Util.getHttpSerivce().getFile(Util.getAccessToken().getToken(), couponList.get(i).id + "." + couponList.get(i).fileEx);
//                                        byte[] bytes = FileHelper.getBytesFromStream(response.getBody().in());


                                        try {

                                            InputStream stream = (response.getBody().in());

                                            byte[] fileBytes = streamToBytes(stream);

                                            File pdfFile = new File(Environment.getExternalStorageDirectory() + "/Download/" + setFilePath(couponList.get(i).title) + "." + couponList.get(i).fileEx);
                                            File filePath = new File(Environment.getExternalStorageDirectory() + "/Download/");
                                            filePath.mkdir();
                                            Log.d(TAG, "file : " + pdfFile.getAbsolutePath() + " name : " + pdfFile.getName() + " size : " + fileBytes.length);

                                            FileOutputStream output = null;
                                            output = new FileOutputStream(pdfFile);
                                            output.write(fileBytes);
                                            output.flush();
                                            output.close();
//                                            org.apache.commons.io.IOUtils.write(fileBytes, output);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "file error : " + e.getMessage());
                                            ((Activity) context).runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context, context.getResources().getText(R.string.message_download_coupon_fail), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                            return;
                                        }
                                        ((Activity) context).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (couponList.get(i).type.equals("file")){
                                                    Toast.makeText(context, context.getResources().getText(R.string.message_download_coupon_success), Toast.LENGTH_LONG).show();

                                                }
                                                else{
                                                    Toast.makeText(context, "이미지를 다운받았습니다.", Toast.LENGTH_LONG).show();

                                                }

                                                ((Button) mDetailsLayout.findViewById(R.id.btn_couponitem_proc)).setText("열기");
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }
                    }
                });

            }
        });


        return v;

    }

    public String setFilePath(String title){
        return title.replace(' ','_');
    }

    private String getMimeType(String url)
    {
        String parts[]=url.split("\\.");
        String extension=parts[parts.length-1];
        String type = null;
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    byte[] streamToBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (stream != null) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = stream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }
}
