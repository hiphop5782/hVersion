package com.hakademy.util;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtil {
	public static final String folder = "repository";
	public static final File directory = new File(folder);
	public static boolean isExistingProgram(String program){
		if(!directory.exists()) return false;
		
		File[] list = directory.listFiles();
		for(File file : list){
			if(file.getName().contains(program)){
				return true;
			}
		}
		return false;
	}
	public static File getProgram(List<String> args) throws Exception{
		if(!isExistingProgram(args.get(1))){
			throw new Exception("������Ʈ ó�� ������ ���α׷��� �������� �ʽ��ϴ�");
		}
		
		File[] list = directory.listFiles();
		File target = null;
		for(File file : list){
			if(file.getName().contains(args.get(1))){
				target = file;
			}
		}
		
		if(target == null) throw new Exception("���α׷� Ž�� ����");
		
		Matcher m = Pattern.compile("\\d_\\d_\\d").matcher(target.getName());
		if(!m.find()) throw new Exception("���� �Ǻ� �Ұ�");
		
		String recentVersion = m.group().replace("_", "0");
		String clientVersion = args.get(1).replace(".", "0");
		int rv = Integer.parseInt(recentVersion);
		int cv = Integer.parseInt(clientVersion);
		if(rv > cv){
			return target;
		}
		return null;
	}
}








