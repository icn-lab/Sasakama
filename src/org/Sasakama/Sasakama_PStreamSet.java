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

public class Sasakama_PStreamSet {
	Sasakama_PStream[] pstream;
	int nstream;
	int total_frame;

	Sasakama_PStreamSet(){
		initialize();
	}
	
	public void initialize(){
		pstream = null;
		nstream = 0;
		total_frame = 0;
	}
	
	public Boolean create(Sasakama_SStreamSet sss, double[] msd_threshold, double[] gv_weight){
		Boolean not_bound = false;
		
		if(nstream != 0){
			Sasakama_Misc.error("Sasakama_PStreamSet.create: Sasakama_PStreamSet should be clear.");
			return false;
		}
		
		nstream = sss.get_nstream();
		pstream = new Sasakama_PStream[nstream];
		total_frame = sss.get_total_frame();
		
		for(int i=0;i < nstream;i++){
			pstream[i]      = new Sasakama_PStream();
			Sasakama_PStream pst = pstream[i];
			if(sss.is_msd(i)){ /* for MSD */
				pst.length = 0;
				for(int state = 0;state < sss.get_total_state();state++)
				     if (sss.get_msd(i, state) > msd_threshold[i])
				    	 pst.length += sss.get_duration(state);
				
				pst.msd_flag = new Boolean[total_frame];
				for(int state = 0, frame = 0;state < sss.get_total_state();state++)
					if(sss.get_msd(i, state) > msd_threshold[i])
						for(int j=0;j < sss.get_duration(state);j++){
							pst.msd_flag[frame] = true;
							frame++;
						}
					else
						for(int j=0;j < sss.get_duration(state);j++){
							pst.msd_flag[frame] = false;
							frame++;
						}
			}
			else{
				pst.length = total_frame;
				pst.msd_flag = null;
			}
			pst.vector_length = sss.get_vector_length(i);
			pst.width = sss.get_window_max_width(i) * 2 + 1; /* band width of R */
			
			/* copy dynamic window */
			pst.win = new Sasakama_Window(sss.sstream[i].win);
			
			/* copy GV */
			if(sss.use_gv(i)){
				//pst.gv = new Sasakama_Gv(sss.sstream[i].gv);
				pst.gv = new Sasakama_Gv();
				pst.gv.mean = new double[pst.vector_length];
		        pst.gv.vari = new double[pst.vector_length];
		        pst.gv.gv_switch = new Boolean[pst.length];
				for(int j=0;j < pst.vector_length;j++){
					pst.gv.mean[j] = sss.get_gv_mean(i, j) * gv_weight[i];
					pst.gv.vari[j] = sss.get_gv_vari(i, j);
				}
				if(sss.is_msd(i)){
					int frame = 0;
					int msd_frame = 0;
					for (int state = 0; state < sss.get_total_state(); state++)
			               for (int j = 0; j < sss.get_duration(state); j++, frame++)
			                  if (pst.msd_flag[frame]){
			                      pst.gv.gv_switch[msd_frame++] = sss.get_gv_switch(i, state);
			    //                  System.err.printf("sss.get_gv_switch: (%d, %d):%s\n", 
			      //              		  i, state, sss.get_gv_switch(i, state)?"true":"false");
			                  }
				}
				else {       
					/* for non MSD */
					int frame = 0;
			        for (int state = 0; state < sss.get_total_state(); state++)
			        	for (int j = 0;j < sss.get_duration(state);j++){
			                  pst.gv.gv_switch[frame++] = sss.get_gv_switch(i, state);
			        //          System.err.printf("sss.get_gv_switch: (%d, %d):%s\n", 
		                //    		  i, state, sss.get_gv_switch(i, state)?"true":"false");
			        	}
			        }

				pst.gv.length = 0;
				for (int j = 0; j < pst.length; j++)
		            if (pst.gv.gv_switch[j])
		               pst.gv.length++;
			}
			else{
				pst.gv = new Sasakama_Gv();
				pst.gv.gv_switch = null;
		        pst.gv.length = 0;
		        pst.gv.mean = null;
		        pst.gv.vari = null;
			}
			
			if(pst.length > 0){
				pst.sm = new Sasakama_SMatrices();
				pst.sm.create(pst.length, pst.vector_length, pst.win.size, pst.width);
				pst.par = new double[pst.length][pst.vector_length];
			}
			
			/* copy pdf */
			if(sss.is_msd(i)){
				int frame = 0;
				int msd_frame = 0;
	
				for(int state = 0;state < sss.get_total_state();state++){
					for(int j=0;j < sss.get_duration(state);j++){
						if(pst.msd_flag[frame]){
							/* check current frames is MSD boundary or not */
							for(int k=0;k < pst.win.size;k++){
								not_bound = true;
								for(int shift = pst.win.l_width[k];shift <= pst.win.r_width[k];shift++)
									if(frame + shift < 0 || 
											total_frame <= frame + shift || 
											!pst.msd_flag[frame+shift]){
										not_bound = false;
										break;
									}
								for(int l=0;l < pst.vector_length;l++){
									int m = pst.vector_length * k + l;
									pst.sm.mean[msd_frame][m] = sss.get_mean(i, state, m);
							//		System.err.printf("sm.mean[%d][%d]:%5.2f\n", msd_frame, m, pst.sm.mean[msd_frame][m]);
									if(not_bound || k == 0)
										pst.sm.ivar[msd_frame][m] = finv(sss.get_vari(i, state, m));
									else
										pst.sm.ivar[msd_frame][m] = 0.0;
								//	System.err.printf("sm.ivar[%d][%d]:%5.2f\n", msd_frame, m, pst.sm.ivar[msd_frame][m]);

								}
							}
							msd_frame++;
						}
						frame++;
					}
				}
			} 
			else{
				int frame = 0;
				for(int state = 0;state < sss.get_total_state();state++){
					for(int j=0;j < sss.get_duration(state);j++){
						for(int k=0;k < pst.win.size;k++){
							not_bound = true;
							for(int shift = pst.win.l_width[k];shift<=pst.win.r_width[k];shift++)
								if(frame+shift < 0 || total_frame <= frame + shift){
									not_bound = false;
									break;
								}
							for(int l=0;l < pst.vector_length;l++){
								int m = pst.vector_length * k + l;
								pst.sm.mean[frame][m] = sss.get_mean(i, state, m);
								// System.err.printf("sm.mean[%d][%d]:%5.2f\n", frame, m, pst.sm.mean[frame][m]);

								if(not_bound || k == 0)
									pst.sm.ivar[frame][m] = finv(sss.get_vari(i, state, m));
								else
									pst.sm.ivar[frame][m] = 0;
								// System.err.printf("sm.ivar[%d][%d]:%5.2f\n", frame, m, pst.sm.ivar[frame][m]);

							}
						}
						frame++;
					}
				}
			}
			/* parameter generation */
			pst.mlpg();
		}
		return true;
	}
	
	private double finv(final double x){
		if(x >= Sasakama_Constant.INFTY2)
			return 0.0;
		if(x <= -Sasakama_Constant.INFTY2)
			return 0.0;
		if(x <= Sasakama_Constant.INVINF2 && x >= 0)
			return Sasakama_Constant.INFTY;
		if(x >= -Sasakama_Constant.INVINF2 && x <= 0)
			return -Sasakama_Constant.INFTY;
		
		return(1.0/x);
	}
	
	public int get_nstream(){
		return nstream;
	}
	
	public int get_vector_length(int stream_index){
		return pstream[stream_index].vector_length;
	}
	
	public int get_total_frame(){
		return total_frame;
	}
	
	public double get_parameter(int stream_index, int frame_index, int vector_index){
		return pstream[stream_index].par[frame_index][vector_index];
	}
	
	public double[] get_parameter_vector(int stream_index, int frame_index){
		return pstream[stream_index].par[frame_index];
	}
	
	public Boolean get_msd_flag(int stream_index, int frame_index){
		return pstream[stream_index].msd_flag[frame_index];
	}
	
	public Boolean is_msd(int stream_index){
		return pstream[stream_index].msd_flag != null ? true : false;
	}
	
	public void clear(){
		initialize();
	}
}
