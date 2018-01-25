package stochasticModel;

import ilog.concert.*;
import ilog.cplex.*;
import solution.*;

import java.util.*;

public class EV {
	public static double solve(Solution openingEV){
		double result = 0;
		
		//Sets
		int IMax = 5; // Number of hydro plants
		int JMax = 4; // Number of thermal plants
		int TMax = 24; // Number time periods
		int PMax = 4; // Number of projects
		
		Set<Integer> P = new HashSet<Integer>(); // set of projects
		for(int p=0; p<PMax; p++){
			P.add(p);
		}
		Set<Integer> P_M = new HashSet<Integer>(); // set of mandatory projects
		int[][] P_C = {{},{},{},{}}; // set of projects, which conﬂict with project p
		int[][] P_A = {{},{},{},{}}; // set of projects, which are associated with project p
		Set<Integer> T = new HashSet<Integer>(); // set of stages
		Set<Integer> T1 = new HashSet<Integer>(); // set of stages incl. T+1
		for(int t=0; t<TMax; t++){
			T.add(t);
			T1.add(t+1);
		}
		T1.add(0);
		int[][] T_p = {{4,6,9},{4,8,12},{4,11,19},{4,14,22}}; // set of stages, in which project p can be realized
		Set<Integer> T_E = new HashSet<Integer>(); // set of stages, in which new CO 2 emission allowances become available
		for(int i=0; i< (int) Math.floor(TMax/5); i++){
			T_E.add(1+5*i);
		}
		Set<Integer> I = new HashSet<Integer>(); // set of hydro plants
		for(int i=0; i<IMax; i++){
			I.add(i);
		}
		int[] I_ex = {0,2,4}; // hydro plants, which already exist. Guajoya + Cerron Grande, 15 de Septiembre, Rio Lindo
		int[][] I_p = {{1},{3},{},{}}; // set of hydro plants, which are associated with project p
		int[][] U_i = {{},{0},{1},{},{3}}; // set of hydro plants, which are immediately upstream of plant i
		Set<Integer> J = new HashSet<Integer>(); // set of thermal plants
		for(int j=0; j<JMax; j++){
			J.add(j);
		}
		int[] J_ex = {0,3}; // thermal plants, which already exist.
		int[][] J_p = {{},{},{1},{2}}; // set of thermal plants, which are associated with project p
		
		//Parameters
		double[] U_min = {0, 0, 0, 0, 0}; // minimum release level of hydro generation in plant i
		double[] U_max = {1060.64, 547.2, 1982.02, 60, 70.71}; // maximum release level of hydro generation in plant i
		double[] V_min = {1156.66, 66.05, 219.24, 1270, 0}; // lower limit of hydro reservoir level in plant i
		double[] V_max = {2931.9, 107.09, 306.95, 1785, 0}; // upper limit of hydro reservoir level in plant i
		double[] G_min = {0, 0, 0, 0}; // minimum level of thermal generation in plant j
		double[] G_max = {303.3, 290.27, 46.66, 78.49}; // maximum level of thermal generation in plant j
		double[] rho = {0.24, 0.13, 0.07, 0.36, 0.84}; // hydro generation eﬃciency coeﬃcient of plant i
		double[][] c_inv = new double[PMax][TMax]; // discounted and adjusted investment cost for project p at stage t
		for(int p:P){
			for(int t:T_p[p]){
				if(p==0){
					c_inv[p][t] = 3333.46*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==1){
					c_inv[p][t] = 18594.6*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==2){
					c_inv[p][t] = 7.75*G_max[1]*Math.pow(10, 3)*Math.pow(1.001, t);
				}
				else if(p==3){
					c_inv[p][t] = 1.26*G_max[2]*Math.pow(10, 4)*Math.pow(1.001, t);
				}
			}
		}
		double[][] c_op = new double[JMax][TMax]; // thermal generation price in plant j at stage t
		for(int j:J){
			for(int t:T){
				if(j==0){
					c_op[j][t] = 10;
				}
				else if(j==1){
					c_op[j][t] = 25;
				}
				else if(j==2){
					c_op[j][t] = 68.7;
				}
				else if(j==3){
					c_op[j][t] = 56.41;
				}
			}
		}
		double[] M_r = new double[TMax]; // penalty for violating electricity demand fulﬁllment at stage t
		for(int t:T){
			M_r[t] = 6000;
		}
		double[] M_o = new double[TMax]; // penalty for emission overspending at stage t
		for(int t:T){
			M_o[t] = 80;
		}
		double[] B = {0, 0, 0.56, 1.45}; // CO2 emissions coeﬃcient of plant j,
		double[] E_allow = new double[TMax]; // emissions allowance, which become available at stage t
		for(int t:T){
			E_allow[t] = 190000*2*Math.pow(0.98, t);
		}
		double[] D = {940.93, 872.4, 983.23, 951.7, 1029.93, 975.13, 1007.77, 995.57, 953.93, 986.27, 929.8, 948.2, 940.93, 872.4, 983.23, 951.7, 1029.93, 975.13, 1007.77, 995.57, 953.93, 986.27, 929.8, 948.2}; // electricity demand at stage t
		double[][] A = {{53.62, 40.50, 41.84, 42.98, 104.89, 478.17, 680.96, 923.46, 1662.25, 976.39, 189.68, 82.71, 67.71, 53.46, 32.36, 51.11, 218.29, 557.80, 708.38, 940.12, 1187.14, 633.17, 152.20, 95.46},{54.26, 45.34, 43.87, 55.16, 131.99, 509.54, 716.58, 1042.16, 1713.62, 997.33, 188.34, 83.08, 66.37, 52.83, 39.48, 56.04, 260.07, 613.21, 737.85, 1034.08, 1161.37, 678.17, 151.58, 102.21},{128.56, 116.22, 87.15, 122.76, 322.48, 961.37, 1358.43, 1912.32, 3114.60, 2359.78, 324.05, 180.47, 132.47, 101.61, 86.57, 114.41, 486.99, 1206.84, 1245.56, 1709.57, 2137.11, 1422.98, 382.27, 172.97},{12.24, 8.66, 6.66, 7.28, 31.25, 73.90, 66.51, 80.59, 92.79, 84.90, 35.35, 23.61, 12.92, 6.95, 6.26, 8.08, 30.02, 79.57, 101.46, 72.66, 96.36, 95.77, 38.66, 25.22},{16.71, 12.11, 10.33, 12.14, 40.44, 79.89, 78.50, 106.96, 111.59, 88.01, 43.46, 27.53, 21.26, 12.08, 12.63, 11.82, 32.35, 75.11, 84.57, 82.70, 88.41, 90.48, 52.43, 37.92}}; // water inﬂow in plant i at stage t
 		
		try{
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
			
			//Decision Variables
			IloIntVar x_inv[][] = new IloIntVar[PMax][]; // investment decision of project p at stage t
			for(int p:P){
				x_inv[p] = cplex.intVarArray(TMax, 0, 1);
			}
			IloNumVar x_r[] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE); // violation of electricity demand fulﬁllment at stage t
			IloNumVar x_o[] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE); // emission overspending at stage t
			IloNumVar v[][] = new IloNumVar[IMax][]; // hydro reservoir level of plant i at the beginning of stage t
			IloNumVar u[][] = new IloNumVar[IMax][]; // hydro generation level in plant i at stage t due to generation
			IloNumVar s[][] = new IloNumVar[IMax][]; // spillage level in plant i at stage t
			for(int i:I){
				v[i] = cplex.numVarArray(TMax+1, 0, Double.MAX_VALUE);
				u[i] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE);
				s[i] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE);
			}
			IloNumVar g[][] = new IloNumVar[JMax][]; // thermal generation level in plant j at stage t due to generation
			for(int j:J){
				g[j] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE);
			}
			IloNumVar e[] = cplex.numVarArray(TMax, 0, Double.MAX_VALUE); // emissions allowances left at the end of stage t
			
			//Constraints
			for(int p:P){
				IloLinearNumExpr c1 = cplex.linearNumExpr(); // A project can be realized at most once.
				for(int t:T_p[p]){
					c1.addTerm(1, x_inv[p][t]);
				}
				cplex.addLe(c1, 1);
			}
			
			for(int p:P_M){
				IloLinearNumExpr c2 = cplex.linearNumExpr(); // Mandatory projects must be realized.
				for(int t:T_p[p]){
					c2.addTerm(1, x_inv[p][t]);
				}
				cplex.addEq(c2, 1);
			}
			
			for(int p:P){
				IloLinearNumExpr c3 = cplex.linearNumExpr(); // Conﬂicting projects cannot be realized together.
				for(int q:P_C[p]){
					for(int t1:T_p[q]){
						c3.addTerm(1, x_inv[q][t1]);
					}
				}
				for(int t:T_p[p]){
					c3.addTerm(1, x_inv[p][t]);
				}
				cplex.addLe(c3, 1);
			}
			
			for(int p:P){
				for(int q:P_A[p]){
					IloLinearNumExpr c4 = cplex.linearNumExpr(); // Interdepentent projects need to be realized together.
					for(int t_p:T_p[p]){
						c4.addTerm(1, x_inv[p][t_p]);
					}
					for(int t_q:T_p[q]){
						c4.addTerm(-1, x_inv[q][t_q]);
					}
					cplex.addEq(c4, 0);
				}
			}
			
			for(int i:I){
				for(int t:T){
					IloLinearNumExpr c5 = cplex.linearNumExpr(); // Water balance in hydro reservoir.
					c5.addTerm(1, v[i][t+1]);
					c5.addTerm(-1, v[i][t]);
					c5.addTerm(1, u[i][t]);
					c5.addTerm(1, s[i][t]);
					for(int h:U_i[i]){
						c5.addTerm(-1, u[h][t]);
						c5.addTerm(-1, s[h][t]);
					}
					cplex.addEq(c5, A[i][t]);	
				}
			}
			
			for(int i:I_ex){
				for(int t:T){
					cplex.addLe(u[i][t], U_max[i]); // C6 Hydro generation.
					cplex.addGe(u[i][t], U_min[i]); // C6 Hydro generation.
				}
			}
			
			for(int t:T){
				for(int p:P){
					for(int i:I_p[p]){
						IloLinearNumExpr c7 = cplex.linearNumExpr(); //Hydro generation.
						for(int tau=0; tau<=t; tau++){
							c7.addTerm(1, x_inv[p][tau]);
						}
						cplex.addLe(cplex.prod(U_min[i], c7), u[i][t]);
						cplex.addGe(cplex.prod(U_max[i], c7), u[i][t]);
					}
				}
			}
			
			for(int i:I_ex){
				for(int t:T1){
					cplex.addLe(v[i][t], V_max[i]); // C8 Hydro reservoir levels.
					cplex.addGe(v[i][t], V_min[i]); // C8 Hydro reservoir levels.
				}
			}
			
			for(int t:T1){
				for(int p:P){
					for(int i:I_p[p]){
						IloLinearNumExpr c9 = cplex.linearNumExpr(); //Hydro reservoir levels.
						if(t != TMax){
							for(int tau=0; tau<=t; tau++){
								c9.addTerm(1, x_inv[p][tau]);
							}
						}
						else if(t == TMax){
							for(int tau=0; tau<t; tau++){
								c9.addTerm(1, x_inv[p][tau]);
							}
						}
						cplex.addLe(cplex.prod(V_min[i], c9), v[i][t]);
						cplex.addGe(cplex.prod(V_max[i], c9), v[i][t]);
					}
				}
			}
			
			for(int j:J_ex){
				for(int t:T){
					cplex.addLe(g[j][t], G_max[j]); // C10 Thermal generation.
					cplex.addGe(g[j][t], G_min[j]); // C10 Thermal generation.
				}
			}
			
			for(int t:T){
				for(int p:P){
					for(int j:J_p[p]){
						IloLinearNumExpr c11 = cplex.linearNumExpr(); // Thermal generation.
						for(int tau=0; tau<=t; tau++){
							c11.addTerm(1, x_inv[p][tau]);
						}
						cplex.addLe(cplex.prod(G_min[j], c11), g[j][t]);
						cplex.addGe(cplex.prod(G_max[j], c11), g[j][t]);
					}
				}
			}	
			
			for(int t:T){
				if(T_E.contains(t)){
					IloLinearNumExpr c13 = cplex.linearNumExpr(); // Emission reservoir.
					c13.addTerm(1, e[t+1]);
					for(int j:J){
						c13.addTerm(B[j], g[j][t]);
					}
					c13.addTerm(-1, x_o[t]);
					cplex.addEq(c13, E_allow[t]);
				}
				else if(t != TMax-1){
					IloLinearNumExpr c12 = cplex.linearNumExpr(); // Emission reservoir.
					c12.addTerm(1, e[t+1]);
					c12.addTerm(-1, e[t]);
					for(int j:J){
						c12.addTerm(B[j], g[j][t]);
					}
					c12.addTerm(-1, x_o[t]);
					cplex.addEq(c12, 0);
				}
				else{
					
				}
			}
			
			for(int t:T){
				IloLinearNumExpr c14 = cplex.linearNumExpr(); // Demand violation.
				c14.addTerm(1, x_r[t]);
				for(int j:J){
					c14.addTerm(1, g[j][t]);
				}
				for(int i:I){
					c14.addTerm(rho[i], u[i][t]);
				}
				cplex.addGe(c14, D[t]);
			}
			
			for(int p:P){
				for(int t:T){
					if(Project.notPossible(T_p[p], t)){
						cplex.addEq(x_inv[p][t], 0); // Investment only is only in some time periods possible
						//System.out.println("Project "+p+" can't be realized in period "+t);
					}
				}
			}
			
			//Decision Expressions
			IloLinearNumExpr z1 = cplex.linearNumExpr(); // Investment cost.
			for(int p:P){
				for(int t:T_p[p]){
					z1.addTerm(c_inv[p][t], x_inv[p][t]);
				}
			}
			IloLinearNumExpr z2 = cplex.linearNumExpr(); // Thermal generation cost.
			for(int j:J){
				for(int t:T){
					z2.addTerm(c_op[j][t], g[j][t]);
				}
			}
			IloLinearNumExpr z3 = cplex.linearNumExpr(); // Penalty cost for electricity rationing.
			for(int t:T){
				z3.addTerm(M_r[t], x_r[t]);
			}
			IloLinearNumExpr z4 = cplex.linearNumExpr(); // Cost for emission overspending.
			for(int t:T){
				z4.addTerm(M_o[t], x_o[t]);
			}
			
			//Objective Function
			IloNumExpr ZF = cplex.sum(z1, z2, z3, z4);
			cplex.addMinimize(ZF);
			if(cplex.solve()){
				result = cplex.getObjValue();
				System.out.println("Optimal objective value of the EV is: "+result+".");
				System.out.println("Z1 (EV) is: "+cplex.getValue(z1));
				System.out.println("Z2 (EV) is: "+cplex.getValue(z2));
				System.out.println("Z3 (EV) is: "+cplex.getValue(z3));
				System.out.println("Z4 (EV) is: "+cplex.getValue(z4));
				for(int p:P){
					for(int t:T_p[p]){
						if(cplex.getValue(x_inv[p][t]) == 1){
							System.out.println("Project "+p+" is realized at stage "+t);
							openingEV.opening[p][t] = 1;
						}	
					}
				}
			}
			else{
				System.out.println("CPLEX could not solve the problem.");
			}
			cplex.end();
		}
		catch (IloException exc) {
			exc.printStackTrace();
		}
		return result;
	}
}
