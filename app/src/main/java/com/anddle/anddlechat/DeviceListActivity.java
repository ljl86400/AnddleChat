package com.anddle.anddlechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.Set;

/**
 *  创建一个用来展示可用蓝牙设备的列表activity（活动）类
 */
public class DeviceListActivity extends AppCompatActivity {

    private final static String TAG = "BT discover";
    private final int BT_SEARCH_STATE_IDLE = 0;
    private final int BT_SEARCH_STATE_SEARCHING = 1;

    private ListView mBTDeviceInfoView;
    private BluetoothAdapter mBluetoothAdapter;
    private int mBTSearchingState;
    private MenuItem mSearchMenuItem;

    /**
     * 声明一个创建活动的方法
     *   恢复窗口关闭之前的状态
     *   页面布局设定按照R.layout.device_list_activity.xml内容安排
     *   在页面的左上角设置一个返回主页的按钮setDisplayHomeAsUpEnabled(true)
     *   实例一个适配器，用来管理需要展示的蓝牙器件列表信息BTDeviceInfoAdapter
     *   按照R.id.device_list的设定安排mBTDeviceInfoView
     *   为mBTDeviceInfoView视图添加监听器，当选中该视图时，之后以下lambda表达式
     *     <lambda>
     *         如果蓝牙正在搜索其他蓝牙设备中，就停止搜索
     *         将选中的蓝牙设备相关信息反馈给ChatActivity，并关闭当前页面
     *     </lambda>
     *   将蓝牙的搜索状态设置为空闲
     *   将mReceiver设置成只接受：ACTION_FOUND、ACTION_DISCOVERY_STARTED、ACTION_DISCOVERY_FINISHED
     *   三个动作请求的接收器
     *   结束当前活动
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        DeviceItemAdapter BTDeviceInfoAdapter = new DeviceItemAdapter(this, R.layout.device_list_item);
        mBTDeviceInfoView = (ListView) findViewById(R.id.device_list);
        mBTDeviceInfoView.setAdapter(BTDeviceInfoAdapter);
        mBTDeviceInfoView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                ArrayAdapter adapter = (ArrayAdapter) mBTDeviceInfoView.getAdapter();
                BluetoothDevice device = (BluetoothDevice) adapter.getItem(position);
                Intent i = new Intent();
                /*
                 * 将设备地址存储到Intent当中
                 */
                i.putExtra("DEVICE_ADDR", device.getAddress());
                /*
                 * 将数据结果返回给ChatActivity，并关闭当前的Activity界面
                 */
                setResult(RESULT_OK, i);
                finish();
            }
        });
        mBTSearchingState = BT_SEARCH_STATE_IDLE;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // IntentFilter详解：http://blog.csdn.net/today520/article/details/7000048
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        updateDeviceList();
    }

    /**
     *  更新蓝牙列表，无返回值、无传入参数
     *  新建一个DeviceItemAdapter适配器变量adapter，管理mBTDeviceInfoView的信息
     *  清除adapter中的所有元素
     *  创建一个集合变量pairedDevices，用来存放那些可以被连接的蓝牙信息
     *  通知系统，数据集合已经发生改变
     *  如果mBluetoothAdapter正在搜索，就结束搜索
     *  判断是否有获取ACCESS_COARSE_LOCATION的权限，coarse：粗略的
     *    如果没有权限，申请权限
     *    如果有权限：打印日志
     *
     */
    private void updateDeviceList() {
        DeviceItemAdapter adapter = (DeviceItemAdapter) mBTDeviceInfoView.getAdapter();
        adapter.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "BT device bounded:" + device.getName());
                // D/BT discover: BT device bounded:iPhone6s  ---log.d的某一次输出结果
                adapter.add(device);
            }
        }
        adapter.notifyDataSetChanged();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        int hasPermission = ActivityCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceListActivity.this,
                    new String[]{
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        } else {
            boolean ret = mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "BT device discover about to start: ret=" + ret);
            // D/BT discover: BT device discover about to start: ret=true ---log.d某一次的运行结果
        }
    }

    /**
     *  销毁活动，如果正在搜索，则停止
     *  注销mReceiver（前面设定的可以接受三种动作的消息）
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    /**
     * 在面板上添加一个按钮
     * 更新界面
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_menu, menu);
        mSearchMenuItem = menu.findItem(R.id.search_menu);;
        updateUI();
        return true;
    }

    /**
     * 更新界面
     */
    private void updateUI() {
        switch (mBTSearchingState)
        {
            case BT_SEARCH_STATE_IDLE:
            {
                if(mSearchMenuItem != null)
                {
                    mSearchMenuItem.setTitle(R.string.search);
                }
            }
            break;
            case BT_SEARCH_STATE_SEARCHING:
            {
                if(mSearchMenuItem != null)
                {
                    mSearchMenuItem.setTitle(R.string.cancel);
                }
            }
            break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.search_menu:
            {
                if(mBTSearchingState == BT_SEARCH_STATE_IDLE) {
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    updateDeviceList();
                }
                else if(mBTSearchingState == BT_SEARCH_STATE_SEARCHING) {
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                }
            }
            break;

            case android.R.id.home:
                this.finish();
            break;
        }
        return true;
    }

    /**
     * lambda表达式，当调用此公式时候执行大括号里的程序，
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "BT device found:" + device.getName());
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    DeviceItemAdapter adapter = (DeviceItemAdapter) mBTDeviceInfoView.getAdapter();
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "BT device discover started");
                mBTSearchingState = BT_SEARCH_STATE_SEARCHING;
                updateUI();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "BT device discover finished");
                mBTSearchingState = BT_SEARCH_STATE_IDLE;
                updateUI();
            }
            else {
                Log.d(TAG, "BT device got action:"+action);
            }
        }
    };
}
