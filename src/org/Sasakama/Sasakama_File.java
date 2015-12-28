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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


public class Sasakama_File{
	static final int SEEK_SET=0,SEEK_CUR=1,SEEK_END=2;
	static final int EOF=-1;
	String filename;
	ByteOrder byte_order;
	byte[] buffer;
	int position;
	
	public Sasakama_File(){
		filename = null;
		buffer   = null;
		byte_order = ByteOrder.LITTLE_ENDIAN;
		position = 0;
	}
	
	public Boolean open(String filename, String opt){
		this.filename = filename;
		
		try{
			File file = new File(filename);
			long fsize = file.length();
			buffer = new byte[(int)fsize];
			
			if(opt.equals("r")){
				FileInputStream fis = new FileInputStream(file);
				fis.read(buffer);
				fis.close();
			}
		}
		catch( IOException e ){
			Sasakama_Misc.error("Sasakama_File: can't open file:"+filename);
			e.printStackTrace();

			return false;
		}
	
		return true;
	}
	
	public Boolean open(String filename, String opt, ByteOrder byte_order){
		this.byte_order = byte_order;
		return open(filename, opt);
	}
	
	public Boolean open(String string){
		buffer = string.getBytes();
		return true;
	}
	
	public int read(Sasakama_File fp, int size){
		position = 0;
		buffer = new byte[size];
		int nread = fp.fread(buffer);
		if(nread != size){
			Sasakama_Misc.error("Sasakama_File.read: size error");
			System.exit(1);
		}
		return nread;	
	}
	
	public void close(){
		this.buffer = null;
	}
	
	public Boolean feof(){
		if(buffer.length <= position)
			return true;
		else
			return false;
	}
	
	public int fgetc(){
		if(feof() == true)
			return EOF;
		
		byte[] temp = new byte[1];
		int nread = fread(temp);
		if(nread == 1){
			int ival = temp[0] & 0xff;
			return ival;
		}
		else{
			Sasakama_Misc.error("fgetc:read size error");
			return -1;
		}
	}
	
	public int ftell(){
		return position;
	}
	
	public Boolean fseek(int offset, int origin){
		int new_pos = 0;
		
		switch(origin){
		case SEEK_SET:
			new_pos = offset;
			break;
		case SEEK_CUR:
			new_pos = position + offset;
			break;
		case SEEK_END:
			new_pos = buffer.length + offset;
			break;
		}
		
		if(new_pos < 0 || new_pos > buffer.length)
			return true;
		else{
			position = new_pos;
			return false;
		}
	}
	
	public int fread(byte[] buf){
		int copylen = buf.length;
	//	System.err.printf("pos:%d,  copylen:%d, buflen:%d\n", position, copylen, buffer.length);		
		if(position+copylen > buffer.length){
			Sasakama_Misc.error("Error: Sasakama_File.fread: position + copylen > buffer.length");
			//System.err.printf("pos:%d,  copylen:%d, buflen:%d\n", position, copylen, buffer.length);
			System.exit(1);
		}

		for(int i=0;i < copylen;i++){
			buf[i] = buffer[position++];
		}
		
		return copylen;
	}
	
	public int fread(short[] buf){
		int nbyte = Short.SIZE / Byte.SIZE;
		byte[] temp = new byte[buf.length * nbyte];
		
		int nread = fread(temp);
		nread /= nbyte;
		
		ByteBuffer bb = ByteBuffer.wrap(temp).order(byte_order);
		for(int i=0;i < nread;i++){
			buf[i] = bb.getShort();
		}

		return nread;
	}
	
	public int fread(int[] buf){
		int nbyte = Integer.SIZE / Byte.SIZE;
		byte[] temp = new byte[buf.length * nbyte];
		
		int nread = fread(temp);
		nread /= nbyte;

		ByteBuffer bb = ByteBuffer.wrap(temp).order(byte_order);
		for(int i=0;i < nread;i++){
			buf[i] = bb.getInt();
		}
		
		return nread;
	}
	
	public int fread(long[] buf){
		int nbyte = Long.SIZE / Byte.SIZE;
		byte[] temp = new byte[buf.length * nbyte];
		
		int nread = fread(temp);
		nread /= nbyte;
		
		ByteBuffer bb = ByteBuffer.wrap(temp).order(byte_order);
		for(int i=0;i < nread;i++){
			buf[i] = bb.getLong();
		}
		
		return nread;
	}
	
	public int fread(float[] buf){
		int nbyte = Float.SIZE / Byte.SIZE;
		byte[] temp = new byte[buf.length * nbyte];

		int nread = fread(temp);
		nread /= nbyte;
		
		ByteBuffer bb = ByteBuffer.wrap(temp).order(byte_order);
		for(int i=0;i < nread;i++){
			buf[i] = bb.getFloat();
		}
		
		return nread;
	}

	public int fread(double[] buf){
		int nbyte = Double.SIZE / Byte.SIZE;
		byte[] temp = new byte[buf.length * nbyte];
		
		int nread = fread(temp);
		nread /= nbyte;
		
		ByteBuffer bb = ByteBuffer.wrap(temp).order(byte_order);
		for(int i=0;i < nread;i++){
			buf[i] = bb.getDouble();
		}
		
		return nread;
	}

	public String readLine(){
		ArrayList<Byte> temp = new ArrayList<Byte>();
		
		while(true){
			byte b = (byte)fgetc();
			if(b == '\n')
				break;
			temp.add(b);
		}
		
		byte[] bbuf = new byte[temp.size()];
		for(int i=0;i < temp.size();i++)
			bbuf[i] = temp.get(i).byteValue();
		String retString = new String(bbuf);

		return retString;
	}
	
	public Boolean get_pattern_token(StringBuffer sb){
		sb.delete(0, sb.length());
		ArrayList<Byte> buff = new ArrayList<Byte>();
		Boolean squote = false, dquote = false;
		
		if(feof() == true)
			return false;
		
		byte c = (byte)fgetc();
		
		while(c == ' ' || c == '\n'){
			if(feof() == true)
				return false;
			c = (byte)fgetc();
		}
		
		if( c == '\''){
			if(feof() == true)
				return false;
			
			c = (byte)fgetc();
			squote = true;
		}
		if(c == '\"'){
			if(feof() == true)
				return false;
			
			c = (byte)fgetc();
			dquote = true;
		}
		
		if( c == ','){
			byte[] bb = new byte[1];
			bb[0] = c;
			sb.append(bb);
			return true;
		}
		
		while(true){
			buff.add(new Byte(c));
			c = (byte)fgetc();
			if(squote && c == '\'')
				break;
			if(dquote && c == '\"')
				break;
			if(!squote && !dquote){
				if(c == ' ')
					break;
				if(c == '\n')
					break;
				if(feof() == true)
					break;
			}
		}
		
		byte[] bbuf = new byte[buff.size()];
		for(int i=0;i < buff.size();i++)
			bbuf[i] = buff.get(i).byteValue();
		sb.append(new String(bbuf));
		
		return true;
	}
	
	Boolean get_token(StringBuffer sb){
		ArrayList<Byte> buff = new ArrayList<Byte>();
		sb.delete(0, sb.length());
		
		if(feof() == true)
			return false;
		
		byte c = (byte)fgetc();
		while(c == ' ' || c == '\n' || c == '\t'){
			if(feof() == true)
				return false;
			c = (byte)fgetc();
			if(c == EOF)
				return false;
		}
		for(;c != ' ' && c != '\n' && c!= '\t';){
			buff.add(new Byte(c));
			if(feof() == true)
				break;
			c = (byte)fgetc();
			if(c == EOF)
				break;
		}
		byte[] bbuf = new byte[buff.size()];
		for(int i=0;i < buff.size();i++)
			bbuf[i] = buff.get(i).byteValue();

		sb.append(new String(bbuf));
		return true;
	}
	
	Boolean get_token_with_separator(StringBuffer sb, char separator){
		sb.delete(0, sb.length());
		ArrayList<Character> buff = new ArrayList<Character>();
//		char[] buff = new char[Sasakama_Constant.MAXBUFLEN];
		if(feof() == true)
			return false;
		char c = (char)fgetc();
		while ( c == separator ){
			if(feof() == true)
				return false;
			c = (char)fgetc();
			if( c == EOF )
				return false;
		}
		for(; c!= separator;){
			buff.add(new Character(c));
			if(feof() == true)
				break;
			c = (char)fgetc();
			if(c == EOF)
				break;
		}
		char[] bbuf = new char[buff.size()];
		for(int i=0;i < buff.size();i++)
			bbuf[i] = buff.get(i).charValue();
		sb.append(new String(bbuf));
		
		return true;
	}
}
