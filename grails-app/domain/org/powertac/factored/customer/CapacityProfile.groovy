/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.factored.customer

import org.powertac.common.enumerations.PowerType

/**
 * @author Prashant
 *
 */
class CapacityProfile
{
	enum CapacityType { CONSUMPTION, PRODUCTION, STORAGE }
	enum CapacitySubType { NONE, INTERRUPTIBLE, THERMAL_STORAGE, 
						   SOLAR,  WIND, RUN_OF_RIVER, PUMPED_STORAGE, CHP, FOSSIL, 
						   BATTERY_STORAGE, ELECTRIC_VEHICLE }
	enum SpecType { BEHAVIORS, FACTORED }
	
	String capacityId
	CapacityType capacityType
	CapacitySubType capacitySubType
	SpecType specType
	String description 
	double baseBenchmarkRate
		
	static constraints = {
		capacityId(nullable: true)
		capacityType(nullable: true)
		capacitySubType(nullable: true)
		specType(nullable: true)
		description(nullable: true)
	}
	
	/**
	 * @param xml - XML GPathResult from config
	 */
	void init(def xml, Random random) 
	{
		capacityId = xml.@id.text()
		capacityType = Enum.valueOf(CapacityType.class, xml.@type.text())
		capacitySubType = Enum.valueOf(CapacitySubType.class, xml.@subType.text())
		specType = Enum.valueOf(SpecType.class, xml.@specType.text())
		description = xml.@description.text()
		baseBenchmarkRate = xml.baseBenchmarkRate.@value.text().toDouble()
	}

	PowerType determinePowerType() {
		return reportPowerType(capacityType, capacitySubType)
	}
	
	static PowerType reportPowerType(CapacityType capacityType, CapacitySubType capacitySubType)
	{
		switch (capacityType) {
			case CapacityType.CONSUMPTION:
				switch (capacitySubType) {
					case CapacitySubType.NONE:
						return PowerType.CONSUMPTION
					case CapacitySubType.INTERRUPTIBLE:
						return PowerType.INTERRUPTIBLE_CONSUMPTION
					case CapacitySubType.THERMAL_STORAGE:
						return PowerType.INTERRUPTIBLE_CONSUMPTION
					default: throw new Error("Incompatible capacity subType ${capacitySubType}")
				}
				break
			case CapacityType.PRODUCTION:
				switch (capacitySubType) {
					case CapacitySubType.NONE:
						return PowerType.PRODUCTION
					case CapacitySubType.SOLAR:
						return PowerType.SOLAR_PRODUCTION
					case CapacitySubType.WIND:
						return PowerType.WIND_PRODUCTION
					case CapacitySubType.RUN_OF_RIVER:
						return PowerType.RUN_OF_RIVER_PRODUCTION
					case CapacitySubType.PUMPED_STORAGE:
						return PowerType.PUMPED_STORAGE_PRODUCTION
					case CapacitySubType.CHP:
						return PowerType.CHP_PRODUCTION
					case CapacitySubType.FOSSIL:
						return PowerType.FOSSIL_PRODUCTION
					default: throw new Error("Incompatible capacity subType ${capacitySubType}")
				}
				break
			case CapacityType.STORAGE:
				switch (capacitySubType) {
					case CapacitySubType.BATTERY_STORAGE:
						return PowerType.BATTERY_STORAGE
					case CapacitySubType.ELECTRIC_VEHICLE:
						return PowerType.ELECTRIC_VEHICLE
					default: throw new Error("Incompatible capacity subType ${capacitySubType}")
				}	
		}	
	}
	
	static CapacityType reportCapacityType(PowerType powerType)
	{
		switch (powerType) {
			case [PowerType.CONSUMPTION, PowerType.INTERRUPTIBLE_CONSUMPTION, PowerType.THERMAL_STORAGE_CONSUMPTION]:
				return CapacityType.CONSUMPTION
			case [PowerType.PRODUCTION, PowerType.SOLAR_PRODUCTION, PowerType.WIND_PRODUCTION, PowerType.RUN_OF_RIVER_PRODUCTION, PowerType.PUMPED_STORAGE_PRODUCTION, PowerType.CHP_PRODUCTION, PowerType.FOSSIL_PRODUCTION]:
				return CapacityType.PRODUCTION
			case [PowerType.BATTERY_STORAGE, PowerType.ELECTRIC_VEHICLE]:
				return CapacityType.STORAGE
			default: throw new Error("Unexpected powerType: ${powerType}")
		}
	}	

	
	protected double[][] pairsAsDoubleArray(String input) {
		def pairs = input.tokenize(',')
		def ret = new double[pairs.size()][2]
		for (int i=0; i < pairs.size(); ++i) {
		  def vals = pairs[i].tokenize(':')
		  ret[i][0] = vals.get(0).toDouble()
		  ret[i][1] = vals.get(1).toDouble()
		}
		return ret
	}

	
	static SpecType reportSpecType(xml) 
	{
		return Enum.valueOf(SpecType.class, xml.@specType.text())
	}
	
} // end class

