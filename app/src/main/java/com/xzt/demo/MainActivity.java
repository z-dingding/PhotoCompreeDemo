package com.xzt.demo;


import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    Button btnTakePhoto, btnPhotoAlbum;
    ImageView imageView;

    /**
     * 7.0获取的图片地址，与7.0之前方式不一样
     */

    // 图片拍照的标识,1拍照0相册
    private static int TAKEPAHTO = 1;
    //三个常量全局标识
    //图库
    private static final int PHOTO_PHOTOALBUM = 0;
    //拍照
    private static final int PHOTO_TAKEPHOTO = 1;
    //裁剪

    /**
     * 7.0系统手机设置的图片Uri地址
     */
    private Uri takePhotoSaveAdr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取读取和相机权限
        getRootPermissions();
        initView();
        initData();
    }

    /**
     * 获取6.0读取文件的权限
     */

    public void getRootPermissions() {
        //2.0版本去掉Manifest.permission.ACCESS_COARSE_LOCATION,
        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity instance
        rxPermissions.request(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
        )
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) {
                        if (granted) { // 在android 6.0之前会默认返回true
                            // 已经获取权限
                            Log.e(TAG, "已经获取权限");
                        } else {
                            // 未获取权限
                            Log.e(TAG, "您没有授权该权限，请在设置中打开授权");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {

                    }
                }, new Action() {
                    @Override
                    public void run() {
                        Log.e(TAG, "{run}");
                    }
                });
    }

    private void initData() {
        btnTakePhoto.setOnClickListener(this);
        btnPhotoAlbum.setOnClickListener(this);
    }

    private void initView() {
        btnTakePhoto = findViewById(R.id.btn_takephoto);
        btnPhotoAlbum = findViewById(R.id.btn_photoalbum);
        imageView = findViewById(R.id.imageView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_takephoto:
                TAKEPAHTO=1;
                // 启动系统相机
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 判断7.0android系统
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //临时添加一个拍照权限
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //通过FileProvider获取uri
                    takePhotoSaveAdr = FileProvider.getUriForFile(MainActivity.this,
                            "com.hxzk.bj.photodemo", new File(Environment.getExternalStorageDirectory(), "savephoto" +
                                    ".jpg"));
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoSaveAdr);
                } else {
                    takePhotoSaveAdr = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "savephoto" +
                            ".jpg"));
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoSaveAdr);
                }
                startActivityForResult(intent, PHOTO_TAKEPHOTO);
                break;
            case R.id.btn_photoalbum:
                TAKEPAHTO=0;
                Intent intentAlbum = new Intent(Intent.ACTION_PICK, null);
                //其中External为sdcard下的多媒体文件,Internal为system下的多媒体文件。
                //使用INTERNAL_CONTENT_URI只能显示存储在内部的照片
                intentAlbum.setDataAndType(
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
                //返回结果和标识
                startActivityForResult(intentAlbum, PHOTO_PHOTOALBUM);
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {//避免选图时取消操作
            switch (requestCode) {
                case PHOTO_TAKEPHOTO:
                    Uri clipUri;
                    //判断如果是7.0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        clipUri = takePhotoSaveAdr;
                    } else {
                        clipUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/savephoto.jpg"));
                    }
                    bitmapCompress(clipUri);
                    break;
                case PHOTO_PHOTOALBUM:
                  Uri photoAlbumUri =data.getData();
                    bitmapFactory(photoAlbumUri);
                    break;
            }
        }
    }





    Bitmap photoBitmap;
    File file;
    /**
     * 压缩图片使用,采用BitmapFactory.decodeFile。这里是尺寸压缩
     */

    public void bitmapFactory(Uri imageUri){
        String[] filePathColumns = {MediaStore.Images.Media.DATA};
        Cursor c = getContentResolver().query(imageUri, filePathColumns, null, null, null);
        c.moveToFirst();
        int columnIndex = c.getColumnIndex(filePathColumns[0]);
        String imagePath = c.getString(columnIndex);
        c.close();

        // 配置压缩的参数
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //获取当前图片的边界大小，而不是将整张图片载入在内存中，避免内存溢出
        BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;
        ////inSampleSize的作用就是可以把图片的长短缩小inSampleSize倍，所占内存缩小inSampleSize的平方
        options.inSampleSize = caculateSampleSize(options,500,500);
        Bitmap bm = BitmapFactory.decodeFile(imagePath, options); // 解码文件
        imageView.setImageBitmap(bm);
    }

    /**
     * 计算出所需要压缩的大小
     * @param options
     * @param reqWidth  我们期望的图片的宽，单位px
     * @param reqHeight 我们期望的图片的高，单位px
     * @return
     */
    private int caculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        int picWidth = options.outWidth;
        int picHeight = options.outHeight;
        if (picWidth > reqWidth || picHeight > reqHeight) {
            int halfPicWidth = picWidth / 2;
            int halfPicHeight = picHeight / 2;
            while (halfPicWidth / sampleSize > reqWidth || halfPicHeight / sampleSize > reqHeight) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }


    /**
     * 这里我们生成了一个Pic文件夹，在下面放了我们质量压缩后的图片，用于和原图对比
     * 压缩图片使用Bitmap.compress()，这里是质量压缩
     */
    public void bitmapCompress(Uri uriClipUri) {
        try {
            //裁剪后的图像转成BitMap
            //photoBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uriClipUri));
            photoBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriClipUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //创建路径
        String path = Environment.getExternalStorageDirectory()
                .getPath() + "/Pic";
        //获取外部储存目录
        file = new File(path);
        //创建新目录, 创建此抽象路径名指定的目录，包括创建必需但不存在的父目录。
        file.mkdirs();
        //以当前时间重新命名文件
        long i = System.currentTimeMillis();
        //生成新的文件
        file = new File(file.toString() + "/" + i + ".png");
        Log.e("fileNew", file.getPath());
        //创建输出流
        OutputStream out = null;
        try {
            out = new FileOutputStream(file.getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //压缩文件，返回结果，参数分别是压缩的格式，压缩质量的百分比，输出流
        boolean bCompress = photoBitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);

        try {
            photoBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),Uri.fromFile(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageView.setImageBitmap(photoBitmap);
    }
}
