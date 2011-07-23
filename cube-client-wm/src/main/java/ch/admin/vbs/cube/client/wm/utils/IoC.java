package ch.admin.vbs.cube.client.wm.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoC {
	private static final Logger LOG = LoggerFactory.getLogger(IoC.class);
	
	private HashSet<Object> beans = new HashSet<Object>();
	
	public void addBean(Object o) {
		beans.add(o);
	}
	
	public <T> T getBean(Class<T> clazz) {
		for(Object o : beans) {
			if (clazz.isInstance(o)) {
				return (T)o;
			}
		}
		return null;
	}
	
	/**
	 * Call setup on ALL beans, passing the right parameters (other beans) based on their type.
	 */
	public void setupDependenciesOnAllBeans() {
		BEANS: for(Object bean : beans) {
			for (Method method:  bean.getClass().getDeclaredMethods()) {
				if (method.getName().equals("setup")) {
					// fetch the right arguments and call setup
					Object[] args = new Object[method.getParameterTypes().length];
					int i=0;
					ARGS: for(Class<?> t : method.getParameterTypes()) {
						for(Object b : beans) {
							if (t.isInstance(b)) {
								args[i] = b;
								i++;
								continue ARGS;
							}
						}
						i++;
						LOG.error("no candidate of type [{}] for [{}.setup()]", t, bean.getClass());
						System.exit(0);
					}
					try {
						method.invoke(bean, args);
					} catch (IllegalArgumentException e) {
						LOG.error("Invokation error",e);
					} catch (IllegalAccessException e) {
						LOG.error("Invokation error",e);
					} catch (InvocationTargetException e) {
						LOG.error("Invokation error",e);
					}
					// next
					continue BEANS;
				}
			}
			LOG.warn("no 'setup()' method found for class [{}]",bean.getClass());
		}
	}
}
