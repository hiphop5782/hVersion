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
		Log.debug("서버 시작");
		
		//스레드 풀 생성
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
					String msg = "접속 / "+channel.getRemoteAddress()+" / "+Thread.currentThread();
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
							//클라이언트 
							String msg = "[통신 오류] : "+channel.getRemoteAddress()+" / "+Thread.currentThread();
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
					Log.debug("파일 전송 완료 : "+TimeUtil.getDuration(startTime, endTime)+" ns 소요");
				}catch(IOException e){
					Log.error("파일 전송 오류 : "+e.getMessage());
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
						String msg = "[통신 오류] : "+channel.getRemoteAddress()+" / "+Thread.currentThread();
						Log.error(msg);
						kill();
					}catch(IOException e2){}
				}
			};
			executorService.submit(runnable);
		}
		private void checkRequestType(String data){
			if(data.startsWith("/version")){
				// [명령형식] 		/version [프로그램명] [버전번호]
				Matcher m = Pattern.compile("[(.*?)]").matcher(data);
				List<String> args = new ArrayList<>();
				try{
					while(m.find()){
						args.add(m.group(1));
					}
					if(args.size() != 2) throw new Exception("잘못된 버전 요청.. 클라이언트 전송 형식 오류\n원문 : "+data);
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
		Log.debug("브로드캐스트 : "+data);
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
