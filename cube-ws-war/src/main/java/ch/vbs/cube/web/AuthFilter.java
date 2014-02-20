package ch.vbs.cube.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to implements x509 check here or in CubeManagePortTypeImpl.
 */
public class AuthFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(AuthFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//		X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
//		if (certs == null) {
//			LOG.debug("No cert in requets");
//		} else {
//			int validCert = 0;
//			for (X509Certificate c : certs) {
//				// there are the user but also the CA certificates in this list.
//				// only keep certificate with 'digitalSignature' usage (user
//				// certificate).
//				// @see http://www.alvestrand.no/objectid/2.5.29.15.html
//				if (c.getKeyUsage() == null) {
//					LOG.debug("Cert in requets, but no key usage for ["+c.getSubjectDN().getName()+"]");
//				} else if (c.getKeyUsage()[0]) {
//					String name = c.getSubjectDN().getName();
//					LOG.debug("Cert in requets [" + c.getSubjectDN().getName() + "]");
//					request.setAttribute("x509name", name);
//					request.setAttribute("x509cert", c);
//					validCert++;
//				}
//			}
//			// Dev: I was not able to set keyusage in cert. just hack it to make dev possible without smartcard
//			if (validCert == 0 && certs.length > 0) {
//				LOG.debug("Use arbirtary certficate in dev mode as long as keyusage is not correctly set");
//				X509Certificate c = certs[0];
//				String name = c.getSubjectDN().getName();
//				LOG.debug("Dev cert in requets [" + c.getSubjectDN().getName() + "]");
//				request.setAttribute("x509name", name);
//				request.setAttribute("x509cert", c);
//				
//			}
//			
//			
//			
//		}
//		//
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}
