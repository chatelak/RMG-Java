
/*********************************************************************
	File Path	: RMG\RMG\jing\rxnSys\JDASSL.java
*********************************************************************/

//!********************************************************************************
//!
//!    RMG: Reaction Mechanism Generator
//!
//!    Copyright: Jing Song, MIT, 2002, all rights reserved
//!
//!    Author's Contact: jingsong@mit.edu
//!
//!    Restrictions:
//!    (1) RMG is only for non-commercial distribution; commercial usage
//!        must require other written permission.
//!    (2) Redistributions of RMG must retain the above copyright
//!        notice, this list of conditions and the following disclaimer.
//!    (3) The end-user documentation included with the redistribution,
//!        if any, must include the following acknowledgment:
//!        "This product includes software RMG developed by Jing Song, MIT."
//!        Alternately, this acknowledgment may appear in the software itself,
//!        if and wherever such third-party acknowledgments normally appear.
//!
//!    RMG IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
//!    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
//!    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//!    DISCLAIMED.  IN NO EVENT SHALL JING SONG BE LIABLE FOR
//!    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
//!    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
//!    OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
//!    OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//!    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//!    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
//!    THE USE OF RMG, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//!
//!******************************************************************************



package jing.rxnSys;


import java.awt.image.IndexColorModel;
import jing.rxn.*;
import jing.chem.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import jing.chem.Species;
import jing.rxn.PDepNetwork;
import jing.rxn.Reaction;
import jing.rxn.Structure;
import jing.rxn.TROEReaction;
import jing.rxn.ThirdBodyReaction;
import jing.param.Global;
import jing.param.Pressure;
import jing.param.Temperature;
import jing.param.ParameterInfor;

//## package jing::rxnSys

//----------------------------------------------------------------------------
// jing\rxnSys\JDASPK.java
//----------------------------------------------------------------------------

//## class JDASSL
public class JDASSL extends JDAS {

	private JDASSL() {
        super();
    }
    
	public JDASSL(double p_rtol, double p_atol, int p_parameterInfor, 
			InitialStatus p_initialStatus, int p_index, ValidityTester p_vt, 
			boolean p_autoflag) {
        super(p_rtol, p_atol, p_parameterInfor, p_initialStatus, p_index, p_vt, 
			p_autoflag);
    }

    // DASSL is not used for sensitivity analysis, so this function shouldn't be needed
	/*private LinkedList generateSensitivityStatus(ReactionModel p_reactionModel, double [] p_y, double [] p_yprime, int p_paraNum) {
    	int neq = p_reactionModel.getSpeciesNumber()*(p_paraNum+1);
    	if (p_y.length != neq) throw new DynamicSimulatorException();
    	if (p_yprime.length != neq) throw new DynamicSimulatorException();

    	LinkedList senStatus = new LinkedList();

    	if (p_paraNum > 0){
    		for (int i = p_reactionModel.getSpeciesNumber();i<neq;i++){
    			double sens = p_y[i];
    			double sflux = p_yprime[i];
    			int reaction_num = i/p_reactionModel.getSpeciesNumber();
    			int species_num = (i+1)%p_reactionModel.getSpeciesNumber();
    			if (species_num == 0){
    				species_num = p_reactionModel.getSpeciesNumber();
    			}
    			//String name = "dC"+String.valueOf(species_num)+"/dk"+String.valueOf(reaction_num);
    			//System.out.println(name + '\t' + String.valueOf(sens) + '\t' + String.valueOf(sflux));
    				SensitivityStatus ss = new SensitivityStatus(sens, sflux, species_num, reaction_num);
    				int index = i-p_reactionModel.getSpeciesNumber();
    				senStatus.add(ss);
    		}
    	}
    	return senStatus;
    }*/	 

    //## operation solve(boolean,ReactionModel,boolean,SystemSnapshot,ReactionTime,ReactionTime,Temperature,Pressure,boolean)
    public SystemSnapshot solve(boolean p_initialization, ReactionModel p_reactionModel, boolean p_reactionChanged, SystemSnapshot p_beginStatus, ReactionTime p_beginTime, ReactionTime p_endTime, Temperature p_temperature, Pressure p_pressure, boolean p_conditionChanged, TerminationTester tt, int p_iterationNum) {
        
    	outputString = new StringBuilder();
        //first generate an id for all the species
    	Iterator spe_iter = p_reactionModel.getSpecies();
    	while (spe_iter.hasNext()){
    		Species spe = (Species)spe_iter.next();
    		int id = getRealID(spe);
    	}
    	double startTime = System.currentTimeMillis();
		
		ReactionTime rt = p_beginStatus.getTime();
        if (!rt.equals(p_beginTime)) throw new InvalidBeginStatusException();

        double tBegin = p_beginTime.getStandardTime();
        double tEnd = p_endTime.getStandardTime();

		double T = p_temperature.getK();
		double P = p_pressure.getAtm();
		
		LinkedList initialSpecies = new LinkedList();
		
		// set reaction set
        if (p_initialization || p_reactionChanged || p_conditionChanged) {
			
        	nState = p_reactionModel.getSpeciesNumber();
			nParameter = 0;
			if (parameterInfor != 0) {
				nParameter = p_reactionModel.getReactionNumber(); //svp
				if (initialStatus == null) System.out.println("initialStatus = null");
				spe_iter = initialStatus.getSpeciesStatus();
				while (spe_iter.hasNext()) {
					SpeciesStatus ss = (SpeciesStatus) spe_iter.next();
					String name = ss.getSpecies().getName();
					initialSpecies.add(name);
					nParameter++;
				}
			}
			neq = nState*(nParameter + 1);
			
//			tbrString is a combination of a integer and a real array
			//real array format:  rate, A, n, Ea, Keq, inertEfficiency, e1, e2, ..., e10  (16 elements)
			//int array format :  nReac, nProd, r1, r2, r3, p1, p2, p3, rev(=1 or -1), ncollider, c1, c2,..c10 (20 elements)
			tbrString = generateThirdBodyReactionList(p_reactionModel, p_beginStatus, p_temperature, p_pressure);
			
			//troeString is a combination of a integer and a real array
			//real array format:  rate, A, n, Ea, Keq, inertEfficiency, e1, e2, ..., e10, alpha, Tstar, T2star, T3star, lowRate  (21 elements)
			//int array format :  nReac, nProd, r1, r2, r3, p1, p2, p3, rev(=1 or -1), ncollider, c1, c2,..c10, troe(0=T or 1=F) (21 elements)
        	troeString = generateTROEReactionList(p_reactionModel, p_beginStatus, p_temperature, p_pressure);
			
			//rString is a combination of a integer and a real array
			//real array format:  rate, A, n, Ea, Keq
			//int array format :  nReac, nProd, r1, r2, r3, p1, p2, p3, rev(=1 or -1)
			rString = generatePDepODEReactionList(p_reactionModel, p_beginStatus, p_temperature, p_pressure);
			
			
			initializeWorkSpace();
			initializeConcentrations(p_beginStatus, p_reactionModel, p_beginTime, p_endTime, initialSpecies);
			
			
        }
        else
        	info[0] = 1;
        //5/5/08 gmagoon: (next two lines) for autoflag, get binary 0 or 1 corresponding to boolean true/false
        int af = 0;
        if (autoflag) af = 1;
        if (tt instanceof ConversionTT){
                SpeciesConversion sc = (SpeciesConversion)((ConversionTT)tt).speciesGoalConversionSet.get(0);    
                outputString.append(nState + "\t" + neq + "\t" +  getRealID(sc.species) + "\t" +conversionSet[p_iterationNum]+"\t"+ af+"\n");//5/5/08 gmagoon: added autoflag, needed when using dasslAUTO.exe
    		
        }
        else{
        	outputString.append(nState + "\t" + neq + "\t" +  -1 + "\t" +0+ "\t"+ af+"\n");//5/5/08 gmagoon: added autoflag, needed when using dasslAUTO.exe
    		
        }
        for (int i=0; i<neq; i++)
			outputString.append(y[i]+" ");
		outputString.append("\n");
		for (int i=0; i<neq; i++)
			outputString.append(yprime[i]+" ");
		outputString.append("\n" + tBegin+" "+tEnd+"\n");
		for (int i=0; i<30; i++)
			outputString.append(info[i]+" ");
		outputString.append("\n"+ rtol + " "+atol);
		outputString.append("\n" + p_temperature.getK() + " " + p_pressure.getPa() + "\n" + rList.size() + "\n" + rString.toString() + "\n" + thirdBodyList.size() + "\n"+tbrString.toString() + "\n" + troeList.size() + "\n" + troeString.toString()+"\n");
	
        //4/30/08 gmagoon: code for providing edge reaction info to DASSL in cases if the automatic time stepping flag is set to true
		if (autoflag)
			getAutoEdgeReactionInfo((CoreEdgeReactionModel) p_reactionModel, p_temperature, p_pressure);
        
		// Add flags that specify whether the concentrations are constant or not
		getConcentractionFlags(p_reactionModel);
        
        int idid=0;
        LinkedHashMap speStatus = new LinkedHashMap();
        LinkedList senStatus = new LinkedList();
        
		int temp = 1;
        Global.solverPrepossesor = Global.solverPrepossesor + (System.currentTimeMillis() - startTime)/1000/60;
		if (nParameter==0) {
			startTime = System.currentTimeMillis();
        	
			idid = solveDAE();
			
			if (idid !=1 && idid != 2 && idid != 3)	{
				System.out.println("The idid from DASPK was "+idid );
				throw new DynamicSimulatorException("DASPK: SA off.");
        	}
            System.out.println("After ODE: from " + String.valueOf(tBegin) + " SEC to " + String.valueOf(endTime) + "SEC");
			Global.solvertime = Global.solvertime + (System.currentTimeMillis() - startTime)/1000/60;
			startTime = System.currentTimeMillis();
        	speStatus = generateSpeciesStatus(p_reactionModel, y, yprime, 0);
			Global.speciesStatusGenerator = Global.speciesStatusGenerator + (System.currentTimeMillis() - startTime)/1000/60;
        }
        

		SystemSnapshot sss = new SystemSnapshot(new ReactionTime(endTime,"sec"), speStatus, p_beginStatus.getTemperature(), p_beginStatus.getPressure());
		LinkedList reactionList = new LinkedList();
		reactionList.addAll(rList);
        reactionList.addAll(duplicates);
        reactionList.addAll(thirdBodyList);
        reactionList.addAll(troeList);
        sss.setReactionList(reactionList);
		sss.setReactionFlux(reactionFlux);
                
        return sss;
    }

	private int solveDAE() {	
		super.solveDAE("dasslAUTO.exe");
		return readOutputFile("ODESolver/SolverOutput.dat");
	}
        
	public int readOutputFile(String path) {
	
		//read the result
        File SolverOutput = new File(path);
        try {
        	FileReader fr = new FileReader(SolverOutput);
        	BufferedReader br = new BufferedReader(fr);
        	String line = br.readLine();
        	Global.solverIterations = Integer.parseInt(line.trim());
        	line = br.readLine();
        	
        	if (Double.parseDouble(line.trim()) != neq) {
        		System.out.println("ODESolver didnt generate all species result");
        		System.exit(0);
        	}
        	endTime = Double.parseDouble(br.readLine().trim());
        	for (int i=0; i<neq; i++){
        		line = br.readLine();
        		y[i] = Double.parseDouble(line.trim());
        	}
        	
        	for (int i=0; i<neq; i++){
        		line = br.readLine();
        		yprime[i] = Double.parseDouble(line.trim());
        	}
        	reactionFlux = new double[rList.size()+thirdBodyList.size()+troeList.size()];
        	for (int i=0; i<rList.size()+thirdBodyList.size()+troeList.size(); i++){
        		line = br.readLine();
        		reactionFlux[i] = Double.parseDouble(line.trim());
        	}
        	
        }
        catch (IOException e) {
        	String err = "Error in reading Solver Output File! \n";
        	err += e.toString();
        	e.printStackTrace();
        	System.exit(0);
        }
        SolverOutput.delete();

		return 1;
	}
	
	@Override
	protected void initializeWorkSpace() {
		super.initializeWorkSpace();
		//info[4] = 1; //use analytical jacobian
	}
	
}
