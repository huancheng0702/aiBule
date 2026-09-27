package com.coolwear.bluesdk.tcp;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ConnectionManager {
	public static final String BROADCAST_ACTION="tcp_client_callback";
	public static final String MESSAGE = "message";
	public static final String CONNECT = "connect";
	public static final int CONNECT_STATUS = 1001;
	public static final int MESSAGE_BODY = 1002;
	public static final int CONNECT_SUCC_CODE = 500;//连接成功
	public static final int CONNECT_ERROR_CODE = -501;//连接失败
	private ConnectionConfig config;
	private WeakReference<Context> context;
	private NioSocketConnector connection;
	private IoSession session;
	private InetSocketAddress address;
	
	public ConnectionManager(ConnectionConfig config){
		this.config = config;
		this.context = new WeakReference<Context>(config.getContext());
		init();
	}
	
	private void init(){
		address = new InetSocketAddress(config.getIp(),config.getPort());
		connection = new NioSocketConnector();
		connection.setDefaultRemoteAddress(address);
		connection.getSessionConfig().setWriteTimeout(3*1000);
		connection.getSessionConfig().setReadBufferSize(config.getReadBufferSize());
		connection.getFilterChain().addLast("logger", new LoggingFilter());
		connection.getFilterChain().addLast("codec", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));
		connection.setConnectTimeoutMillis(20*1000);
		connection.setHandler(new DefaultHandler(context.get()));
	}
	
	public boolean connect(){
		try {
			if(connection ==null){
				return false;
			}
			ConnectFuture future = connection.connect();
			future.awaitUninterruptibly();
			session = future.getSession();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(SessionManager.TAG,"连接异常原因："+e.getCause());
		}
		return session != null;
	}
	
	public void disConnect(){
		connection.dispose();
		connection = null;
		session = null;
		address = null;
		context = null;
	}
	private  class DefaultHandler extends IoHandlerAdapter{
		private Context context;
		public DefaultHandler(Context context) {
			this.context = context;
		}
		
		@Override
		public void sessionOpened(IoSession session) throws Exception {
			//put session to sessionmanager for send message
			SessionManager.getInstance().setSession(session);
		}
		
		@Override
		public void messageReceived(IoSession session, Object message)
				throws Exception {
//			if(context != null){
//				Intent intent = new Intent(BROADCAST_ACTION);
//				intent.putExtra(MESSAGE, message.toString());
//				context.sendBroadcast(intent);
//			}
			sendCallBack("200", message.toString());
		}

		@Override
		public void messageSent(IoSession session, Object message) throws Exception {
			super.messageSent(session, message);
		}
	}

	public void sendCallBack(String str,String str2) {
		Intent intent = new Intent(BROADCAST_ACTION);
		intent.putExtra(CONNECT, str);
		intent.putExtra(MESSAGE, str2);

		if(context.get()!=null){
			context.get().sendBroadcast(intent);
		}
	}
}
