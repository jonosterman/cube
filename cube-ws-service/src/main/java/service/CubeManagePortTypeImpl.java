package service;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.security.transport.TLSSessionInfo;
import org.example.contract.cubemanage.CubeManagePortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.x509.X509Key;

import ch.admin.cube.ws.CubeWsServiceProperties;
import ch.admin.cube.ws.ICubeManageDAO;
import ch.admin.cube.ws.impl.CubeManageDAO;

@WebService(targetNamespace = "http://www.example.org/contract/CubeManage", //
portName = "CubeManagePort", //
serviceName = "CubeManageService", //
endpointInterface = "org.example.contract.cubemanage.CubeManagePortType")
public class CubeManagePortTypeImpl implements CubeManagePortType {
	private static final Logger LOG = LoggerFactory.getLogger(CubeManagePortTypeImpl.class);
	private ICubeManageDAO dao = new CubeManageDAO();

	public CubeManagePortTypeImpl() {
	}

	@Override
	public int tripleIt(org.example.schema.cubemanage.SomeParamComplex parameters) {
		if (!performAuth()) {
			LOG.debug("User not logged. Abort.");
			return 0;
		}
		// implementation
		System.out.println("TriplIt: " + parameters.getMachine());
		return 35;
	};

	@Resource
	private WebServiceContext context;

	@Override
	@WebMethod
	public void login() {
		if (!performAuth()) {
			LOG.debug("User not logged. Abort.");
			return;
		}
	}
	
	private boolean performAuth() {
		TLSSessionInfo info = (TLSSessionInfo) context.getMessageContext().get("org.apache.cxf.security.transport.TLSSessionInfo");
		Certificate[] cs = info.getPeerCertificates();
		if (cs == null || cs.length == 0) {
			LOG.debug("No certificate in TLSSessionInfo. No auth is possible.");
		} else {
			for (Certificate c : cs) {
				X509Certificate x509 = (X509Certificate) c;
				if (x509.getKeyUsage() != null && x509.getKeyUsage()[0]) {
					// this is the right certificate. probably from a smartcard
					LOG.debug("Good certificate [" + x509.getSubjectDN().getName() + "]");
					dao.storePublicKey(x509.getSubjectDN().getName(), x509.getPublicKey());
					return true;
				} else {
					LOG.debug("Skip certificate [" + x509.getSubjectDN().getName() + "]");
				}
			}
			if (cs.length == 0) {
				LOG.debug("No certificate in request.");
			} else {
				// probably dev certificate (I did not found how to set keyusage
				// on datastore certs)
				X509Certificate x509 = (X509Certificate) cs[0];
				LOG.debug("Dev certificate [" + x509.getSubjectDN().getName() + "]");
				dao.storePublicKey(x509.getSubjectDN().getName(), x509.getPublicKey());
				return true;
			}
		}
		return false;
	}
	
	
}
