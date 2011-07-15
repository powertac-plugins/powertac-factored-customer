/*
* Copyright 2011 the original author or authors.
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

import org.joda.time.Instant
import org.powertac.common.Tariff
import org.powertac.common.TariffSubscription
import org.powertac.common.Timeslot


/**
 * @author Prashant
 *
 */
class CapacityManager
{
	def timeService // autowire
	
	static final int BASE_CAPACITY_TIMESLOTS = 24
	static final int NUM_HOURS_IN_DAY = 24
	
	CustomerProfile customerProfile 
	CapacityProfile capacityProfile
	
	//static hasMany = [shiftedCapacities : double]
	
	
	CapacityManager init(CustomerProfile customer, CapacityProfile capacity) 
	{
		customerProfile = customer
		capacityProfile = capacity
		return this
	}
	
	double computeDailyUsageCharge(Tariff tariff)
	{
		int hoursToMidnight = NUM_HOURS_IN_DAY - (timeService.getHourOfDay() + 1)
		Timeslot hourlyTimeslot = Timeslot.currentTimeslot()
		for (int i=0; i < hoursToMidnight; ++i) hourlyTimeslot.next()
		
		double totalUsage = 0.0
		double totalCharge = 0.0 
		for (int i=0; i < NUM_HOURS_IN_DAY; ++i) {
			double baseCapacity = 0
			if (capacityProfile.specType == CapacityProfile.SpecType.BEHAVIORS && 
				capacityProfile.baseTotalCapacity != null) {
				baseCapacity = capacityProfile.baseTotalCapacity.drawSample()
			} else {
				double draw = capacityProfile.baseCapacityPerCustomer.drawSample()
				baseCapacity += draw * customerProfile.customerInfo.population
			}
			def hourlyUsage = customerProfile.customerInfo.population * (baseCapacity / BASE_CAPACITY_TIMESLOTS)
			totalCharge += tariff.getUsageCharge(hourlyTimeslot.startInstant, hourlyUsage, totalUsage)
			totalUsage += hourlyUsage
			hourlyTimeslot.next()
		}
		return totalCharge
	}
	
	double drawBaseCapacitySample() 
	{
		double baseCapacity = 0.0
		if (capacityProfile.baseTotalCapacity != null) {
			baseCapacity = capacityProfile.baseTotalCapacity.drawSample() / BASE_CAPACITY_TIMESLOTS
		} else {
			for (int i=0; i < customerProfile.customerInfo.population; ++i) {
				double draw = capacityProfile.baseCapacityPerCustomer.drawSample()
				baseCapacity += draw / BASE_CAPACITY_TIMESLOTS
			}
		}
		return baseCapacity
	}
	
	double computeCapacity(Timeslot timeslot, TariffSubscription subscription)
	{
		if (capacityProfile.specType == CapacityProfile.SpecType.BEHAVIORS) {
			computeCapacityFromBehaviors(timeslot, subscription)
		} else {  // CapacityProfile.SpecType.FACTORED
			computeCapacityFromFactors(timeslot, subscription)
		}
	}
	
	double computeCapacityFromBehaviors(Timeslot timeslot, TariffSubscription subscription)
	{
		double baseCapacity = drawBaseCapacitySample()
		log.info "computeCapacityFromBehaviors() - baseCapacity = ${baseCapacity}"

		double adjustedCapacity = baseCapacity
		if (capacityProfile.elasticityOfCapacity != null) {
			adjustedCapacity = computeElasticCapacity(timeslot, subscription, baseCapacity)
		}
		return adjustedCapacity
	}
	
	double computeCapacityFromFactors(Timeslot timeslot, TariffSubscription subscription)
	{
		double baseCapacity = drawBaseCapacitySample()
		log.info "computeCapacityFromFactors() - baseCapacity = ${baseCapacity}"
		
		double adjustedCapacity = baseCapacity
		if (capacityProfile.elasticityOfCapacity != null) {
			adjustedCapacity = computeElasticCapacity(timeslot, subscription, baseCapacity)
		}
		
		// TODO: Adjust for the other specified factors
		
		return adjustedCapacity
	}

	double computeElasticCapacity(Timeslot timeslot, TariffSubscription subscription, double baseCapacity)
	{
		double chargeForBase = subscription.tariff.getUsageCharge(timeslot.startInstant, baseCapacity, subscription.totalUsage)
		double rateForBase = chargeForBase / baseCapacity
		double rateRatio = rateForBase / capacityProfile.baseBenchmarkRate
		double[][] elasticity = capacityProfile.elasticityOfCapacity
		double elasticityFactor = lookupElasticityFactor(rateRatio, elasticity)
		log.debug "computeElasticCapacity() - elasticityFactor = ${elasticityFactor}"
		double elasticCapacity = baseCapacity * elasticityFactor
		return elasticCapacity
	}
	
	double lookupElasticityFactor(double rateRatio, double[][] elasticity)
	{
		if (rateRatio == 1 || elasticity.size() == 0) return 1.0
		
		final int RATE_RATIO_INDEX = 0
		final int CAPACITY_FACTOR_INDEX = 1		
		double rateLowerBound = Double.NEGATIVE_INFINITY
		double rateUpperBound = Double.POSITIVE_INFINITY
		double lowerBoundCapacityFactor = 1.0
		double upperBoundCapacityFactor = 1.0
		for (int i=0; i < elasticity.size(); ++i) {
			double r = elasticity[i][RATE_RATIO_INDEX]
			if (r <= rateRatio && r > rateLowerBound) {
				rateLowerBound = r
				lowerBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX]
			}
			if (r >= rateRatio && r < rateUpperBound) {
				rateUpperBound = r
				upperBoundCapacityFactor = elasticity[i][CAPACITY_FACTOR_INDEX]
			}
		}	
		return (rateRatio < 1) ? upperBoundCapacityFactor : lowerBoundCapacityFactor
	}
		
}
