package com.anddle.anddlechat;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 *  创建一个adapter类，用于构造可连接蓝牙器件展示列表
 *  自建一个构造器，用于实例域值
 *  重写一个方法，
 */
public class DeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mInflater;
    private int mResource;

    public DeviceItemAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    /*
     * 创建一个获取View的方法【只是变量名字叫convertView而已，跟android默认的covertView（如在移动滚动
     * 条时候那些超出显示边界的item视图并未被销毁，而是被暂时缓存起来放在了convertView中）类似乎不是一回事】，
     * 传入需要展示的视图的信息、一个需要被编辑的视图对象convertView、以及视图所在的视图组
     *   选择执行【如果传入的convertView是空的】：直接按照mResource和parent设置convertView
     *   视图关系
     *   创建两个convertView文本视图，一个视图用来显示蓝牙名称，一个是用来展示蓝牙相关信息的视图
     *   两个视图中分别填入蓝牙的名称和蓝牙的信息
     *   返回名字为convertView的View对象
     *
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView nameView = (TextView) convertView.findViewById(R.id.device_name);
        TextView infoView = (TextView) convertView.findViewById(R.id.device_info);
        BluetoothDevice device = getItem(position);
        nameView.setText(device.getName());
        infoView.setText(device.getAddress());

        return convertView;
    }
}
