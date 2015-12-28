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

public class Sasakama_GStreamSet {
	static final double SASAKAMA_NODATA = Sasakama_Constant.LZERO;
	int total_nsample;
	int total_frame;
	int nstream;
	Sasakama_GStream[] gstream;
	double[] gspeech;
	
	public Sasakama_GStreamSet(){
		initialize();
	}
	
	public void initialize(){
		nstream = 0;
		total_frame = 0;
		total_nsample = 0;
		gstream = null;
		gspeech = null;
	}
	
	public Boolean create(Sasakama_PStreamSet pss, Sasakama_Condition condition, Sasakama_Audio audio){
		/* check */
		if(gstream != null || gspeech != null){
			Sasakama_Misc.error("Sasakama_GStreaSet.create: not initialized().");
			return false;
		}
		
		nstream       = pss.get_nstream();
		total_frame   = pss.get_total_frame();
		total_nsample = condition.fperiod * total_frame;
		gstream       = new Sasakama_GStream[nstream];
		
		for(int i=0;i < nstream;i++){
			gstream[i] = new Sasakama_GStream(total_frame, pss.get_vector_length(i));
		}
		
		gspeech = new double[total_nsample];
		
		/* copy generated parameter */
		for(int i=0;i < nstream;i++){
			if(pss.is_msd(i)){
			//	System.err.printf("is_msd(%d):true\n", i);
				int msd_frame = 0;
				for(int j=0;j < total_frame;j++)
					if(pss.get_msd_flag(i, j)){
						//System.err.printf("msd_flag(%d,%d):true\n", i, j);
						for(int k=0;k < gstream[i].vector_length;k++){
							gstream[i].par[j][k] = pss.get_parameter(i, msd_frame, k);
							//System.err.printf("gstream[%d].par[%d][%d]:%5.2f\n", i, j, k, gstream[i].par[j][k]);
						}
						msd_frame++;
					}
					else
						for(int k=0;k < gstream[i].vector_length;k++)
							gstream[i].par[j][k] = SASAKAMA_NODATA;
			}
			else{
				for(int j=0;j < total_frame;j++)
					for(int k=0;k < gstream[i].vector_length;k++){
						gstream[i].par[j][k] = pss.get_parameter(i,j,k);
						//System.err.printf("gstream[%d].par[%d][%d]:%5.2f\n", i, j, k, gstream[i].par[j][k]);
					}
			}
		}
		
		/* check */
		if(nstream != 2 && nstream != 3){
			Sasakama_Misc.error("Sasakama_GStreamSet.create: The number of streams should be 2 or 3.");
			clear();
			return false;
		}
		if(pss.get_vector_length(1) != 1){
			Sasakama_Misc.error("Sasakama_GStreamSet.create: The size of lf0 static vector should be 1.");
			return false;
		}
		if(nstream >= 3 && gstream[2].vector_length % 2 == 0){
			Sasakama_Misc.error("Sasakama_GStreamSet.create: The sizeo of lf0 static vector should be 1.");
			return false;
		}
		
		/* synthesize speech waveform */
		Sasakama_Vocoder v = new Sasakama_Vocoder();
		v.initialize(gstream[0].vector_length-1, condition);
		int nlpf = 0;
		if(nstream >=3)
			nlpf = gstream[2].vector_length;
		//System.err.printf("total_frame:%d\n", total_frame);
		
		if(audio != null){
			//System.err.printf("audio_open!\n");
			audio.open();
			if(audio.isActive() == false)
				audio.start();
		}
		
		for(int i=0;i < total_frame && condition.stop == false;i++){
			int j = i * condition.fperiod;
			double[] lpf = null;
			if(nstream >= 3)
				lpf = gstream[2].par[i];
			v.synthesize(gstream[0].vector_length-1, gstream[1].par[i][0], gstream[0].par[i], 0, nlpf, lpf, 
						condition, gspeech, j, audio);
		}
		
		if(audio != null){
			audio.close();
		}
		
		v.clear();

		return true;
	}
	
	public int get_total_nsamples(){
		return total_nsample;
	}
	
	public int get_total_frame(){
		return total_frame;
	}
	
	public int get_vector_length(int stream_index){
		return gstream[stream_index].vector_length;
	}
	
	public double get_speech(int sample_index){
		return gspeech[sample_index];
	}
	
	public double get_parameter(int stream_index, int frame_index, int vector_index){
		return gstream[stream_index].par[frame_index][vector_index];
	}
	
	public void clear(){
		gstream = null;	
		gspeech = null;
		
		initialize();
	}
}
