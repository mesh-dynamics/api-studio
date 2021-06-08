package io.md.utils;

import java.util.regex.Pattern;

public class ApiPathRegex{

	public final String pathRegex;
	public final Pattern pattern ;

	//Todo : validation
	public ApiPathRegex(String name){
		this.pathRegex = name ;
		this.pattern = getPattern(name);
	}

	public  boolean matches(String apiPath){
		return pattern.matcher(apiPath).matches();
	}

	private Pattern getPattern(String apiPathRegex){

		String[] paths = apiPathRegex.split("/");
		int len = paths.length;
		if(len<1 || apiPathRegex.indexOf('*')==-1) throw new IllegalArgumentException("Not a valid apiPathRegex "+apiPathRegex);
		for(String path : paths){
			if(path.length()>1 && path.indexOf('*')!=-1) throw new IllegalArgumentException("Not a valid apiPathRegex "+apiPathRegex);
		}

		for(int i=0 ; i<len ; i++){
			if(paths[i].equals("*")){
				paths[i] = "[^/]+";
			}
		}
		paths[0] = "^"+paths[0];
		paths[len-1] = paths[len-1] + "$";

		return Pattern.compile(String.join("/" , paths));
	}


	private void validate(String regex) throws Exception{
		// should not start or end with /
		//todo
		// case /
		// case *
	}
}
