package com.hakademy.util;

import java.text.DecimalFormat;

public class TimeUtil {
	private static DecimalFormat d = new DecimalFormat("#,###");
	public static String getDuration(long start, long end){
		return d.format(start-end);
	}
}
