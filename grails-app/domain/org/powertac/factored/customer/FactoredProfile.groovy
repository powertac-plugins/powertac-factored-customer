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
class FactoredProfile extends BehaviorsProfile
{
	enum InfluenceKind { DIRECT, DEVIATION, NONE }
	enum WeatherGranularity { MONTHLY, HOURLY, NONE }
	
	long minCapacityPerCustomer 
	long maxCapacityPerCustomer 
	
	ProbabilityDistribution curtailableCapacity 
	double curtailingThreshold 
	ProbabilityDistribution shiftableCapacity 
	double shiftingThreshold 
	
	double[] capacityRatioByMonth 
	double[] capacityRatioByDay 
	double[] capacityRatioByHour 
	
	InfluenceKind temperatureInfluence
	double temperatureCorrelation	
	WeatherGranularity temperatureGranularity
	InfluenceKind windSpeedInfluence
	double windSpeedCorrelation	
	InfluenceKind cloudCoverInfluence
	double cloudCoverCorrelation

	static constraints = {
		curtailableCapacity(nullable: true)
		shiftableCapacity(nullable: true)
		capacityRatioByMonth(nullable: true)
		capacityRatioByDay(nullable: true)
		capacityRatioByHour(nullable: true)
		temperatureInfluence(nullable: true)
		temperatureGranularity(nullable: true)
		windSpeedInfluence(nullable: true)
		cloudCoverInfluence(nullable: true)
	}
	
	/**
	 * @Override
	 * @param xml - XML GPathResult from config
	 */
	void init(def xml, Random random) 
	{
		super.init(xml, random)
		
		minCapacityPerCustomer = xml.minCapacityPerCustomer.@value.text().toLong()
		maxCapacityPerCustomer = xml.maxCapacityPerCustomer.@value.text().toLong()
		
		curtailableCapacity = new ProbabilityDistribution().init(xml.curtailableCapacity, random.nextLong())
		curtailingThreshold = xml.curtailingThreshold.@changeRatio.text().toDouble()
		shiftableCapacity = new ProbabilityDistribution().init(xml.shiftableCapacity, random.nextLong())
		shiftingThreshold = xml.shiftingThreshold.@changeRatio.text().toDouble()
		
		capacityRatioByMonth = xml.capacityRatioByMonth.@array.text()
		capacityRatioByDay = xml.capacityRatioByDay.@array.text()
		capacityRatioByHour = xml.capacityRatioByHour.@array.text()
		
		temperatureInfluence = Enum.valueOf(InfluenceKind.class, xml.temperatureInfluence.@kind.text())
		temperatureCorrelation = xml.temperatureInfluence.@correlation.text().toDouble()
		temperatureGranularity = Enum.valueOf(WeatherGranularity.class, xml.temperatureInfluence.@granularity.text())
		windSpeedInfluence = Enum.valueOf(InfluenceKind.class, xml.windSpeedInfluence.@kind.text())
		windSpeedCorrelation = xml.windSpeedInfluence.@correlation.text().toDouble()
		cloudCoverInfluence = Enum.valueOf(InfluenceKind.class, xml.cloudCoverInfluence.@kind.text())
		cloudCoverCorrelation = xml.cloudCoverInfluence.@correlation.text().toDouble()
	}

} // end class

