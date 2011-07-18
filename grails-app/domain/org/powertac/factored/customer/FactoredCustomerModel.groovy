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

import org.powertac.common.AbstractCustomer
import org.powertac.common.Constants
import org.powertac.common.Tariff
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot
import org.powertac.common.TimeService
import org.powertac.common.enumerations.PowerType
import org.powertac.common.interfaces.NewTariffListener
import org.powertac.factored.customer.CapacityProfile.CapacityType

/**
 * @author Prashant
 *
 */
class FactoredCustomerModel extends AbstractCustomer implements NewTariffListener
{
	CustomerProfile customerProfile
	boolean canConsume = false
	boolean canProduce = false
	
	static hasMany = [capacityManagers: CapacityManager, ignoredTariffs: Tariff]
	
	/**
	 * Static method registered with </code>CustomerModelFactory</code>.
	 */
	static def createModel()
	{
		return new FactoredCustomerModel()
	}

	void init(profile) 
	{		
		customerInfo = profile.customerInfo
		super.init()  // customerInfo needs to be set before call init() here
		
		customerProfile = profile 
		customerProfile.capacityProfiles.each { capacityProfile ->
			if (capacityProfile.capacityType == CapacityType.CONSUMPTION) canConsume = true
			else if (capacityProfile.capacityType == CapacityType.PRODUCTION) canProduce = true
			
			def capacityManager = new CapacityManager().init(customerProfile, capacityProfile)
			this.addToCapacityManagers(capacityManager)
		}
		assert(this.save())
		log.info "FactoredCustomerModel created for customer profile ${customerProfile.name}."			
	}

	/** @Override **/
	void subscribe(Tariff tariff, int customerCount)
	{
	  def ts = tariffMarketService.subscribeToTariff(tariff, this, customerCount)
	  this.addToSubscriptions(ts)
	  log.info "subscribe(): ${toString()} subscribed ${customerCount} customers to tariff ${tariff}."
	}
  
	/** @Override **/
	void unsubscribe(TariffSubscription subscription, int customerCount) {
	  subscription.unsubscribe(customerCount)
	  log.info "unsubscribe(): ${toString()} unsubscribed ${customerCount} customers from tariff subscription ${subscription}."
	}
  
	///////////////// TARIFF EVALUATION //////////////////////
	/**
	 * @Override @code{NewTariffListener}
	 */
	void publishNewTariffs (List<Tariff> newTariffs)
	{
		log.info "publishNewTariffs() begin - ${toString()}, timeslot: ${timeService.currentTime}"
		if (subscriptions == null || subscriptions.size() == 0) {
			subscribeDefault()
		} else { 
			handleNewTariffs(newTariffs) 
		}
		assert(this.save())
		log.info "publishNewTariffs() end - ${toString()}, timeslot: ${timeService.currentTime}"
	}
	
	/**
	 * @Override
	 */
	void subscribeDefault() 
	{
		customerInfo.powerTypes.each { powerType ->
			if (tariffMarketService.getDefaultTariff(powerType) == null) {
				log.info "subscribeDefault() - ${toString()}: No default tariff for power type ${powerType.toString()}; trying less specific type."
		
				CapacityType capacityType = CapacityProfile.reportCapacityType(powerType)
				PowerType generalType = CapacityProfile.reportPowerType(capacityType, CapacityProfile.CapacitySubType.NONE)
		  
				if (tariffMarketService.getDefaultTariff(generalType) == null) {
					log.warn "subscribeDefault() - ${toString()}: No default tariff for general power type ${powerType.toString()} either!"
				} else {
			  		this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(generalType), this, population))
					log.info "subscribeDefault() - ${toString()} subscribed to default ${generalType} tariff successfully."
				} 
			} else {
		  	 	this.addToSubscriptions(tariffMarketService.subscribeToTariff(tariffMarketService.getDefaultTariff(powerType), this, population))
				log.info "subscribeDefault() - ${toString()} subscribed to default ${powerType} tariff successfully."
			}
		}
	}
  
	void handleNewTariffs(List<Tariff> newTariffs) 
	{
		log.info "handleNewTariffs(): begin: ${toString()} Received ${newTariffs.size()} new tariffs: ${newTariffs}"
	
		if (customerProfile.tariffSwitchingInertia != null) {
			double inertia = customerProfile.tariffSwitchingInertia.drawSample()
			if (customerProfile.random.nextDouble() < inertia) {
				log.info "handleNewTariffs() - Ignoring new tariffs for now due to tariff switching inertia."
				newTariffs.each { newTariff -> this.addToIgnoredTariffs(newTariff) }
				return
			}
		}
		// include previously ignored tariffs and currently subscribed tariffs in evaluation 
		List<Tariff> allTariffs = new ArrayList<Tariff>(newTariffs)
		Collections.copy(allTariffs, newTariffs)
		
		ignoredTariffs?.each { ignoredTariff -> allTariffs.add(ignoredTariff) }
		ignoredTariffs?.clear()
		
		allTariffs.addAll(subscriptions*.tariff)
		log.info "handleNewTariffs(): Total number of tariffs for evaluation: ${allTariffs.size()}"
	
		manageSubscriptions(allTariffs, CapacityType.CONSUMPTION)	
		manageSubscriptions(allTariffs, CapacityType.PRODUCTION)	

		assert(this.save())
		
		log.info "handleNewTariffs(): end: ${toString()}"
	}
	
	void manageSubscriptions(List<Tariff> allTariffs, CapacityType capacityType)
	{
		log.info "manageSubscriptions(): begin: ${toString()}:  capacityType: ${capacityType}"
		
		def evalTariffs = new ArrayList<Tariff>()
		allTariffs.each { tariff ->
			if (CapacityProfile.reportCapacityType(tariff.tariffSpec.powerType) == capacityType) {
				evalTariffs.add(tariff)
			}
		}

		if (evalTariffs.isEmpty()) {
			log.info "manageSubscriptions(): end early - No new tariffs to evaluate for capacity type ${capacityType}"
			return
		}

		List<Double> estimatedPayments = new ArrayList<Double>(evalTariffs.size())
		for (int i=0; i < evalTariffs.size(); ++i) {
			def tariff = evalTariffs[i]
			if (tariff.isExpired()) {
				estimatedPayments[i] = Double.POSITIVE_INFINITY  // sort it to the end
			} else {
				double totalVariablePayments = 0.0
				capacityManagers.each { capacityManager ->
					if (capacityManager.capacityProfile.capacityType == capacityType) {
						totalVariablePayments += capacityManager.computeDailyUsageCharge(tariff)
					}
				}
				estimatedPayments[i] = estimateFixedTariffPayments(tariff) + totalVariablePayments
			} 
		}		
		double[] allocations = determineAllocations(evalTariffs, estimatedPayments, capacityType)
		log.debug "manageSubscriptions(): ${capacityType} allocations: ${allocations}"

		int overAllocations = 0
		
		for (int i=0; i < evalTariffs.size(); ++i) {
			def evalTariff = evalTariffs[i]
			def subscription = findSubscriptionForTariff(evalTariff) // could be null
			if (subscription != null && subscription.customersCommitted > allocations[i]) {
				int numChange = subscription.customersCommitted - allocations[i]
				log.info("handleNewTariffs() - Unsubscribing ${numChange} ${capacityType} customers from tariff ${evalTariff}")
				unsubscribe(subscription, numChange)
			} else if (allocations[i] > 0 && (subscription == null || subscription.customersCommitted < allocations[i])) {
				int currentCommitted = (subscription != null) ? subscription.customersCommitted : 0
				int numChange = (subscription == null) ? allocations[i] : allocations[i] - subscription.customersCommitted
				if (numChange > 0) {
					if (evalTariff.isExpired()) {
						overAllocations += numChange
						if (currentCommitted > 0) {
							log.info("handleNewTariffs() - Maintaining ${currentCommitted} ${capacityType} customers in expired tariff ${evalTariff}")
						}
						log.info("handleNewTariffs() - Reallocating ${numChange} ${capacityType} customers from expired tariff ${evalTariff} to other tariffs")
					} else {
						log.info("handleNewTariffs() - Subscribing ${numChange} ${capacityType} customers to tariff ${evalTariff}")
						subscribe(evalTariff, numChange)
					}
				}
			} else if (subscription != null && subscription.customersCommitted == allocations[i]) {
				log.info("handleNewTariffs() - Maintaining ${subscription.customersCommitted} ${capacityType} customers in tariff ${evalTariff}")
			} else { // subscription == null && allocations[i] == 0
				log.info("handleNewTariffs() - Not allocating any ${capacityType} customers to tariff ${evalTariff}")
			}
		}
		if (overAllocations > 0) {
			int minIndex = 0
			double minEstimate = Double.POSITIVE_INFINITY
			for (int i=0; i < estimatedPayments.size(); ++i) {
				if (estimatedPayments[i] < minEstimate && ! evalTariffs[i].isExpired()) {
					minIndex = i
					minEstimate = estimatedPayments[i]
				}
			}
			log.info("handleNewTariffs() - Subscribing ${overAllocations} over-allocated customers to tariff ${evalTariffs[i]}")
			subscribe(evalTariffs[i], overAllocations)
		}
		log.info "manageSubscriptions(): end - ${toString()}: capacityType: ${capacityType}"
	}
	
	/**
	 * Note: This method can return null if there is no matching subscription.
	 */
	TariffSubscription findSubscriptionForTariff(Tariff tariff) 
	{
		subscriptions.each { subscription ->
			if (subscription.tariff == tariff) return subscription
		}
		return null
	}

	/**
	 * @Override
	 */
	double estimateFixedTariffPayments(Tariff tariff)
	{
	  double lifecyclePayment = (double) tariff.getEarlyWithdrawPayment() + (double) tariff.getSignupPayment()
	  
	  double minDuration
	  // When there is not a Minimum Duration of the contract, you cannot divide with the duration because you don't know it.
	  if (tariff.getMinDuration() == 0) minDuration = Constants.MEAN_TARIFF_DURATION * TimeService.DAY
	  else minDuration = tariff.getMinDuration()
  
	  return ((double) tariff.getPeriodicPayment() + (lifecyclePayment / minDuration))
	}
  
	double[] determineAllocations(List<Tariff> evalTariffs, List<Double> estimatedPayments, CapacityType capacityType) 
	{
		int numTariffs = evalTariffs.size()
		List allRules = customerProfile.tariffUtilityAllocations
		List allocationRule
		if (allRules == null) {
			allocationRule = new ArrayList(numTariffs)
			for (int i=0; i < numTariffs; ++i) {
				allocationRule[i] = (i==0) ? 1.0 : 0.0
			}
		} else if (numTariffs <= allRules.size()) {
			allocationRule = allRules[numTariffs - 1]
		} else {			
			allocationRule = new ArrayList(numTariffs)
			List largestRule = allRules[allRules.size() - 1]
			for (int i=0; i < numTariffs; ++i) {
				allocationRule[i] = (i < largestRule.size()) ? largestRule[i] : 0.0
			}
		}
		// payments are negative for production, so sorting is still valid
		ArrayList<Double> sortedPayments = new ArrayList<Double>(estimatedPayments)
		Collections.copy(sortedPayments, estimatedPayments)
		Collections.sort(sortedPayments)
		log.debug "manageSubscriptions(): ${capacityType} sorted payments: ${sortedPayments}"
		
		def allocations = new double[numTariffs]
		for (int i=0; i < numTariffs; ++i) {
			if (allocationRule[i] > 0) {
				double nextBest = sortedPayments[i]
				for (int j=0; j < numTariffs; ++j) {
					if (estimatedPayments[j] == nextBest) {
						allocations[j] = Math.round(customerInfo.population * allocationRule[i])
					}
				}
			}	
		}
		return allocations
	}
  
	///////////////// TIMESLOT ACTIVITY //////////////////////
	/**
	 * @Override
	 */
	void step()
	{
		log.info "step() begin - ${toString()}, timeslot: ${timeService.currentTime}"
		
		checkRevokedSubscriptions()
		consumePower()
		producePower()
		assert(this.save())

		log.info "step() end - ${toString()}, timeslot: ${timeService.currentTime}"
	}
	
	/**
	 * @Override
	 */
	void consumePower() 
	{				
		Timeslot timeslot =  Timeslot.currentTimeslot()
			
		double totalConsumption = 0.0
		subscriptions.each { subscription ->
			if (subscription.customersCommitted > 0 && 
				CapacityProfile.reportCapacityType(subscription.tariff.tariffSpec.powerType) == CapacityType.CONSUMPTION) {
				capacityManagers.each { capacityManager -> 		
					if (capacityManager.capacityProfile.capacityType == CapacityType.CONSUMPTION) {
						double currCapacity = capacityManager.computeCapacity(timeslot, subscription)
						subscription.usePower(currCapacity) 
						totalConsumption += currCapacity 
					}
				}
			}			
		}
		log.info "Total consumption for timeslot ${timeslot} = ${totalConsumption}"
	}
	
	/**
	 * @Override
	 */
	void producePower() 
	{
		Timeslot timeslot =  Timeslot.currentTimeslot()
			
		double totalProduction = 0.0
		subscriptions.each { subscription ->		
			if (subscription.customersCommitted > 0 && 
				CapacityProfile.reportCapacityType(subscription.tariff.tariffSpec.powerType) == CapacityType.PRODUCTION) {	
					capacityManagers.each { capacityManager -> 		
					if (capacityManager.capacityProfile.capacityType == CapacityType.PRODUCTION) {
						double currCapacity = capacityManager.computeCapacity(timeslot, subscription)
						subscription.usePower(- currCapacity) 
						totalProduction += currCapacity 
					}			
				}
			}
		}
		log.info "Total production for timeslot ${timeslot} = ${totalProduction}"
	}
	
	/**
	 * @Override
	 */
	double[][] getBootstrapData() 
	{
		final int numDays = 14
		final int numHours = 24
		def bsData = new double[numDays][numHours]
		for (int i=0; i < numDays; i++) {
			for (int j=0; j < numHours; j++) {
				double netCapacity = 0.0 
				capacityManagers.each { capacityManager ->
					double draw = capacityManager.drawBaseCapacitySample()
					if (capacityManager.capacityProfile.capacityType == CapacityType.CONSUMPTION) {
						netCapacity += draw
					} else {
						netCapacity -= draw
					}
				}
				bsData[i][j] = netCapacity
			}
		}
		return bsData
	}
		
	/**
	 * @Override
	 */
	String toString() 
	{
		return "FactoredCustomerModel: ${customerProfile.name}"
	}
	
} // end class


