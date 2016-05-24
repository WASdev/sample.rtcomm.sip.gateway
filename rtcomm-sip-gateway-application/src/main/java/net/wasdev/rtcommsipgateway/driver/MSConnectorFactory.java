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


public class MSConnectorFactory {
	public static final String DRIVER_NAME_PROPERTY_CONFIGURATION = "driverName";

	public static JSR309Driver getDriver(Properties properties){
		String driverName = properties.getProperty(DRIVER_NAME_PROPERTY_CONFIGURATION);
		JSR309Driver driver = new JSR309DriverImpl(driverName);
		driver.loadDriver();
		driver.createMsControlFactory(properties);

		return driver;
	}
}
