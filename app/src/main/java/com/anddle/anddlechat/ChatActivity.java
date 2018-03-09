package com.anddle.anddlechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * ChatActivity实现三个主要功能：
 * 当蓝牙没有开启或者设备不能被发现的时候，请求用户打开对应的功能；
 * 下方有输入框输入要发送的文字内容，点击按钮后能实现文字的发送；输入框上方的大部分区域用来显示聊天的内容；
 * 菜单栏根据当前蓝牙连接的状态，显示不同的菜单项。例如，没有连接时启动蓝牙设备选择界面；
 */

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private final int RESULT_CODE_BTDEVICE = 0;

    private ConnectionManager mConnectionManager;
    private EditText mMessageEditor;
    private ImageButton mSendBtn;
    private ListView mMessageListView;
    private MenuItem mConnectionMenuItem;

    private final static int MSG_SENT_DATA = 0;
    private final static int MSG_RECEIVE_DATA = 1;
    private final static int MSG_UPDATE_UI = 2;

    /*
     * 重写handler类的某些方法，成为一个新的类然后直接当做匿名内部类使用，实例一个新的对象
     */
    private Handler mHandler = new Handler() {

        /*
         * 重写的方法没有返回值，但是完成了对外部类的相关域的赋值
         *
         * 如果方法传入参数msg是0，执行发送消息的相关操作：
         *  1、新建一个比特数组data，引用msg.obj类型转换后的结果
         *  2、新建一个布朗变量suc，引用msg.arg1值是否为1的判断结果
         *  如果有传入数据，而且suc为true：
         *     a、实例一个ChatMessage对象chatMsg
         *     b、对chatMsg.messageSender（消息发送者）、chatMsg.messageContent（消息内容）进行赋值
         *     c、创建一个MessageAdapter（信息适配器）变量adapter，引用mMessageListView.getAdapter()方法的返回值
         *     d、adapter.add(chatMsg)方法将信息添加到信息列表的末尾
         *     e、adapter.notifyDataSetChanged()方法发出信息已经更新的通知
         *     f、mMessageEditor.setText("")方法对信息进行设置
         *
         * 如果方法传入参数是1，执行接收消息的相关操作
         *   1、新建一个比特数组，引用msg.obj类型转换后的结果
         *   2、如果有接收到数据：
         *      a、创建一个变量chat.Msg，引用实例对象ChatMessage
         *      b、对象的发送接收人字段chatMsg.messageSender设置为ChatMessage.MSG_SENDER_OTHERS（一个宏定义的常量）
         *      c、将接收到的数据装箱，并赋值给chatMsg.messageContent（字符串变量）
         *      d、执行适配器添加消息、发送消息改动通知、设置消息内容等动作
         *
         * 如果方法传入参数是2，执行刷新界面的操作
         *
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SENT_DATA: {
                    byte [] data = (byte []) msg.obj;
                    boolean suc = msg.arg1 == 1;
                    if(data != null && suc) {
                        // ChatMessage()这个类是自己建的类，里面只有几个字段，没有任何方法
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.messageSender = ChatMessage.MSG_SENDER_ME;
                        chatMsg.messageContent = new String(data);
                        MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                        adapter.add(chatMsg);
                        adapter.notifyDataSetChanged();
                        mMessageEditor.setText("");
                    }
                }
                break;

                case MSG_RECEIVE_DATA: {
                    byte [] data = (byte []) msg.obj;
                    if(data != null) {
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.messageSender = ChatMessage.MSG_SENDER_OTHERS;
                        chatMsg.messageContent = new String(data);
                        MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                        adapter.add(chatMsg);
                        adapter.notifyDataSetChanged();
                    }
                }
                break;

                case MSG_UPDATE_UI: {
                    updateUI();
                }
                break;
            }

        }
    };

    /*
     * ChatActivity.onCreate是整个app程序的入口，在manifest.xml文档中定义
     */
    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * 按照R.layout.activity_chat设定规划聊天界面，一个列表区域、一个文本编辑区域加一个按钮
         */
        setContentView(R.layout.activity_chat);

        /*
         * 在开启应用程序之后，判断蓝牙是否已经开启，如果没有开启，
         * 使用Intent启动确认窗口，让用户选择是否允许打开：
         *  ****************************************
         *  某个应用想要开启蓝牙功能，是否允许？   *
         *                             是    否    *
         *  ****************************************
         *  BluetoothAdapter应该是个静态类，所以可以直接使用类名称.字段的方法访问里面的
         *  域值BluetoothAdapter.ACTION_REQUEST_ENABLE
         *
         */
        BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BTAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);
            finish();
            return;
        }

        /*
         * 检查是否有权限，int变量hasPermission存放权限标志数
         * 如果权限数值与授权码PackageManager.PERMISSION_GRANTED不相等就是没有权限，则打开一个权限
         * 申请窗口，让用户决定是否授权
         * ActivityCompat.requestPermissions(activity,permissions,requestCode)
         * 第二个参数是一个String数组,
         * 第三个参数是请求码，便于在onRequestPermissionsResult方法中根据requestCode进行判断
         *
         */
        int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    0);
            finish();
            return;
        }

        /*
         * 根据R.id.msg_editor的设定，设置聊天编辑窗口的控件，以及控件的发送请求相应
         */
        mMessageEditor = (EditText) findViewById(R.id.msg_editor);
        mMessageEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {


            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        /*
         * 根据R.id.send_btn的配置设定发送按钮的控件
         */
        mSendBtn = (ImageButton) findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener(mSendClickListener);

        /*
         * 根据R.id.message_list的配置，通过adapter将聊天的信息内容显示在mMessageListView中
         */
        mMessageListView = (ListView) findViewById(R.id.message_list);
        MessageAdapter adapter = new MessageAdapter(this, R.layout.me_list_item, R.layout.others_list_item);
        mMessageListView.setAdapter(adapter);

        /*
         * 创建一个连接管理器，并通过连接监听器监听连接动作，并开始监听
         */
        mConnectionManager = new ConnectionManager(mConnectionListener);
        mConnectionManager.startListen();

        /*
         * 判断自己的设备能否被发现，如果不能被发现，创建一个对话，让用户决定能否被发现
         * value：0 是设定可以被发现之后，可被发现状态的持续时间，0表示一直能被发现
         * *************************************************************************************
         * 某个应用想让其他蓝牙设备检测到您的平板电脑。之后，您可以在“蓝牙”设置中更改此设    *
         * 置。                                                                                *
         *                                                               拒绝   允许           *
         * *************************************************************************************
         */
        if(BTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }

    /*
     * 销毁对话窗口
     * 清除聊天数据
     * 如果有设备还处于连接状态，则断开连接、停止监听
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UPDATE_UI);
        mHandler.removeMessages(MSG_SENT_DATA);
        mHandler.removeMessages(MSG_RECEIVE_DATA);

        if(mConnectionManager != null) {
            mConnectionManager.disconnect();
            mConnectionManager.stopListen();
        }
    }

    /*
     * 使用部分方法重写过的ConnectionManager的中的接口ConnectionListener()实例一个监听器接口，
     * 并用mConnectionListener引用
     * 将ConnectionManager通知的内容，转交给主线程的Handler处理
     */
    private ConnectionManager.ConnectionListener mConnectionListener = new ConnectionManager.ConnectionListener() {

        /*
         * 重写这个方法，但是传入的两个参数用来干什么了？自带判断？
         * 连接状态的变化通知给UI线程，请UI线程处理
         */
        @Override
        public void onConnectStateChange(int oldState, int State) {
            // target.sendToTarget（msg）将msg发送给target=mHandler.obtainMessage
            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        /*
         * 监听状态的变化通知给UI线程，请UI线程处理
         */
        @Override
        public void onListenStateChange(int oldState, int State) {
            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        /*
         * 将发送的数据交给UI线程，请UI线程处理
         */
        @Override
        public void onSendData(boolean suc, byte[] data) {
            mHandler.obtainMessage(MSG_SENT_DATA, suc?1:0, 0, data).sendToTarget();
        }

        /*
         * 将收到的数据交给UI线程，请UI线程处理
         */
        @Override
        public void onReadData(byte[] data) {
            mHandler.obtainMessage(MSG_RECEIVE_DATA,  data).sendToTarget();
        }
    };

    /*
     * 创建一个view监听器，如果view上有点击动作发生，则将信息发送出去
     */
    private View.OnClickListener mSendClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendMessage();
        }
    };

    /*
     * 定义发送动作
     * 如果如果有信息则将信息发送出去
     */
    private void sendMessage() {
        String content = mMessageEditor.getText().toString();
        if(content != null) {
            content = content.trim();
            if(content.length() > 0) {
                boolean ret = mConnectionManager.sendData(content.getBytes());
                if(!ret) {
                    Toast.makeText(ChatActivity.this, R.string.send_fail, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /*
     * 按照R.id.connect_menu的设定创建选项按钮
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mConnectionMenuItem = menu.findItem(R.id.connect_menu);
        updateUI();
        return true;
    }

    /*
     * 配置界面上连接和关于按钮的响应
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.connect_menu: {
                if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
                    mConnectionManager.disconnect();
                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
                    mConnectionManager.disconnect();
                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
                    Intent i = new Intent(ChatActivity.this, DeviceListActivity.class);
                    startActivityForResult(i, RESULT_CODE_BTDEVICE);
                }
            }
            return true;

            case R.id.about_menu: {
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
            }
            return true;

            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult, requestCode="+requestCode+" resultCode="+resultCode );
        if(requestCode == RESULT_CODE_BTDEVICE && resultCode == RESULT_OK) {
            String deviceAddr = data.getStringExtra("DEVICE_ADDR");
            mConnectionManager.connect(deviceAddr);
        }
    }

    /*
     * 更新界面
     */
    private void updateUI()
    {
        if(mConnectionManager == null) {
            return;
        }
        if(mConnectionMenuItem == null) {
            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
            return;
        }
        Log.d(TAG, "current BT ConnectState="+mConnectionManager.getState(mConnectionManager.getCurrentConnectState())
                +" ListenState="+mConnectionManager.getState(mConnectionManager.getCurrentListenState()));
        if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
            mConnectionMenuItem.setTitle(R.string.disconnect);
            mMessageEditor.setEnabled(true);
            mSendBtn.setEnabled(true);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
            mConnectionMenuItem.setTitle(R.string.cancel);
            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
            mConnectionMenuItem.setTitle(R.string.connect);
            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
    }
}
