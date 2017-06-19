package com.hakademy.hversion.server;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	private static Format f = new SimpleDateFormat("y-M-d H:m:s");
	public static void error(String text){
		System.out.println("式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式");
		print("Error", text);
		System.out.println("式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式式");
	}
	public static void debug(String text){
		print("Debug", text);
	}
	private static void print(String type, String text){
		Date date = new Date();
		System.out.println("["+type+"] "+text +" ("+f.format(date)+")");
	}
}
