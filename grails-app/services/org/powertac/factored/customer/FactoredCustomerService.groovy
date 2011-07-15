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

import java.util.Random
import org.codehaus.groovy.grails.test.io.SystemOutAndErrSwapper;
import org.joda.time.Instant
import org.powertac.common.CustomerInfo
import org.powertac.common.PluginConfig
import org.powertac.common.enumerations.CustomerType
import org.powertac.common.enumerations.PowerType
import org.powertac.common.interfaces.TimeslotPhaseProcessor
import org.powertac.factored.customer.profile.*
import org.powertac.factored.customer.model.*

/**
* @author Prashant Reddy, CMU
*/

class FactoredCustomerService implements TimeslotPhaseProcessor {

  static transactional = true

  def timeService // autowire
  def competitionControlService // autowire
  def randomSeedService // autowire
  def tariffMarketService // autowire

  PluginConfig pluginConfig  
  Random random

  def customerProfiles = [:]
  def customerModelFactory = new CustomerModelFactory()
  def customerModels = [:] 
  
  void afterPropertiesSet()
  {
    competitionControlService.registerTimeslotPhase(this, 1)
    //competitionControlService.registerTimeslotPhase(this, 2)
  }

  void init(PluginConfig c) 
  {
	random = new Random(randomSeedService.nextSeed('FactoredCustomerService', 'service', 'init'))
	
    pluginConfig = c
	File configFile = new File(pluginConfig.configuration['configFile'].toString())
	log.info "init() - Loading customer profiles from config file: ${configFile}."
	
	def xmlRoot = new XmlSlurper().parse(configFile)
	xmlRoot.profile.each { profile ->
		def customerProfile = new CustomerProfile()
		customerProfile.init(profile, random)
		assert(customerProfile.save())
		customerProfiles[profile.@name.text()] = customerProfile
		log.info "init() - Loaded profile: name = ${customerProfile.name}, population = ${customerProfile.customerInfo.population}."
	}
	log.info "init() - Successfully loaded customer profiles from config file."
	
	log.info "init() - Registering customer model creators with factory."
	customerModelFactory.registerDefaultCreator(FactoredCustomerModel)
	
	log.info "init() - Creating customer models from configured profiles."
	customerProfiles.each { key, value -> 
		def customerModel = customerModelFactory.processProfile(value)
		if (customerModel != null) {
			customerModels.put(key, customerModel)
			customerModel.init(value)
			customerModel.subscribeDefault()
			assert(customerModel.save())
		} else throw new Error("Could not create customer model for profile: ${customerProfile.name}.")
	}
	log.info "init() - Successfully initialized customer models from profiles."	
  }

  void activate(Instant now, int phase) 
  {
    log.info "activate() - now: $now"
		
    if (phase == 1) customerModels*.step()
  }

} // end class

