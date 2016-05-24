/**
 * (C) Copyright IBM Corporation 2016.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.rtcommsipgateway.driver;

import java.util.Properties;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;

import net.wasdev.rtcommsipgateway.utils.JSR309Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JSR309DriverImpl implements JSR309Driver {

	private static Logger log = Logger.getLogger(JSR309DriverImpl.class.getName());

	protected transient Driver driver = null;

	protected transient MsControlFactory mscFactory;

	private String driverName;

	public JSR309DriverImpl(String driverName) {
		this.driverName = driverName;
	}

	@Override
	public void loadDriver(){
		driver = DriverManager.getDriver(getDriverName());
		if (log.isLoggable(Level.FINE)) {
			log.fine("Driver has loaded Succesfully " + getDriverName()
			+ " driver instance " + driver);
		}
		if( driver == null){
			log.warning("Driver is null! aborting");
		}
	}

	@Override
	public MsControlFactory getMsControlFactory(){
		return mscFactory;
	}

	public void createMsControlFactory(Properties factoryOverrideProperties){
		if (log.isLoggable(Level.FINE)) {
			log.fine("JSR309DriverImpl creating factory");
		}
		try {
			Properties driverProperty = JSR309Utils.createFactoryProperties(driver, factoryOverrideProperties);
			this.mscFactory = driver.getFactory(driverProperty);

		} catch (MsControlException e) {
			log.warning("JSR309DriverImpl failed to create MsControlFactory");
			e.printStackTrace();
		}
	}

	protected String getDriverName() {
		return driverName;
	}

}
