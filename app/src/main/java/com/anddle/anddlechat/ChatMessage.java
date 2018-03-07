package com.anddle.anddlechat;

/*
 * 标记信息的相关信息
 * 一个字符常量将我定义为 0
 * 一个字符常量将别人定义为 1
 * 一个字段用来存储信息发送者，一般用来引用前面这两个常量
 * 一个字段用来存储字符串信息
 */
public class ChatMessage {
    public static final int MSG_SENDER_ME = 0;
    public static final int MSG_SENDER_OTHERS = 1;
    public int messageSender;
    public String messageContent;
}
