package com.hakademy.hversion.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hakademy.util.TimeUtil;
import com.hakademy.util.VersionUtil;

public class ConsoleServer {
	private ExecutorService executorService;
	private ServerSocketChannel server;
	private List<Client> connections = new ArrayList<Client>();
	
	private void startServer(){
		Log.debug("���� ����");
		
		//������ Ǯ ����
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try{
			server = ServerSocketChannel.open();
			server.configureBlocking(true);
			server.bind(new InetSocketAddress(20000));
		}catch(Exception e){
			if(server.isOpen()) stopServer();
			return;
		}
		
		Runnable runnable = ()->{
			while(true){
				try{
					SocketChannel channel = server.accept();
					String msg = "���� / "+channel.getRemoteAddress()+" / "+Thread.currentThread();
					Log.debug(msg);
					Client client = new Client(channel);
					connections.add(client);
				}catch(Exception e){
					if(server.isOpen()) stopServer();
				}
			}
		};
		executorService.submit(runnable);
	}
	
	private void stopServer(){
		try{
			Iterator<Client> iterator = connections.iterator();
			
			while(iterator.hasNext()){
				Client client = iterator.next();
				client.channel.close();
				iterator.remove();
			}
			
			if(server != null && server.isOpen()){
				server.close();
			}
			if(executorService != null && executorService.isShutdown()){
				executorService.shutdown();
			}
		}catch(Exception e){
			Log.error(e.getMessage());
		}
	}
	
	private class Client{
		SocketChannel channel;
		public Client(SocketChannel channel){
			this.channel = channel;
			receive();
		}
		void receive(){
			Runnable runnable = ()->{
				while(true){
					try{
						ByteBuffer buffer = ByteBuffer.allocate(8192);
						int count = channel.read(buffer);
						
						if(count == -1)
							throw new IOException();
						
						buffer.flip();
						Charset charset = Charset.forName("UTF-8");
						String data = charset.decode(buffer).toString();
						checkRequestType(data);
					}catch(IOException e){
						try{
							//Ŭ���̾�Ʈ 
							String msg = "[��� ����] : "+channel.getRemoteAddress()+" / "+Thread.currentThread();
							Log.error(msg);
							kill();
						}catch(IOException e2){}
						break;
					}
				}
			};
			executorService.submit(runnable);
		}
		void sendFile(File file, String mimeType){
			Runnable runnable = ()->{
				try(
					FileInputStream in = new FileInputStream(file);
				){
					Path path = Paths.get(file.getAbsolutePath());
					long totalLength = Files.size(path);
					String t1 = "/file size "+totalLength;
					Charset charset = Charset.forName("UTF-8");
					ByteBuffer sizeBuffer = charset.encode(t1);
					channel.write(sizeBuffer);
					
					FileChannel rc = in.getChannel();
					ByteBuffer rb = ByteBuffer.allocate(4096);
					long moveLength = 0L;
					long startTime = System.nanoTime();
					while(true){
						rb.clear();
						int size = rc.read(rb);
						if(size == -1) break;
						moveLength += size;
						rb.flip();
						channel.write(rb);
					}
					long endTime = System.nanoTime();
					Log.debug("���� ���� �Ϸ� : "+TimeUtil.getDuration(startTime, endTime)+" ns �ҿ�");
				}catch(IOException e){
					Log.error("���� ���� ���� : "+e.getMessage());
				}
			};
			executorService.submit(runnable);
		}
		void send(String data){
			Runnable runnable = ()->{
				try{
					Charset charset = Charset.forName("UTF-8");
					ByteBuffer buffer = charset.encode(data);
					channel.write(buffer);
				}catch(Exception e){
					try{
						String msg = "[��� ����] : "+channel.getRemoteAddress()+" / "+Thread.currentThread();
						Log.error(msg);
						kill();
					}catch(IOException e2){}
				}
			};
			executorService.submit(runnable);
		}
		private void checkRequestType(String data){
			if(data.startsWith("/version")){
				// [�������] 		/version [���α׷���] [������ȣ]
				Matcher m = Pattern.compile("[(.*?)]").matcher(data);
				List<String> args = new ArrayList<>();
				try{
					while(m.find()){
						args.add(m.group(1));
					}
					if(args.size() != 2) throw new Exception("�߸��� ���� ��û.. Ŭ���̾�Ʈ ���� ���� ����\n���� : "+data);
					File file = VersionUtil.getProgram(args);
					if(file == null){
						send("/version not-required");
					}
					else{
//						sendFile(file, mimeType);
					}
				}catch(Exception e){
					Log.error(e.getMessage());
					return;
				}
			}
			else{
				broadcast(data);
			}
		}
		void kill() throws IOException{
			connections.remove(Client.this);
			channel.close();
		}
	
	}
	
	private void broadcast(String data){
		Log.debug("��ε�ĳ��Ʈ : "+data);
		for(Client client : connections){
			client.send(data);
		}
	}
	
	private  ConsoleServer(){
		
	}
	
	public static void work(){
		ConsoleServer consoleServer = new ConsoleServer();
		consoleServer.startServer();
	}
}
