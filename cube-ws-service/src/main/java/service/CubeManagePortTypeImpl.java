package service;

import java.security.Principal;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

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

	@Resource
	private WebServiceContext wsContext;

	@Override
	@WebMethod
	public void login() {
		System.out.println("#####################");
		Principal princip = wsContext.getUserPrincipal();
		System.out.println("#####################");
		if (princip == null) {
			System.out.println("No user logged in.");			
		} else {
			System.out.println("User logged ["+princip.getName()+"].");			
		}
	}
}
