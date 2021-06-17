/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
