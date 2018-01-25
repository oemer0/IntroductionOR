package run;

import stochasticModel.*;
import demandUncertainty.*;
import inflowUncertainty.*;
import solution.*;

public class Calculate {
	public static void main(String[] args){
		System.out.println("Solving EV.");
		Solution openingEV = new Solution();
		double solutionEV = EV.solve(openingEV);
		System.out.println("\n");
		
		System.out.println("Solving the problem under demand uncertainty.");
		double solutionEEV_demand = EEV_demand.solve(openingEV);
		double solutionRP_demand = RP_demand.solve();
		double solutionWS_demand = WS_demand.solve();
		double VSS_demand = solutionEEV_demand-solutionRP_demand;
		double EVPI_demand = solutionRP_demand - solutionWS_demand;
		System.out.println("\n");
		
		System.out.println("Solving the problem under water inflow uncertainty.");
		double solutionEEV_inflow = EEV_inflow.solve(openingEV);
		double solutionRP_inflow = RP_inflow.solve();
		double solutionWS_inflow = WS_inflow.solve();
		double VSS_inflow = solutionEEV_inflow-solutionRP_inflow;
		double EVPI_inflow = solutionRP_inflow - solutionWS_inflow;
		System.out.println("\n");
		
		System.out.println("Solving the problem under demand and water inflow uncertainty.");
		double solutionEEV = EEV.solve(openingEV);
		double solutionRP = RP.solve();
		double solutionWS = WS.solve();
		double VSS = solutionEEV-solutionRP;
		double EVPI = solutionRP - solutionWS;
		System.out.println("\n");
		
		System.out.println("EV solution is: "+solutionEV+"\n");
		
		System.out.println("EEV under demand uncertainty is: "+ solutionEEV_demand);
		System.out.println("RP under demand uncertainty is: "+ solutionRP_demand);
		System.out.println("WS solution under demand uncertainty is: "+ solutionWS_demand);
		System.out.println("VSS of the problem (only demand uncertainty) is: "+ VSS_demand);
		System.out.println("EVPI of the problem (only demand uncertainty) is: "+ EVPI_demand +"\n");
		
		System.out.println("EEV under water inflow uncertainty is: "+solutionEEV_inflow);
		System.out.println("RP under water inflow uncertainty is: "+solutionRP_inflow);
		System.out.println("WS solution under water inflow uncertainty is: "+ solutionWS_inflow);
		System.out.println("VSS of the problem (only water inflow uncertainty) is: "+ VSS_inflow);
		System.out.println("EVPI of the problem (only water inflow uncertainty) is: "+ EVPI_inflow +"\n");
		
		System.out.println("EEV under demand and water inflow uncertainty is: "+solutionEEV);
		System.out.println("RP under demand and water inflow uncertainty is: "+solutionRP);
		System.out.println("WS solution under demand and water inflow uncertainty is: "+solutionWS);
		System.out.println("VSS of the problem is: "+ VSS);
		System.out.println("EVPI of the problem is: "+ EVPI);

	}
}
