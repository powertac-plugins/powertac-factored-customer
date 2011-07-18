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

import org.powertac.common.enumerations.*
import org.powertac.common.CustomerInfo

/**
 * @author Prashant
 *
 */
class CustomerProfile
{
	def profileCacheService // autowire
	
	enum EntityType { HOUSEHOLD, COMMERCIAL, INDUSTRIAL, AGRICULTURAL }
	enum CustomerRole { CONSUMER, PRODUCER, COMBINED }
	enum ModelType { INDIVIDUAL, POPULATION }
	
	enum TariffUtilityCriteria { BEST_VALUE, PREFER_GREEN }
	
	Random random
	String name
	CustomerCategory category
	CustomerInfo customerInfo
	
	// Customer behaviors
	TariffUtilityCriteria tariffUtilityCriteria = null
	String tariffUtilityAllocationsConfig = null
	ProbabilityDistribution tariffSwitchingInertia = null
	
	// Customer factors
	double[] temperatureByMonth = null
	ProbabilityDistribution customerWealth = null
	ProbabilityDistribution newTariffsExposure = null
	ProbabilityDistribution tariffSwitchDelay = null
	ProbabilityDistribution waitAfterTariffSwitch = null
	boolean preferFewerTariffs = null
	
	static hasMany = [capacityProfiles : CapacityProfile]
	
	static constraints = {
		tariffUtilityCriteria(nullable: true)
		tariffUtilityAllocationsConfig(nullable: true)
		tariffSwitchingInertia(nullable: true)
		temperatureByMonth(nullable: true)
		customerWealth(nullable: true)
		newTariffsExposure(nullable: true)
		tariffSwitchDelay(nullable: true)
		waitAfterTariffSwitch(nullable: true)
	}	
		
	/**
	 * @param xml - XML GPathResult from config
	 */
	void init(def xml, Random r)
	{
		random = new Random(r.nextLong())
		
		name = xml.@name.text()
		
		category = new CustomerCategory()
		category.entityType = Enum.valueOf(EntityType.class, xml.category.@entityType.text())
		category.customerRole = Enum.valueOf(CustomerRole.class, xml.category.@customerRole.text())
		category.modelType = Enum.valueOf(ModelType.class, xml.category.@modelType.text())
		assert(category.save())
		
		customerInfo = new CustomerInfo()
		customerInfo.name = name
		customerInfo.population = xml.population.@value.text().toInteger()
		if (category.entityType == EntityType.HOUSEHOLD) {
			customerInfo.customerType = CustomerType.CustomerHousehold
		} else if (category.entityType == EntityType.COMMERCIAL) {
			customerInfo.customerType = CustomerType.CustomerOffice
		} else if (category.entityType == EntityType.INDUSTRIAL) {
			customerInfo.customerType = CustomerType.CustomerFactory
		} else {  // EntityType.AGRICULTURAL
			customerInfo.customerType = CustomerType.CustomerOther
		}
		customerInfo.powerTypes = []
		customerInfo.multiContracting = xml.multiContracting.@value.text().toBoolean()
		customerInfo.canNegotiate = xml.canNegotiate.@value.text().toBoolean()		
		
		boolean customerBehaviorsInitialized = false
		boolean customerFactorsInitialized = false
		xml.capacity.each { capacitySpec ->
			def specType = CapacityProfile.reportSpecType(capacitySpec)
			def capacityProfile
			if (specType == CapacityProfile.SpecType.BEHAVIORS) {
				capacityProfile = new BehaviorsProfile()
				if (! customerBehaviorsInitialized) {
					tariffUtilityCriteria = Enum.valueOf(TariffUtilityCriteria.class, xml.tariffUtility.@criteria.text())
					tariffUtilityAllocationsConfig = xml.tariffUtility.@allocations.text()
					profileCacheService.tariffUtilityAllocations[name] = allocationRulesAsNestedList(tariffUtilityAllocationsConfig)
					tariffSwitchingInertia = new ProbabilityDistribution().init(xml.tariffSwitchingInertia, random.nextLong())
					customerBehaviorsInitialized = true
				}
			} else { // CapacityProfile.SpecType.FACTORED
				capacityProfile = new FactoredProfile()
				if (! customerFactorsInitialized) {
					temperatureByMonth = xml.temperatureByMonth.@array.text()
					customerWealth = new ProbabilityDistribution().init(xml.customerWealth, random.nextLong())
					newTariffsExposure = new ProbabilityDistribution().init(xml.newTariffsExposure, random.nextLong())
					tariffSwitchDelay = new ProbabilityDistribution().init(xml.tariffSwitchDelay, random.nextLong())
					waitAfterTariffSwitch = new ProbabilityDistribution().init(xml.waitAfterTariffSwitch, random.nextLong())
					preferFewerTariffs = xml.preferFewerTariffs.@value.text().toBoolean()
					customerFactorsInitialized = true
				}
			}
			capacityProfile.init(capacitySpec, random)
			assert(capacityProfile.save())
			this.addToCapacityProfiles(capacityProfile)
			PowerType powerType = capacityProfile.determinePowerType()
			if (! customerInfo.powerTypes.contains(powerType)) {
				customerInfo.powerTypes.add(powerType)
			}
		}
		assert(customerInfo.save())
	}
	
	List allocationRulesAsNestedList(String input) 
	{
		// example input: "0.7:0.3, 0.5:0.3:0.2, 0.4:0.3:0.2:0.1, 0.4:0.3:0.2:0.05:0.05"
		// which yields the following rules:
		// 		size = 2, rule = [0.7, 0.3]
		// 		size = 3, rule = [0.5, 0.3, 0.2]
		// 		size = 4, rule = [0.4, 0.3, 0.2, 0.1]
		// 		size = 5, rule = [0.4, 0.3, 0.2, 0.05, 0.05]
		
		List rules = input.tokenize(',')
		List ret = new ArrayList(rules.size() + 1)		
		List degenerateRule = new ArrayList(1)
		degenerateRule.add(1.0)
		ret.add(degenerateRule)
		for (int i=0; i < rules.size(); ++i) {			
			List vals = rules[i].tokenize(':')
			List rule = new ArrayList(vals.size())
			for (int j=0; j < vals.size(); ++j) {
				rule.add(vals[j].toDouble())
			}
			ret.add(rule)
		}
		return ret
	}	

} // end class

