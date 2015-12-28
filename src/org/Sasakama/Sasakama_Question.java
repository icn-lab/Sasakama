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

public class Sasakama_Question {
	String string;
	Sasakama_Pattern head;
	Sasakama_Question next;

	public Sasakama_Question(){
		initialize();
	}
	
	public void initialize(){
		string = null;
		head   = null;
		next   = null;
	}
	
	public void clear(){
		if(string != null)
			string = null;
	
		head = null;
		initialize();
	}
	
	public Boolean load(Sasakama_File hf){
		if(hf.feof() == true)
			return false;
		clear();

		StringBuffer sb= new StringBuffer();
		if(hf.get_pattern_token(sb) == false)
			return false;
		this.string = sb.toString();
		//System.err.printf("string1:%s\n", this.string);
		
		sb = new StringBuffer();
		if(hf.get_pattern_token(sb) == false){
			clear();
			return false;
		}
		//System.err.printf("string2:%s\n", sb.toString());
			
		Sasakama_Pattern last_pattern = null;
		if(sb.charAt(0) == '{'){
			while(true){
				sb = new StringBuffer();
				if(hf.get_pattern_token(sb) == false){
					clear();
					return false;
				}
				//System.err.printf("string3:%s\n", sb.toString());
				
				Sasakama_Pattern pattern = new Sasakama_Pattern();
				if(head != null)
					last_pattern.next = pattern;
				else
					head = pattern;
				pattern.string = sb.toString();
				pattern.next = null;
				sb = new StringBuffer();
				if(hf.get_pattern_token(sb) == false){
					clear();
					return false;
				}
				//System.err.printf("string4:%s\n", sb.toString());
				if(sb.charAt(0) == '}')
					break;

				last_pattern = pattern;
			}
		}
		return true;
	}
	
	public Boolean match(final String string){
		for(Sasakama_Pattern pattern = head;pattern != null;pattern=pattern.next)
			if(Sasakama_Misc.pattern_match(string, pattern.string))
				return true;

		return false;
	}
	
	public Sasakama_Question find(final String string){
		for(Sasakama_Question question = this;question != null;question=question.next)
			if(string.equals(question.string) == true)
				return question;
		return null;
	}
	
	
}	

