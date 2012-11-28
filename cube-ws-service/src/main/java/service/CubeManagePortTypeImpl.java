package service;

import javax.jws.WebService;

import org.example.contract.cubemanage.CubeManagePortType;

@WebService(targetNamespace = "http://www.example.org/contract/CubeManage", //
portName = "CubeManagePort", //
serviceName = "CubeManageService", //
endpointInterface = "org.example.contract.cubemanage.CubeManagePortType")
public class CubeManagePortTypeImpl implements CubeManagePortType {
	@Override
	public int tripleIt(org.example.schema.cubemanage.SomeParamComplex parameters) {
		System.out.println(parameters.getMachine());
		return 35;
		
	};
}
