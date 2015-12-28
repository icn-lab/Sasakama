// This software is a language translation version of "hts_engine API" developed by HTS Working Group.
// 
// Copyright (c) 2015 Intelligent Communication Network (Ito-Nose) Laboratory
// Tohoku University
// Copyright (c) 2001-2015 Nagoya Institute of Technology
// Department of Computer Science
// 2001-2008 Tokyo Institute of Technology
// Interdisciplinary Graduate School of
// Science and Engineering
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright notice, 
// this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice, 
// this list of conditions and the following disclaimer in the documentation 
// and/or other materials provided with the distribution.
// * Neither the name of the "Intelligent Communication Network Laboratory, Tohoku University" nor the names of its contributors 
// may be used to endorse or promote products derived from this software 
// without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.Sasakama;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Sasakama_Misc {
	public static Boolean is_num(String string){
		return string.matches("^[-]?\\d+$");
	}
	
	public static Boolean is_graph(String string){
		return string.matches("^\\S+$");
	}
	
	public static int name2num(String buff){
		String regex = "(\\d+)$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(buff);
		
		if(m.find()){
			int value = new Integer(m.group(1)).intValue();
			return value;
		}
		else{
			return -1;
		}
	}
	
	public static int get_state_num(String string){
		String regex = "\\[(\\d+)\\]";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(string);
		
		if(m.find()){
			int value = new Integer(m.group(1)).intValue();
			return value;
		}
		else{
			return 0;
		}
	}
	
	public static Boolean strequal(final String s1, final String s2){	
		if(s1 == null && s2 == null)
			return true;
		else if(s1 == null || s2 == null)
			return false;
		else
			return s1.equals(s2);
	}
	
	public static Boolean get_token_from_string(final String string, int[] index, StringBuffer retstr){
		char  c;
		
		if(index[0] > string.length()-1)
			return false;
		c = string.charAt(index[0]);
		if(index[0]+1 > string.length()-1)
			return false;
		c = string.charAt(index[0]++);
		
		while( c == ' ' || c == '\n' || c == '\t'){
			if(index[0] > string.length()-1)
				return false;
			c = string.charAt(index[0]++);
		}
		for(;c!=' ' && c!='\n' && c!='\t' && index[0] < string.length();){
			retstr.append(c);
			c = string.charAt(index[0]++);
		}

		return true;
	}
	
	public static Boolean get_token_from_string_sith_separator(final String str, int[] index, StringBuffer retstr, char separator){		
		if(str == null)
			return false;

		if(index[0]+1 >= str.length())
			return false;
		
        char c = str.charAt(index[0]);
		
		while(c == separator){
			if(index[0] > str.length()-1)
				return false;
			index[0]++;
			c = str.charAt(index[0]);
		}
	
		while( c != separator && index[0] < str.length()){
			retstr.append(c);
			index[0]++;
			c = str.charAt(index[0]);
		}
		if(index[0] != str.length())
			index[0]++;

		if(retstr.length() > 0){
			return true;
		}
		else{
			return false;
		}
	}
	
	public static Boolean dp_match(final String string, final String pattern, int pos, int max){
		//System.err.printf("in dp_match. string:%s pattern:%s pos:%d max:%d\n", string, pattern, pos, max);
		if(pos > max)
			return false;
		if(string.length() == 0 && pattern.length() == 0)
			return true;
		if(pattern.length() > 0 && pattern.charAt(0) == '*'){
			if(string.length() > 0 && Sasakama_Misc.dp_match(string.substring(1), pattern, pos+1, max) == true)
				return true;
			else
				return Sasakama_Misc.dp_match(string, pattern.substring(1), pos, max); 
		}
		if(pattern.length() > 0 && string.length() > 0 && (string.charAt(0) == pattern.charAt(0) || pattern.charAt(0) == '?')){
			if(Sasakama_Misc.dp_match(string.substring(1), pattern.substring(1),  pos+1,  max+1) == true)
				return true;
		}
		
		return false;
	}
	
	public static Boolean pattern_match(final String string, final String pattern){
		int max=0, nstar = 0, nquestion = 0;
		
		for(int i=0;i < pattern.length();i++){
			switch(pattern.charAt(i)){
			case '*':
				nstar++;
				break;
			case '?':
				nquestion++;
				max++;
				break;
			default:
				max++;
			}
		}
		
		//System.err.printf("pattern:%s nstar:%d nquestion:%d\n", pattern, nstar, nquestion);
		if(nstar == 2 && nquestion == 0 && pattern.charAt(0) == '*' && pattern.charAt(pattern.length()-1)=='*'){
			int buff_length = pattern.length()-2;
			int j = 1;
			char[] buff = new char[buff_length];
			for(int i=0;i < buff_length;i++, j++)
				buff[i] = pattern.charAt(j);
			String tmpstr = new String(buff);
			//System.err.printf("string:%s tmpstr:%s\n", string, tmpstr);
			if(string.indexOf(tmpstr) != -1)
				return true;
			else
				return false;
		}
		else
			return Sasakama_Misc.dp_match(string, pattern, 0, string.length() - max);
	}
	
	public static void error(String string){
		System.err.println(string);
	}
}
