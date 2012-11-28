package client;

import org.example.contract.cubemanage.CubeManagePortType;
import org.example.contract.cubemanage.CubeManageService;
import org.example.schema.cubemanage.SomeParamComplex;

public class WSClient {
    public static void main (String[] args) {
    	CubeManageService service = new CubeManageService();
    	CubeManagePortType port = service.getCubeManagePort();

        doubleIt(port, 10);
        doubleIt(port, 0);
        doubleIt(port, -10);
        System.out.println("done.");
    } 
    
    public static void doubleIt(CubeManagePortType port, 
            int numToDouble) {
    	SomeParamComplex p = new SomeParamComplex();
    	p.setMachine("blah");
    	p.setSize(32);
        int resp = port.tripleIt(p);
        System.out.println("The number " + numToDouble + " doubled is " 
            + resp);
    }
}
