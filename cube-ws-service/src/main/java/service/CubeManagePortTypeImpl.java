package service;

import java.io.File;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.security.transport.TLSSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.admin.cube.ws.ICubeManageDAO;
import ch.admin.cube.ws.impl.CubeManageDAO;
import ch.admin.vbs.cube.cubemanage.CubeManagePortType;

@WebService(targetNamespace = "http://www.example.org/contract/CubeManage", //
portName = "CubeManagePort", //
serviceName = "CubeManageService", //
endpointInterface = "org.example.contract.cubemanage.CubeManagePortType")
public class CubeManagePortTypeImpl implements CubeManagePortType {
	private static final Logger LOG = LoggerFactory.getLogger(CubeManagePortTypeImpl.class);
	private ICubeManageDAO dao = new CubeManageDAO();
	@Resource
	private WebServiceContext context;

	public CubeManagePortTypeImpl() {
	}

	@Override
	@WebMethod
	public void login(byte[] encPubkey) {
		if (performAuth() == null) {
			LOG.debug("User not logged. Abort.");
			return;
		}
	}

	@Override
	public void report(String message, long timestamp) {
		X509Certificate x509 = performAuth();
		if (x509 == null) {
			LOG.debug("User not logged. Abort.");
			return;
		}
		// log
		dao.report(x509, message, timestamp);
	}

	@Override
	public DataHandler listVMs() {
		X509Certificate x509 = performAuth();
		if (x509 == null) {
			LOG.debug("User not logged. Abort.");
			return null;
		}
		// list all VM 'desc' and 'key' file in a zip
		File descs = dao.listVMs(x509);
		return new DataHandler(new FileDataSource(descs));
	}

	private X509Certificate performAuth() {
		TLSSessionInfo info = (TLSSessionInfo) context.getMessageContext().get("org.apache.cxf.security.transport.TLSSessionInfo");
		Certificate[] cs = info.getPeerCertificates();
		if (cs == null || cs.length == 0) {
			LOG.debug("No certificate in TLSSessionInfo. No auth is possible.");
		} else {
			for (Certificate c : cs) {
				X509Certificate x509 = (X509Certificate) c;
				LOG.debug("List certificate [" + x509.getSubjectDN().getName() + "]");
			}
			for (Certificate c : cs) {
				X509Certificate x509 = (X509Certificate) c;
				if (x509.getKeyUsage() != null && x509.getKeyUsage()[0]) {
					// this is the right certificate. probably from a smartcard
					LOG.debug("Good certificate [" + x509.getSubjectDN().getName() + "]");
					dao.storePublicKey(x509);
					return x509;
				} else {
					LOG.debug("Skip certificate [" + x509.getSubjectDN().getName() + "]");
				}
			}
			if (cs.length == 0) {
				LOG.debug("No valid certificate in request.");
			}
		}
		return null;
	}
}
