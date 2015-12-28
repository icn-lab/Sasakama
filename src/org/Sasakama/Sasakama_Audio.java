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

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;



public class Sasakama_Audio extends Thread{
	int sampleRate, channels, sampleSizeInBits;
	ByteBuffer buffer;

	private SourceDataLine source;
	private AudioFormat format;
	private byte[] playBuffer;
	
	Boolean flag_play;
	Boolean active;
	
	Sasakama_Audio(){
		initialize();
	}
	
	public void initialize(){
		buffer           = null;
		sampleRate       = -1;
		sampleSizeInBits = 16;
		channels         = 1;
		flag_play        = false;
		playBuffer       = null;
		active           = false;
	}
	
	public void set_parameter(int sampling_frequency, int max_buff_size){
		sampleRate           = sampling_frequency;		

		// sampleRate, sampleSizeInBits, channels, signed?, bigendian?
		format = new AudioFormat((float)sampleRate, sampleSizeInBits, channels, true, true);
		//System.err.println(format.toString());
		
		buffer = ByteBuffer.allocate((Short.SIZE/Byte.SIZE) * max_buff_size);
	}
	
	public void open(){
		DataLine.Info info = new DataLine.Info( SourceDataLine.class, format );
        try {
			source = (SourceDataLine)AudioSystem.getLine( info );
			source.open( format );
		    source.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	public void run(){        
        active = true;

		while(active){
			if(flag_play){
				int pos = 0;
				do{
					int nwrite = source.write(playBuffer, pos, playBuffer.length - pos);
					pos += nwrite;
					//System.err.printf("play!");
				}while(pos < playBuffer.length);
				flag_play = false;
			}
			else{
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void waitPlayEnd(){
		while(flag_play)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void write(short data){
		buffer.putShort(data);
		
		if(buffer.hasRemaining() == false){
			byte[] temp_src = buffer.array();
			byte[] temp_buf = new byte[temp_src.length];
			for(int i=0;i < temp_buf.length;i++){
				temp_buf[i] = temp_src[i];
			}
			waitPlayEnd();
			playBuffer  = temp_buf;
			flag_play   = true;
			buffer.rewind();
		}
	}
	
	public Boolean isActive(){
		return active;
	}
	
	public void close(){
		flush();
		waitPlayEnd();
		source.drain();
		source.stop();
		source.close();
	}
	
	public void flush(){
		if(buffer.position() != 0){
			byte[] temp_src = buffer.array(); 
			byte[] temp_buf = new byte[buffer.position()];
			for(int i=0;i < temp_buf.length;i++){
				temp_buf[i] = temp_src[i];
			}
			waitPlayEnd();
			playBuffer = temp_buf;
			flag_play  = true;
		}
	}
	
	public void stopThread(){
		active = false;
	}
	
	public void clear(){
	}
}
